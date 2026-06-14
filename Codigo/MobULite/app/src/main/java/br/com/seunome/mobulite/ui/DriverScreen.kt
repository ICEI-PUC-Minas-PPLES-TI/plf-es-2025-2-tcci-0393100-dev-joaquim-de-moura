package br.com.seunome.mobulite.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material.icons.filled.UTurnLeft
import androidx.compose.material.icons.filled.UTurnRight
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import coil.compose.AsyncImage
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import br.com.seunome.mobulite.BuildConfig
import br.com.seunome.mobulite.R
import br.com.seunome.mobulite.data.remote.BillingCyclesResponse
import br.com.seunome.mobulite.data.remote.ConfirmPaymentRequest
import br.com.seunome.mobulite.data.remote.DriverMeResponse
import br.com.seunome.mobulite.data.remote.DriverRideResponse
import br.com.seunome.mobulite.data.remote.DriverStatusRequest
import br.com.seunome.mobulite.data.remote.RetrofitClient
import br.com.seunome.mobulite.data.remote.RideRequestItem
import br.com.seunome.mobulite.data.remote.DirectionsStep
import br.com.seunome.mobulite.data.remote.fetchRoute
import br.com.seunome.mobulite.data.remote.fetchRoutePoints
import br.com.seunome.mobulite.service.DriverLocationForegroundService
import br.com.seunome.mobulite.service.FcmEventBus
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.HttpException
import kotlin.math.*

private val DriverMapAttributionSafeInset = 48.dp
private val DriverFloatingControlsBottomGap = 12.dp

