package br.com.seunome.mobulite

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import br.com.seunome.mobulite.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.seunome.mobulite.data.local.SessionStore
import br.com.seunome.mobulite.data.remote.CouponValidationResponse
import br.com.seunome.mobulite.data.remote.EstimateRideRequest
import br.com.seunome.mobulite.data.remote.EstimateRideResponse
import br.com.seunome.mobulite.data.remote.FcmTokenRequest
import br.com.seunome.mobulite.data.remote.LoginRequest
import br.com.seunome.mobulite.data.remote.RetrofitClient
import com.google.firebase.messaging.FirebaseMessaging
import br.com.seunome.mobulite.data.remote.UpdateStatusRequest
import br.com.seunome.mobulite.ui.DriverHomeScreen
import br.com.seunome.mobulite.ui.DriverRegisterScreen
import br.com.seunome.mobulite.ui.DriverScreen
import br.com.seunome.mobulite.ui.ForgotPasswordScreen
import br.com.seunome.mobulite.ui.LoginScreen
import br.com.seunome.mobulite.ui.OfflineBanner
import br.com.seunome.mobulite.ui.OnboardingScreen
import br.com.seunome.mobulite.ui.PassengerProfileScreen
import br.com.seunome.mobulite.ui.PassengerRegisterScreen
import br.com.seunome.mobulite.ui.SupportTicketScreen
import br.com.seunome.mobulite.util.NetworkStatus
import br.com.seunome.mobulite.util.observeNetwork
import androidx.compose.runtime.collectAsState
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import br.com.seunome.mobulite.worker.CancelPendingRideWorker
import br.com.seunome.mobulite.ui.ChatScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import br.com.seunome.mobulite.ui.PassengerRideUiState
import br.com.seunome.mobulite.service.PassengerRideForegroundService
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import br.com.seunome.mobulite.data.remote.DriverLocationSummary
import br.com.seunome.mobulite.ui.PassengerHomeScreen
import br.com.seunome.mobulite.ui.PassengerPaymentScreen
import br.com.seunome.mobulite.ui.ReceiptScreen
import br.com.seunome.mobulite.ui.theme.AppAmberOrange
import br.com.seunome.mobulite.ui.theme.AppGreen
import br.com.seunome.mobulite.ui.theme.AppLilac
import br.com.seunome.mobulite.ui.theme.AppLilacSoft
import br.com.seunome.mobulite.ui.theme.AppPurple
import br.com.seunome.mobulite.ui.theme.AppPurpleLight
import br.com.seunome.mobulite.ui.theme.AppRed
import br.com.seunome.mobulite.ui.theme.AppVioletDark
import br.com.seunome.mobulite.ui.theme.MobULiteTheme
import br.com.seunome.mobulite.ui.theme.Slate100
import br.com.seunome.mobulite.ui.theme.Slate200
import br.com.seunome.mobulite.ui.theme.Slate500
import br.com.seunome.mobulite.ui.theme.Slate900

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
        }

        // RetrofitClient já inicializado em MobUApplication — não duplicar aqui

        // Pedir permissão de notificações em runtime (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        setContent {
            MobULiteTheme {
                App()
            }
        }
    }
}

private enum class AuthRoute {
    LOGIN,
    PASSENGER_REGISTER,
    DRIVER_REGISTER,
    FORGOT_PASSWORD
}

