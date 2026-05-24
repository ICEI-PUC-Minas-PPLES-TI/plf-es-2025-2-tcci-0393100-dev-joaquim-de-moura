package br.com.seunome.mobulite.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TripOrigin
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.seunome.mobulite.data.remote.DriverMeResponse
import br.com.seunome.mobulite.data.remote.DriverProfileUpdateRequest
import br.com.seunome.mobulite.data.remote.DriverRatingSummary
import br.com.seunome.mobulite.data.remote.DriverRideHistoryItem
import br.com.seunome.mobulite.data.remote.DriverRideHistoryResponse
import br.com.seunome.mobulite.data.remote.RatingItem
import br.com.seunome.mobulite.data.remote.RetrofitClient
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import br.com.seunome.mobulite.ui.theme.AppAmber
import br.com.seunome.mobulite.ui.theme.AppAmberBright
import br.com.seunome.mobulite.ui.theme.AppAmberDark
import br.com.seunome.mobulite.ui.theme.AppAmberLight
import br.com.seunome.mobulite.ui.theme.AppGreen
import br.com.seunome.mobulite.ui.theme.AppGreenDarker
import br.com.seunome.mobulite.ui.theme.AppGreenLight
import br.com.seunome.mobulite.ui.theme.AppLilacMedium
import br.com.seunome.mobulite.ui.theme.AppLilacSoft
import br.com.seunome.mobulite.ui.theme.AppPurpleLight
import br.com.seunome.mobulite.ui.theme.AppRed
import br.com.seunome.mobulite.ui.theme.AppRedDark
import br.com.seunome.mobulite.ui.theme.AppRedLight
import br.com.seunome.mobulite.ui.theme.AppLilac
import br.com.seunome.mobulite.ui.theme.AppPurple
import br.com.seunome.mobulite.ui.theme.AppVioletDark
import br.com.seunome.mobulite.ui.theme.AppVioletDarker
import br.com.seunome.mobulite.ui.theme.Slate100
import br.com.seunome.mobulite.ui.theme.Slate150
import br.com.seunome.mobulite.ui.theme.Slate400
import br.com.seunome.mobulite.ui.theme.Slate200
import br.com.seunome.mobulite.ui.theme.Slate300
import br.com.seunome.mobulite.ui.theme.Slate500
import br.com.seunome.mobulite.ui.theme.Slate600
import br.com.seunome.mobulite.ui.theme.Slate700
import br.com.seunome.mobulite.ui.theme.Slate850
import br.com.seunome.mobulite.ui.theme.Slate900

private enum class DriverRideFilter(val label: String) {
    ALL("Todas"), FINISHED("Concluídas"), CANCELED("Canceladas")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverProfileScreen(
    onBack: () -> Unit,
    onSupport: () -> Unit = {},
    onFinance: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var profile by remember { mutableStateOf<DriverMeResponse?>(null) }
    var ratingSummary by remember { mutableStateOf<DriverRatingSummary?>(null) }
    var history by remember { mutableStateOf<DriverRideHistoryResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var uploadingPhoto by remember { mutableStateOf(false) }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                uploadingPhoto = true
                try {
                    val dest = File(context.filesDir, "driver_profile_photo.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(dest).use { output -> input.copyTo(output) }
                    }
                    val requestFile = dest.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("photo", dest.name, requestFile)
                    profile = RetrofitClient.driverApi.uploadDriverPhoto(part)
                } catch (_: Exception) {
                } finally {
                    uploadingPhoto = false
                }
            }
        }
    }

    suspend fun loadProfileData() {
        loading = true
        errorMsg = null
        try {
            profile = RetrofitClient.driverApi.getDriverMe()
            runCatching { RetrofitClient.driverApi.getMyRatings() }.onSuccess { ratingSummary = it }
            runCatching { RetrofitClient.driverApi.getRideHistory() }.onSuccess { history = it }
        } catch (e: Exception) {
            errorMsg = friendlyProfileError(e)
        } finally {
            loading = false
        }
    }

    suspend fun refreshProfile() {
        profile = RetrofitClient.driverApi.getDriverMe()
    }

    LaunchedEffect(Unit) { loadProfileData() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppPurple
                )
            )
        },
        containerColor = AppLilacSoft
    ) { padding ->
        when {
            loading -> LoadingProfile(Modifier.padding(padding))
            errorMsg != null -> ProfileErrorState(
                message = errorMsg.orEmpty(),
                modifier = Modifier.padding(padding),
                onRetry = { scope.launch { loadProfileData() } }
            )
            else -> DriverProfileContent(
                profile = profile,
                ratingSummary = ratingSummary,
                history = history,
                modifier = Modifier.padding(padding),
                uploadingPhoto = uploadingPhoto,
                onPhotoClick = { photoLauncher.launch("image/*") },
                onProfileSaved = { scope.launch { refreshProfile() } },
                onSupport = onSupport,
                onFinance = onFinance,
                onLogout = onLogout
            )
        }
    }
}

