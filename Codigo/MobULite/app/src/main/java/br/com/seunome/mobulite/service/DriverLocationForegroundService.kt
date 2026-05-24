package br.com.seunome.mobulite.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import br.com.seunome.mobulite.R
import br.com.seunome.mobulite.data.local.SessionStore
import br.com.seunome.mobulite.data.remote.DriverLocationRequest
import br.com.seunome.mobulite.data.remote.RealtimeClient
import br.com.seunome.mobulite.data.remote.RetrofitClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DriverLocationForegroundService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val fusedClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private var realtimeClient: RealtimeClient? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            scope.launch {
                runCatching {
                    RetrofitClient.driverApi.updateLocation(
                        DriverLocationRequest(
                            lat = location.latitude,
                            lng = location.longitude
                        )
                    )
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(applicationContext)
        createNotificationChannel()
        try {
            startForeground(NOTIFICATION_ID, buildNotification())
        } catch (e: Exception) {
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch {
            val token = SessionStore(applicationContext).tokenFlow.first()
            if (token.isNullOrBlank()) {
                stopSelf()
                return@launch
            }
            RetrofitClient.setToken(token)
            startLocationUpdates()
            startRealtime(token)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        fusedClient.removeLocationUpdates(locationCallback)
        realtimeClient?.disconnect()
        realtimeClient = null
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation) {
            stopSelf()
            return
        }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L
        )
            .setMinUpdateIntervalMillis(2500L)
            .build()

        fusedClient.requestLocationUpdates(request, locationCallback, mainLooper)
    }

    private fun createNotificationChannel() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Localização do motorista", NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(RIDE_CHANNEL_ID, "Novas corridas", NotificationManager.IMPORTANCE_HIGH).also {
                it.description = "Alertas de novas ofertas de corrida"
                it.enableVibration(true)
            }
        )
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("MobU motorista online")
            .setContentText("Compartilhando localização para corridas em tempo real.")
            .setOngoing(true)
            .build()
    }

    private fun startRealtime(token: String) {
        if (realtimeClient != null) return

        realtimeClient = RealtimeClient(
            tokenProvider = { token },
            onEvent = { type, payload ->
                if (type == "ride_offer") {
                    val origin = payload?.optString("originAddress")?.takeIf { it.isNotBlank() }
                    val destination = payload?.optString("destinationAddress")?.takeIf { it.isNotBlank() }
                    showRideOfferNotification(origin, destination)
                }
            }
        ).also { it.connect() }
    }

    private fun showRideOfferNotification(origin: String?, destination: String?) {
        val text = when {
            origin != null && destination != null -> "$origin → $destination"
            origin != null -> "Origem: $origin"
            else -> "Abra o app para aceitar ou rejeitar."
        }

        val notification = NotificationCompat.Builder(this, RIDE_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Nova corrida disponível")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        getSystemService(NotificationManager::class.java)
            ?.notify(RIDE_OFFER_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "driver_location"
        private const val RIDE_CHANNEL_ID = "driver_ride_offers"
        private const val NOTIFICATION_ID = 7001
        private const val RIDE_OFFER_NOTIFICATION_ID = 7002
    }
}