@Composable
fun App() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = remember { SessionStore(context) }

    var token by remember { mutableStateOf<String?>(null) }
    var role by remember { mutableStateOf<String?>(null) }
    var loadingSession by remember { mutableStateOf(true) }
    var authRoute by remember { mutableStateOf(AuthRoute.LOGIN) }
    var showOnboarding by remember { mutableStateOf(false) }

    val networkStatus by observeNetwork(context).collectAsState(initial = NetworkStatus.Available)

    val logout: () -> Unit = {
        scope.launch {
            session.clear()
            RetrofitClient.setToken(null)
            token = null
            role = null
            authRoute = AuthRoute.LOGIN
        }
    }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        showOnboarding = !prefs.getBoolean("has_seen_onboarding", false)

        token = session.tokenFlow.first()
        role = session.roleFlow.first()
        RetrofitClient.setToken(token)

        // Send FCM token on every app start so the backend always has a current token,
        // even when the user opens the app via saved session (no explicit login).
        if (!token.isNullOrBlank()) {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
                scope.launch {
                    try {
                        RetrofitClient.authApi.saveFcmToken(FcmTokenRequest(fcmToken))
                        context.getSharedPreferences("fcm", android.content.Context.MODE_PRIVATE)
                            .edit().remove("pending_token").apply()
                    } catch (_: Exception) { }
                }
            }
        }

        // Crash-recovery: enqueue WorkManager to cancel any orphaned ride.
        // WorkManager runs even if the network is temporarily unavailable and retries gracefully.
        // The server auto-expires stale rides too, so this is a belt-and-suspenders cleanup.
        WorkManager.getInstance(context).enqueueUniqueWork(
            "cancel_pending_ride",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<CancelPendingRideWorker>().build(),
        )

        loadingSession = false
    }

    if (showOnboarding) {
        OnboardingScreen(onFinish = {
            context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                .edit().putBoolean("has_seen_onboarding", true).apply()
            showOnboarding = false
        })
        return
    }

    Box(Modifier.fillMaxSize()) {

    when {
        loadingSession -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        token.isNullOrBlank() -> {
            when (authRoute) {
                AuthRoute.LOGIN -> {
                    LoginScreen(
                        onLogin = { phone, password ->
                            val res = RetrofitClient.authApi.login(
                                LoginRequest(phone, password)
                            )

                            session.saveSession(
                                token = res.accessToken,
                                role = res.user.role,
                                userId = res.user.id
                            )

                            RetrofitClient.setToken(res.accessToken)
                            token = res.accessToken
                            role = res.user.role

                            // Registra token FCM no backend após login
                            FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
                                scope.launch {
                                    try {
                                        RetrofitClient.authApi.saveFcmToken(FcmTokenRequest(fcmToken))
                                        // Limpa token pendente já que foi enviado com sucesso
                                        context.getSharedPreferences("fcm", android.content.Context.MODE_PRIVATE)
                                            .edit().remove("pending_token").apply()
                                    } catch (_: Exception) { }
                                }
                            }
                        },
                        onGoToPassengerRegister = {
                            authRoute = AuthRoute.PASSENGER_REGISTER
                        },
                        onGoToDriverRegister = {
                            authRoute = AuthRoute.DRIVER_REGISTER
                        },
                        onForgotPassword = {
                            authRoute = AuthRoute.FORGOT_PASSWORD
                        }
                    )
                }

                AuthRoute.DRIVER_REGISTER -> {
                    DriverRegisterScreen(
                        onBackToLogin = {
                            authRoute = AuthRoute.LOGIN
                        },
                        onRegistered = {
                            authRoute = AuthRoute.LOGIN
                        }
                    )
                }

                AuthRoute.PASSENGER_REGISTER -> {
                    PassengerRegisterScreen(
                        onBackToLogin = {
                            authRoute = AuthRoute.LOGIN
                        },
                        onRegistered = {
                            authRoute = AuthRoute.LOGIN
                        }
                    )
                }

                AuthRoute.FORGOT_PASSWORD -> {
                    ForgotPasswordScreen(
                        onBack = { authRoute = AuthRoute.LOGIN }
                    )
                }
            }
        }
        else -> {
            when (role) {
                "PASSENGER" -> PassengerScreen(onLogout = logout)
                "DRIVER" -> DriverRootScreen(onLogout = logout)
                "ADMIN" -> AdminPlaceholderScreen(onLogout = logout)
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Role desconhecida: $role")
                        Button(onClick = logout) {
                            Text("Sair")
                        }
                    }
                }
            }
        }
    }

    OfflineBanner(isOffline = networkStatus == NetworkStatus.Unavailable)

    } // end outer Box
}

@Composable
fun DriverRootScreen(onLogout: () -> Unit) {
    var atHome by remember { mutableStateOf(true) }

    if (atHome) {
        DriverHomeScreen(
            onStartShift = { atHome = false },
            onLogout     = onLogout
        )
    } else {
        DriverScreen(
            onLogout = onLogout,
            onHome   = { atHome = true }
        )
    }
}

