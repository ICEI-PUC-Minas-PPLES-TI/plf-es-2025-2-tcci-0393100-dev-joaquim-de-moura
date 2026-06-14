package br.com.seunome.mobulite.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import br.com.seunome.mobulite.data.remote.RatingRequest
import br.com.seunome.mobulite.data.remote.RetrofitClient
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val positiveTags = listOf("Pontual", "Carro limpo", "Dirigiu bem", "Simpático", "Rota correta")
private val negativeTags = listOf("Atrasado", "Carro sujo", "Dirigiu mal", "Rota errada", "Grosseiro")
private val neutralTags = listOf("Carro limpo", "Dirigiu bem", "Poderia melhorar")

private fun tagsForScore(score: Int) = when {
    score >= 4 -> positiveTags
    score in 1..2 -> negativeTags
    score == 3 -> neutralTags
    else -> emptyList()
}

private fun scoreLabel(score: Int) = when (score) {
    5 -> "Excelente!"
    4 -> "Bom"
    3 -> "Regular"
    2 -> "Ruim"
    1 -> "Muito ruim"
    else -> ""
}

private fun scoreLabelColor(score: Int) = when (score) {
    in 4..5 -> AppGreen
    3 -> AppAmber
    in 1..2 -> AppRed
    else -> Slate500
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RatingScreen(
    rideId: String,
    driverName: String?,
    fareFormatted: String,
    onDone: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedScore by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var submitted by remember { mutableStateOf(false) }
    var alreadyRated by remember { mutableStateOf(false) }
    var actualFare by remember { mutableStateOf(fareFormatted) }
    var driverPhotoUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(rideId) {
        runCatching {
            val ride = RetrofitClient.api.getRideById(rideId)
            if (ride.hasRating) alreadyRated = true
            ride.estimatedFareCents?.let { cents ->
                actualFare = "R$ ${"%.2f".format(cents / 100.0).replace(".", ",")}"
            }
            driverPhotoUrl = RetrofitClient.photoUrl(ride.driver?.photoUrl)
        }
    }

    LaunchedEffect(submitted) {
        if (submitted) {
            delay(2500)
            onDone()
        }
    }

    LaunchedEffect(selectedScore) {
        selectedTags = emptySet()
    }

    val tags = tagsForScore(selectedScore)
    val firstName = driverName?.split(" ")?.firstOrNull() ?: "o motorista"

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate100)
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = onDone,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp, top = 4.dp)
                ) {
                    Text("Pular", color = Slate500, fontSize = 14.sp)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(AppPurpleLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = AppPurple,
                        modifier = Modifier.size(52.dp)
                    )
                }
                Text(
                    "Chegou ao destino!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Text(
                    actualFare,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppPurple
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(AppLilacMedium),
                        contentAlignment = Alignment.Center
                    ) {
                        if (driverPhotoUrl != null) {
                            AsyncImage(
                                model = driverPhotoUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(72.dp).clip(CircleShape)
                            )
                        } else {
                            Text(
                                text = driverName?.firstOrNull()?.uppercaseChar()?.toString() ?: "M",
                                color = Color.White,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (driverName != null) {
                        Text(
                            driverName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Slate900
                        )
                    }

                    Text(
                        "Como foi sua viagem com $firstName?",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Slate500,
                        textAlign = TextAlign.Center
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (i in 1..5) {
                            val filled = i <= selectedScore
                            val tint by animateColorAsState(
                                targetValue = if (filled) RatingStarYellow else Slate200,
                                label = "star_$i"
                            )
                            IconButton(
                                onClick = { selectedScore = i },
                                modifier = Modifier.size(52.dp)
                            ) {
                                Icon(
                                    imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                    contentDescription = "$i estrelas",
                                    tint = tint,
                                    modifier = Modifier.size(40.dp)
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
                            scoreLabel(selectedScore),
                            fontWeight = FontWeight.SemiBold,
                            color = scoreLabelColor(selectedScore),
                            fontSize = 15.sp
                        )
                    }

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
                            val chipSelected = if (selectedScore >= 4) AppPurpleLight else AppRedLight
                            val chipLabel = if (selectedScore >= 4) AppVioletDark else AppRed
                            tags.forEach { tag ->
                                val sel = tag in selectedTags
                                FilterChip(
                                    selected = sel,
                                    onClick = {
                                        selectedTags = if (sel) selectedTags - tag else selectedTags + tag
                                    },
                                    label = { Text(tag, fontSize = 13.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = chipSelected,
                                        selectedLabelColor = chipLabel
                                    )
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Comentário adicional (opcional)") },
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
                        comment.isNotBlank() -> comment.trim()
                        else -> null
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
                                    Log.e("RatingScreen", "createRating failed", e)
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
                            Text(
                                "Enviar Avaliação",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

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
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = AppGreen,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            "Corrida já avaliada",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Você já enviou uma avaliação para esta corrida.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate500,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onDone,
                            colors = ButtonDefaults.buttonColors(containerColor = AppPurple),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Voltar ao início")
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = submitted,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
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
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(96.dp)
                    )
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
