package br.com.seunome.mobulite.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.seunome.mobulite.ui.theme.AppAmberLight
import br.com.seunome.mobulite.ui.theme.AppAmberOrange
import br.com.seunome.mobulite.ui.theme.AppBlue
import br.com.seunome.mobulite.ui.theme.AppBlueLight
import br.com.seunome.mobulite.ui.theme.AppGreen
import br.com.seunome.mobulite.ui.theme.AppGreenBg
import br.com.seunome.mobulite.ui.theme.AppGreenLight
import br.com.seunome.mobulite.ui.theme.AppPurple
import br.com.seunome.mobulite.ui.theme.AppPurpleLight
import br.com.seunome.mobulite.ui.theme.AppRed
import br.com.seunome.mobulite.ui.theme.AppRedLight
import br.com.seunome.mobulite.ui.theme.AppLilacSoft
import br.com.seunome.mobulite.ui.theme.AppVioletDark
import br.com.seunome.mobulite.ui.theme.AppAmberBright

@Composable
fun PassengerRideStatusBanner(
    uiState: PassengerRideUiState,
    driverName: String? = null,
    etaMinutes: Int? = null
) {
    if (uiState == PassengerRideUiState.IDLE) return

    data class StatusStyle(
        val containerColor: Color,
        val accentColor: Color,
        val icon: ImageVector,
        val title: String,
        val subtitle: String
    )

    val style = when (uiState) {
        PassengerRideUiState.REQUESTING -> StatusStyle(
            containerColor = AppAmberLight,
            accentColor    = AppAmberBright,
            icon           = Icons.Default.DirectionsCar,
            title          = "Solicitando corrida",
            subtitle       = "Enviando sua solicitação..."
        )
        PassengerRideUiState.SEARCHING_DRIVER -> StatusStyle(
            containerColor = AppBlueLight,
            accentColor    = AppBlue,
            icon           = Icons.Default.Search,
            title          = "Buscando motorista",
            subtitle       = "Procurando motorista próximo"
        )
        PassengerRideUiState.DRIVER_ACCEPTED -> StatusStyle(
            containerColor = AppGreenLight,
            accentColor    = AppGreen,
            icon           = Icons.Default.Person,
            title          = "Motorista a caminho",
            subtitle       = driverName?.let { "Motorista: $it" } ?: "Sua corrida foi aceita!"
        )
        PassengerRideUiState.DRIVER_ARRIVING -> StatusStyle(
            containerColor = AppAmberLight,
            accentColor    = AppAmberOrange,
            icon           = Icons.Default.DirectionsCar,
            title          = "Motorista chegando",
            subtitle       = etaMinutes?.let { "Chega em ~$it min" }
                ?: driverName?.let { "$it está quase lá!" }
                ?: "O motorista está chegando ao seu local"
        )
        PassengerRideUiState.DRIVER_ARRIVED -> StatusStyle(
            containerColor = AppGreenBg,
            accentColor    = AppVioletDark,
            icon           = Icons.Default.LocationOn,
            title          = "Motorista chegou!",
            subtitle       = driverName?.let { "$it chegou ao seu local" } ?: "O motorista está no local de embarque"
        )
        PassengerRideUiState.IN_PROGRESS -> StatusStyle(
            containerColor = AppPurpleLight,
            accentColor    = AppPurple,
            icon           = Icons.Default.DirectionsCar,
            title          = "Em andamento",
            subtitle       = "Sua viagem está acontecendo"
        )
        PassengerRideUiState.FINISHED -> StatusStyle(
            containerColor = AppLilacSoft,
            accentColor    = AppPurple,
            icon           = Icons.Default.CheckCircle,
            title          = "Corrida finalizada",
            subtitle       = "Obrigado por usar o MobU!"
        )
        PassengerRideUiState.CANCELED -> StatusStyle(
            containerColor = AppRedLight,
            accentColor    = AppRed,
            icon           = Icons.Default.Cancel,
            title          = "Corrida cancelada",
            subtitle       = "A solicitação foi cancelada"
        )
        PassengerRideUiState.ERROR -> StatusStyle(
            containerColor = AppRedLight,
            accentColor    = AppRed,
            icon           = Icons.Default.Error,
            title          = "Ocorreu um erro",
            subtitle       = "Tente novamente em instantes"
        )
        PassengerRideUiState.IDLE -> return
    }

    val showProgress = uiState == PassengerRideUiState.REQUESTING ||
            uiState == PassengerRideUiState.SEARCHING_DRIVER

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = style.containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment   = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier            = Modifier.fillMaxWidth()
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape    = CircleShape,
                    color    = style.accentColor.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector    = style.icon,
                            contentDescription = style.title,
                            tint           = style.accentColor,
                            modifier       = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text  = style.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = style.accentColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text  = style.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = style.accentColor.copy(alpha = 0.75f)
                    )
                }
            }

            AnimatedVisibility(visible = showProgress, enter = fadeIn(), exit = fadeOut()) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        modifier   = Modifier.fillMaxWidth(),
                        color      = style.accentColor,
                        trackColor = style.accentColor.copy(alpha = 0.2f),
                        strokeCap  = StrokeCap.Round
                    )
                }
            }
        }
    }
}
