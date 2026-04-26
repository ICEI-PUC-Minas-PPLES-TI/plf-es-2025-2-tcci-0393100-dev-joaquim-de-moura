package br.com.seunome.mobulite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import br.com.seunome.mobulite.data.local.SessionStore
import br.com.seunome.mobulite.data.remote.CreateRideRequest
import br.com.seunome.mobulite.data.remote.EstimateRideRequest
import br.com.seunome.mobulite.data.remote.EstimateRideResponse
import br.com.seunome.mobulite.data.remote.LoginRequest
import br.com.seunome.mobulite.data.remote.RetrofitClient
import br.com.seunome.mobulite.ui.DriverRegisterScreen
import br.com.seunome.mobulite.ui.DriverScreen
import br.com.seunome.mobulite.ui.LoginScreen
import br.com.seunome.mobulite.ui.PassengerRegisterScreen
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import br.com.seunome.mobulite.data.remote.UpdateStatusRequest
import br.com.seunome.mobulite.ui.PassengerRideStatusCard
import br.com.seunome.mobulite.ui.PassengerRideUiState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyDkk3z1jiuVuXYqKoJJExNl6i-7acH4ujM")
        }

        RetrofitClient.init(applicationContext)

        setContent {
            MaterialTheme {
                App()
            }
        }
    }
}

private enum class AuthRoute {
    LOGIN,
    PASSENGER_REGISTER,
    DRIVER_REGISTER
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
        token = session.tokenFlow.first()
        role = session.roleFlow.first()
        RetrofitClient.setToken(token)
        loadingSession = false
    }

    if (loadingSession) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (token.isNullOrBlank()) {
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
                    },
                    onGoToPassengerRegister = {
                        authRoute = AuthRoute.PASSENGER_REGISTER
                    },
                    onGoToDriverRegister = {
                        authRoute = AuthRoute.DRIVER_REGISTER
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
        }
        return
    }

    when (role) {
        "PASSENGER" -> PassengerScreen(onLogout = logout)
        "DRIVER" -> DriverScreen(onLogout = logout)
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

@Composable
fun AdminPlaceholderScreen(onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Admin logado")
        Button(onClick = onLogout) {
            Text("Sair")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerScreen(
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()

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

    var menuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MobU") },
                actions = {
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
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            MapRideScreen(
                modifier = Modifier.weight(1f),
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
                }

            )

            LaunchedEffect(origin, destination) {
                val o = origin
                val d = destination
                if (o == null || d == null) {
                    estimate = null
                    estimateError = null
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

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Sua corrida",
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (estimating) {
                        Text("Calculando estimativa...")
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    estimateError?.let {
                        Text(
                            text = "Erro: $it",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    estimate?.let { est ->
                        val price = est.estimatedFareCents / 100.0

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Preço estimado",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = "R$ ${"%.2f".format(price)}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary
                            )

                            est.distanceMeters?.let { distance ->
                                Text(
                                    text = "Distância: %.1f km".format(distance / 1000.0),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            est.durationSeconds?.let { duration ->
                                Text(
                                    text = "Tempo estimado: ${duration / 60} min",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}