@Composable
fun AdminPlaceholderScreen(onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = AppPurpleLight,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = AppPurple
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Conta Administrativa",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Slate900
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "O painel administrativo do MobU está disponível apenas na versão web.\n\nAcesse pelo navegador em: localhost:3001",
            style = MaterialTheme.typography.bodyMedium,
            color = Slate500,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onLogout,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = AppPurple)
        ) {
            Text("Sair")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerScreen(
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = remember { SessionStore(context) }
    val density = LocalDensity.current

    // ── Navigation state ──────────────────────────────────────────────────────
    var showHome by remember { mutableStateOf(true) }
    var showProfile by remember { mutableStateOf(false) }
    var showSupport by remember { mutableStateOf(false) }
    var initialDestinationQuery by remember { mutableStateOf<String?>(null) }

    // ── Ride state ────────────────────────────────────────────────────────────
    var origin by remember { mutableStateOf<LatLng?>(null) }
    var destination by remember { mutableStateOf<LatLng?>(null) }
    var originText by remember { mutableStateOf("") }
    var destText by remember { mutableStateOf("") }
    var estimate by remember { mutableStateOf<EstimateRideResponse?>(null) }
    var estimating by remember { mutableStateOf(false) }
    var estimateError by remember { mutableStateOf<String?>(null) }
    var rideUiState by remember { mutableStateOf(PassengerRideUiState.IDLE) }
    var currentRideId by remember { mutableStateOf<String?>(null) }
    var acceptedDriverName by remember { mutableStateOf<String?>(null) }
    var rideError by remember { mutableStateOf<String?>(null) }
    var showPaymentScreen by remember { mutableStateOf(false) }
    var showReceiptScreen by remember { mutableStateOf(false) }
    var receiptRideId by remember { mutableStateOf<String?>(null) }
    var driverInfo by remember { mutableStateOf<DriverLocationSummary?>(null) }
    var driverEtaMinutes by remember { mutableStateOf<Int?>(null) }
    var passPaymentMethod by remember { mutableStateOf("CASH") }
    var appliedCoupon by remember { mutableStateOf<CouponValidationResponse?>(null) }
    var passengerChatRideId by remember { mutableStateOf<String?>(null) }
    var passengerChatPartnerName by remember { mutableStateOf("Motorista") }

    // ── Dynamic bottom panel height for map padding ───────────────────────────
    var bottomPanelHeightPx by remember { mutableStateOf(0) }

    // ── Snapshots for DisposableEffect ────────────────────────────────────────
    val latestRideId = rememberUpdatedState(currentRideId)
    val latestRideUiState = rememberUpdatedState(rideUiState)

    // If an active ride is restored, go directly to the map
    LaunchedEffect(currentRideId) {
        if (currentRideId != null) showHome = false
    }

    // Cancel the active ride when PassengerScreen leaves composition
    DisposableEffect(Unit) {
        onDispose {
            val id = latestRideId.value
            val state = latestRideUiState.value
            val cancelable = id != null && state in listOf(
                PassengerRideUiState.REQUESTING,
                PassengerRideUiState.SEARCHING_DRIVER
            )
            if (cancelable) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        RetrofitClient.api.updateRideStatus(
                            rideId = id!!,
                            body = UpdateStatusRequest(status = "CANCELED")
                        )
                    } catch (_: Exception) { }
                }
            }
        }
    }

    // Restore active ride on crash recovery
    LaunchedEffect(Unit) {
        val pendingId = session.pendingRideIdFlow.first() ?: return@LaunchedEffect
        try {
            val ride = RetrofitClient.api.getRideById(pendingId)
            when (ride.status) {
                "PENDING_DRIVER" -> {
                    currentRideId = pendingId
                    rideUiState = PassengerRideUiState.SEARCHING_DRIVER
                    PassengerRideForegroundService.start(context, pendingId)
                }
                "ACCEPTED" -> {
                    currentRideId = pendingId
                    acceptedDriverName = ride.driver?.name
                    rideUiState = PassengerRideUiState.DRIVER_ACCEPTED
                    PassengerRideForegroundService.start(context, pendingId)
                }
                "DRIVER_ARRIVING" -> {
                    currentRideId = pendingId
                    acceptedDriverName = ride.driver?.name
                    rideUiState = PassengerRideUiState.DRIVER_ARRIVING
                    PassengerRideForegroundService.start(context, pendingId)
                }
                "DRIVER_ARRIVED" -> {
                    currentRideId = pendingId
                    acceptedDriverName = ride.driver?.name
                    rideUiState = PassengerRideUiState.DRIVER_ARRIVED
                    PassengerRideForegroundService.start(context, pendingId)
                }
                "IN_PROGRESS" -> {
                    currentRideId = pendingId
                    acceptedDriverName = ride.driver?.name
                    rideUiState = PassengerRideUiState.IN_PROGRESS
                    PassengerRideForegroundService.start(context, pendingId)
                }
                "FINISHED" -> {
                    receiptRideId = pendingId
                    showPaymentScreen = true
                    session.savePendingRideId(null)
                }
                else -> session.savePendingRideId(null)
            }
        } catch (_: Exception) {
            session.savePendingRideId(null)
        }
    }

    // ── Screen routing ────────────────────────────────────────────────────────

    if (showSupport) {
        SupportTicketScreen(onBack = { showSupport = false })
        return
    }

    if (showProfile) {
        PassengerProfileScreen(
            onBack = { showProfile = false },
            onSupport = { showProfile = false; showSupport = true },
            onLogout = { showProfile = false; onLogout() }
        )
        return
    }

    // ── Map screen + overlays (home, payment, receipt) ───────────────────────
    // MapRideScreen stays in composition at all times so the map never reloads.
    // Home / payment / receipt are shown as full-screen AnimatedVisibility overlays.
    Box(Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Image(
                        painter = painterResource(id = R.drawable.logo1),
                        contentDescription = "MobU",
                        modifier = Modifier.height(30.dp),
                        contentScale = ContentScale.Fit
                    )
                },
                navigationIcon = {
                    if (rideUiState == PassengerRideUiState.IDLE) {
                        IconButton(onClick = {
                            initialDestinationQuery = null
                            destination = null
                            estimate = null
                            estimateError = null
                            showHome = true
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                                tint = AppPurple
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showProfile = true }) {
                        Surface(
                            shape = CircleShape,
                            color = AppPurpleLight,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Meu perfil",
                                    modifier = Modifier.size(18.dp),
                                    tint = AppPurple
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            MapRideScreen(
                modifier = Modifier.fillMaxSize(),
                mapBottomPadding = with(density) { bottomPanelHeightPx.toDp() }.coerceAtLeast(0.dp),
                encodedPolyline = estimate?.encodedPolyline,
                initialDestinationQuery = initialDestinationQuery,
                initialRideUiState = rideUiState,
                initialRideId = currentRideId,
                onPointsChanged = { o, d, oText, dText ->
                    origin = o
                    destination = d
                    originText = oText
                    destText = dText
                },
                onRideStateChanged = { uiState, rideId, driverName, errorMessage ->
                    rideUiState = uiState
                    currentRideId = rideId
                    acceptedDriverName = driverName
                    rideError = errorMessage
                    val idToSave = when (uiState) {
                        PassengerRideUiState.REQUESTING,
                        PassengerRideUiState.SEARCHING_DRIVER,
                        PassengerRideUiState.DRIVER_ACCEPTED,
                        PassengerRideUiState.DRIVER_ARRIVING,
                        PassengerRideUiState.DRIVER_ARRIVED,
                        PassengerRideUiState.IN_PROGRESS -> rideId
                        else -> null
                    }
                    scope.launch { session.savePendingRideId(idToSave) }
                    if (uiState == PassengerRideUiState.FINISHED && rideId != null) {
                        receiptRideId = rideId
                        showPaymentScreen = true
                    }
                },
                onDriverUpdate = { info, eta, payment ->
                    driverInfo = info
                    driverEtaMinutes = eta
                    passPaymentMethod = payment
                },
                onOpenChat = { rideId, partnerName ->
                    passengerChatRideId = rideId
                    passengerChatPartnerName = partnerName
                },
                chatIsOpen = (passengerChatRideId != null),
                onCouponChanged = { appliedCoupon = it }
            )

            LaunchedEffect(origin, destination) {
                appliedCoupon = null
                val o = origin
                val d = destination
                if (o == null || d == null || currentRideId != null) {
                    if (d == null) { estimate = null; estimateError = null }
                    return@LaunchedEffect
                }
                estimating = true
                estimateError = null
                estimate = try {
                    RetrofitClient.api.estimateRide(
                        EstimateRideRequest(
                            originLat = o.latitude,
                            originLng = o.longitude,
                            destLat = d.latitude,
                            destLng = d.longitude
                        )
                    )
                } catch (e: Exception) {
                    estimateError = e.message
                    null
                } finally {
                    estimating = false
                }
            }

            if (passengerChatRideId == null) {
                PassengerRideBottomPanel(
                    rideUiState = rideUiState,
                    estimate = estimate,
                    estimating = estimating,
                    estimateError = estimateError,
                    appliedCoupon = appliedCoupon,
                    driverInfo = driverInfo,
                    driverName = acceptedDriverName,
                    etaMinutes = driverEtaMinutes,
                    paymentMethod = passPaymentMethod,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onGloballyPositioned { bottomPanelHeightPx = it.size.height }
                )
            }
        }
    }

    passengerChatRideId?.let { rideId ->
        ChatScreen(
            rideId = rideId,
            myRole = "PASSENGER",
            partnerName = passengerChatPartnerName,
            isDriverSide = false,
            onBack = { passengerChatRideId = null }
        )
    }

    // Home overlay — covers the map without unmounting it
    AnimatedVisibility(
        visible = showHome,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(180))
    ) {
        PassengerHomeScreen(
            onRequestRide = { addr ->
                initialDestinationQuery = addr
                showHome = false
            },
            onGoToProfile = { showProfile = true }
        )
    }

    // Payment overlay — slides up from bottom, covers the map
    AnimatedVisibility(
        visible = showPaymentScreen,
        enter = slideInVertically { it } + fadeIn(tween(300)),
        exit = slideOutVertically { it } + fadeOut(tween(250))
    ) {
        Box(Modifier.fillMaxSize()) {
            PassengerPaymentScreen(
                rideId = receiptRideId ?: "",
                onContinue = {
                    showPaymentScreen = false
                    showReceiptScreen = true
                }
            )
        }
    }

    // Rating overlay — slides up from bottom, covers the map
    AnimatedVisibility(
        visible = showReceiptScreen,
        enter = slideInVertically { it } + fadeIn(tween(300)),
        exit = slideOutVertically { it } + fadeOut(tween(250))
    ) {
        Box(Modifier.fillMaxSize()) {
            ReceiptScreen(
                rideId = receiptRideId ?: "",
                driverName = acceptedDriverName,
                onDone = {
                    showPaymentScreen = false
                    showReceiptScreen = false
                    receiptRideId = null
                    rideUiState = PassengerRideUiState.IDLE
                    currentRideId = null
                    acceptedDriverName = null
                    rideError = null
                    estimate = null
                    // Delay returning to home so exit animation can complete
                    scope.launch {
                        delay(350)
                        showHome = true
                    }
                }
            )
        }
    }
} // end outer Box
}