@Composable
private fun DriverProfileContent(
    profile: DriverMeResponse?,
    ratingSummary: DriverRatingSummary?,
    history: DriverRideHistoryResponse?,
    modifier: Modifier = Modifier,
    uploadingPhoto: Boolean = false,
    onPhotoClick: () -> Unit = {},
    onProfileSaved: () -> Unit,
    onSupport: () -> Unit = {},
    onFinance: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val finishedRides = remember(history) { history?.rides?.count { it.status == "FINISHED" } ?: 0 }
    val canceledRides = remember(history) { history?.rides?.count { it.status != "FINISHED" } ?: 0 }
    val totalEarned = history?.totalEarned ?: 0
    val totalReceivable = history?.totalReceivable ?: 0
    val totalPlatformFee = history?.totalPlatformFee ?: 0
    val todayEarned = history?.todayEarned ?: 0
    val weekEarned = history?.weekEarned ?: 0
    val acceptedCount = history?.acceptedCount ?: 0
    val rejectedCount = history?.rejectedCount ?: 0
    val acceptanceRate = if (acceptedCount + rejectedCount > 0)
        (acceptedCount.toFloat() / (acceptedCount + rejectedCount) * 100).toInt() else null
    val totalKm = remember(history) {
        (history?.rides?.sumOf { it.distanceMeters ?: 0 } ?: 0) / 1000.0
    }

    var rideFilter by remember { mutableStateOf(DriverRideFilter.ALL) }
    val filteredRides = remember(history, rideFilter) {
        val all = history?.rides.orEmpty()
        when (rideFilter) {
            DriverRideFilter.FINISHED -> all.filter { it.status == "FINISHED" }
            DriverRideFilter.CANCELED -> all.filter { it.status != "FINISHED" }
            DriverRideFilter.ALL -> all
        }
    }

    var notifNewRides by remember { mutableStateOf(true) }
    var notifPayments by remember { mutableStateOf(true) }
    var notifLongRides by remember { mutableStateOf(false) }
    var showDeleteDlg by remember { mutableStateOf(false) }
    var showLegal by remember { mutableStateOf<LegalType?>(null) }

    showLegal?.let { legalType ->
        LegalScreen(type = legalType, onBack = { showLegal = null })
        return
    }

    if (showDeleteDlg) {
        AlertDialog(
            onDismissRequest = { showDeleteDlg = false },
            confirmButton = {
                TextButton(onClick = { showDeleteDlg = false; onLogout() }) {
                    Text("Excluir", color = AppRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDlg = false }) { Text("Cancelar") }
            },
            title = { Text("Excluir conta", fontWeight = FontWeight.Bold) },
            text = { Text("Esta ação é irreversível. Todos os seus dados, histórico e configurações serão apagados permanentemente.") }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            DriverHeroHeader(
                profile = profile,
                average = ratingSummary?.average,
                totalRatings = ratingSummary?.total ?: 0,
                finishedRides = finishedRides,
                acceptanceRate = acceptanceRate,
                uploadingPhoto = uploadingPhoto,
                onPhotoClick = onPhotoClick
            )
        }

        // Financial section
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DSectionLabel("Financeiro")
                DriverFinancialCard(
                    receivableCents = totalReceivable,
                    earnedCents = totalEarned,
                    platformFeeCents = totalPlatformFee,
                    todayEarnedCents = todayEarned,
                    weekEarnedCents = weekEarned,
                    totalKm = totalKm,
                    canceledRides = canceledRides
                )
            }
        }

        // Work data
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DSectionLabel("Dados de trabalho")
                DriverDocumentCard(profile = profile, onSaved = onProfileSaved)
            }
        }
        item {
            Box(Modifier.padding(horizontal = 16.dp).padding(top = 10.dp)) {
                DriverVehicleCard(profile = profile, onSaved = onProfileSaved)
            }
        }
        item {
            Box(Modifier.padding(horizontal = 16.dp).padding(top = 10.dp)) {
                DriverPixCard(profile = profile, onSaved = onProfileSaved)
            }
        }

        // Ratings
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DSectionLabel("Avaliações")
                if (!ratingSummary?.ratings.isNullOrEmpty()) {
                    DriverRatingSummaryCard(ratingSummary!!)
                }
            }
        }
        if (ratingSummary?.ratings.isNullOrEmpty()) {
            item {
                Box(Modifier.padding(horizontal = 16.dp).padding(top = 10.dp)) {
                    EmptyCard("Sem avaliações recebidas.")
                }
            }
        } else {
            items(ratingSummary!!.ratings.take(8)) { rating ->
                Box(Modifier.padding(horizontal = 16.dp).padding(top = 8.dp)) {
                    RatingItemCard(rating)
                }
            }
        }

        // Ride history
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DSectionLabel("Corridas recentes")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DriverRideFilter.values().forEach { f ->
                        val selected = f == rideFilter
                        val bgColor by animateColorAsState(
                            if (selected) AppPurple else Color.White,
                            animationSpec = tween(200), label = "tab_bg"
                        )
                        val textColor by animateColorAsState(
                            if (selected) Color.White else Slate500,
                            animationSpec = tween(200), label = "tab_txt"
                        )
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = bgColor,
                            shadowElevation = if (selected) 0.dp else 1.dp,
                            modifier = Modifier.clickable { rideFilter = f }
                        ) {
                            Text(
                                f.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = textColor,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
        if (filteredRides.isEmpty()) {
            item {
                Box(Modifier.padding(horizontal = 16.dp).padding(top = 8.dp)) {
                    EmptyCard("Nenhuma corrida no histórico.")
                }
            }
        } else {
            items(filteredRides.take(15)) { ride ->
                Box(Modifier.padding(horizontal = 16.dp).padding(top = 8.dp)) {
                    DriverHistoryRideCard(ride)
                }
            }
        }

        // Preferences
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DSectionLabel("Preferências")
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        DPrefToggle(
                            icon = Icons.Default.Notifications,
                            iconTint = AppPurple,
                            iconBg = AppPurpleLight,
                            title = "Alertas de novas corridas",
                            subtitle = "Notificação ao receber solicitações",
                            checked = notifNewRides,
                            onCheckedChange = { notifNewRides = it }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 64.dp), color = Slate200)
                        DPrefToggle(
                            icon = Icons.Default.Payment,
                            iconTint = AppGreenDarker,
                            iconBg = AppGreenLight,
                            title = "Notificações de pagamento",
                            subtitle = "Avisos de repasse e cobranças",
                            checked = notifPayments,
                            onCheckedChange = { notifPayments = it }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 64.dp), color = Slate200)
                        DPrefToggle(
                            icon = Icons.Default.DirectionsCar,
                            iconTint = AppVioletDark,
                            iconBg = AppPurpleLight,
                            title = "Aceitar corridas longas",
                            subtitle = "Corridas acima de 20 km",
                            checked = notifLongRides,
                            onCheckedChange = { notifLongRides = it }
                        )
                    }
                }
            }
        }

        // About
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DSectionLabel("Sobre o app")
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        DAboutRow(
                            icon = Icons.Default.Info,
                            iconTint = AppPurple,
                            iconBg = AppPurpleLight,
                            label = "Suporte ao motorista",
                            onClick = onSupport
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 64.dp), color = Slate200)
                        DAboutRow(
                            icon = Icons.Default.AccountBalanceWallet,
                            iconTint = AppGreenDarker,
                            iconBg = AppGreenLight,
                            label = "Central de Pagamentos",
                            onClick = onFinance
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 64.dp), color = Slate200)
                        DAboutRow(
                            icon = Icons.Default.Description,
                            iconTint = Slate600,
                            iconBg = Slate100,
                            label = "Termos de uso",
                            onClick = { showLegal = LegalType.TERMS }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 64.dp), color = Slate200)
                        DAboutRow(
                            icon = Icons.Default.Lock,
                            iconTint = Slate600,
                            iconBg = Slate100,
                            label = "Política de privacidade",
                            onClick = { showLegal = LegalType.PRIVACY }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 64.dp), color = Slate200)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Versão do app", style = MaterialTheme.typography.bodyMedium, color = Slate500)
                            Surface(shape = RoundedCornerShape(8.dp), color = AppPurpleLight) {
                                Text(
                                    "1.0.0",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AppVioletDark,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Logout + Delete account
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onLogout)
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(shape = RoundedCornerShape(12.dp), color = AppRedLight, modifier = Modifier.size(40.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = AppRed, modifier = Modifier.size(20.dp))
                                }
                            }
                            Text(
                                "Sair da conta",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AppRed,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = AppRed.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                        }
                        HorizontalDivider(modifier = Modifier.padding(start = 64.dp), color = Slate200)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDeleteDlg = true }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(shape = RoundedCornerShape(12.dp), color = AppRedLight, modifier = Modifier.size(40.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Delete, null, tint = AppRed, modifier = Modifier.size(20.dp))
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Excluir conta", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = AppRed)
                                Text("Apaga todos os seus dados permanentemente", style = MaterialTheme.typography.bodySmall, color = Slate500)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Hero header ──────────────────────────────────────────────────────────────

@Composable
private fun DriverHeroHeader(
    profile: DriverMeResponse?,
    average: Double?,
    totalRatings: Int,
    finishedRides: Int,
    acceptanceRate: Int?,
    uploadingPhoto: Boolean,
    onPhotoClick: () -> Unit
) {
    val pixReady = !profile?.pixKey.isNullOrBlank() || !profile?.pixQrPayload.isNullOrBlank()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppPurple)
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 28.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .border(3.dp, AppLilac, CircleShape)
                            .background(AppLilacMedium)
                            .clickable(enabled = !uploadingPhoto, onClick = onPhotoClick),
                        contentAlignment = Alignment.Center
                    ) {
                        val serverPhotoUrl = RetrofitClient.photoUrl(profile?.profilePhotoUrl)
                        when {
                            uploadingPhoto -> CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 3.dp,
                                color = Color.White
                            )
                            serverPhotoUrl != null -> AsyncImage(
                                model = serverPhotoUrl,
                                contentDescription = "Foto de perfil",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                            else -> Text(
                                text = profile?.name?.firstOrNull()?.uppercase() ?: "M",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Surface(shape = CircleShape, color = AppVioletDarker, modifier = Modifier.size(28.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CameraAlt, "Alterar foto", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = profile?.name?.takeIf { it.isNotBlank() } ?: "Motorista",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = profile?.phone.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                    Text(
                        text = vehicleSummary(profile),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.62f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusPill(
                            label = approvalLabel(profile?.approvalStatus),
                            container = approvalColor(profile?.approvalStatus),
                            content = Color.White
                        )
                        StatusPill(
                            label = availabilityLabel(profile),
                            container = if (profile?.online == true) AppVioletDarker else Slate600,
                            content = Color.White
                        )
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DHeroStatChip(value = finishedRides.toString(), label = "Corridas")
                DHeroStatChip(value = average?.let { "%.1f ★".format(it) } ?: "-", label = "Nota")
                DHeroStatChip(value = acceptanceRate?.let { "$it%" } ?: "-", label = "Aceite")
                DHeroStatChip(value = if (pixReady) "Pronto" else "Pendente", label = "Pix")
            }
        }
    }
}

@Composable
private fun DHeroStatChip(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.65f))
    }
}

