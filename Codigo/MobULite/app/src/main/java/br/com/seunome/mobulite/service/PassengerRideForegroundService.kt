package br.com.seunome.mobulite.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import br.com.seunome.mobulite.R
import br.com.seunome.mobulite.data.local.SessionStore
import br.com.seunome.mobulite.data.remote.RealtimeClient
import br.com.seunome.mobulite.data.remote.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Foreground service that holds a WebSocket connection for the passenger's active ride.
 * Keeps the connection alive even when the app is in background, delivering real-time
 * status updates via [rideEventFlow] that composables collect.
 */
class PassengerRideForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var realtimeClient: RealtimeClient? = null
    private var isDone = false
    private var activeRideId: String? = null

    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(applicationContext)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Aguardando motorista..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val rideId = intent?.getStringExtra(EXTRA_RIDE_ID) ?: run { stopSelf(); return START_NOT_STICKY }
        activeRideId = rideId

        scope.launch {
            val token = SessionStore(applicationContext).tokenFlow.first()
            if (token.isNullOrBlank()) { stopSelf(); return@launch }
            RetrofitClient.setToken(token)
            connectWebSocket(token, rideId)
        }

        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        isDone = true
        realtimeClient?.disconnect()
        realtimeClient = null
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun connectWebSocket(token: String, rideId: String) {
        realtimeClient?.disconnect()
        realtimeClient = RealtimeClient(
            tokenProvider = { token },
            onEvent = { type, payload ->
                if (type == "ride_status_update") {
                    val status = payload?.optString("status") ?: return@RealtimeClient
                    val event = RideStatusEvent(
                        rideId = rideId,
                        status = status,
                        driverName = payload.optString("driverName").takeIf { it.isNotBlank() },
                        driverLat = if (payload.has("driverLat") && !payload.isNull("driverLat")) payload.getDouble("driverLat") else null,
                        driverLng = if (payload.has("driverLng") && !payload.isNull("driverLng")) payload.getDouble("driverLng") else null,
                        reason = payload.optString("reason").takeIf { it.isNotBlank() },
                    )
                    handleStatusUpdate(event)
                }
            },
            onStateChanged = { connected ->
                if (connected) {
                    realtimeClient?.subscribeRide(rideId)
                } else if (!isDone) {
                    // Reconnect after 5 s if the connection drops
                    scope.launch {
                        delay(5_000)
                        if (!isDone) connectWebSocket(token, rideId)
                    }
                }
            }
        ).also { it.connect() }
    }

    private fun handleStatusUpdate(event: RideStatusEvent) {
        val text = when (event.status) {
            "ACCEPTED"       -> "Motorista a caminho!"
            "DRIVER_ARRIVING"-> "Motorista chegando..."
            "DRIVER_ARRIVED" -> "Motorista chegou! Saia agora."
            "IN_PROGRESS"    -> "Corrida em andamento"
            "FINISHED"       -> "Corrida finalizada"
            "CANCELED"       -> event.reason ?: "Corrida cancelada"
            "PENDING_DRIVER" -> "Motorista cancelou — buscando outro..."
            else             -> "Aguardando motorista..."
        }
        updateNotification(text)
        _rideEventFlow.value = event

        if (event.status == "DRIVER_ARRIVED") {
            showDriverArrivedAlert()
        }

        if (event.status == "FINISHED" || event.status == "CANCELED") {
            isDone = true
            stopSelf()
        }
        // PENDING_DRIVER means re-queued — keep the service alive
    }

    private fun updateNotification(text: String) {
        val notification = buildNotification(text)
        getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, notification)
    }

    private fun showDriverArrivedAlert() {
        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Motorista chegou!")
            .setContentText("Seu motorista está esperando. Saia agora.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java)?.notify(ALERT_NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Corrida em andamento", NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(ALERT_CHANNEL_ID, "Alertas de corrida", NotificationManager.IMPORTANCE_HIGH).also {
                it.description = "Notificações quando o motorista chega"
                it.enableVibration(true)
            }
        )
    }

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("MobU — sua corrida")
            .setContentText(text)
            .setOngoing(true)
            .build()

    // ── Companion: shared state consumed by composables ───────────────────────

    data class RideStatusEvent(
        val rideId: String,
        val status: String,
        val driverName: String? = null,
        val driverLat: Double? = null,
        val driverLng: Double? = null,
        val reason: String? = null,
    )

    companion object {
        private const val CHANNEL_ID = "passenger_ride"
        private const val ALERT_CHANNEL_ID = "passenger_ride_alerts"
        private const val NOTIFICATION_ID = 8001
        private const val ALERT_NOTIFICATION_ID = 8002
        const val EXTRA_RIDE_ID = "rideId"

        private val _rideEventFlow = MutableStateFlow<RideStatusEvent?>(null)

        /** Composables collect this to receive real-time ride status changes. */
        val rideEventFlow: StateFlow<RideStatusEvent?> = _rideEventFlow.asStateFlow()

        /** Call after consuming the event to prevent re-processing on recomposition. */
        fun clearEvent() { _rideEventFlow.value = null }

        fun start(context: Context, rideId: String) {
            val intent = Intent(context, PassengerRideForegroundService::class.java)
                .putExtra(EXTRA_RIDE_ID, rideId)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PassengerRideForegroundService::class.java))
        }
    }
}
