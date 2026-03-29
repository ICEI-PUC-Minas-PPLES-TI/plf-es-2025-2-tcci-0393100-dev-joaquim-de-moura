package br.com.seunome.mobulite.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.seunome.mobulite.data.remote.DriverStatusRequest
import br.com.seunome.mobulite.data.remote.RetrofitClient
import br.com.seunome.mobulite.data.remote.RideRequestItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.HttpException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverScreen(
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var isOnline by remember { mutableStateOf(false) }
    var loadingStatus by remember { mutableStateOf(false) }
    var loadingRides by remember { mutableStateOf(false) }
    var loadingProfile by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }
    var pendingRides by remember { mutableStateOf<List<RideRequestItem>>(emptyList()) }
    var driverStatus by remember { mutableStateOf<String?>(null) }
    var rejectionReason by remember { mutableStateOf<String?>(null) }

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

    suspend fun loadPendingRides() {
        if (!isOnline || driverStatus != "APPROVED") {
            pendingRides = emptyList()
            return
        }

        loadingRides = true
        try {
            pendingRides = RetrofitClient.driverApi.getPendingRides()
        } catch (e: Exception) {
            message = "Erro ao buscar corridas: ${e.message}"
        } finally {
            loadingRides = false
        }
    }

    LaunchedEffect(Unit) {
        loadDriverProfile()
    }

    LaunchedEffect(isOnline, driverStatus) {
        if (isOnline && driverStatus == "APPROVED") {
            while (true) {
                loadPendingRides()
                delay(5000)
            }
        } else {
            pendingRides = emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Área do Motorista") }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (loadingProfile) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text("Carregando status do motorista...")
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                when (driverStatus) {
                    "PENDING" -> {
                        Text(
                            text = "Sua conta está aguardando aprovação do administrador.",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Aguarde a análise para começar a receber corridas.")
                    }

                    "REJECTED" -> {
                        Text(
                            text = "Seu cadastro foi recusado.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )

                        rejectionReason?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Motivo: $it")
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Corrija seus dados e tente novamente.")
                    }

                    "APPROVED" -> {
                        Text(
                            text = if (isOnline) "Status: ONLINE" else "Status: OFFLINE",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    else -> {
                        Text(
                            text = "Status do motorista indisponível.",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                loadingStatus = true
                                try {
                                    val response = RetrofitClient.driverApi.updateStatus(
                                        DriverStatusRequest(true)
                                    )
                                    isOnline = response.online
                                    message = response.message ?: "Motorista online"
                                    loadDriverProfile()
                                } catch (e: HttpException) {
                                    val body = e.response()?.errorBody()?.string()
                                    message = "Erro ao ficar online: HTTP ${e.code()} - ${body ?: e.message()}"
                                } catch (e: Exception) {
                                    message = "Erro ao ficar online: ${e.message}"
                                } finally {
                                    loadingStatus = false
                                }
                            }
                        },
                        enabled = !loadingStatus && !isOnline && driverStatus == "APPROVED"
                    ) {
                        Text("Ficar online")
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                loadingStatus = true
                                try {
                                    val response = RetrofitClient.driverApi.updateStatus(
                                        DriverStatusRequest(false)
                                    )
                                    isOnline = response.online
                                    pendingRides = emptyList()
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
                            }
                        },
                        enabled = !loadingStatus && isOnline && driverStatus == "APPROVED"
                    ) {
                        Text("Ficar offline")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                message?.let {
                    Text(text = it)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (driverStatus == "APPROVED") {
                    if (loadingRides) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (isOnline) {
                        Text(
                            text = "Corridas pendentes",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(pendingRides) { ride ->
                                DriverRideCard(
                                    ride = ride,
                                    onAccept = {
                                        scope.launch {
                                            try {
                                                val response = RetrofitClient.driverApi.acceptRide(ride.rideId)
                                                message = response.message
                                                loadPendingRides()
                                            } catch (e: Exception) {
                                                message = "Erro ao aceitar corrida: ${e.message}"
                                            }
                                        }
                                    },
                                    onReject = {
                                        scope.launch {
                                            try {
                                                val response = RetrofitClient.driverApi.rejectRide(ride.rideId)
                                                message = response.message
                                                loadPendingRides()
                                            } catch (e: Exception) {
                                                message = "Erro ao recusar corrida: ${e.message}"
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        Text("Fique online para visualizar corridas disponíveis.")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(onClick = onLogout) {
                    Text("Sair")
                }
            }
        }
    }
}

@Composable
fun DriverRideCard(
    ride: RideRequestItem,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            val distanceKm = (ride.distanceMeters ?: 0.0) / 1000.0
            val priceValue = (ride.price ?: 0.0) / 100.0

            Text("Passageiro: ${ride.passengerName}")
            Text("Origem: ${ride.originAddress}")
            Text("Destino: ${ride.destinationAddress}")
            Text("Valor: R$ %.2f".format(priceValue))
            Text("Distância: %.2f km".format(distanceKm))
            Text("Status: ${ride.status}")

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onAccept) {
                    Text("Aceitar")
                }

                OutlinedButton(onClick = onReject) {
                    Text("Recusar")
                }
            }
        }
    }
}