// ── Financial card ────────────────────────────────────────────────────────────

@Composable
private fun DriverFinancialCard(
    receivableCents: Int,
    earnedCents: Int,
    platformFeeCents: Int,
    todayEarnedCents: Int,
    weekEarnedCents: Int,
    totalKm: Double,
    canceledRides: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = AppPurple,
            shadowElevation = 4.dp
        ) {
            Box(
                modifier = Modifier
                    .background(
                        AppPurple,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Ganho líquido total",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                    Text(
                        formatMoney(receivableCents),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Text("Bruto: ${formatMoney(earnedCents)}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.62f))
                        Text("Taxa: ${formatMoney(platformFeeCents)}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.62f))
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DStatCell(label = "Hoje (bruto)", value = formatMoney(todayEarnedCents), modifier = Modifier.weight(1f))
            DStatCell(label = "Semana (bruto)", value = formatMoney(weekEarnedCents), modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DStatCell(label = "Km rodados", value = "%.0f km".format(totalKm), modifier = Modifier.weight(1f))
            DStatCell(label = "Canceladas", value = canceledRides.toString(), modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun DStatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Slate500, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Slate850, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DSectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = Slate700,
        modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
    )
}

// ── Rating summary card ───────────────────────────────────────────────────────

@Composable
private fun DriverRatingSummaryCard(ratingSummary: DriverRatingSummary) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = RoundedCornerShape(16.dp), color = AppAmberLight, modifier = Modifier.size(56.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Star, null, tint = AppAmber, modifier = Modifier.size(28.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = ratingSummary.average?.let { "%.1f".format(it) } ?: "-",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate850
                )
                StarRow(score = ratingSummary.average?.toInt() ?: 0)
                Text("${ratingSummary.total} avaliações recebidas", style = MaterialTheme.typography.bodySmall, color = Slate500)
            }
        }
    }
}

// ── Preferences toggle row ────────────────────────────────────────────────────

@Composable
private fun DPrefToggle(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(shape = RoundedCornerShape(12.dp), color = iconBg, modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Slate850)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Slate500)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AppPurple)
        )
    }
}

