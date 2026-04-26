package br.com.seunome.mobulite.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import br.com.seunome.mobulite.data.remote.DriverRideResponse
import br.com.seunome.mobulite.data.remote.DriverStatusRequest
import br.com.seunome.mobulite.data.remote.RetrofitClient
import br.com.seunome.mobulite.data.remote.RideRequestItem
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.HttpException
import androidx.compose.material3.Switch
import androidx.compose.material.icons.filled.MyLocation
import android.media.MediaPlayer
import android.os.VibrationEffect
import android.os.Vibrator
import android.content.Context
import br.com.seunome.mobulite.R

@SuppressLint("MissingPermission")
@Composable
fun DriverScreen(
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var isOnline by remember { mutableStateOf(false) }
    var loadingStatus by remember { mutableStateOf(false) }
    var loadingRides by remember { mutableStateOf(false) }
    var loadingProfile by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }

    var pendingRides by remember { mutableStateOf<List<RideRequestItem>>(emptyList()) }
    var driverStatus by remember { mutableStateOf<String?>(null) }
    var rejectionReason by remember { mutableStateOf<String?>(null) }

    var lastNotifiedRideId by remember { mutableStateOf<String?>(null) }

    var currentRide by remember { mutableStateOf<DriverRideResponse?>(null) }
    var paymentRide by remember { mutableStateOf<DriverRideResponse?>(null) }
    var finishedRide by remember { mutableStateOf<DriverRideResponse?>(null) }
    var finishedQrPayload by remember { mutableStateOf<String?>(null) }

    var driverLocation by remember { mutableStateOf<LatLng?>(null) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var finishedPaymentMethod by remember { mutableStateOf<String?>(null) }

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
            driverStatus = profile.approvalStatus
            rejectionReason = profile.rejectionReason
            isOnline = profile.online
        } catch (e: Exception) {
            message = "Erro ao carregar dados do motorista: ${e.message}"
        } finally {
            loadingProfile = false
        }
    }

    suspend fun loadCurrentRide() {
        if (finishedQrPayload != null) return

        try {
            currentRide = RetrofitClient.driverApi.getCurrentRide()
        } catch (e: Exception) {
            message = "Erro ao buscar corrida atual: ${e.message}"
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
        if (!isOnline || driverStatus != "APPROVED" || currentRide != null) {
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


    fun goOnline() {
        scope.launch {
            loadingStatus = true
            try {
                val response = RetrofitClient.driverApi.updateStatus(
                    DriverStatusRequest(true)
                )
                isOnline = response.online
                message = response.message ?: "Motorista online"
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
                pendingRides = emptyList()
                currentRide = null
                paymentRide = null
                message = response.message ?: "Motorista offline"
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
    }

    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) return@LaunchedEffect

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            3000L
        ).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val latLng = LatLng(loc.latitude, loc.longitude)
                driverLocation = latLng
                cameraPositionState.move(
                    CameraUpdateFactory.newLatLngZoom(latLng, 16f)
                )
            }
        }

        fusedClient.requestLocationUpdates(
            request,
            callback,
            Looper.getMainLooper()
        )
    }

    LaunchedEffect(isOnline, driverStatus, currentRide?.rideId, finishedQrPayload) {
        if (isOnline && driverStatus == "APPROVED") {
            while (true) {
                if (finishedQrPayload != null) break

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

    Box(
        modifier = Modifier.fillMaxSize()
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
            )
        ) {
            currentRide?.let { ride ->
                Marker(
                    state = MarkerState(
                        position = LatLng(ride.originLat, ride.originLng)
                    ),
                    title = "Passageiro"
                )

                Marker(
                    state = MarkerState(
                        position = LatLng(ride.destLat, ride.destLng)
                    ),
                    title = "Destino"
                )
            }
        }

        DriverTopFloatingBar(
            isOnline = isOnline,
            loadingStatus = loadingStatus,
            onGoOnline = { goOnline() },
            onGoOffline = { goOffline() },
            onLogout = onLogout
        )

        IconButton(
            onClick = {
                driverLocation?.let {
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(it, 16f)
                        )
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 82.dp, end = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Minha localização"
            )
        }

        DriverBottomContent(
            isOnline = isOnline,
            loadingRides = loadingRides,
            pendingRides = pendingRides,
            currentRide = currentRide,
            paymentRide = paymentRide,
            finishedRide = finishedRide,
            finishedQrPayload = finishedQrPayload,
            onAccept = { ride ->
                scope.launch {
                    try {
                        stopRideAlert()
                        RetrofitClient.driverApi.acceptRide(ride.rideId)
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
                        message = "Corrida recusada."
                        loadPendingRides()
                    } catch (e: Exception) {
                        message = "Erro ao recusar corrida: ${e.message}"
                    }
                }
            },
            onNavigateToPassenger = {
                currentRide?.let {
                    openGoogleMapsNavigation(
                        context = context,
                        lat = it.originLat,
                        lng = it.originLng
                    )
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
                    openGoogleMapsNavigation(
                        context = context,
                        lat = it.destLat,
                        lng = it.destLng
                    )
                }
            },
            onGoToPayment = {
                paymentRide = currentRide
            },
            onPixPaid = {
                scope.launch {
                    try {
                        val response = RetrofitClient.driverApi.finishRide(paymentRide!!.rideId)
                        finishedRide = response
                        finishedQrPayload = response.driverPixQrPayload ?: "QR_PIX_NAO_CADASTRADO"
                        finishedPaymentMethod = "PIX"
                        currentRide = null
                        paymentRide = null
                        pendingRides = emptyList()
                        message = "Pagamento via Pix confirmado. Corrida finalizada."
                    } catch (e: Exception) {
                        message = "Erro ao finalizar corrida: ${e.message}"
                    }
                }
            },
            onCashPaid = {
                scope.launch {
                    try {
                        val response = RetrofitClient.driverApi.finishRide(paymentRide!!.rideId)
                        finishedRide = response
                        finishedQrPayload = response.driverPixQrPayload ?: "QR_PIX_NAO_CADASTRADO"
                        finishedPaymentMethod = "DINHEIRO"
                        currentRide = null
                        paymentRide = null
                        pendingRides = emptyList()
                        message = "Pagamento em dinheiro confirmado. Corrida finalizada."
                    } catch (e: Exception) {
                        message = "Erro ao finalizar corrida: ${e.message}"
                    }
                }
            },
            onBackPayment = {
                paymentRide = null
            },
            finishedPaymentMethod = finishedPaymentMethod,
            onCloseReceipt = {
                finishedRide = null
                finishedQrPayload = null
                finishedPaymentMethod = null
                message = null
            }
        )
    }
}

