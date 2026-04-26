package br.com.seunome.mobulite.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PassengerRideStatusBanner(
    uiState: PassengerRideUiState,
    driverName: String? = null
) {
    if (uiState == PassengerRideUiState.IDLE) return

    val title = when (uiState) {
        PassengerRideUiState.REQUESTING -> "Solicitando corrida"
        PassengerRideUiState.SEARCHING_DRIVER -> "Procurando motorista"
        PassengerRideUiState.DRIVER_ACCEPTED -> "Motorista encontrado"
        PassengerRideUiState.IN_PROGRESS -> "Corrida em andamento"
        PassengerRideUiState.FINISHED -> "Corrida finalizada"
        PassengerRideUiState.CANCELED -> "Corrida cancelada"
        PassengerRideUiState.ERROR -> "Erro na corrida"
        PassengerRideUiState.IDLE -> ""
    }

    val subtitle = when (uiState) {
        PassengerRideUiState.REQUESTING -> "Enviando sua solicitação..."
        PassengerRideUiState.SEARCHING_DRIVER -> "Estamos buscando um motorista próximo."
        PassengerRideUiState.DRIVER_ACCEPTED -> driverName?.let { "Motorista: $it" } ?: "Sua corrida foi aceita."
        PassengerRideUiState.IN_PROGRESS -> "Sua viagem começou."
        PassengerRideUiState.FINISHED -> "Viagem encerrada com sucesso."
        PassengerRideUiState.CANCELED -> "A solicitação foi cancelada."
        PassengerRideUiState.ERROR -> "Tente novamente em instantes."
        PassengerRideUiState.IDLE -> ""
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )

            if (
                uiState == PassengerRideUiState.REQUESTING ||
                uiState == PassengerRideUiState.SEARCHING_DRIVER
            ) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}