// ── About row ─────────────────────────────────────────────────────────────────

@Composable
private fun DAboutRow(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    label: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(shape = RoundedCornerShape(12.dp), color = iconBg, modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Slate850,
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Slate300, modifier = Modifier.size(14.dp))
    }
}

// ── Edit cards (unchanged functional logic) ───────────────────────────────────

@Composable
private fun DriverDocumentCard(profile: DriverMeResponse?, onSaved: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var editing by remember { mutableStateOf(false) }
    var name by remember(profile) { mutableStateOf(profile?.name.orEmpty()) }
    var cnhNumber by remember(profile) { mutableStateOf(profile?.cnhNumber.orEmpty()) }
    var cnhCategory by remember(profile) { mutableStateOf(profile?.cnhCategory.orEmpty()) }
    var cnhExpiresAt by remember(profile) { mutableStateOf(profile?.cnhExpiresAt?.let { isoToDisplayDate(it) }.orEmpty()) }
    var hasEar by remember(profile) { mutableStateOf(profile?.hasEar ?: false) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var uploadingCnh by remember { mutableStateOf(false) }
    var cnhUploadError by remember { mutableStateOf<String?>(null) }

    val cnhLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                uploadingCnh = true
                cnhUploadError = null
                try {
                    val dest = File(context.filesDir, "driver_cnh_upload.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(dest).use { output -> input.copyTo(output) }
                    }
                    val requestFile = dest.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("photo", dest.name, requestFile)
                    RetrofitClient.driverApi.uploadCnhPhoto(part)
                    onSaved()
                } catch (_: Exception) {
                    cnhUploadError = "Falha ao enviar. Tente novamente."
                } finally {
                    uploadingCnh = false
                }
            }
        }
    }

    val cnhImageUrl = RetrofitClient.photoUrl(profile?.cnhImageUrl)

    ProfileEditCard(
        title = "Cadastro e CNH",
        subtitle = if (profile?.cnhNumber.isNullOrBlank()) "CNH não informada" else "CNH ${profile?.cnhCategory.orEmpty()}",
        editing = editing,
        onToggle = { message = null; editing = !editing }
    ) {
        if (editing) {
            ProfileTextField(value = name, onValueChange = { name = it }, label = "Nome completo", capitalization = KeyboardCapitalization.Words)
            InfoRow("CPF", profile?.cpf?.takeIf { it.isNotBlank() } ?: "Não informado")
            ProfileTextField(value = cnhNumber, onValueChange = { cnhNumber = it.filter(Char::isDigit).take(20) }, label = "Número da CNH", keyboardType = KeyboardType.Number)
            ProfileTextField(value = cnhCategory, onValueChange = { cnhCategory = it.uppercase().take(5) }, label = "Categoria CNH")
            ProfileTextField(
                value = cnhExpiresAt,
                onValueChange = { raw ->
                    val digits = raw.filter(Char::isDigit).take(8)
                    cnhExpiresAt = buildString {
                        digits.forEachIndexed { i, c -> if (i == 2 || i == 4) append('/'); append(c) }
                    }
                },
                label = "Validade da CNH (DD/MM/AAAA)"
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = hasEar, onCheckedChange = { hasEar = it })
                Text("EAR ativa", style = MaterialTheme.typography.bodyMedium, color = Slate700)
            }

            HorizontalDivider(color = Slate200)

            // CNH photo upload section (in edit mode)
            CnhPhotoSection(
                cnhImageUrl = cnhImageUrl,
                uploading = uploadingCnh,
                uploadError = cnhUploadError,
                onPickImage = { cnhLauncher.launch("image/*") }
            )

            ProfileFormMessage(message)
            ProfileSaveActions(
                saving = saving,
                onCancel = { editing = false; message = null },
                onSave = {
                    scope.launch {
                        saving = true; message = null
                        try {
                            val expiresIso = displayDateToIso(cnhExpiresAt)
                            RetrofitClient.driverApi.updateProfile(
                                DriverProfileUpdateRequest(
                                    name = name.ifBlank { null },
                                    cnhNumber = cnhNumber.ifBlank { null },
                                    cnhCategory = cnhCategory.ifBlank { null },
                                    cnhExpiresAt = expiresIso,
                                    hasEar = hasEar
                                )
                            )
                            editing = false; onSaved()
                        } catch (e: Exception) {
                            message = friendlyProfileError(e)
                        } finally { saving = false }
                    }
                }
            )
        } else {
            InfoRow("Nome", profile?.name ?: "Não informado")
            InfoRow("CPF", profile?.cpf?.takeIf { it.isNotBlank() } ?: "Não informado")
            InfoRow("CNH", profile?.cnhNumber ?: "Não informada")
            InfoRow("Categoria", profile?.cnhCategory ?: "Não informada")
            InfoRow("Validade CNH", profile?.cnhExpiresAt?.let { isoToDisplayDate(it) } ?: "Não informada")
            InfoRow("EAR", if (profile?.hasEar == true) "Sim" else "Não")

            HorizontalDivider(color = Slate200)

            // CNH photo section (in view mode)
            CnhPhotoSection(
                cnhImageUrl = cnhImageUrl,
                uploading = uploadingCnh,
                uploadError = cnhUploadError,
                onPickImage = { cnhLauncher.launch("image/*") }
            )
        }
    }
}