@Composable
fun DriverTopFloatingBar(
    isOnline: Boolean,
    loadingStatus: Boolean,
    onGoOnline: () -> Unit,
    onGoOffline: () -> Unit,
    onLogout: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            shape = RoundedCornerShape(50),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isOnline) "Online" else "Offline",
                    style = MaterialTheme.typography.bodyMedium
                )

                Switch(
                    checked = isOnline,
                    enabled = !loadingStatus,
                    onCheckedChange = { checked ->
                        if (checked) {
                            onGoOnline()
                        } else {
                            onGoOffline()
                        }
                    }
                )
            }
        }

        Card(
            shape = RoundedCornerShape(50),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu"
                    )
                }

                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Sair") },
                        onClick = {
                            menuOpen = false
                            onLogout()
                        }
                    )
                }
            }
        }
    }
}


@Composable
fun DriverBottomContent(
    isOnline: Boolean,
    loadingRides: Boolean,
    pendingRides: List<RideRequestItem>,
    currentRide: DriverRideResponse?,
    paymentRide: DriverRideResponse?,
    finishedRide: DriverRideResponse?,
    finishedQrPayload: String?,
    onAccept: (RideRequestItem) -> Unit,
    onReject: (RideRequestItem) -> Unit,
    onNavigateToPassenger: () -> Unit,
    onPassengerBoarded: () -> Unit,
    onNavigateToDestination: () -> Unit,
    onGoToPayment: () -> Unit,
    onPixPaid: () -> Unit,
    onCashPaid: () -> Unit,
    onBackPayment: () -> Unit,
    finishedPaymentMethod: String?,
    onCloseReceipt: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            when {
                paymentRide != null -> {
                    DriverPaymentChoiceCard(
                        ride = paymentRide,
                        qrPayload = paymentRide.driverPixQrPayload ?: "QR_PIX_NAO_CADASTRADO",
                        onPixPaid = onPixPaid,
                        onCashPaid = onCashPaid,
                        onBack = onBackPayment
                    )
                }

                finishedQrPayload != null && finishedRide != null -> {
                    DriverPaymentQrCard(
                        ride = finishedRide,
                        qrPayload = finishedQrPayload,
                        paymentMethod = finishedPaymentMethod ?: "Não informado",
                        onClose = onCloseReceipt
                    )
                }

                currentRide != null && currentRide.status == "ACCEPTED" -> {
                    DriverAcceptedRideCard(
                        ride = currentRide,
                        onNavigateToPassenger = onNavigateToPassenger,
                        onPassengerBoarded = onPassengerBoarded
                    )
                }

                currentRide != null && currentRide.status == "IN_PROGRESS" -> {
                    DriverInProgressRideCard(
                        ride = currentRide,
                        onNavigateToDestination = onNavigateToDestination,
                        onFinishRide = onGoToPayment
                    )
                }

                isOnline && pendingRides.isNotEmpty() -> {
                    val ride = pendingRides.first()
                    DriverPendingRideFloatingCard(
                        ride = ride,
                        onAccept = { onAccept(ride) },
                        onReject = { onReject(ride) }
                    )
                }

                isOnline && loadingRides -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Buscando corridas próximas...")
                        }
                    }
                }

                isOnline -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            text = "Você está online. Aguardando corridas próximas...",
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }

                else -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            text = "Você está offline. Fique online para receber corridas.",
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DriverPendingRideFloatingCard(
    ride: RideRequestItem,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val price = (ride.price ?: 0.0) / 100.0
            val distance = (ride.distanceMeters ?: 0.0) / 1000.0

            Text(
                text = "Nova corrida",
                style = MaterialTheme.typography.titleLarge
            )

            Text("Passageiro: ${ride.passengerName}")
            Text("Origem: ${ride.originAddress ?: "Origem não informada"}")
            Text("Destino: ${ride.destinationAddress ?: "Destino não informado"}")

            Text(
                text = "R$ %.2f • %.1f km".format(price, distance),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Recusar")
                }

                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Aceitar")
                }
            }
        }
    }
}
@Composable
fun DriverAcceptedRideCard(
    ride: DriverRideResponse,
    onNavigateToPassenger: () -> Unit,
    onPassengerBoarded: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Corrida aceita", style = MaterialTheme.typography.titleLarge)
            Text("Passageiro: ${ride.passengerName ?: "Sem nome"}")
            Text("Origem: ${ride.originAddress ?: "${ride.originLat}, ${ride.originLng}"}")

            Button(
                onClick = onNavigateToPassenger,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Navegar até passageiro")
            }

            Button(
                onClick = onPassengerBoarded,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Passageiro embarcou")
            }
        }
    }
}

@Composable
fun DriverInProgressRideCard(
    ride: DriverRideResponse,
    onNavigateToDestination: () -> Unit,
    onFinishRide: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Corrida em andamento", style = MaterialTheme.typography.titleLarge)
            Text("Destino: ${ride.destinationAddress ?: "${ride.destLat}, ${ride.destLng}"}")

            Button(
                onClick = onNavigateToDestination,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Navegar até destino")
            }

            Button(
                onClick = onFinishRide,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ir para pagamento")
            }
        }
    }
}