// ── Bottom panel: switches between estimate, searching, active ride, done ──────

@Composable
private fun PassengerRideBottomPanel(
    rideUiState: PassengerRideUiState,
    estimate: EstimateRideResponse?,
    estimating: Boolean,
    estimateError: String?,
    appliedCoupon: CouponValidationResponse? = null,
    driverInfo: DriverLocationSummary?,
    driverName: String?,
    etaMinutes: Int?,
    paymentMethod: String,
    modifier: Modifier = Modifier
) {
    val mode = when (rideUiState) {
        PassengerRideUiState.IDLE -> "estimate"
        PassengerRideUiState.REQUESTING,
        PassengerRideUiState.SEARCHING_DRIVER -> "searching"
        PassengerRideUiState.DRIVER_ACCEPTED,
        PassengerRideUiState.DRIVER_ARRIVING,
        PassengerRideUiState.DRIVER_ARRIVED,
        PassengerRideUiState.IN_PROGRESS -> "active"
        else -> "done"
    }

    AnimatedContent(
        targetState = mode,
        transitionSpec = {
            (slideInVertically { it / 3 } + fadeIn()) togetherWith
                    (slideOutVertically { -it / 3 } + fadeOut())
        },
        label = "ride_panel",
        modifier = modifier.fillMaxWidth()
    ) { currentMode ->
        when (currentMode) {
            // Estimate / idle: sem card — conteúdo flutua diretamente sobre o mapa
            "estimate" -> {
                PanelEstimate(
                    estimate = estimate,
                    estimating = estimating,
                    estimateError = estimateError,
                    appliedCoupon = appliedCoupon,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                )
            }
            // Buscando / ativo / finalizado: card branco com elevação
            else -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    when (currentMode) {
                        "searching" -> PanelSearching()
                        "active"    -> PanelActive(rideUiState, driverInfo, driverName, etaMinutes, paymentMethod)
                        else        -> PanelDone(rideUiState)
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelSearching() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            strokeWidth = 2.5.dp,
            color = AppPurple
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                "Buscando motorista",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Slate900
            )
            Text(
                "Procurando o motorista mais próximo...",
                style = MaterialTheme.typography.bodySmall,
                color = Slate500
            )
        }
    }
}