@Composable
private fun CnhPhotoSection(
    cnhImageUrl: String?,
    uploading: Boolean,
    uploadError: String?,
    onPickImage: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Foto da CNH",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate700
                )
                Text(
                    if (cnhImageUrl != null) "Documento enviado" else "Nenhuma foto enviada",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (cnhImageUrl != null) AppGreen else Slate500
                )
            }
            OutlinedButton(
                onClick = onPickImage,
                enabled = !uploading,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp, if (cnhImageUrl != null) AppPurple else AppAmber
                )
            ) {
                if (uploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = AppPurple
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Enviando...", style = MaterialTheme.typography.labelMedium, color = AppPurple)
                } else {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (cnhImageUrl != null) AppPurple else AppAmber
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (cnhImageUrl != null) "Alterar foto" else "Enviar foto",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (cnhImageUrl != null) AppPurple else AppAmber
                    )
                }
            }
        }

        if (cnhImageUrl != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Slate200, RoundedCornerShape(16.dp))
            ) {
                AsyncImage(
                    model = cnhImageUrl,
                    contentDescription = "Foto da CNH",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                    color = Color.Black.copy(alpha = 0.45f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = AppGreenLight, modifier = Modifier.size(14.dp))
                        Text(
                            "Documento verificado pelo sistema",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, AppAmber.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .background(AppAmberLight.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Description, null, tint = AppAmber, modifier = Modifier.size(28.dp))
                    Text(
                        "Envie uma foto legível da sua CNH",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppAmberDark,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (!uploadError.isNullOrBlank()) {
            Text(uploadError, style = MaterialTheme.typography.labelSmall, color = AppRed)
        }
    }
}

@Composable
private fun DriverVehicleCard(profile: DriverMeResponse?, onSaved: () -> Unit) {
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf(false) }
    var vehicleModel by remember(profile) { mutableStateOf(profile?.vehicleModel.orEmpty()) }
    var vehiclePlate by remember(profile) { mutableStateOf(profile?.vehiclePlate.orEmpty()) }
    var vehicleColor by remember(profile) { mutableStateOf(profile?.vehicleColor.orEmpty()) }
    var vehicleYear by remember(profile) { mutableStateOf(profile?.vehicleYear?.toString().orEmpty()) }
    var vehicleCapacity by remember(profile) { mutableStateOf(profile?.vehicleCapacity ?: 4) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    ProfileEditCard(
        title = "Veículo",
        subtitle = vehicleSummary(profile),
        editing = editing,
        onToggle = { message = null; editing = !editing }
    ) {
        if (editing) {
            ProfileTextField(value = vehicleModel, onValueChange = { vehicleModel = it }, label = "Modelo", capitalization = KeyboardCapitalization.Words)
            ProfileTextField(value = vehiclePlate, onValueChange = { vehiclePlate = it.uppercase().take(8) }, label = "Placa")
            ProfileTextField(value = vehicleColor, onValueChange = { vehicleColor = it }, label = "Cor", capitalization = KeyboardCapitalization.Words)
            ProfileTextField(value = vehicleYear, onValueChange = { vehicleYear = it.filter(Char::isDigit).take(4) }, label = "Ano", keyboardType = KeyboardType.Number)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Capacidade de passageiros", style = MaterialTheme.typography.bodyMedium, color = Slate500)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(2, 3, 4, 5, 6, 7).forEach { cap ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (vehicleCapacity == cap) AppPurple else Slate200,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { vehicleCapacity = cap }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    "$cap",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (vehicleCapacity == cap) Color.White else Slate700
                                )
                            }
                        }
                    }
                }
            }
            ProfileFormMessage(message)
            ProfileSaveActions(
                saving = saving,
                onCancel = { editing = false; message = null },
                onSave = {
                    scope.launch {
                        saving = true; message = null
                        try {
                            RetrofitClient.driverApi.updateProfile(
                                DriverProfileUpdateRequest(
                                    vehicleModel = vehicleModel.ifBlank { null },
                                    vehiclePlate = vehiclePlate.ifBlank { null },
                                    vehicleColor = vehicleColor.ifBlank { null },
                                    vehicleYear = vehicleYear.toIntOrNull(),
                                    vehicleCapacity = vehicleCapacity
                                )
                            )
                            editing = false; onSaved()
                        } catch (e: Exception) {
                            message = friendlyProfileError(e)
                        } finally { saving = false }
                    }
                }
            )
        } else {
            InfoRow("Modelo", profile?.vehicleModel ?: "Não informado")
            InfoRow("Placa", profile?.vehiclePlate ?: "Não informada")
            InfoRow("Cor", profile?.vehicleColor ?: "Não informada")
            InfoRow("Ano", profile?.vehicleYear?.toString() ?: "Não informado")
            InfoRow("Capacidade", profile?.vehicleCapacity?.let { "$it passageiros" } ?: "Não informada")
        }
    }
}

