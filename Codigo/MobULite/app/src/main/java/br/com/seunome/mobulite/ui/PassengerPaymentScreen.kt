package br.com.seunome.mobulite.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.seunome.mobulite.data.remote.RetrofitClient
import br.com.seunome.mobulite.data.remote.RideStatusResponse
import br.com.seunome.mobulite.ui.theme.AppAmber
import br.com.seunome.mobulite.ui.theme.AppGreen
import br.com.seunome.mobulite.ui.theme.AppGreenLight
import br.com.seunome.mobulite.ui.theme.AppLilacSoft
import br.com.seunome.mobulite.ui.theme.AppPurple
import br.com.seunome.mobulite.ui.theme.AppPurpleLight
import br.com.seunome.mobulite.ui.theme.AppVioletDark
import br.com.seunome.mobulite.ui.theme.Slate100
import br.com.seunome.mobulite.ui.theme.Slate200
import br.com.seunome.mobulite.ui.theme.Slate50
import br.com.seunome.mobulite.ui.theme.Slate500
import br.com.seunome.mobulite.ui.theme.Slate700
import br.com.seunome.mobulite.ui.theme.Slate900
import kotlinx.coroutines.delay

@Composable
fun PassengerPaymentScreen(
    rideId: String,
    onContinue: () -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var ride by remember { mutableStateOf<RideStatusResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(rideId) {
        runCatching { ride = RetrofitClient.api.getRideById(rideId) }
        loading = false
    }

    LaunchedEffect(copied) {
        if (copied) { delay(2000); copied = false }
    }

    val isPix = ride?.paymentMethod == "PIX"
    val driverPixKey = ride?.driver?.pixKey
    val pixPayload = ride?.paymentPixPayload
    val pixBitmap = remember(pixPayload) {
        pixPayload
            ?.takeIf { it.isNotBlank() }
            ?.let { payload -> runCatching { generateQrCodeBitmap(payload, 520) }.getOrNull() }
    }
    val fareFormatted = ride?.estimatedFareCents?.let { "R$ ${"%.2f".format(it / 100.0)}" } ?: "—"

    var checkVisible by remember { mutableStateOf(false) }
    LaunchedEffect(loading) { if (!loading) { delay(80); checkVisible = true } }
    val checkScale by animateFloatAsState(
        targetValue = if (checkVisible) 1f else 0.3f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "check"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
            .verticalScroll(rememberScrollState())
    ) {
        // Skip button
        Box(Modifier.fillMaxWidth().padding(top = 48.dp)) {
            TextButton(
                onClick = onContinue,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
            ) {
                Text("Pular", color = Slate500, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Success header ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    color = AppPurple,
                    modifier = Modifier.size(56.dp),
                    strokeWidth = 3.dp
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(checkScale)
                        .clip(CircleShape)
                        .background(AppGreenLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = AppGreen,
                        modifier = Modifier.size(52.dp)
                    )
                }
                Text(
                    "Você chegou!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Text(
                    fareFormatted,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppPurple
                )
                Text(
                    if (isPix) "Realize o pagamento via Pix" else "Pagamento em dinheiro",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate500
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        if (!loading) {
            val origin = ride?.originAddress
            val dest = ride?.destinationAddress
            val dist = ride?.distanceMeters
            val dur = ride?.durationSeconds

            // ── Route + stats card ─────────────────────────────────────────
            if (!origin.isNullOrBlank() || !dest.isNullOrBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(3.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (dist != null) {
                                Surface(shape = RoundedCornerShape(50), color = AppPurpleLight) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.DirectionsCar, null, Modifier.size(12.dp), tint = AppVioletDark)
                                        Text(
                                            "%.1f km".format(dist / 1000.0),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = AppVioletDark,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                            if (dur != null) {
                                Surface(shape = RoundedCornerShape(50), color = AppPurpleLight) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.AccessTime, null, Modifier.size(12.dp), tint = AppVioletDark)
                                        Text(
                                            "${dur / 60} min",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = AppVioletDark,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = Slate100)

                        if (!origin.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .offset(y = 4.dp)
                                        .clip(CircleShape)
                                        .background(AppGreen)
                                )
                                Column {
                                    Text("Origem", style = MaterialTheme.typography.labelSmall, color = Slate500)
                                    Text(
                                        origin,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = Slate900,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        if (!origin.isNullOrBlank() && !dest.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .width(2.dp)
                                    .height(14.dp)
                                    .background(Slate200)
                            )
                        }

                        if (!dest.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    null,
                                    tint = AppPurple,
                                    modifier = Modifier.size(14.dp).offset(y = 2.dp)
                                )
                                Column {
                                    Text("Destino", style = MaterialTheme.typography.labelSmall, color = Slate500)
                                    Text(
                                        dest,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = Slate900,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
            }

            // ── Payment section ────────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(3.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Payment header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isPix) AppGreenLight else AppPurpleLight,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (isPix) Icons.Default.AccountBalance else Icons.Default.AttachMoney,
                                    contentDescription = null,
                                    tint = if (isPix) AppGreen else AppPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                if (isPix) "Pagamento via Pix" else "Pagamento em dinheiro",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                "Total: $fareFormatted",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500
                            )
                        }
                    }

                    if (isPix) {
                        HorizontalDivider(color = Slate100)

                        if (pixBitmap != null && !pixPayload.isNullOrBlank()) {
                            Image(
                                bitmap = pixBitmap.asImageBitmap(),
                                contentDescription = "QR Code Pix",
                                modifier = Modifier
                                    .size(188.dp)
                                    .align(Alignment.CenterHorizontally)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.White)
                                    .padding(12.dp)
                            )

                            TextButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Pix copia e cola", pixPayload))
                                    copied = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Icon(
                                    if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = if (copied) AppGreen else AppPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (copied) "Pix copiado" else "Copiar Pix copia e cola",
                                    color = if (copied) AppGreen else AppPurple,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            ride?.paymentTxId?.let { txId ->
                                Text(
                                    "ID da transação: $txId",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate500,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        if (driverPixKey != null) {
                            Text(
                                "Chave Pix do motorista",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate500,
                                fontWeight = FontWeight.Medium
                            )

                            // PIX key copy box
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AppLilacSoft)
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    driverPixKey,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppVioletDark,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Chave Pix", driverPixKey))
                                        copied = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                                        contentDescription = "Copiar chave Pix",
                                        tint = if (copied) AppGreen else AppPurple,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = copied,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Text(
                                    "Chave copiada! Abra o app do seu banco para pagar.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppGreen,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            if (!copied) {
                                Text(
                                    "Copie a chave e realize o pagamento no app do seu banco.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate500,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Warning, null, tint = AppAmber, modifier = Modifier.size(16.dp))
                                Text(
                                    "Motorista ainda não cadastrou uma chave Pix. Combine com ele outra forma de pagamento.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate700
                                )
                            }
                        }
                    } else {
                        HorizontalDivider(color = Slate100)
                        Text(
                            "Realize o pagamento em dinheiro diretamente ao motorista.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Continue button ────────────────────────────────────────────
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppPurple)
            ) {
                Text(
                    if (isPix) "Já realizei o pagamento" else "Continuar para avaliação",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(36.dp))
        }
    }
}
