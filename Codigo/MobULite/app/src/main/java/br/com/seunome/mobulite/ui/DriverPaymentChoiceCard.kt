package br.com.seunome.mobulite.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.seunome.mobulite.data.remote.DriverRideResponse
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun DriverPaymentChoiceCard(
    ride: DriverRideResponse,
    qrPayload: String,
    onPixPaid: () -> Unit,
    onCashPaid: () -> Unit,
    onBack: () -> Unit
) {
    val bitmap = generateQrCodeBitmap(qrPayload)

    val price = (ride.price ?: 0) / 100.0
    val distanceKm = (ride.distanceMeters ?: 0) / 1000.0
    val minutes = (ride.durationSeconds ?: 0) / 60

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Pagamento da corrida",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Confirme o pagamento antes de finalizar a corrida.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            Text(
                text = "Valor total",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "R$ %.2f".format(price),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Text("Passageiro: ${ride.passengerName ?: "Não informado"}")
            Text("Distância: %.1f km".format(distanceKm))
            Text("Tempo estimado: $minutes min")

            HorizontalDivider()

            Text(
                text = "Pagamento via Pix",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "QR Code Pix",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
            )

            Button(
                onClick = onPixPaid,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Recebi via Pix e finalizar")
            }

            OutlinedButton(
                onClick = onCashPaid,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Recebi em dinheiro e finalizar")
            }

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Voltar para corrida")
            }
        }
    }
}