@Composable
private fun DriverPixCard(profile: DriverMeResponse?, onSaved: () -> Unit) {
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf(false) }
    var pixKey by remember(profile) { mutableStateOf(profile?.pixKey.orEmpty()) }
    var payload by remember(profile) { mutableStateOf(profile?.pixQrPayload.orEmpty()) }
    var qrCodeUrl by remember(profile) { mutableStateOf(profile?.pixQrCodeUrl.orEmpty()) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val pixConfigured = pixKey.isNotBlank() || payload.isNotBlank() || qrCodeUrl.isNotBlank()

    ProfileEditCard(
        title = "Recebimento Pix",
        subtitle = if (pixConfigured) "Configurado" else "Não configurado",
        editing = editing,
        onToggle = { message = null; editing = !editing }
    ) {
        if (editing) {
            ProfileTextField(value = pixKey, onValueChange = { pixKey = it }, label = "Chave Pix")
            OutlinedTextField(
                value = payload, onValueChange = { payload = it },
                label = { Text("Pix copia e cola") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3, maxLines = 5,
                shape = RoundedCornerShape(16.dp)
            )
            ProfileTextField(value = qrCodeUrl, onValueChange = { qrCodeUrl = it }, label = "URL do QR Code")
            ProfileFormMessage(message)
            ProfileSaveActions(
                saving = saving,
                onCancel = { editing = false; message = null },
                onSave = {
                    scope.launch {
                        saving = true; message = null
                        try {
                            RetrofitClient.driverApi.updateProfile(
                                DriverProfileUpdateRequest(pixKey = pixKey.ifBlank { null }, pixQrPayload = payload.ifBlank { null }, pixQrCodeUrl = qrCodeUrl.ifBlank { null })
                            )
                            editing = false; onSaved()
                        } catch (e: Exception) {
                            message = friendlyProfileError(e)
                        } finally { saving = false }
                    }
                }
            )
        } else {
            if (!profile?.pixQrPayload.isNullOrBlank()) {
                val qrBitmap = remember(profile?.pixQrPayload) {
                    generateQrCodeBitmap(profile?.pixQrPayload.orEmpty(), 280)
                }
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "QR Code Pix",
                    modifier = Modifier
                        .size(132.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(18.dp))
                )
            }
            InfoRow("Chave", profile?.pixKey ?: "Não informada")
            InfoRow("Pix copia e cola", if (profile?.pixQrPayload.isNullOrBlank()) "Não informado" else "Configurado")
            InfoRow("QR Code URL", profile?.pixQrCodeUrl ?: "Não informada")
        }
    }
}

