package br.com.seunome.mobulite.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PassengerRideStatusCard(
    uiState: PassengerRideUiState,
    originAddress: String,
    destinationAddress: String,
    driverName: String? = null,
    errorMessage: String? = null,
    onCancelRide: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (uiState) {
                PassengerRideUiState.REQUESTING -> {
                    Text(
                        text = "Solicitando corrida",
                        style = MaterialTheme.typography.titleMedium
                    )
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Estamos enviando sua solicitação.")
                }

                PassengerRideUiState.SEARCHING_DRIVER -> {
                    Text(
                        text = "Procurando motorista",
                        style = MaterialTheme.typography.titleMedium
                    )
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Estamos buscando um motorista próximo para sua corrida.")
                }

                PassengerRideUiState.DRIVER_ACCEPTED -> {
                    Text(
                        text = "Motorista encontrado",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text("Sua corrida foi aceita.")
                    driverName?.let {
                        Text("Motorista: $it")
                    }
                }

                PassengerRideUiState.IN_PROGRESS -> {
                    Text(
                        text = "Corrida em andamento",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text("Sua viagem começou.")
                }

                PassengerRideUiState.FINISHED -> {
                    Text(
                        text = "Corrida finalizada",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text("Esperamos que tenha tido uma boa viagem.")
                }

                PassengerRideUiState.CANCELED -> {
                    Text(
                        text = "Corrida cancelada",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text("Sua solicitação foi cancelada.")
                }

                PassengerRideUiState.ERROR -> {
                    Text(
                        text = "Erro ao solicitar corrida",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = errorMessage ?: "Tente novamente.",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                PassengerRideUiState.IDLE -> {
                    Text(
                        text = "Escolha a rota",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text("Defina origem e destino para solicitar sua corrida.")
                }
            }

            Divider()

            Text("Origem: ${originAddress.ifBlank { "Não definida" }}")
            Text("Destino: ${destinationAddress.ifBlank { "Não definido" }}")

            if (uiState == PassengerRideUiState.REQUESTING ||
                uiState == PassengerRideUiState.SEARCHING_DRIVER
            ) {
                onCancelRide?.let {
                    OutlinedButton(
                        onClick = it,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancelar corrida")
                    }
                }
            }

            if (uiState == PassengerRideUiState.FINISHED ||
                uiState == PassengerRideUiState.CANCELED ||
                uiState == PassengerRideUiState.ERROR
            ) {
                onClose?.let {
                    Button(
                        onClick = it,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Fechar")
                    }
                }
            }
        }
    }
}