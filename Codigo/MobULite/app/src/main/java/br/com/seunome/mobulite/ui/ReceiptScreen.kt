package br.com.seunome.mobulite.ui

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.seunome.mobulite.data.remote.RatingRequest
import br.com.seunome.mobulite.data.remote.RetrofitClient
import br.com.seunome.mobulite.data.remote.RideStatusResponse
import br.com.seunome.mobulite.ui.theme.AppAmber
import br.com.seunome.mobulite.ui.theme.AppGreen
import br.com.seunome.mobulite.ui.theme.AppPurple
import br.com.seunome.mobulite.ui.theme.AppRed
import br.com.seunome.mobulite.ui.theme.AppRedLight
import br.com.seunome.mobulite.ui.theme.AppVioletDark
import br.com.seunome.mobulite.ui.theme.AppPurpleLight
import br.com.seunome.mobulite.ui.theme.AppLilacMedium
import br.com.seunome.mobulite.ui.theme.RatingStarYellow
import br.com.seunome.mobulite.ui.theme.Slate100
import br.com.seunome.mobulite.ui.theme.Slate200
import br.com.seunome.mobulite.ui.theme.Slate300
import br.com.seunome.mobulite.ui.theme.Slate500
import br.com.seunome.mobulite.ui.theme.Slate900
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ratingPositiveTags = listOf("Pontual", "Carro limpo", "Dirigiu bem", "Simpático", "Rota correta")
private val ratingNegativeTags = listOf("Atrasado", "Carro sujo", "Dirigiu mal", "Rota errada", "Grosseiro")
private val ratingNeutralTags  = listOf("Carro limpo", "Dirigiu bem", "Poderia melhorar")

private fun ratingTagsFor(score: Int) = when {
    score >= 4       -> ratingPositiveTags
    score in 1..2    -> ratingNegativeTags
    score == 3       -> ratingNeutralTags
    else             -> emptyList()
}

private fun ratingLabel(score: Int) = when (score) {
    5 -> "Excelente!"; 4 -> "Bom"; 3 -> "Regular"; 2 -> "Ruim"; 1 -> "Péssimo"; else -> ""
}