// ── Shared utilities ──────────────────────────────────────────────────────────

@Composable
private fun ProfileEditCard(
    title: String,
    subtitle: String,
    editing: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Slate850)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Slate500, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (editing) Icons.Default.Close else Icons.Default.Edit,
                        contentDescription = if (editing) "Fechar" else "Editar",
                        tint = AppPurple
                    )
                }
            }
            HorizontalDivider(color = Slate200)
            content()
        }
    }
}

@Composable
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, capitalization = capitalization)
    )
}

@Composable
private fun ProfileSaveActions(saving: Boolean, onCancel: () -> Unit, onSave: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f), enabled = !saving, shape = RoundedCornerShape(14.dp)) {
            Text("Cancelar")
        }
        Button(
            onClick = onSave,
            modifier = Modifier.weight(1f),
            enabled = !saving,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppPurple)
        ) {
            if (saving) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Salvar")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Slate500, modifier = Modifier.weight(0.85f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Slate850, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.15f), maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun RatingItemCard(rating: RatingItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                StarRow(score = rating.score)
                Text(formatDate(rating.createdAt), style = MaterialTheme.typography.labelMedium, color = Slate500)
            }
            if (!rating.comment.isNullOrBlank()) {
                Text(rating.comment, style = MaterialTheme.typography.bodyMedium, color = Slate700)
            }
        }
    }
}