@SuppressLint("MissingPermission")
@Composable
fun DriverScreen(
    onLogout: () -> Unit,
    onHome: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val density = LocalDensity.current

    var isOnline by remember { mutableStateOf(false) }
    var loadingStatus by remember { mutableStateOf(false) }
    var loadingRides by remember { mutableStateOf(false) }
    var loadingProfile by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }

    var pendingRides by remember { mutableStateOf<List<RideRequestItem>>(emptyList()) }
    var driverProfile by remember { mutableStateOf<DriverMeResponse?>(null) }
    var driverStatus by remember { mutableStateOf<String?>(null) }
    var rejectionReason by remember { mutableStateOf<String?>(null) }

    var currentRide by remember { mutableStateOf<DriverRideResponse?>(null) }
    // paymentRide: ride já FINISHED aguardando confirmação do método de pagamento
    var paymentRide by remember { mutableStateOf<DriverRideResponse?>(null) }
    // receiptMethod: definido quando motorista confirma pagamento → mostra recibo
    var receiptMethod by remember { mutableStateOf<String?>(null) }

    var driverLocation by remember { mutableStateOf<LatLng?>(null) }
    var showProfile by remember { mutableStateOf(false) }
    var showSupport by remember { mutableStateOf(false) }
    var showFinance by remember { mutableStateOf(false) }
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var offerCountdown by remember { mutableStateOf(15) }
    var waitSeconds by remember { mutableStateOf(0) }
    var showCancelRideDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }
    var showFinishConfirmDialog by remember { mutableStateOf(false) }

    var showBilling by remember { mutableStateOf(false) }
    var billingData by remember { mutableStateOf<BillingCyclesResponse?>(null) }
    var billingLoading by remember { mutableStateOf(false) }

    var showNavSheet by remember { mutableStateOf(false) }
    var navTargetLat by remember { mutableStateOf(0.0) }
    var navTargetLng by remember { mutableStateOf(0.0) }
    var navSteps by remember { mutableStateOf<List<DirectionsStep>>(emptyList()) }
    var currentStepIndex by remember { mutableStateOf(0) }
    var inAppNavActive by remember { mutableStateOf(false) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var finishingRide by remember { mutableStateOf(false) }
    var earningsToday by remember { mutableStateOf(0) }
    var ridesToday by remember { mutableStateOf(0) }
    var acceptedThisShift by remember { mutableStateOf(0) }
    var rejectedThisShift by remember { mutableStateOf(0) }
    var etaToPickupMinutes by remember { mutableStateOf<Int?>(null) }
    var isPaused by remember { mutableStateOf(false) }
    var showReportSheet by remember { mutableStateOf(false) }
    var showDriverChat by remember { mutableStateOf(false) }
    var driverChatUnread by remember { mutableStateOf(0) }
    var driverChatPrevCount by remember { mutableStateOf(0) }
    var driverIncomingMsg by remember { mutableStateOf<String?>(null) }
    var bottomContentHeightPx by remember { mutableStateOf(0) }
    val bottomContentPadding = with(density) { bottomContentHeightPx.toDp() }

    val cameraPositionState = rememberCameraPositionState()
    val fusedClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasLocationPermission =
            perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    suspend fun loadDriverProfile() {
        try {
            loadingProfile = true
            val profile = RetrofitClient.driverApi.getDriverMe()
            driverProfile = profile
            driverStatus = profile.approvalStatus
            rejectionReason = profile.rejectionReason
            isOnline = profile.online
            isPaused = profile.online && profile.available == false
        } catch (_: Exception) {
            // silent — polling loop, no toast needed
        } finally {
            loadingProfile = false
        }
    }

    suspend fun loadCurrentRide() {
        if (paymentRide != null) return

        try {
            currentRide = RetrofitClient.driverApi.getCurrentRide()
        } catch (_: Exception) {
            // silent — polling loop, no toast needed
        }
    }
    var rideAlertPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    fun startRideAlert(context: Context) {
        if (rideAlertPlayer != null) return

        try {
            rideAlertPlayer = MediaPlayer.create(context, R.raw.new_ride_pro)

            rideAlertPlayer?.isLooping = true
            rideAlertPlayer?.start()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopRideAlert() {
        rideAlertPlayer?.stop()
        rideAlertPlayer?.release()
        rideAlertPlayer = null
    }

    suspend fun loadPendingRides() {
        if (!isOnline || isPaused || driverStatus != "APPROVED" || currentRide != null) {
            pendingRides = emptyList()
            stopRideAlert()
            return
        }

        loadingRides = true

        try {
            val rides = RetrofitClient.driverApi.getPendingRides()
            pendingRides = rides

            if (rides.isNotEmpty()) {
                startRideAlert(context) // fica em loop
            } else {
                stopRideAlert()
            }

        } catch (e: Exception) {
            stopRideAlert()
        } finally {
            loadingRides = false
        }
    }


    fun startForegroundService() {
        val intent = Intent(context, DriverLocationForegroundService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopForegroundService() {
        context.stopService(Intent(context, DriverLocationForegroundService::class.java))
    }

    fun cancelRide() {
        scope.launch {
            try {
                val rideId = currentRide?.rideId ?: return@launch
                RetrofitClient.driverApi.cancelRide(rideId)
                currentRide = null
                pendingRides = emptyList()
                routePoints = emptyList()
                inAppNavActive = false
                showNavSheet = false
                message = "Corrida cancelada com sucesso."
                loadPendingRides()
            } catch (e: Exception) {
                message = "Erro ao cancelar: ${e.message}"
            }
        }
    }

    fun goOnline() {
        scope.launch {
            loadingStatus = true
            try {
                val response = RetrofitClient.driverApi.updateStatus(
                    DriverStatusRequest(online = true, available = true)
                )
                isOnline = response.online
                isPaused = response.online && response.available == false
                message = response.message ?: "Motorista online"
                startForegroundService()
                loadDriverProfile()
                loadCurrentRide()
                loadPendingRides()
            } catch (e: HttpException) {
                val body = e.response()?.errorBody()?.string()
                message = "Erro ao ficar online: HTTP ${e.code()} - ${body ?: e.message()}"
            } catch (e: Exception) {
                message = "Erro ao ficar online: ${e.message}"
            } finally {
                loadingStatus = false
            }
        }
    }

    fun goOffline() {
        scope.launch {
            loadingStatus = true
            try {
                val response = RetrofitClient.driverApi.updateStatus(
                    DriverStatusRequest(false)
                )
                isOnline = response.online
                isPaused = false
                pendingRides = emptyList()
                currentRide = null
                paymentRide = null
                receiptMethod = null
                routePoints = emptyList()
                message = response.message ?: "Motorista offline"
                stopForegroundService()
                loadDriverProfile()
            } catch (e: HttpException) {
                val body = e.response()?.errorBody()?.string()
                message = "Erro ao ficar offline: HTTP ${e.code()} - ${body ?: e.message()}"
            } catch (e: Exception) {
                message = "Erro ao ficar offline: ${e.message}"
            } finally {
                loadingStatus = false
            }
            stopRideAlert()
        }
    }

    fun pauseDriver() {
        scope.launch {
            loadingStatus = true
            try {
                val response = RetrofitClient.driverApi.updateStatus(DriverStatusRequest(online = true, available = false))
                isOnline = response.online
                isPaused = response.online && response.available == false
                pendingRides = emptyList()
                loadingRides = false
                stopRideAlert()
                message = "Modo pausado ativado. Você não receberá novas corridas."
            } catch (e: Exception) {
                message = "Erro ao pausar: ${e.message}"
            } finally {
                loadingStatus = false
            }
        }
    }

    fun resumeDriver() {
        scope.launch {
            loadingStatus = true
            try {
                val response = RetrofitClient.driverApi.updateStatus(DriverStatusRequest(online = true, available = true))
                isOnline = response.online
                isPaused = response.online && response.available == false
                message = "Modo retomado. Você está recebendo corridas novamente."
                loadPendingRides()
            } catch (e: Exception) {
                message = "Erro ao retomar: ${e.message}"
            } finally {
                loadingStatus = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        loadDriverProfile()
        loadCurrentRide()
        try {
            val history = RetrofitClient.driverApi.getRideHistory()
            earningsToday = history.todayEarned
            ridesToday = history.todayCount
        } catch (_: Exception) {}
        try {
            billingLoading = true
            billingData = RetrofitClient.driverApi.getBillingCycles()
        } catch (_: Exception) {} finally { billingLoading = false }
    }

    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) return@LaunchedEffect

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            3000L
        ).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc    = result.lastLocation ?: return
                val latLng = LatLng(loc.latitude, loc.longitude)
                driverLocation = latLng

                // Enviar localização ao backend quando online e em corrida
                if (isOnline && currentRide != null) {
                    scope.launch {
                        try {
                            RetrofitClient.driverApi.updateLocation(
                                br.com.seunome.mobulite.data.remote.DriverLocationRequest(
                                    lat = loc.latitude,
                                    lng = loc.longitude
                                )
                            )
                        } catch (_: Exception) {}
                    }
                }

                // Câmera GPS: tilt + bearing + zoom quando navegando; overview quando não
                scope.launch {
                    runCatching {
                        if (inAppNavActive) {
                            val nextStep = navSteps.getOrNull(currentStepIndex)
                            val bearing: Float = when {
                                loc.hasBearing() && loc.speed > 1f -> loc.bearing
                                nextStep != null ->
                                    driverBearingTo(latLng, nextStep.endLocation).toFloat()
                                else -> cameraPositionState.position.bearing
                            }
                            cameraPositionState.animate(
                                update = CameraUpdateFactory.newCameraPosition(
                                    com.google.android.gms.maps.model.CameraPosition.Builder()
                                        .target(latLng)
                                        .zoom(18f)
                                        .bearing(bearing)
                                        .tilt(45f)
                                        .build()
                                ),
                                durationMs = 1000
                            )
                        } else {
                            cameraPositionState.animate(
                                update = CameraUpdateFactory.newLatLngZoom(latLng, 16f),
                                durationMs = 500
                            )
                        }
                    }
                }
            }
        }

        fusedClient.requestLocationUpdates(
            request,
            callback,
            Looper.getMainLooper()
        )
    }

    // GPS integrado: inicia automaticamente ao aceitar corrida.
    // Rota sempre reflete a posição atual do motorista → destino relevante ao estado.
    LaunchedEffect(currentRide?.status, driverLocation != null) {
        val ride = currentRide
        val loc  = driverLocation

        if (ride == null) {
            routePoints    = emptyList()
            navSteps       = emptyList()
            inAppNavActive = false
            currentStepIndex = 0
            etaToPickupMinutes = null
            return@LaunchedEffect
        }
        if (loc == null) return@LaunchedEffect

        val (navOrigin, navDest, withSteps) = when (ride.status) {
            "ACCEPTED", "DRIVER_ARRIVING" ->
                Triple(loc, LatLng(ride.originLat, ride.originLng), true)
            "DRIVER_ARRIVED" ->
                Triple(LatLng(ride.originLat, ride.originLng), LatLng(ride.destLat, ride.destLng), false)
            "IN_PROGRESS" ->
                Triple(loc, LatLng(ride.destLat, ride.destLng), true)
            else -> {
                // Status desconhecido ou CANCELED — limpa toda a navegação
                routePoints    = emptyList()
                navSteps       = emptyList()
                inAppNavActive = false
                currentStepIndex = 0
                etaToPickupMinutes = null
                if (ride.status == "CANCELED") currentRide = null
                return@LaunchedEffect
            }
        }

        try {
            val result = fetchRoute(BuildConfig.MAPS_API_KEY, navOrigin, navDest)
            routePoints = result.polyline
            if (ride.status == "ACCEPTED" || ride.status == "DRIVER_ARRIVING") {
                etaToPickupMinutes = result.totalDurationSeconds.takeIf { it > 0 }?.div(60)?.coerceAtLeast(1)
            } else {
                etaToPickupMinutes = null
            }
            if (withSteps && result.steps.isNotEmpty()) {
                // Só reinicia o índice se for uma mudança real de destino (ex: aceitar→em corrida)
                val destKey = "${navDest.latitude},${navDest.longitude}"
                val prevKey = "${navTargetLat},${navTargetLng}"
                if (destKey != prevKey) {
                    navSteps         = result.steps
                    currentStepIndex = 0
                    navTargetLat     = navDest.latitude
                    navTargetLng     = navDest.longitude
                } else if (navSteps.isEmpty()) {
                    navSteps = result.steps
                }
                inAppNavActive = true
            } else if (!withSteps) {
                navSteps       = emptyList()
                inAppNavActive = false
            }
        } catch (_: Exception) {
            routePoints = emptyList()
        }
    }

    // Countdown automático para oferta de corrida (15 segundos)
    LaunchedEffect(pendingRides.firstOrNull()?.rideId) {
        val ride = pendingRides.firstOrNull() ?: run { offerCountdown = 15; return@LaunchedEffect }
        offerCountdown = 15
        while (offerCountdown > 0) {
            delay(1000)
            offerCountdown--
        }
        if (pendingRides.firstOrNull()?.rideId == ride.rideId) {
            try {
                stopRideAlert()
                RetrofitClient.driverApi.rejectRide(ride.rideId)
                pendingRides = emptyList()
                message = "Tempo esgotado — corrida ignorada."
                loadPendingRides()
            } catch (_: Exception) {}
        }
    }

    // Contador de tempo de espera quando motorista chegou ao local
    LaunchedEffect(currentRide?.status) {
        waitSeconds = 0
        if (currentRide?.status == "DRIVER_ARRIVED") {
            while (true) { delay(1000); waitSeconds++ }
        }
    }

    // Badge de mensagens não lidas — poll em background quando chat está fechado
    LaunchedEffect(currentRide?.rideId, showDriverChat) {
        val rideId = currentRide?.rideId
        if (rideId == null) { driverChatUnread = 0; driverChatPrevCount = 0; return@LaunchedEffect }
        // Busca o count atual para inicializar baseline (abre ou fecha o chat)
        runCatching {
            val msgs = RetrofitClient.driverApi.getChatMessages(rideId)
            driverChatPrevCount = msgs.size
        }
        driverChatUnread = 0
        if (showDriverChat) return@LaunchedEffect   // chat aberto: ChatScreen faz o próprio poll
        // Chat fechado: detecta novas mensagens a cada 5 s
        while (true) {
            delay(5_000)
            val status = currentRide?.status
            if (status == "IN_PROGRESS" || status == "DRIVER_ARRIVING" || status == "DRIVER_ARRIVED") {
                runCatching {
                    val msgs = RetrofitClient.driverApi.getChatMessages(rideId)
                    val n = msgs.size
                    if (n > driverChatPrevCount) {
                        val newMsgs = msgs.drop(driverChatPrevCount)
                        driverChatUnread += newMsgs.size
                        driverChatPrevCount = n
                        val lastPassenger = newMsgs.lastOrNull { it.senderRole == "PASSENGER" }
                        if (lastPassenger != null) driverIncomingMsg = lastPassenger.content
                    }
                }
            }
        }
    }

    // Transição imediata de câmera ao entrar/sair do modo GPS
    LaunchedEffect(inAppNavActive) {
        val loc = driverLocation ?: return@LaunchedEffect
        runCatching {
            if (inAppNavActive) {
                val nextStep = navSteps.getOrNull(currentStepIndex)
                val bearing = if (nextStep != null)
                    driverBearingTo(loc, nextStep.endLocation).toFloat()
                else cameraPositionState.position.bearing
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newCameraPosition(
                        com.google.android.gms.maps.model.CameraPosition.Builder()
                            .target(loc).zoom(18f).bearing(bearing).tilt(45f).build()
                    ),
                    durationMs = 800
                )
            } else {
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLngZoom(loc, 15f),
                    durationMs = 600
                )
            }
        }
    }

    // Avança passo de navegação interna quando driver chega perto do ponto final do passo
    LaunchedEffect(driverLocation) {
        if (!inAppNavActive || navSteps.isEmpty()) return@LaunchedEffect
        val step = navSteps.getOrNull(currentStepIndex) ?: return@LaunchedEffect
        val loc = driverLocation ?: return@LaunchedEffect
        val distM = driverHaversineKm(
            loc.latitude, loc.longitude,
            step.endLocation.latitude, step.endLocation.longitude
        ) * 1000.0
        if (distM < 30.0 && currentStepIndex < navSteps.size - 1) {
            currentStepIndex++
        }
    }

    LaunchedEffect(isOnline, isPaused, driverStatus, currentRide?.rideId, paymentRide?.rideId) {
        if (isOnline && !isPaused && driverStatus == "APPROVED") {
            while (true) {
                if (paymentRide != null) break

                loadCurrentRide()

                if (currentRide == null) {
                    loadPendingRides()
                } else {
                    pendingRides = emptyList()
                }

                delay(5000)
            }
        } else {
            pendingRides = emptyList()
        }
    }

    DisposableEffect(Unit) {
        onDispose { stopRideAlert() }
    }

    LaunchedEffect(showBilling, showFinance) {
        if (!showBilling && !showFinance) {
            try {
                billingLoading = true
                billingData = RetrofitClient.driverApi.getBillingCycles()
            } catch (_: Exception) {} finally { billingLoading = false }
        }
    }

    // Refresh billing data every 60s so the inline billing summary reflects admin settlements quickly
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            try { billingData = RetrofitClient.driverApi.getBillingCycles() } catch (_: Exception) {}
        }
    }

    // FCM foreground events — refresh pending rides immediately on new-ride notification
    LaunchedEffect(Unit) {
        FcmEventBus.events.collect {
            if (isOnline && !isPaused && driverStatus == "APPROVED" && currentRide == null) {
                loadPendingRides()
            }
        }
    }

    if (showBilling) {
        DriverBillingScreen(onBack = { showBilling = false })
        return
    }

    if (showSupport) {
        SupportTicketScreen(
            onBack = { showSupport = false },
            isDriver = true,
            initialRideId = currentRide?.rideId
        )
        return
    }

    if (showFinance) {
        DriverFinanceScreen(
            onBack = { showFinance = false },
            onBillingCycles = { showFinance = false; showBilling = true }
        )
        return
    }

    if (showProfile) {
        DriverProfileScreen(
            onBack = { showProfile = false },
            onSupport = { showProfile = false; showSupport = true },
            onFinance = { showProfile = false; showFinance = true },
            onLogout = { showProfile = false; onLogout() }
        )
        return
    }

    // Exibe tela de status enquanto perfil carrega ou quando rejeitado/pendente
    if (loadingProfile) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (driverStatus == "REJECTED") {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Cadastro reprovado",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    if (!rejectionReason.isNullOrBlank()) {
                        Text(
                            text = "Motivo: $rejectionReason",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = "Entre em contato com o suporte para solicitar uma nova análise.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                        Text("Sair")
                    }
                }
            }
        }
        return
    }

    if (driverStatus == "PENDING") {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Cadastro em análise",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Seu cadastro está sendo analisado pelo administrador. Aguarde a aprovação para começar a receber corridas.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                        Text("Sair")
                    }
                }
            }
        }
        return
    }

    if (showCancelRideDialog) {
        val cancelReasons = listOf(
            "Passageiro não apareceu",
            "Endereço de origem incorreto",
            "Emergência pessoal",
            "Problema no veículo",
            "Outro"
        )
        AlertDialog(
            onDismissRequest = { showCancelRideDialog = false; cancelReason = "" },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Motivo do cancelamento", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Selecione o motivo para cancelar a corrida. O passageiro será notificado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                    Spacer(Modifier.height(8.dp))
                    cancelReasons.forEach { reason ->
                        val selected = cancelReason == reason
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { cancelReason = reason },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) Color(0xFFF3E8FF) else Color(0xFFF8FAFC),
                            border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF9333EA)) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = { cancelReason = reason },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF9333EA))
                                )
                                Text(
                                    reason,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selected) Color(0xFF6B21A8) else Color(0xFF1E293B)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showCancelRideDialog = false; cancelRide(); cancelReason = "" },
                    enabled = cancelReason.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancelar corrida", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelRideDialog = false; cancelReason = "" }) {
                    Text("Voltar", color = Color(0xFF64748B))
                }
            }
        )
    }

    if (showFinishConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!finishingRide) showFinishConfirmDialog = false },
            title = { Text("Finalizar corrida?") },
            text = { Text("Confirme que você chegou ao destino e deseja encerrar a corrida.") },
            confirmButton = {
                TextButton(
                    enabled = !finishingRide,
                    onClick = {
                        val rideId = currentRide?.rideId ?: return@TextButton
                        showFinishConfirmDialog = false
                        scope.launch {
                            finishingRide = true
                            try {
                                // Chama finishRide imediatamente → ride vira FINISHED no backend
                                // O passageiro verá a tela de pagamento ao próximo polling
                                val response = RetrofitClient.driverApi.finishRide(rideId)
                                paymentRide = response
                                currentRide = null
                                routePoints = emptyList()
                                pendingRides = emptyList()
                                inAppNavActive = false
                                navSteps = emptyList()
                            } catch (e: Exception) {
                                message = "Erro ao finalizar: ${e.message}"
                            } finally {
                                finishingRide = false
                            }
                        }
                    }
                ) {
                    if (finishingRide) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Confirmar")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !finishingRide,
                    onClick = { showFinishConfirmDialog = false }
                ) { Text("Voltar") }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = hasLocationPermission
            ),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = false,
                zoomControlsEnabled = false
            ),
            contentPadding = PaddingValues(
                top = if (inAppNavActive) 156.dp else 128.dp,
                bottom = bottomContentPadding + DriverMapAttributionSafeInset,
                start = 12.dp,
                end = 12.dp,
            )
        ) {
            if (routePoints.isNotEmpty()) {
                Polyline(
                    points = routePoints,
                    color = if (inAppNavActive) Color(0xFF2563EB) else Color(0xFF64B5F6),
                    width = if (inAppNavActive) 14f else 8f,
                    zIndex = 1f
                )
            }

            currentRide?.let { ride ->
                Marker(
                    state = MarkerState(position = LatLng(ride.originLat, ride.originLng)),
                    title = "Passageiro"
                )
                Marker(
                    state = MarkerState(position = LatLng(ride.destLat, ride.destLng)),
                    title = "Destino"
                )
            }
        }

        AnimatedVisibility(
            visible = !inAppNavActive,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it }
        ) {
            DriverTopFloatingBar(
                profile = driverProfile,
                isOnline = isOnline,
                isPaused = isPaused,
                onShowProfile = { showProfile = true },
                onHome = onHome
            )
        }

        if (!hasLocationPermission) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 80.dp, start = 12.dp, end = 12.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFEF3C7),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.LocationOff, null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                    Text(
                        "Permissão de localização negada. O turno não funcionará sem ela.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF92400E),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        permissionLauncher.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
                    }) {
                        Text("Permitir", color = Color(0xFFD97706), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        DriverBottomContent(
            isOnline = isOnline,
            loadingRides = loadingRides,
            driverName = driverProfile?.name,
            driverPixKey = driverProfile?.pixKey,
            driverPixQrPayload = driverProfile?.pixQrPayload,
            onGoOnline = { goOnline() },
            onGoOffline = { goOffline() },
            loadingStatus = loadingStatus,
            pendingRides = pendingRides,
            currentRide = currentRide,
            paymentRide = paymentRide,
            offerCountdown = offerCountdown,
            waitSeconds = waitSeconds,
            driverLocation = driverLocation,
            earningsToday = earningsToday,
            ridesToday = ridesToday,
            acceptedThisShift = acceptedThisShift,
            rejectedThisShift = rejectedThisShift,
            etaToPickupMinutes = etaToPickupMinutes,
            billingData = billingData,
            billingLoading = billingLoading,
            onShowBilling = { showFinance = true },
            onAccept = { ride ->
                scope.launch {
                    try {
                        stopRideAlert()
                        RetrofitClient.driverApi.acceptRide(ride.rideId)
                        acceptedThisShift++
                        message = "Corrida aceita com sucesso."
                        loadCurrentRide()
                        pendingRides = emptyList()
                    } catch (e: Exception) {
                        message = "Erro ao aceitar corrida: ${e.message}"
                    }
                }
            },
            onReject = { ride ->
                scope.launch {
                    try {
                        stopRideAlert()
                        RetrofitClient.driverApi.rejectRide(ride.rideId)
                        rejectedThisShift++
                        message = "Corrida recusada."
                        loadPendingRides()
                    } catch (e: Exception) {
                        message = "Erro ao recusar corrida: ${e.message}"
                    }
                }
            },
            onNavigateToPassenger = {
                currentRide?.let {
                    navTargetLat = it.originLat
                    navTargetLng = it.originLng
                    showNavSheet = true
                }
            },
            onMarkArriving = {
                scope.launch {
                    try {
                        currentRide = RetrofitClient.driverApi.markArriving(currentRide!!.rideId)
                        message = "Status atualizado: a caminho do passageiro."
                    } catch (e: Exception) {
                        message = "Erro ao atualizar status: ${e.message}"
                    }
                }
            },
            onMarkArrived = {
                scope.launch {
                    try {
                        currentRide = RetrofitClient.driverApi.markArrived(currentRide!!.rideId)
                        message = "Status atualizado: chegou ao local."
                    } catch (e: Exception) {
                        message = "Erro ao atualizar status: ${e.message}"
                    }
                }
            },
            onPassengerBoarded = {
                scope.launch {
                    try {
                        currentRide = RetrofitClient.driverApi.startRide(currentRide!!.rideId)
                        message = "Passageiro embarcou. Corrida iniciada."
                    } catch (e: Exception) {
                        message = "Erro ao iniciar corrida: ${e.message}"
                    }
                }
            },
            onNavigateToDestination = {
                currentRide?.let {
                    navTargetLat = it.destLat
                    navTargetLng = it.destLng
                    showNavSheet = true
                }
            },
            onGoToPayment = { showFinishConfirmDialog = true },
            onCancelRide = { showCancelRideDialog = true },
            onCallPassenger = {
                val phone = currentRide?.passengerPhone
                if (!phone.isNullOrBlank()) {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                }
            },
            onPixPaid = {
                scope.launch {
                    val rideId = paymentRide?.rideId ?: return@launch
                    runCatching {
                        RetrofitClient.driverApi.confirmPayment(
                            rideId,
                            ConfirmPaymentRequest(receiptNote = "Confirmado pelo motorista via Pix")
                        )
                    }.onSuccess { response ->
                        paymentRide = paymentRide?.copy(
                            paymentStatus = response.paymentStatus,
                            paymentTxId = response.paymentTxId ?: paymentRide?.paymentTxId
                        )
                    }
                    receiptMethod = "PIX"
                }
            },
            onCashPaid = {
                scope.launch {
                    val rideId = paymentRide?.rideId ?: return@launch
                    runCatching {
                        RetrofitClient.driverApi.confirmPayment(
                            rideId,
                            ConfirmPaymentRequest(receiptNote = "Confirmado pelo motorista em dinheiro")
                        )
                    }.onSuccess { response ->
                        paymentRide = paymentRide?.copy(paymentStatus = response.paymentStatus)
                    }
                    receiptMethod = "DINHEIRO"
                }
            },
            onBackPayment = { paymentRide = null; receiptMethod = null },
            receiptMethod = receiptMethod,
            onCloseReceipt = {
                paymentRide = null
                receiptMethod = null
                scope.launch {
                    runCatching {
                        val h = RetrofitClient.driverApi.getRideHistory()
                        earningsToday = h.todayEarned
                        ridesToday = h.todayCount
                    }
                }
            },
            isPaused = isPaused,
            onPause = { pauseDriver() },
            onResume = { resumeDriver() },
            onReportProblem = { showReportSheet = true },
            onHeightChanged = { bottomContentHeightPx = it }
        )

        if (!inAppNavActive) {
            val fabBottomPadding = (if (bottomContentPadding > 0.dp) bottomContentPadding else 160.dp) + DriverFloatingControlsBottomGap
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = fabBottomPadding),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val rideStatus = currentRide?.status
                if (rideStatus == "IN_PROGRESS" || rideStatus == "DRIVER_ARRIVING" || rideStatus == "DRIVER_ARRIVED") {
                    ChatFab(unreadCount = driverChatUnread, onClick = { showDriverChat = true })
                }

                if (hasLocationPermission && driverLocation != null) {
                    FloatingActionButton(
                        onClick = {
                            val loc = driverLocation ?: return@FloatingActionButton
                            scope.launch {
                                runCatching {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(loc, 16f)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.size(52.dp),
                        containerColor = Color.White,
                        contentColor = Color(0xFF6D28D9)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Minha localização"
                        )
                    }
                }
            }
        }

        if (showDriverChat && currentRide != null) {
            ChatScreen(
                rideId = currentRide!!.rideId,
                myRole = "DRIVER",
                partnerName = currentRide!!.passengerName ?: "Passageiro",
                isDriverSide = true,
                onBack = { showDriverChat = false }
            )
        }

        if (showReportSheet) {
            DriverReportProblemSheet(
                rideId = currentRide?.rideId,
                onDismiss = { showReportSheet = false }
            )
        }

        // Floating in-app navigation instruction card
        if (inAppNavActive && navSteps.isNotEmpty()) {
            navSteps.getOrNull(currentStepIndex)?.let { step ->
                InAppNavCard(
                    step = step,
                    stepIndex = currentStepIndex,
                    totalSteps = navSteps.size,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 8.dp, start = 16.dp, end = 16.dp),
                    onClose = {
                        inAppNavActive = false
                        navSteps = emptyList()
                        currentStepIndex = 0
                    }
                )
            }
        }

        // Floating feedback message (success/error)
        message?.let { msg ->
            LaunchedEffect(msg) {
                delay(3_500)
                message = null
            }
            val isError = msg.startsWith("Erro")
            // When nav card is active push the toast below it (~140dp), otherwise sit below the top bar (~76dp visible + 20dp margins)
            val msgTopPadding = if (inAppNavActive) 140.dp else 96.dp
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = msgTopPadding, start = 20.dp, end = 20.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = if (isError) Color(0xFFDC2626).copy(alpha = 0.95f) else Color(0xFF059669).copy(alpha = 0.95f),
                shadowElevation = 8.dp
            ) {
                Text(
                    text = msg,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Notificação de mensagem recebida — some após 4 s
        LaunchedEffect(driverIncomingMsg) {
            if (driverIncomingMsg != null) {
                delay(4_000)
                driverIncomingMsg = null
            }
        }
        AnimatedVisibility(
            visible = driverIncomingMsg != null,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = if (message != null || inAppNavActive) 160.dp else 96.dp, start = 20.dp, end = 20.dp)
                .fillMaxWidth()
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF6D28D9).copy(alpha = 0.95f),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        driverIncomingMsg = null
                        showDriverChat = true
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentRide?.passengerName ?: "Passageiro",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                        Text(
                            text = driverIncomingMsg ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Navigation mode choice bottom sheet
        if (showNavSheet) {
            NavigationChoiceSheet(
                onDismiss = { showNavSheet = false },
                onGoogleMaps = {
                    showNavSheet = false
                    openGoogleMapsNavigation(context, navTargetLat, navTargetLng)
                },
                onWaze = {
                    showNavSheet = false
                    openWazeNavigation(context, navTargetLat, navTargetLng)
                },
                onInApp = {
                    showNavSheet = false
                    val loc    = driverLocation ?: return@NavigationChoiceSheet
                    val target = LatLng(navTargetLat, navTargetLng)
                    scope.launch {
                        try {
                            val result = fetchRoute(BuildConfig.MAPS_API_KEY, loc, target)
                            routePoints      = result.polyline
                            navSteps         = result.steps
                            currentStepIndex = 0
                            inAppNavActive   = result.steps.isNotEmpty()
                            if (result.steps.isEmpty()) message = "Rota não encontrada."
                        } catch (_: Exception) {
                            message = "Erro ao recalcular rota."
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun DriverTopFloatingBar(
    profile: DriverMeResponse?,
    isOnline: Boolean,
    isPaused: Boolean,
    onShowProfile: () -> Unit = {},
    onHome: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.97f)),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onShowProfile() }
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DriverAvatar(
                    initial = profile?.name?.firstOrNull()?.uppercase(),
                    isOnline = isOnline,
                    isPaused = isPaused,
                    photoUrl = RetrofitClient.photoUrl(profile?.profilePhotoUrl)
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = profile?.name?.takeIf { it.isNotBlank() } ?: "Motorista",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Perfil, veículo e Pix",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF6D28D9),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            DriverOnlineBadge(isOnline = isOnline, isPaused = isPaused)

            if (onHome != null) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFF5F3FF),
                    modifier = Modifier
                        .size(38.dp)
                        .clickable { onHome() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "Início",
                            tint = Color(0xFF6D28D9),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DriverAvatar(
    initial: String?,
    isOnline: Boolean,
    isPaused: Boolean,
    photoUrl: String? = null
) {
    val avatarColor = when {
        isPaused -> Color(0xFFD97706)
        isOnline -> Color(0xFF6D28D9)
        else -> Color(0xFF475569)
    }
    val statusDotColor = when {
        isPaused -> Color(0xFFF59E0B)
        isOnline -> Color(0xFF22C55E)
        else -> Color(0xFFCBD5E1)
    }

    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(avatarColor),
            contentAlignment = Alignment.Center
        ) {
            if (photoUrl != null) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(46.dp).clip(CircleShape)
                )
            } else if (initial.isNullOrBlank()) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(23.dp)
                )
            } else {
                Text(
                    text = initial,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Box(
            modifier = Modifier
                .size(13.dp)
                .clip(CircleShape)
                .background(statusDotColor)
        )
    }
}

@Composable
private fun DriverOnlineBadge(isOnline: Boolean, isPaused: Boolean) {
    val label = when {
        isPaused -> "Pausado"
        isOnline -> "Online"
        else -> "Offline"
    }
    val background = when {
        isPaused -> Color(0xFFFFFBEB)
        isOnline -> Color(0xFFE6F7F1)
        else -> Color(0xFFF1F5F9)
    }
    val foreground = when {
        isPaused -> Color(0xFFD97706)
        isOnline -> Color(0xFF047857)
        else -> Color(0xFF64748B)
    }

    Surface(
        shape = RoundedCornerShape(100.dp),
        color = background
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = foreground,
            maxLines = 1
        )
    }
}


@Composable
fun DriverBottomContent(
    isOnline: Boolean,
    loadingRides: Boolean,
    driverName: String?,
    driverPixKey: String? = null,
    driverPixQrPayload: String? = null,
    onGoOnline: () -> Unit,
    onGoOffline: () -> Unit,
    loadingStatus: Boolean,
    pendingRides: List<RideRequestItem>,
    currentRide: DriverRideResponse?,
    paymentRide: DriverRideResponse?,
    offerCountdown: Int,
    waitSeconds: Int,
    driverLocation: LatLng?,
    earningsToday: Int,
    ridesToday: Int,
    acceptedThisShift: Int = 0,
    rejectedThisShift: Int = 0,
    etaToPickupMinutes: Int?,
    billingData: BillingCyclesResponse? = null,
    billingLoading: Boolean = false,
    onShowBilling: () -> Unit = {},
    onAccept: (RideRequestItem) -> Unit,
    onReject: (RideRequestItem) -> Unit,
    onNavigateToPassenger: () -> Unit,
    onMarkArriving: () -> Unit,
    onMarkArrived: () -> Unit,
    onPassengerBoarded: () -> Unit,
    onNavigateToDestination: () -> Unit,
    onGoToPayment: () -> Unit,
    onCancelRide: () -> Unit,
    onCallPassenger: () -> Unit,
    onPixPaid: () -> Unit,
    onCashPaid: () -> Unit,
    onBackPayment: () -> Unit,
    receiptMethod: String?,
    onCloseReceipt: () -> Unit,
    isPaused: Boolean = false,
    onPause: () -> Unit = {},
    onResume: () -> Unit = {},
    onReportProblem: () -> Unit = {},
    onHeightChanged: (Int) -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { onHeightChanged(it.size.height) }
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            when {
                paymentRide != null && receiptMethod == null -> {
                    DriverPaymentChoiceCard(
                        ride = paymentRide,
                        qrPayload = paymentRide.paymentPixPayload ?: paymentRide.driverPixQrPayload ?: driverPixQrPayload ?: "",
                        pixKey = driverPixKey,
                        onPixPaid = onPixPaid,
                        onCashPaid = onCashPaid,
                        onBack = onBackPayment
                    )
                }

                paymentRide != null && receiptMethod != null -> {
                    DriverPaymentQrCard(
                        ride = paymentRide,
                        paymentMethod = receiptMethod,
                        onClose = onCloseReceipt
                    )
                }

                currentRide != null && currentRide.status == "ACCEPTED" -> {
                    DriverAcceptedRideCard(
                        ride = currentRide,
                        onNavigateToPassenger = onNavigateToPassenger,
                        onMarkArriving = onMarkArriving,
                        onCancelRide = onCancelRide,
                        onCallPassenger = onCallPassenger,
                        etaMinutes = etaToPickupMinutes
                    )
                }

                currentRide != null && currentRide.status == "DRIVER_ARRIVING" -> {
                    DriverArrivingRideCard(
                        ride = currentRide,
                        onNavigateToPassenger = onNavigateToPassenger,
                        onMarkArrived = onMarkArrived,
                        onCancelRide = onCancelRide,
                        onCallPassenger = onCallPassenger,
                        etaMinutes = etaToPickupMinutes
                    )
                }

                currentRide != null && currentRide.status == "DRIVER_ARRIVED" -> {
                    DriverArrivedRideCard(
                        ride = currentRide,
                        waitSeconds = waitSeconds,
                        onPassengerBoarded = onPassengerBoarded,
                        onCallPassenger = onCallPassenger
                    )
                }

                currentRide != null && currentRide.status == "IN_PROGRESS" -> {
                    DriverInProgressRideCard(
                        ride = currentRide,
                        onNavigateToDestination = onNavigateToDestination,
                        onFinishRide = onGoToPayment,
                        paymentMethod = currentRide.paymentMethod,
                        onReportProblem = onReportProblem
                    )
                }

                isOnline && pendingRides.isNotEmpty() -> {
                    val ride = pendingRides.first()
                    val distKm = remember(driverLocation, ride.rideId) {
                        val dLat = driverLocation?.latitude ?: return@remember null
                        val dLng = driverLocation?.longitude ?: return@remember null
                        val oLat = ride.originLat ?: return@remember null
                        val oLng = ride.originLng ?: return@remember null
                        driverHaversineKm(dLat, dLng, oLat, oLng)
                    }
                    DriverPendingRideFloatingCard(
                        ride = ride,
                        countdown = offerCountdown,
                        distanceToPickupKm = distKm,
                        onAccept = { onAccept(ride) },
                        onReject = { onReject(ride) }
                    )
                }

                isOnline -> {
                    DriverIdleOnlineCard(
                        driverName = driverName,
                        onGoOffline = onGoOffline,
                        loadingRides = loadingRides,
                        loadingStatus = loadingStatus,
                        earningsToday = earningsToday,
                        ridesToday = ridesToday,
                        acceptedThisShift = acceptedThisShift,
                        rejectedThisShift = rejectedThisShift,
                        billingData = billingData,
                        billingLoading = billingLoading,
                        onShowBilling = onShowBilling,
                        isPaused = isPaused,
                        onPause = onPause,
                        onResume = onResume
                    )
                }

                else -> {
                    DriverIdleOfflineCard(
                        driverName = driverName,
                        onGoOnline = onGoOnline,
                        loadingStatus = loadingStatus,
                        earningsToday = earningsToday,
                        ridesToday = ridesToday,
                        billingData = billingData,
                        billingLoading = billingLoading,
                        onShowBilling = onShowBilling
                    )
                }
            }
        }
    }
}

@Composable
fun DriverPendingRideFloatingCard(
    ride: RideRequestItem,
    countdown: Int,
    distanceToPickupKm: Double?,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val priceFmt = "R$ %.2f".format((ride.price ?: 0.0) / 100.0).replace(".", ",")
    val distKm = (ride.distanceMeters ?: 0.0) / 1000.0
    val isUrgent = countdown <= 5

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header: título + timer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nova corrida", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Surface(shape = RoundedCornerShape(100.dp),
                        color = if (isUrgent) Color(0xFFFEE2E2) else Color(0xFFF0FDF4)) {
                        Text(priceFmt,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold,
                            color = if (isUrgent) Color(0xFFDC2626) else Color(0xFF16A34A))
                    }
                }
                Surface(shape = RoundedCornerShape(100.dp),
                    color = if (isUrgent) Color(0xFFFEE2E2) else Color(0xFFFEF3C7)) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Default.Schedule, contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = if (isUrgent) Color(0xFFDC2626) else Color(0xFFD97706))
                        Text("${countdown}s", style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isUrgent) Color(0xFFDC2626) else Color(0xFFD97706))
                    }
                }
            }

            LinearProgressIndicator(
                progress = { countdown / 15f },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(100.dp)),
                color = if (isUrgent) Color(0xFFDC2626) else Color(0xFFF59E0B),
                trackColor = Color(0xFFF1F5F9)
            )

            // Passageiro + distância + método
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PassengerAvatarSmall(name = ride.passengerName, photoUrl = ride.passengerPhotoUrl)
                Text(ride.passengerName ?: "Passageiro", modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    distanceToPickupKm?.let {
                        Surface(shape = RoundedCornerShape(100.dp), color = Color(0xFFF1F5F9)) {
                            Text("%.1f km".format(it).replace(".", ","), modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                        }
                    }
                    val payBg = if (ride.paymentMethod == "PIX") Color(0xFFEFF6FF) else Color(0xFFF0FDF4)
                    val payFg = if (ride.paymentMethod == "PIX") Color(0xFF2563EB) else Color(0xFF16A34A)
                    Surface(shape = RoundedCornerShape(100.dp), color = payBg) {
                        Text(if (ride.paymentMethod == "PIX") "Pix" else "Din.",
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
                            color = payFg)
                    }
                    ride.passengerTrips?.let { trips ->
                        Surface(shape = RoundedCornerShape(100.dp), color = Color(0xFFF1F5F9)) {
                            Text(
                                "$trips corridas",
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            // Endereços
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RideAddressRow(address = ride.originAddress, isOrigin = true)
                RideAddressRow(address = ride.destinationAddress, isOrigin = false)
            }

            // Botões aceitar/recusar
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f).height(46.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))) {
                    Text("Recusar", fontWeight = FontWeight.SemiBold, color = Color(0xFF475569),
                        style = MaterialTheme.typography.labelMedium)
                }
                Button(onClick = onAccept, modifier = Modifier.weight(1f).height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9))) {
                    Text("Aceitar", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
@Composable
fun DriverAcceptedRideCard(
    ride: DriverRideResponse,
    onNavigateToPassenger: () -> Unit,
    onMarkArriving: () -> Unit,
    onCancelRide: () -> Unit,
    onCallPassenger: () -> Unit,
    etaMinutes: Int? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RideStatusChip("Corrida aceita", Color(0xFFECFDF5), Color(0xFF059669))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    driverFmtPrice(ride.price)?.let { price ->
                        Surface(shape = RoundedCornerShape(100.dp), color = Color(0xFFF0FDF4)) {
                            Text(price, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                                color = Color(0xFF16A34A))
                        }
                    }
                    etaMinutes?.let { eta ->
                        Surface(shape = RoundedCornerShape(100.dp), color = Color(0xFFEFF6FF)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null,
                                    modifier = Modifier.size(11.dp), tint = Color(0xFF2563EB))
                                Text("≈ ${eta}min", style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold, color = Color(0xFF2563EB))
                            }
                        }
                    }
                    if (!ride.passengerPhone.isNullOrBlank()) {
                        IconButton(onClick = onCallPassenger, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Call, contentDescription = "Ligar",
                                tint = Color(0xFF6D28D9), modifier = Modifier.size(17.dp))
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PassengerAvatarSmall(name = ride.passengerName, photoUrl = ride.passengerPhotoUrl)
                Column(modifier = Modifier.weight(1f)) {
                    Text(ride.passengerName ?: "Passageiro",
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    RideAddressRow(ride.originAddress, isOrigin = true)
                }
            }

            OutlinedButton(
                onClick = onMarkArriving,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                border = BorderStroke(1.5.dp, Color(0xFF6D28D9))
            ) {
                Icon(Icons.Default.DirectionsCar, contentDescription = null,
                    modifier = Modifier.size(15.dp), tint = Color(0xFF6D28D9))
                Spacer(Modifier.width(4.dp))
                Text("A caminho", color = Color(0xFF6D28D9), fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelMedium)
            }

            // External navigation shortcuts
            val ctx = LocalContext.current
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { openWazeNavigation(ctx, ride.originLat, ride.originLng) },
                    modifier = Modifier.weight(1f).height(36.dp),
                    border = BorderStroke(1.dp, Color(0xFF1ABCFE))
                ) {
                    Icon(Icons.Default.Navigation, null, Modifier.size(13.dp), tint = Color(0xFF1ABCFE))
                    Spacer(Modifier.width(4.dp))
                    Text("Waze", color = Color(0xFF1ABCFE), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = { openGoogleMapsNavigation(ctx, ride.originLat, ride.originLng) },
                    modifier = Modifier.weight(1f).height(36.dp),
                    border = BorderStroke(1.dp, Color(0xFF4F46E5))
                ) {
                    Icon(Icons.Default.Map, null, Modifier.size(13.dp), tint = Color(0xFF4F46E5))
                    Spacer(Modifier.width(4.dp))
                    Text("Maps", color = Color(0xFF4F46E5), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                }
            }

            TextButton(onClick = onCancelRide, modifier = Modifier.fillMaxWidth().height(30.dp)) {
                Text("Cancelar corrida", color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun DriverArrivingRideCard(
    ride: DriverRideResponse,
    onNavigateToPassenger: () -> Unit,
    onMarkArrived: () -> Unit,
    onCancelRide: () -> Unit,
    onCallPassenger: () -> Unit,
    etaMinutes: Int? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RideStatusChip("A caminho", Color(0xFFFEF3C7), Color(0xFFD97706))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    etaMinutes?.let { eta ->
                        Surface(shape = RoundedCornerShape(100.dp), color = Color(0xFFEFF6FF)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null,
                                    modifier = Modifier.size(11.dp), tint = Color(0xFF2563EB))
                                Text("≈ ${eta}min", style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold, color = Color(0xFF2563EB))
                            }
                        }
                    }
                    if (!ride.passengerPhone.isNullOrBlank()) {
                        IconButton(onClick = onCallPassenger, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Call, contentDescription = "Ligar",
                                tint = Color(0xFFD97706), modifier = Modifier.size(17.dp))
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PassengerAvatarSmall(name = ride.passengerName, photoUrl = ride.passengerPhotoUrl)
                Column(modifier = Modifier.weight(1f)) {
                    Text(ride.passengerName ?: "Passageiro",
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    RideAddressRow(ride.originAddress, isOrigin = true)
                }
            }

            Button(
                onClick = onMarkArrived,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("Cheguei", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }

            // External navigation shortcuts
            val ctx2 = LocalContext.current
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { openWazeNavigation(ctx2, ride.originLat, ride.originLng) },
                    modifier = Modifier.weight(1f).height(36.dp),
                    border = BorderStroke(1.dp, Color(0xFF1ABCFE))
                ) {
                    Icon(Icons.Default.Navigation, null, Modifier.size(13.dp), tint = Color(0xFF1ABCFE))
                    Spacer(Modifier.width(4.dp))
                    Text("Waze", color = Color(0xFF1ABCFE), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = { openGoogleMapsNavigation(ctx2, ride.originLat, ride.originLng) },
                    modifier = Modifier.weight(1f).height(36.dp),
                    border = BorderStroke(1.dp, Color(0xFF4F46E5))
                ) {
                    Icon(Icons.Default.Map, null, Modifier.size(13.dp), tint = Color(0xFF4F46E5))
                    Spacer(Modifier.width(4.dp))
                    Text("Maps", color = Color(0xFF4F46E5), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                }
            }

            TextButton(onClick = onCancelRide, modifier = Modifier.fillMaxWidth().height(30.dp)) {
                Text("Cancelar corrida", color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun DriverArrivedRideCard(
    ride: DriverRideResponse,
    waitSeconds: Int,
    onPassengerBoarded: () -> Unit,
    onCallPassenger: () -> Unit
) {
    val waitMin = waitSeconds / 60
    val waitSec = waitSeconds % 60
    val waitLabel = if (waitMin > 0) "${waitMin}min ${waitSec}s" else "${waitSec}s"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RideStatusChip("No local", Color(0xFFF0FDF4), Color(0xFF16A34A))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Schedule, contentDescription = null,
                        modifier = Modifier.size(13.dp), tint = Color(0xFF64748B))
                    Text(waitLabel, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                    if (!ride.passengerPhone.isNullOrBlank()) {
                        IconButton(onClick = onCallPassenger, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Call, contentDescription = "Ligar",
                                tint = Color(0xFF16A34A), modifier = Modifier.size(17.dp))
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PassengerAvatarSmall(name = ride.passengerName, photoUrl = ride.passengerPhotoUrl)
                Text(ride.passengerName ?: "Passageiro",
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Button(
                onClick = onPassengerBoarded,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Passageiro embarcou", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DriverInProgressRideCard(
    ride: DriverRideResponse,
    onNavigateToDestination: () -> Unit,
    onFinishRide: () -> Unit,
    paymentMethod: String? = null,
    onReportProblem: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RideStatusChip("Em corrida", Color(0xFFEFF6FF), Color(0xFF2563EB))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    driverFmtPrice(ride.price)?.let { price ->
                        Surface(shape = RoundedCornerShape(100.dp), color = Color(0xFFF0FDF4)) {
                            Text(price, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                                color = Color(0xFF16A34A))
                        }
                    }
                    driverFmtDist(ride.distanceMeters)?.let { dist ->
                        Surface(shape = RoundedCornerShape(100.dp), color = Color(0xFFF1F5F9)) {
                            Text(dist, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall, color = Color(0xFF475569))
                        }
                    }
                    paymentMethod?.let { pm ->
                        val payBg = if (pm == "PIX") Color(0xFFEFF6FF) else Color(0xFFF0FDF4)
                        val payFg = if (pm == "PIX") Color(0xFF2563EB) else Color(0xFF16A34A)
                        Surface(shape = RoundedCornerShape(100.dp), color = payBg) {
                            Text(
                                if (pm == "PIX") "Pix" else "Dinheiro",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = payFg
                            )
                        }
                    }
                }
            }

            RideAddressRow(ride.destinationAddress, isOrigin = false)

            Button(
                onClick = onFinishRide,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("Finalizar", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }

            // External navigation shortcuts
            val ctxInProgress = LocalContext.current
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { openWazeNavigation(ctxInProgress, ride.destLat, ride.destLng) },
                    modifier = Modifier.weight(1f).height(36.dp),
                    border = BorderStroke(1.dp, Color(0xFF1ABCFE))
                ) {
                    Icon(Icons.Default.Navigation, null, Modifier.size(13.dp), tint = Color(0xFF1ABCFE))
                    Spacer(Modifier.width(4.dp))
                    Text("Waze", color = Color(0xFF1ABCFE), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = { openGoogleMapsNavigation(ctxInProgress, ride.destLat, ride.destLng) },
                    modifier = Modifier.weight(1f).height(36.dp),
                    border = BorderStroke(1.dp, Color(0xFF4F46E5))
                ) {
                    Icon(Icons.Default.Map, null, Modifier.size(13.dp), tint = Color(0xFF4F46E5))
                    Spacer(Modifier.width(4.dp))
                    Text("Maps", color = Color(0xFF4F46E5), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                }
            }

            TextButton(
                onClick = onReportProblem,
                modifier = Modifier.fillMaxWidth().height(30.dp)
            ) {
                Text(
                    "Reportar problema",
                    color = Color(0xFF64748B),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun DriverIdleOfflineCard(
    driverName: String?,
    onGoOnline: () -> Unit,
    loadingStatus: Boolean,
    earningsToday: Int = 0,
    ridesToday: Int = 0,
    billingData: BillingCyclesResponse? = null,
    billingLoading: Boolean = false,
    onShowBilling: () -> Unit = {}
) {
    val firstName = driverName?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (firstName != null) "Olá, $firstName!" else "Bem-vindo!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF94A3B8))
                        )
                        Text(
                            text = "Offline",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF64748B)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Text(
                text = "Fique online para começar a receber corridas próximas de você.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B)
            )

            DriverBillingInlineSummary(
                data = billingData,
                loading = billingLoading,
                onClick = onShowBilling
            )

            if (ridesToday > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF0FDF4),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Hoje",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF16A34A)
                        )
                        Text(
                            "R$ ${"%.2f".format(earningsToday / 100.0).replace(".", ",")} · $ridesToday ${if (ridesToday == 1) "corrida" else "corridas"}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
                        )
                    }
                }
            }

            Button(
                onClick = onGoOnline,
                enabled = !loadingStatus,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9))
            ) {
                if (loadingStatus) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Ficar Online",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun DriverBillingInlineSummary(
    data: BillingCyclesResponse?,
    loading: Boolean,
    onClick: () -> Unit
) {
    val urgentCount = data?.cycles?.count { it.status == "OVERDUE" } ?: 0
    val pendingCount = data?.pendingCount ?: 0
    val hasSent = data?.hasPendingRequest ?: false
    val currentCycle = data?.cycles?.firstOrNull { it.isCurrentWeek } ?: data?.cycles?.firstOrNull()

    val colorState = when {
        urgentCount > 0 -> Triple(Color(0xFFFEF2F2), Color(0xFFB91C1C), Icons.Default.Warning)
        pendingCount > 0 && !hasSent -> Triple(Color(0xFFFFFBEB), Color(0xFFD97706), Icons.Default.Schedule)
        hasSent -> Triple(Color(0xFFF0FDF4), Color(0xFF15803D), Icons.Default.CheckCircle)
        else -> Triple(Color(0xFFF5F3FF), Color(0xFF6D28D9), Icons.Default.Schedule)
    }
    val (backgroundColor, contentColor, icon) = colorState

    val title = when {
        loading && data == null -> "Taxa semanal"
        urgentCount > 0 -> "Taxa vencida"
        pendingCount > 0 && !hasSent -> "Taxa pendente"
        hasSent -> "Pagamento enviado"
        currentCycle != null -> "Semana atual"
        else -> "Taxa semanal"
    }

    val subtitle = when {
        loading && data == null -> "Carregando resumo..."
        urgentCount > 0 -> "$urgentCount semana${if (urgentCount == 1) "" else "s"} em atraso · toque para regularizar"
        pendingCount > 0 && !hasSent -> "$pendingCount semana${if (pendingCount == 1) "" else "s"} aguardando pagamento"
        hasSent -> "Aguardando confirmação do admin"
        currentCycle != null -> {
            val period = "${driverFmtWeekShort(currentCycle.weekStart)} – ${driverFmtWeekShort(currentCycle.weekEnd)}"
            val billingAmount = when {
                currentCycle.balanceCents == 0 && currentCycle.paidCents > 0 -> "Sem saldo pendente"
                currentCycle.paidCents > 0 -> "Restante ${driverFmtMoney(currentCycle.balanceCents)}"
                else -> "Taxa ${driverFmtMoney(currentCycle.totalFeeCents)}"
            }
            val rideLabel = "${currentCycle.rideCount} ${if (currentCycle.rideCount == 1) "corrida" else "corridas"}"
            "$period · $billingAmount · $rideLabel"
        }
        else -> "Abrir detalhes de taxas e pagamentos"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF475569),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (loading && data == null) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = contentColor,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Abrir",
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun DriverIdleOnlineCard(
    driverName: String?,
    onGoOffline: () -> Unit,
    loadingRides: Boolean,
    loadingStatus: Boolean,
    earningsToday: Int = 0,
    ridesToday: Int = 0,
    acceptedThisShift: Int = 0,
    rejectedThisShift: Int = 0,
    billingData: BillingCyclesResponse? = null,
    billingLoading: Boolean = false,
    onShowBilling: () -> Unit = {},
    isPaused: Boolean = false,
    onPause: () -> Unit = {},
    onResume: () -> Unit = {}
) {
    val statusLabel = if (isPaused) "Pausado" else "Online"
    val statusTitle = if (isPaused) "Jornada pausada" else "Procurando corridas próximas..."
    val statusDescription = if (isPaused) {
        "Você continua conectado, mas não receberá novas corridas."
    } else {
        "Você está visível para passageiros próximos."
    }
    val statusColor = if (isPaused) Color(0xFFD97706) else Color(0xFF059669)
    val statusDotColor = if (isPaused) Color(0xFFF59E0B) else Color(0xFF22C55E)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(statusDotColor)
                        )
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                    Text(
                        text = statusTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    )
                }
                if (loadingRides && !isPaused) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = Color(0xFF6D28D9),
                        strokeWidth = 2.5.dp
                    )
                }
            }

            Text(
                text = statusDescription,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B)
            )

            DriverBillingInlineSummary(
                data = billingData,
                loading = billingLoading,
                onClick = onShowBilling
            )

            if (ridesToday > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF0FDF4),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Hoje",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF16A34A)
                        )
                        Text(
                            "R$ ${"%.2f".format(earningsToday / 100.0).replace(".", ",")} · $ridesToday ${if (ridesToday == 1) "corrida" else "corridas"}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
                        )
                    }
                }
            }

            val totalOffersThisShift = acceptedThisShift + rejectedThisShift
            if (totalOffersThisShift > 0) {
                val rate = (acceptedThisShift * 100) / totalOffersThisShift
                val rateColor = when {
                    rate >= 80 -> Color(0xFF059669)
                    rate >= 50 -> Color(0xFFD97706)
                    else -> Color(0xFFDC2626)
                }
                val rateBgColor = when {
                    rate >= 80 -> Color(0xFFF0FDF4)
                    rate >= 50 -> Color(0xFFFFFBEB)
                    else -> Color(0xFFFEF2F2)
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = rateBgColor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.TrendingUp, null, tint = rateColor, modifier = Modifier.size(14.dp))
                            Text(
                                "Taxa de aceitação",
                                style = MaterialTheme.typography.labelMedium,
                                color = rateColor
                            )
                        }
                        Text(
                            "$rate% ($acceptedThisShift/$totalOffersThisShift)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = rateColor
                        )
                    }
                }
            }

            if (isPaused) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFFBEB),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(shape = RoundedCornerShape(50), color = Color(0xFFFEF3C7)) {
                            Text(
                                "Pausado",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD97706)
                            )
                        }
                        Text(
                            "Você não receberá novas corridas.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF92400E),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isPaused) {
                    Button(
                        onClick = onResume,
                        enabled = !loadingStatus,
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9))
                    ) {
                        Text(
                            "Retomar",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onPause,
                        enabled = !loadingStatus,
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFD97706))
                    ) {
                        Text(
                            "Pausar",
                            color = Color(0xFFD97706),
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                TextButton(
                    onClick = onGoOffline,
                    enabled = !loadingStatus,
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Text(
                        text = if (loadingStatus) "Aguardando..." else "Ir Offline",
                        color = Color(0xFF64748B),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun driverHaversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}

// Bearing (azimute) de `from` para `to`, em graus 0–360 (0 = Norte)
private fun driverBearingTo(from: LatLng, to: LatLng): Double {
    val lat1 = Math.toRadians(from.latitude)
    val lat2 = Math.toRadians(to.latitude)
    val dLng = Math.toRadians(to.longitude - from.longitude)
    val y = sin(dLng) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
    return (Math.toDegrees(atan2(y, x)) + 360) % 360
}

private fun driverFmtPrice(cents: Int?) =
    if (cents != null && cents > 0) "R$ %.2f".format(cents / 100.0).replace(".", ",") else null

private fun driverFmtMoney(cents: Int): String =
    "R$ ${"%.2f".format(cents / 100.0).replace(".", ",")}"

private fun driverFmtDist(meters: Int?) =
    if (meters != null && meters > 0) "%.1f km".format(meters / 1000.0).replace(".", ",") else null

private val driverBillingMonthLabels = listOf(
    "jan.", "fev.", "mar.", "abr.", "mai.", "jun.",
    "jul.", "ago.", "set.", "out.", "nov.", "dez."
)

private fun driverFmtWeekShort(iso: String): String = runCatching {
    val parts = iso.take(10).split("-")
    val day = parts[2].toInt()
    val month = driverBillingMonthLabels[parts[1].toInt() - 1]
    "$day $month"
}.getOrElse { iso.take(10) }

@Composable
private fun RideStatusChip(label: String, bg: Color, textColor: Color) {
    Surface(shape = RoundedCornerShape(100.dp), color = bg) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

@Composable
private fun PassengerAvatarSmall(name: String?, photoUrl: String?, size: Int = 34) {
    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).background(Color(0xFFEEF2FF)),
        contentAlignment = Alignment.Center
    ) {
        val fullUrl = RetrofitClient.photoUrl(photoUrl)
        if (fullUrl != null) {
            AsyncImage(
                model = fullUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size.dp).clip(CircleShape)
            )
        } else {
            Text(
                text = name?.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4F46E5)
            )
        }
    }
}

