package br.com.seunome.mobulite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.seunome.mobulite.data.remote.*
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch
import com.google.android.libraries.places.api.Places
import br.com.seunome.mobulite.data.local.SessionStore
import br.com.seunome.mobulite.data.remote.LoginRequest
import br.com.seunome.mobulite.ui.LoginScreen
import kotlinx.coroutines.flow.first
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyDkk3z1jiuVuXYqKoJJExNl6i-7acH4ujM")
        }
        RetrofitClient.init(applicationContext)
        setContent { MaterialTheme { App() } }
    }
}

@Composable
fun App() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = remember { SessionStore(context) }

    // estado da sessão
    var token by remember { mutableStateOf<String?>(null) }
    var role by remember { mutableStateOf<String?>(null) }
    var loadingSession by remember { mutableStateOf(true) }

    // ✅ logout só depois que token/role existem
    val logout: () -> Unit = {
        scope.launch {
            session.clear()
            RetrofitClient.setToken(null)
            token = null
            role = null
        }
    }

    LaunchedEffect(Unit) {
        token = session.tokenFlow.first()
        role = session.roleFlow.first()
        loadingSession = false
        RetrofitClient.setToken(token)
    }

    if (loadingSession) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (token.isNullOrBlank()) {
        LoginScreen { phone, password ->
            val res = RetrofitClient.authApi.login(LoginRequest(phone, password))

            session.saveSession(
                token = res.accessToken,
                role = res.user.role,
                userId = res.user.id
            )

            RetrofitClient.setToken(res.accessToken)
            token = res.accessToken
            role = res.user.role
        }
        return
    }

    when (role) {
        "PASSENGER" -> PassengerScreen(onLogout = logout)
        "DRIVER" -> DriverScreen(onLogout = logout)
        "ADMIN" -> AdminPlaceholderScreen(onLogout = logout)
        else -> Text("Role desconhecida: $role")
    }
}

@Composable
fun AdminPlaceholderScreen(onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Admin logado")
        Button(onClick = onLogout) { Text("Sair") }
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

    var creating by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }

    // menu ⋮
    var menuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Passageiro") },
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

            // ✅ MAPA ocupa o resto da tela
            MapRideScreen(
                modifier = Modifier.weight(1f),
            ) { o, d, oText, dText ->
                origin = o
                destination = d
                originText = oText
                destText = dText
            }

            // ✅ Sempre que tiver origem+destino → chama /estimate
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

            // ✅ Painel fixo embaixo
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    // (opcional) mostrar endereços


                    if (estimating) Text("Calculando preço...")
                    estimateError?.let { Text("Erro: $it") }

                    estimate?.let { est ->
                        val price = est.estimatedFareCents / 100.0
                        Text("Estimativa: R$ ${"%.2f".format(price)}")
                    }

                    Button(
                        enabled = origin != null && destination != null && estimate != null && !creating,
                        onClick = {
                            val o = origin ?: return@Button
                            val d = destination ?: return@Button

                            scope.launch {
                                creating = true
                                msg = try {
                                    val ride = RetrofitClient.api.createRide(
                                        CreateRideRequest(
                                            passengerId = "passenger_1", // depois vamos trocar pelo userId do SessionStore
                                            originLat = o.latitude,
                                            originLng = o.longitude,
                                            destLat = d.latitude,
                                            destLng = d.longitude
                                        )
                                    )
                                    "Corrida criada: ${ride.id}"
                                } catch (e: Exception) {
                                    "Erro ao criar: ${e.message}"
                                } finally {
                                    creating = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(if (creating) "Solicitando..." else "Solicitar corrida")
                    }

                    if (msg.isNotBlank()) Text(msg)
                }
            }
        }
    }
}

@Composable
fun DriverScreen(onLogout: () -> Unit) {
    val scope = rememberCoroutineScope()
    var rides by remember { mutableStateOf<List<Ride>>(emptyList()) }
    var msg by remember { mutableStateOf("") }

    fun reload() {
        scope.launch {
            rides = try {
                RetrofitClient.api.getOpenRides()
            } catch (e: Exception) {
                msg = "Erro: ${e.message}"
                emptyList()
            }
        }
    }

    LaunchedEffect(Unit) { reload() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = { reload() }) { Text("Atualizar") }

            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Sair")
            }
        }

        if (msg.isNotBlank()) Text(msg)

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(rides) { ride ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Origem: ${ride.originLat}, ${ride.originLng}")
                        Text("Destino: ${ride.destLat}, ${ride.destLng}")
                        Text("Status: ${ride.status}")

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        RetrofitClient.api.acceptRide(
                                            id = ride.id,
                                            body = AcceptRideRequest(driverId = "driver_1")
                                        )
                                        reload()
                                    } catch (e: Exception) {
                                        msg = "Erro: ${e.message}"
                                    }
                                }
                            }
                        ) {
                            Text("Aceitar")
                        }
                    }
                }
            }
        }
    }
}