private fun ratingLabelColor(score: Int) = when (score) {
    in 4..5 -> AppGreen; 3 -> AppAmber; in 1..2 -> AppRed; else -> Slate500
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReceiptScreen(
    rideId: String,
    driverName: String?,
    onDone: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    var rideDetail   by remember { mutableStateOf<RideStatusResponse?>(null) }
    var loading      by remember { mutableStateOf(true) }
    var alreadyRated by remember { mutableStateOf(false) }

    var selectedScore by remember { mutableStateOf(0) }
    var selectedTags  by remember { mutableStateOf(setOf<String>()) }
    var comment       by remember { mutableStateOf("") }
    var submitting    by remember { mutableStateOf(false) }
    var errorMsg      by remember { mutableStateOf<String?>(null) }
    var submitted     by remember { mutableStateOf(false) }

    LaunchedEffect(rideId) {
        try {
            val r = RetrofitClient.api.getRideById(rideId)
            rideDetail = r
            if (r.hasRating) alreadyRated = true
        } catch (_: Exception) {}
        loading = false
    }

    LaunchedEffect(submitted) { if (submitted) { delay(2200); onDone() } }
    LaunchedEffect(selectedScore) { selectedTags = emptySet() }

    val displayDriver   = driverName ?: rideDetail?.driver?.name
    val driverRating    = rideDetail?.driver?.averageRating
    val driverPhotoUrl  = RetrofitClient.photoUrl(rideDetail?.driver?.photoUrl)
    val firstName       = displayDriver?.split(" ")?.firstOrNull() ?: "o motorista"
    val vehicle         = rideDetail?.driver?.let { d ->
        listOfNotNull(
            d.vehicleModel?.takeIf { it.isNotBlank() },
            d.vehicleColor?.takeIf { it.isNotBlank() },
            d.vehiclePlate?.takeIf { it.isNotBlank() }
        ).joinToString(" · ")
    } ?: ""

    val tags = ratingTagsFor(selectedScore)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate100)
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
        ) {
            // ── Gradient hero with driver avatar ─────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppPurple)
                    .padding(top = 12.dp, bottom = 40.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                // Skip
                TextButton(
                    onClick = onDone,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 8.dp)
                ) {
                    Text("Pular", color = Color.White.copy(alpha = 0.75f), fontSize = 14.sp)
                }

                // Driver avatar — centered
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .border(3.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(AppLilacMedium),
                        contentAlignment = Alignment.Center
                    ) {
                        if (driverPhotoUrl != null) {
                            AsyncImage(
                                model = driverPhotoUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(96.dp).clip(CircleShape)
                            )
                        } else {
                            Text(
                                displayDriver?.firstOrNull()?.uppercaseChar()?.toString() ?: "M",
                                color = Color.White,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        displayDriver ?: "Motorista",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    if (vehicle.isNotBlank()) {
                        Text(
                            vehicle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    if (driverRating != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Filled.Star, null, Modifier.size(14.dp), tint = RatingStarYellow)
                            Text(
                                "%.1f".format(driverRating).replace(".", ","),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // ── Rating card — overlaps slightly over gradient bottom ──────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 0.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Text(
                        "Como foi sua viagem com $firstName?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate900,
                        textAlign = TextAlign.Center
                    )

                    // Stars
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        for (i in 1..5) {
                            val filled = i <= selectedScore
                            val tint by animateColorAsState(
                                targetValue = if (filled) RatingStarYellow else Slate200,
                                animationSpec = tween(160),
                                label = "star_$i"
                            )
                            IconButton(onClick = { selectedScore = i }, modifier = Modifier.size(56.dp)) {
                                Icon(
                                    imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                    contentDescription = "$i estrelas",
                                    tint = tint,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = selectedScore > 0,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        Text(
                            ratingLabel(selectedScore),
                            fontWeight = FontWeight.Bold,
                            color = ratingLabelColor(selectedScore),
                            fontSize = 17.sp
                        )
                    }

                    // Tags
                    AnimatedVisibility(
                        visible = tags.isNotEmpty(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val chipBg    = if (selectedScore >= 4) AppPurpleLight else AppRedLight
                            val chipLabel = if (selectedScore >= 4) AppVioletDark  else AppRed
                            tags.forEach { tag ->
                                val sel = tag in selectedTags
                                FilterChip(
                                    selected = sel,
                                    onClick = {
                                        selectedTags = if (sel) selectedTags - tag else selectedTags + tag
                                    },
                                    label = { Text(tag, fontSize = 13.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = chipBg,
                                        selectedLabelColor = chipLabel
                                    )
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Comentário (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppVioletDark,
                            focusedLabelColor = AppVioletDark,
                            cursorColor = AppVioletDark
                        )
                    )

                    if (errorMsg != null) {
                        Text(
                            errorMsg!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }

                    val tagComment = selectedTags.joinToString(", ")
                    val finalComment = when {
                        tagComment.isNotEmpty() && comment.isNotBlank() -> "$tagComment. ${comment.trim()}"
                        tagComment.isNotEmpty() -> tagComment
                        comment.isNotBlank()    -> comment.trim()
                        else                    -> null
                    }

                    Button(
                        onClick = {
                            if (selectedScore == 0) {
                                errorMsg = "Selecione uma nota de 1 a 5 estrelas"
                                return@Button
                            }
                            scope.launch {
                                submitting = true
                                errorMsg = null
                                try {
                                    RetrofitClient.api.createRating(
                                        RatingRequest(
                                            rideId = rideId,
                                            score = selectedScore,
                                            comment = finalComment
                                        )
                                    )
                                    submitted = true
                                } catch (e: Exception) {
                                    Log.e("ReceiptScreen", "createRating failed", e)
                                    errorMsg = e.message ?: "Erro ao enviar. Tente novamente."
                                }
                                submitting = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        enabled = !submitting,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppPurple,
                            disabledContainerColor = Slate300
                        )
                    ) {
                        if (submitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text("Enviar avaliação", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }

        // ── Already rated overlay ────────────────────────────────────────────
        if (alreadyRated) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Slate100),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(32.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, Modifier.size(64.dp), tint = AppGreen)
                        Text(
                            "Corrida já avaliada",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            "Você já enviou uma avaliação para esta corrida.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate500,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onDone,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppPurple)
                        ) {
                            Text("Voltar ao início", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // ── Submitted confirmation overlay ───────────────────────────────────
        AnimatedVisibility(visible = submitted, enter = fadeIn(tween(350)), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppPurple),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, null, Modifier.size(96.dp), tint = Color.White)
                    Text(
                        "Avaliação enviada!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Obrigado pelo seu feedback",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}