@Composable
private fun RidePassengerRow(name: String?, phone: String?, photoUrl: String? = null, onCall: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        PassengerAvatarSmall(name = name, photoUrl = photoUrl, size = 44)
        Spacer(Modifier.width(12.dp))
        Text(
            text = name ?: "Passageiro",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RideAddressRow(address: String?, isOrigin: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(if (isOrigin) CircleShape else RoundedCornerShape(2.dp))
                .background(if (isOrigin) Color(0xFF16A34A) else Color(0xFF0F172A))
        )
        Text(
            text = address ?: "Endereço não informado",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF334155),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NavOptionCard(
    icon: ImageVector,
    iconTint: Color,
    bg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }
            Icon(
                Icons.Default.ArrowForward, contentDescription = null,
                tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavigationChoiceSheet(
    onDismiss: () -> Unit,
    onGoogleMaps: () -> Unit,
    onWaze: () -> Unit,
    onInApp: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Abrir GPS externo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = "O GPS integrado já está ativo. Abra um app externo se preferir.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B)
            )
            Spacer(Modifier.height(4.dp))
            NavOptionCard(
                icon = Icons.Default.Navigation,
                iconTint = Color(0xFF2563EB),
                bg = Color(0xFFEFF6FF),
                title = "Recalcular rota integrada",
                subtitle = "Atualizar GPS do app a partir da posição atual",
                onClick = onInApp
            )
            NavOptionCard(
                icon = Icons.Default.Map,
                iconTint = Color(0xFF6D28D9),
                bg = Color(0xFFECFDF5),
                title = "Google Maps",
                subtitle = "Abrir no Google Maps",
                onClick = onGoogleMaps
            )
            NavOptionCard(
                icon = Icons.Default.Navigation,
                iconTint = Color(0xFF1ABCFE),
                bg = Color(0xFFF0F9FF),
                title = "Waze",
                subtitle = "Abrir no Waze",
                onClick = onWaze
            )
        }
    }
}