@Composable
private fun PanelActive(
    rideUiState: PassengerRideUiState,
    driverInfo: DriverLocationSummary?,
    driverName: String?,
    etaMinutes: Int?,
    paymentMethod: String
) {
    val ctx = LocalContext.current
    val activeStep = when (rideUiState) {
        PassengerRideUiState.DRIVER_ACCEPTED -> 0
        PassengerRideUiState.DRIVER_ARRIVING -> 1
        PassengerRideUiState.DRIVER_ARRIVED  -> 2
        PassengerRideUiState.IN_PROGRESS     -> 3
        else -> 0
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        RideStepStrip(activeStep = activeStep)
        HorizontalDivider(color = Slate200)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(AppPurple),
                contentAlignment = Alignment.Center
            ) {
                val driverPhotoUrl = RetrofitClient.photoUrl(driverInfo?.photoUrl)
                if (driverPhotoUrl != null) {
                    AsyncImage(
                        model = driverPhotoUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(44.dp).clip(CircleShape)
                    )
                } else {
                    Text(
                        (driverInfo?.name ?: driverName)?.firstOrNull()?.uppercase() ?: "M",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    driverInfo?.name ?: driverName ?: "Motorista",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate900
                )
                val vehicle = driverInfo?.let {
                    listOfNotNull(
                        it.vehicleModel?.takeIf { v -> v.isNotBlank() },
                        it.vehicleColor?.takeIf { v -> v.isNotBlank() },
                        it.vehiclePlate?.takeIf { v -> v.isNotBlank() }
                    ).joinToString(" · ")
                } ?: ""
                if (vehicle.isNotBlank()) {
                    Text(vehicle, style = MaterialTheme.typography.bodySmall, color = Slate500)
                }
                if (driverInfo?.averageRating != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(Icons.Default.Star, null, Modifier.size(11.dp), tint = Color(0xFFFFC107))
                        Text(
                            "%.1f".format(driverInfo.averageRating).replace(".", ","),
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (etaMinutes != null && (rideUiState == PassengerRideUiState.DRIVER_ACCEPTED ||
                            rideUiState == PassengerRideUiState.DRIVER_ARRIVING)) {
                    Surface(shape = RoundedCornerShape(50), color = Slate100) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AccessTime, null, Modifier.size(11.dp), tint = AppAmberOrange)
                            Text(
                                "~$etaMinutes min",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppAmberOrange,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Surface(shape = RoundedCornerShape(50), color = AppPurpleLight) {
                    Text(
                        if (paymentMethod == "PIX") "Pix" else "Dinheiro",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppVioletDark,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (!driverInfo?.phone.isNullOrBlank()) {
                IconButton(
                    onClick = {
                        ctx.startActivity(
                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:${driverInfo!!.phone}"))
                        )
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = AppLilac,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = "Ligar para motorista",
                                modifier = Modifier.size(17.dp),
                                tint = AppPurple
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelDone(rideUiState: PassengerRideUiState) {
    val finished = rideUiState == PassengerRideUiState.FINISHED
    val tint = if (finished) AppGreen else AppRed
    val icon = if (finished) Icons.Default.CheckCircle else Icons.Default.Cancel
    val title = if (finished) "Corrida finalizada!" else "Corrida cancelada"
    val subtitle = if (finished) "Obrigado por usar o MobU" else "Sua solicitação foi cancelada"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(shape = CircleShape, color = tint.copy(alpha = 0.1f), modifier = Modifier.size(44.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(24.dp), tint = tint)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = tint)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Slate500)
        }
    }
}

@Composable
private fun PanelEstimate(
    estimate: EstimateRideResponse?,
    estimating: Boolean,
    estimateError: String?,
    appliedCoupon: CouponValidationResponse? = null,
    modifier: Modifier = Modifier
) {
    // Só renderiza quando há conteúdo útil para mostrar
    val hasContent = estimating || estimateError != null || estimate != null
    if (!hasContent) return

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.96f),
        shadowElevation = 12.dp,
        tonalElevation = 0.dp
    ) {
        when {
            estimating -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = AppPurple
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Calculando preço...", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Slate900)
                        Text("Aguarde um momento", style = MaterialTheme.typography.bodySmall, color = Slate500)
                    }
                }
            }
            estimateError != null -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(shape = CircleShape, color = AppRed.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Warning, null, tint = AppRed, modifier = Modifier.size(20.dp))
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Não foi possível calcular", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Slate900)
                        Text(estimateError, style = MaterialTheme.typography.bodySmall, color = AppRed, maxLines = 2)
                    }
                }
            }
            estimate != null -> {
                val originalCents = estimate.estimatedFareCents
                val discountedCents: Int? = when {
                    appliedCoupon?.discountPercent != null ->
                        (originalCents * (1.0 - appliedCoupon.discountPercent / 100.0)).toInt().coerceAtLeast(0)
                    appliedCoupon?.discountCents != null ->
                        (originalCents - appliedCoupon.discountCents).coerceAtLeast(0)
                    else -> null
                }
                val displayPrice = (discountedCents ?: originalCents) / 100.0
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "Preço estimado",
                                style = MaterialTheme.typography.labelMedium,
                                color = Slate500
                            )
                            if (discountedCents != null) {
                                Text(
                                    "R$ ${"%.2f".format(originalCents / 100.0).replace(".", ",")}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Normal,
                                    color = Slate500,
                                    textDecoration = TextDecoration.LineThrough
                                )
                            }
                            Text(
                                "R$ ${"%.2f".format(displayPrice).replace(".", ",")}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (discountedCents != null) AppGreen else AppPurple
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(shape = RoundedCornerShape(50), color = AppLilacSoft) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.DirectionsCar, null, Modifier.size(12.dp), tint = AppVioletDark)
                                    Text("%.1f km".format(estimate.distanceMeters / 1000.0).replace(".", ","), style = MaterialTheme.typography.labelMedium, color = AppVioletDark)
                                }
                            }
                            Surface(shape = RoundedCornerShape(50), color = AppLilacSoft) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AccessTime, null, Modifier.size(12.dp), tint = AppVioletDark)
                                    Text("${estimate.durationSeconds / 60} min", style = MaterialTheme.typography.labelMedium, color = AppVioletDark)
                                }
                            }
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun RideStepStrip(activeStep: Int) {
    val steps = listOf("Aceito", "A caminho", "Chegou", "Em corrida")
    val muted = Slate200

    Column(modifier = Modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
        ) {
            val w = constraints.maxWidth.toFloat()
            val n = steps.size
            val cellW = w / n
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cy = size.height / 2
                val dotXs = (0 until n).map { i -> (i + 0.5f) * cellW }
                drawLine(muted, Offset(dotXs.first(), cy), Offset(dotXs.last(), cy), strokeWidth = 2.dp.toPx())
                if (activeStep > 0) {
                    drawLine(AppPurple, Offset(dotXs.first(), cy), Offset(dotXs[activeStep], cy), strokeWidth = 2.dp.toPx())
                }
                dotXs.forEachIndexed { i, x ->
                    val filled = i <= activeStep
                    val r = if (i == activeStep) 5.5.dp.toPx() else 4.dp.toPx()
                    drawCircle(if (filled) AppPurple else muted, r, Offset(x, cy))
                    if (filled && i < activeStep) {
                        drawCircle(Color.White, 2.dp.toPx(), Offset(x, cy))
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            steps.forEachIndexed { i, label ->
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (i <= activeStep) AppPurple else Slate500,
                    fontWeight = if (i == activeStep) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    }
}
