package br.com.seunome.mobulite.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TripOrigin
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.seunome.mobulite.R
import br.com.seunome.mobulite.data.remote.RideHistoryItem
import br.com.seunome.mobulite.data.remote.RetrofitClient
import coil.compose.AsyncImage
import androidx.compose.ui.draw.clip
import br.com.seunome.mobulite.ui.theme.AppLilacSoft
import br.com.seunome.mobulite.ui.theme.AppPurple
import br.com.seunome.mobulite.ui.theme.AppPurpleLight
import br.com.seunome.mobulite.ui.theme.AppVioletDark
import br.com.seunome.mobulite.ui.theme.AppVioletDarker
import br.com.seunome.mobulite.ui.theme.Slate100
import br.com.seunome.mobulite.ui.theme.Slate200
import br.com.seunome.mobulite.ui.theme.Slate500
import br.com.seunome.mobulite.ui.theme.Slate700
import br.com.seunome.mobulite.ui.theme.Slate900
import br.com.seunome.mobulite.ui.theme.AppRed
import br.com.seunome.mobulite.ui.theme.Slate50

private fun relativeTime(createdAt: String): String = try {
    val epochMs = java.time.Instant.parse(createdAt).toEpochMilli()
    val diffSec = (System.currentTimeMillis() - epochMs) / 1000
    when {
        diffSec < 60      -> "agora"
        diffSec < 3600    -> "há ${diffSec / 60} min"
        diffSec < 86400   -> "há ${diffSec / 3600} h"
        diffSec < 172800  -> "ontem"
        else              -> SimpleDateFormat("dd/MM", Locale("pt", "BR")).format(Date(epochMs))
    }
} catch (_: Exception) { "" }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerHomeScreen(
    onRequestRide: (String?) -> Unit,
    onGoToProfile: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var userName by remember { mutableStateOf<String?>(null) }
    var profilePhotoUrl by remember { mutableStateOf<String?>(null) }
    var recentRides by remember { mutableStateOf<List<RideHistoryItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }

    fun loadData() = scope.launch {
        loading = true; loadError = false
        var anySuccess = false
        runCatching {
            val me = RetrofitClient.api.me()
            userName = me.name
            profilePhotoUrl = RetrofitClient.photoUrl(me.profilePhotoUrl)
            anySuccess = true
        }
        runCatching {
            recentRides = RetrofitClient.api.getMyRides()
                .filter { it.status == "FINISHED" && !it.destinationAddress.isNullOrBlank() }
                .take(4)
            anySuccess = true
        }
        if (!anySuccess) loadError = true
        loading = false
    }

    LaunchedEffect(Unit) { loadData() }

    val firstName = userName?.split(" ")?.firstOrNull() ?: "você"
    val initial = userName?.firstOrNull()?.uppercaseChar()?.toString() ?: "U"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Image(
                        painter = painterResource(R.drawable.logo1),
                        contentDescription = "MobU",
                        modifier = Modifier.height(28.dp),
                        contentScale = ContentScale.Fit
                    )
                },
                actions = {
                    IconButton(onClick = onGoToProfile) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AppPurpleLight),
                            contentAlignment = Alignment.Center
                        ) {
                            if (profilePhotoUrl != null) {
                                AsyncImage(
                                    model = profilePhotoUrl,
                                    contentDescription = "Foto de perfil",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(36.dp).clip(CircleShape)
                                )
                            } else {
                                Text(
                                    initial,
                                    color = AppPurple,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = AppLilacSoft
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Erro de rede com retry
            if (loadError) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = AppRed.copy(alpha = 0.09f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.WifiOff, null, tint = AppRed, modifier = Modifier.size(20.dp))
                            Text(
                                "Sem conexão. Verifique a internet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppRed,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = { loadData() },
                                colors = ButtonDefaults.buttonColors(containerColor = AppRed),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("Tentar", style = MaterialTheme.typography.labelMedium) }
                        }
                    }
                }
            }

            // Saudação
            item {
                if (loading && userName == null) {
                    HomeShimmerCard()
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(listOf(AppVioletDarker, AppPurple)),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 22.dp, vertical = 20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Olá, $firstName!",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "Para onde você vai hoje?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }

            // CTA principal
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = { onRequestRide(null) }),
                    shape = RoundedCornerShape(20.dp),
                    color = AppPurple,
                    shadowElevation = 10.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                "Para onde?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                "Solicitar uma corrida agora",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Corridas recentes
            if (recentRides.isNotEmpty()) {
                item {
                    Text(
                        "Corridas recentes",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate700
                    )
                    Spacer(Modifier.height(4.dp))

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            recentRides.forEachIndexed { index, ride ->
                                RecentRideRow(ride = ride, onClick = { onRequestRide(ride.destinationAddress) })
                                if (index < recentRides.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 70.dp),
                                        color = Slate200
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeShimmerCard() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.25f, targetValue = 0.65f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "shimmerAlpha"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(Color(0xFFE0E0E0).copy(alpha = alpha), RoundedCornerShape(20.dp))
    )
}

@Composable
private fun RecentRideRow(ride: RideHistoryItem, onClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val chevron  by animateFloatAsState(if (expanded) 180f else 0f, animationSpec = tween(200), label = "chevron")

    Column(modifier = Modifier.fillMaxWidth()) {
        // Summary row — always visible
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(shape = CircleShape, color = AppLilacSoft, modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = AppPurple, modifier = Modifier.size(20.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    ride.destinationAddress ?: "Destino",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ride.estimatedFareCents?.let { cents ->
                        Text("R$ ${"%.2f".format(cents / 100.0).replace(".", ",")}", style = MaterialTheme.typography.labelMedium, color = AppVioletDark, fontWeight = FontWeight.SemiBold)
                    }
                    ride.distanceMeters?.let { m ->
                        Text("·", style = MaterialTheme.typography.bodySmall, color = Slate500)
                        Text("%.1f km".format(m / 1000.0).replace(".", ","), style = MaterialTheme.typography.labelMedium, color = Slate500)
                    }
                    val time = relativeTime(ride.createdAt)
                    if (time.isNotEmpty()) {
                        Text("·", style = MaterialTheme.typography.bodySmall, color = Slate500)
                        Icon(Icons.Default.AccessTime, null, tint = Slate500, modifier = Modifier.size(11.dp))
                        Text(time, style = MaterialTheme.typography.labelSmall, color = Slate500)
                    }
                }
            }
            Icon(Icons.Default.ExpandMore, contentDescription = null, tint = Slate500, modifier = Modifier.size(18.dp).rotate(chevron))
        }

        // Expandable details
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HorizontalDivider(modifier = Modifier.padding(bottom = 4.dp), color = Slate200)

                ride.originAddress?.let {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.TripOrigin, null, tint = AppVioletDark, modifier = Modifier.size(14.dp).padding(top = 2.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = Slate700, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    }
                }
                ride.destinationAddress?.let {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.LocationOn, null, tint = AppPurple, modifier = Modifier.size(14.dp).padding(top = 2.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = Slate700, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    }
                }
                ride.driverName?.let {
                    Text("Motorista: $it", style = MaterialTheme.typography.labelSmall, color = Slate500)
                }

                // Repeat ride CTA
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppLilacSoft)
                        .clickable { onClick() }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Refresh, null, tint = AppVioletDark, modifier = Modifier.size(16.dp))
                    Text("Solicitar corrida para este destino", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = AppVioletDark, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