@Composable
private fun InAppNavCard(
    step: DirectionsStep,
    stepIndex: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    val navBg = Color(0xFF0D1B2A)
    val accent = Color(0xFF38BDF8)
    val progressTarget = if (totalSteps > 1) (stepIndex + 1f) / totalSteps else 1f
    val progress by animateFloatAsState(targetValue = progressTarget, animationSpec = tween(600), label = "navProgress")

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = navBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 28.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 10.dp, top = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(accent.copy(alpha = 0.13f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = navManeuverIcon(step.maneuver),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = navFormatDistance(step.distanceMeters),
                        color = accent,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                    Text(
                        text = step.instruction,
                        color = Color.White.copy(alpha = 0.92f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Passo ${stepIndex + 1} de $totalSteps",
                        color = Color.White.copy(alpha = 0.38f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Fechar",
                        tint = Color.White.copy(alpha = 0.45f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.White.copy(alpha = 0.07f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(accent)
                )
            }
        }
    }
}

private fun navManeuverIcon(maneuver: String) = when {
    "uturn" in maneuver && "right" in maneuver -> Icons.Default.UTurnRight
    "uturn" in maneuver -> Icons.Default.UTurnLeft
    "right" in maneuver -> Icons.Default.TurnRight
    "left" in maneuver -> Icons.Default.TurnLeft
    else -> Icons.Default.Navigation
}

private fun navFormatDistance(meters: Int) =
    if (meters >= 1000) "%.1f km".format(meters / 1000.0).replace(".", ",") else "$meters m"


// ─── Driver Report Problem Sheet ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DriverReportProblemSheet(
    rideId: String?,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val reasons = listOf(
        "Passageiro não compareceu",
        "Endereço de origem incorreto",
        "Endereço de destino incorreto",
        "Problema com pagamento",
        "Comportamento inadequado do passageiro",
        "Outro problema"
    )
    var selectedReason by remember { mutableStateOf<String?>(null) }
    var extraDesc by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var success by remember { mutableStateOf(false) }

    if (success) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1500)
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (success) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(shape = CircleShape, color = br.com.seunome.mobulite.ui.theme.AppGreenLight, modifier = Modifier.size(64.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CheckCircle, null, tint = br.com.seunome.mobulite.ui.theme.AppGreen, modifier = Modifier.size(32.dp))
                        }
                    }
                    Text("Relatório enviado!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Nossa equipe analisará em breve.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Text("Reportar problema", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                reasons.forEach { reason ->
                    val isSelected = reason == selectedReason
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) br.com.seunome.mobulite.ui.theme.AppPurpleLight else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, br.com.seunome.mobulite.ui.theme.AppPurple) else null
                    ) {
                        Text(
                            reason,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) br.com.seunome.mobulite.ui.theme.AppVioletDarker else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
                if (selectedReason == "Outro problema") {
                    OutlinedTextField(
                        value = extraDesc,
                        onValueChange = { extraDesc = it },
                        label = { Text("Descreva o problema") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                Button(
                    onClick = {
                        val subject = selectedReason ?: return@Button
                        val desc = if (subject == "Outro problema" && extraDesc.isNotBlank()) extraDesc else subject
                        scope.launch {
                            sending = true
                            runCatching {
                                RetrofitClient.api.createTicket(
                                    br.com.seunome.mobulite.data.remote.CreateTicketRequest(
                                        subject = subject,
                                        description = desc,
                                        type = "RIDE_CANCELLATION",
                                        rideId = rideId
                                    )
                                )
                                success = true
                            }
                            sending = false
                        }
                    },
                    enabled = selectedReason != null && !sending,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = br.com.seunome.mobulite.ui.theme.AppPurple)
                ) {
                    if (sending) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    else Text("Enviar relatório", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