@Composable
private fun DriverHistoryRideCard(ride: DriverRideHistoryItem) {
    var expanded by remember { mutableStateOf(false) }
    val chevron by animateFloatAsState(if (expanded) 180f else 0f, animationSpec = tween(200), label = "chevron")
    val km = (ride.distanceMeters ?: 0) / 1000.0
    val isFinished = ride.status == "FINISHED"

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(ride.passengerName ?: "Passageiro", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate850, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(formatDate(ride.createdAt), style = MaterialTheme.typography.labelMedium, color = Slate500)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(
                        label = if (isFinished) "Finalizada" else "Cancelada",
                        container = if (isFinished) AppGreenLight else AppRedLight,
                        content = if (isFinished) AppGreenDarker else AppRedDark
                    )
                    Icon(Icons.Default.ExpandMore, null, tint = Slate500, modifier = Modifier.size(20.dp).rotate(chevron))
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(color = Slate200)
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.TripOrigin, null, tint = AppPurple, modifier = Modifier.size(12.dp))
                            Text(ride.originAddress ?: "Origem não informada", style = MaterialTheme.typography.bodySmall, color = Slate600, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.LocationOn, null, tint = AppRed, modifier = Modifier.size(12.dp))
                            Text(ride.destinationAddress ?: "Destino não informado", style = MaterialTheme.typography.bodySmall, color = Slate600, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${formatMoney(ride.estimatedFareCents ?: 0)} • ${"%.1f".format(km)} km", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Slate850)
                        ride.ratingScore?.let { StarRow(score = it, compact = true) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(label: String, container: Color, content: Color) {
    Surface(shape = RoundedCornerShape(100.dp), color = container) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = content, modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun StarRow(score: Int, compact: Boolean = false, light: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 1.dp else 2.dp)) {
        repeat(5) { index ->
            Icon(
                imageVector = if (index < score) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = null,
                tint = if (index < score) AppAmberBright else if (light) Slate400 else Slate300,
                modifier = Modifier.size(if (compact) 15.dp else 18.dp)
            )
        }
    }
}

@Composable
private fun LoadingProfile(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().background(Slate150), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(color = AppPurple)
            Text("Carregando perfil", style = MaterialTheme.typography.bodyMedium, color = Slate500)
        }
    }
}

@Composable
private fun ProfileErrorState(message: String, modifier: Modifier = Modifier, onRetry: () -> Unit) {
    Box(modifier = modifier.fillMaxSize().background(Slate150).padding(24.dp), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 2.dp) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(message, style = MaterialTheme.typography.bodyMedium, color = AppRed)
                Button(onClick = onRetry, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = AppPurple)) {
                    Text("Tentar novamente")
                }
            }
        }
    }
}

@Composable
private fun EmptyCard(message: String) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 1.dp) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = Slate500, modifier = Modifier.padding(18.dp))
    }
}

@Composable
private fun ProfileFormMessage(message: String?) {
    if (message.isNullOrBlank()) return
    Text(message, style = MaterialTheme.typography.bodySmall, color = AppRed)
}

// ── Pure helpers ──────────────────────────────────────────────────────────────

private fun vehicleSummary(profile: DriverMeResponse?): String {
    val parts = listOfNotNull(
        profile?.vehicleModel?.takeIf { it.isNotBlank() },
        profile?.vehicleColor?.takeIf { it.isNotBlank() },
        profile?.vehiclePlate?.takeIf { it.isNotBlank() }
    )
    return parts.joinToString(" • ").ifBlank { "Veículo não informado" }
}

private fun availabilityLabel(profile: DriverMeResponse?): String = when {
    profile?.online == true && profile.available == false -> "Em corrida"
    profile?.online == true -> "Disponível"
    else -> "Offline"
}

private fun approvalLabel(status: String?): String = when (status) {
    "APPROVED" -> "Aprovado"
    "PENDING" -> "Em análise"
    "REJECTED" -> "Rejeitado"
    else -> "Cadastro"
}

private fun approvalColor(status: String?): Color = when (status) {
    "APPROVED" -> AppGreen
    "PENDING" -> AppAmber
    "REJECTED" -> AppRed
    else -> Slate600
}

private fun formatMoney(cents: Int): String = "R$ ${"%.2f".format(cents / 100.0)}"

private fun formatDate(iso: String): String = try {
    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    val formatter = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
    formatter.format(parser.parse(iso) ?: Date())
} catch (_: Exception) { iso.take(10) }

private fun isoToDisplayDate(iso: String): String {
    if (iso.length < 10) return iso
    val parts = iso.substring(0, 10).split("-")
    return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else iso
}

private fun displayDateToIso(ddMmYyyy: String): String? {
    val digits = ddMmYyyy.filter { it.isDigit() }
    if (digits.length < 8) return null
    return "${digits.substring(4, 8)}-${digits.substring(2, 4)}-${digits.substring(0, 2)}"
}

private fun friendlyProfileError(error: Throwable): String {
    val message = error.message.orEmpty()
    return when {
        message.contains("Unable to resolve host", ignoreCase = true) -> "Sem conexão com o servidor."
        message.contains("timeout", ignoreCase = true) -> "O servidor demorou para responder."
        message.contains("401", ignoreCase = true) -> "Sessão expirada. Entre novamente."
        message.contains("403", ignoreCase = true) -> "Acesso negado para este perfil."
        else -> "Não foi possível atualizar o perfil agora."
    }
}
