package br.com.seunome.mobulite.ui

import android.content.Context
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import br.com.seunome.mobulite.data.remote.DriverBalanceResponse
import br.com.seunome.mobulite.data.remote.DriverMeResponse
import br.com.seunome.mobulite.data.remote.DriverRatingSummary
import br.com.seunome.mobulite.data.remote.DriverRideHistoryResponse
import br.com.seunome.mobulite.data.remote.RetrofitClient
import br.com.seunome.mobulite.ui.theme.AppAmber
import br.com.seunome.mobulite.ui.theme.AppGreen
import br.com.seunome.mobulite.ui.theme.AppGreenDark
import br.com.seunome.mobulite.ui.theme.AppGreenLight
import br.com.seunome.mobulite.ui.theme.AppGreenOnline
import br.com.seunome.mobulite.ui.theme.RatingStarYellow
import br.com.seunome.mobulite.ui.theme.AppLilacSoft
import br.com.seunome.mobulite.ui.theme.AppPurple
import br.com.seunome.mobulite.ui.theme.AppRed
import br.com.seunome.mobulite.ui.theme.AppVioletDark
import br.com.seunome.mobulite.ui.theme.AppVioletDarker
import br.com.seunome.mobulite.ui.theme.Slate200
import br.com.seunome.mobulite.ui.theme.Slate400
import br.com.seunome.mobulite.ui.theme.Slate500
import br.com.seunome.mobulite.ui.theme.Slate700
import br.com.seunome.mobulite.ui.theme.Slate900
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ── Congrats dialog ─────────────────────────────────────────────────────────────

@Composable
private fun CongratsDialog(driverName: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(28.dp),
        icon = {
            Surface(shape = CircleShape, color = AppGreenLight, modifier = Modifier.size(64.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.CheckCircle, null, tint = AppGreen, modifier = Modifier.size(36.dp))
                }
            }
        },
        title = {
            Text(
                "Parabéns, $driverName!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Seu cadastro foi aprovado! Você já pode iniciar turnos e receber corridas pelo MobU.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate700,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(2.dp))
                listOf(
                    "Configure sua chave Pix para receber pagamentos",
                    "Adicione sua foto de perfil no menu de perfil",
                    "Toque em 'Iniciar Turno' para começar a receber corridas"
                ).forEach { tip ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle, null,
                            tint = AppGreen,
                            modifier = Modifier.size(15.dp).padding(top = 1.dp)
                        )
                        Text(tip, style = MaterialTheme.typography.bodySmall, color = Slate700)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppGreen)
            ) {
                Text("Começar a dirigir!", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    )
}

// ── Helpers ────────────────────────────────────────────────────────────────────

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Bom dia"
    in 12..17 -> "Boa tarde"
    else -> "Boa noite"
}

private fun todayDateString(): String =
    SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR"))
        .format(Date())
        .replaceFirstChar { it.uppercase() }

private fun formatCents(cents: Int) = "R$ ${"%.2f".format(cents / 100.0)}"

// ── Main screen ────────────────────────────────────────────────────────────────

@Composable
fun DriverHomeScreen(
    onStartShift: () -> Unit,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var profile by remember { mutableStateOf<DriverMeResponse?>(null) }
    var history by remember { mutableStateOf<DriverRideHistoryResponse?>(null) }
    var ratings by remember { mutableStateOf<DriverRatingSummary?>(null) }
    var balance by remember { mutableStateOf<DriverBalanceResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }
    var showCongratsDialog by remember { mutableStateOf(false) }

    var showProfile by remember { mutableStateOf(false) }
    var showFinance by remember { mutableStateOf(false) }
    var showBilling by remember { mutableStateOf(false) }
    var showSupport by remember { mutableStateOf(false) }

    suspend fun loadAll() {
        loading = true
        loadError = false
        try {
            coroutineScope {
                val pD = async { runCatching { RetrofitClient.driverApi.getDriverMe()        }.getOrNull() }
                val hD = async { runCatching { RetrofitClient.driverApi.getRideHistory()      }.getOrNull() }
                val rD = async { runCatching { RetrofitClient.driverApi.getMyRatings()        }.getOrNull() }
                val bD = async { runCatching { RetrofitClient.driverApi.getFinancialBalance() }.getOrNull() }
                profile = pD.await()
                history = hD.await()
                ratings = rD.await()
                balance = bD.await()
                if (profile == null && history == null && ratings == null && balance == null) {
                    loadError = true
                }
            }
        } catch (_: Exception) {
            loadError = true
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) { loadAll() }

    LaunchedEffect(profile?.approvalStatus) {
        if (profile?.approvalStatus == "APPROVED") {
            val prefs = context.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE)
            val key = "congrats_shown_${profile?.id ?: "default"}"
            if (!prefs.getBoolean(key, false)) {
                showCongratsDialog = true
            }
        }
    }

    if (showCongratsDialog && profile != null) {
        CongratsDialog(
            driverName = profile!!.name?.split(" ")?.firstOrNull() ?: "Motorista",
            onDismiss = {
                val prefs = context.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("congrats_shown_${profile!!.id}", true).apply()
                showCongratsDialog = false
            }
        )
    }

    // ── Sub-screen routing ─────────────────────────────────────────────────────
    if (showProfile) {
        DriverProfileScreen(
            onBack    = { showProfile = false; scope.launch { loadAll() } },
            onSupport = { showProfile = false; showSupport = true },
            onFinance = { showProfile = false; showFinance = true },
            onLogout  = { showProfile = false; onLogout() }
        )
        return
    }
    if (showFinance) {
        DriverFinanceScreen(
            onBack          = { showFinance = false },
            onBillingCycles = { showFinance = false; showBilling = true }
        )
        return
    }
    if (showBilling) {
        DriverBillingScreen(onBack = { showBilling = false })
        return
    }
    if (showSupport) {
        SupportTicketScreen(onBack = { showSupport = false }, isDriver = true)
        return
    }

    // ── Dashboard ──────────────────────────────────────────────────────────────
    Box(Modifier.fillMaxSize().background(AppLilacSoft)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            HomeHeader(profile = profile, loading = loading, onRefresh = { scope.launch { loadAll() } })

            Spacer(Modifier.height(20.dp))

            // Network error retry banner
            if (!loading && loadError) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFEF2F2)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.WifiOff, null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                        Text(
                            "Sem conexão. Verifique sua internet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFDC2626),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { scope.launch { loadAll() } }) {
                            Text("Tentar novamente", color = Color(0xFFDC2626), fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Approval gate — amber / red banner when not yet approved
            val status = profile?.approvalStatus
            if (!loading && status != null && status != "APPROVED") {
                ApprovalBanner(
                    status    = status,
                    reason    = profile?.rejectionReason,
                    onSupport = { showSupport = true },
                    onLogout  = onLogout
                )
                Spacer(Modifier.height(16.dp))
            }

            // CNH expiry warning (< 30 days)
            val cnhExpiresAt = profile?.cnhExpiresAt
            if (!loading && cnhExpiresAt != null) {
                runCatching {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val expiry = sdf.parse(cnhExpiresAt.take(10))
                    if (expiry != null) {
                        val daysLeft = ((expiry.time - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
                        if (daysLeft in 0..29) {
                            val label = if (daysLeft == 0) "vence hoje!" else "vence em $daysLeft dias"
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFFFFBEB)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Filled.Warning, null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                                    Text(
                                        "Sua CNH $label. Atualize o documento antes do prazo.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF92400E),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }

            TodayStatsRow(history = history, ratings = ratings, loading = loading)

            Spacer(Modifier.height(10.dp))

            DailyGoalCard(todayEarnedCents = history?.todayEarned ?: 0)

            Spacer(Modifier.height(10.dp))

            WeekStatsRow(history = history, loading = loading)

            Spacer(Modifier.height(10.dp))

            BalanceCard(balance = balance, loading = loading)

            Spacer(Modifier.height(22.dp))

            val approved = !loading && status == "APPROVED"
            Button(
                onClick  = onStartShift,
                enabled  = approved,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(58.dp),
                shape  = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor         = AppPurple,
                    disabledContainerColor = Slate200
                )
            ) {
                Icon(Icons.Filled.DirectionsCar, null, Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text          = if (approved) "Iniciar Turno" else "Aguardando aprovação",
                    fontSize      = 17.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            QuickActionsRow(
                onProfile = { showProfile = true },
                onFinance = { showFinance = true },
                onBilling = { showBilling = true },
                onSupport = { showSupport = true }
            )

            Spacer(Modifier.height(28.dp))

            TextButton(
                onClick  = onLogout,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Filled.Logout, null, Modifier.size(15.dp), tint = Slate500)
                Spacer(Modifier.width(6.dp))
                Text("Sair da conta", color = Slate500, fontSize = 13.sp)
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

// ── Header ─────────────────────────────────────────────────────────────────────

@Composable
private fun HomeHeader(
    profile: DriverMeResponse?,
    loading: Boolean,
    onRefresh: () -> Unit
) {
    // Só mostra o estado real depois que a API respondeu; enquanto carrega não exibe nada
    val isOnline     = profile?.online == true
    val statusKnown  = !loading && profile != null
    val fullName     = profile?.name ?: if (loading) "..." else "Motorista"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppPurple)
    ) {
        // Botão de atualizar — canto superior direito, discreto
        IconButton(
            onClick  = onRefresh,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 2.dp, end = 6.dp)
        ) {
            Icon(
                Icons.Filled.Refresh, "Atualizar",
                tint     = Color.White.copy(alpha = if (loading) 0.25f else 0.50f),
                modifier = Modifier.size(19.dp)
            )
        }

        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 14.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Avatar com anel e indicador online
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier         = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    val photoUrl = RetrofitClient.photoUrl(profile?.profilePhotoUrl)
                    if (photoUrl != null) {
                        AsyncImage(
                            model              = photoUrl,
                            contentDescription = null,
                            contentScale       = ContentScale.Crop,
                            modifier           = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier         = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text       = profile?.name?.firstOrNull()?.uppercase() ?: "M",
                                color      = Color.White,
                                fontSize   = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                // Bolinha de status — só aparece quando a API já respondeu
                if (statusKnown) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(AppPurple)
                            .padding(3.dp)
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(if (isOnline) AppGreenOnline else Slate400)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Saudação
            Text(
                text      = greeting(),
                color     = Color.White.copy(alpha = 0.55f),
                fontSize  = 11.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(2.dp))

            // Nome completo
            Text(
                text       = fullName,
                color      = Color.White,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            // Pílula Online / Offline — só após a API responder
            if (statusKnown) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isOnline)
                        AppGreenOnline.copy(alpha = 0.22f)
                    else
                        Color.White.copy(alpha = 0.10f)
                ) {
                    Row(
                        modifier              = Modifier.padding(horizontal = 11.dp, vertical = 4.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isOnline) AppGreenOnline
                                    else Color.White.copy(alpha = 0.35f)
                                )
                        )
                        Text(
                            text       = if (isOnline) "Online" else "Offline",
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = if (isOnline) AppGreenOnline
                                         else Color.White.copy(alpha = 0.50f)
                        )
                    }
                }
            }

            // Chip do veículo
            val vehicleText = profile?.let { p ->
                listOfNotNull(
                    p.vehicleModel?.takeIf(String::isNotBlank),
                    p.vehicleColor?.takeIf(String::isNotBlank),
                    p.vehiclePlate?.takeIf(String::isNotBlank)
                ).joinToString("  ·  ")
            }
            if (!vehicleText.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.09f)
                ) {
                    Row(
                        modifier              = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Icon(
                            Icons.Filled.DirectionsCar, null,
                            Modifier.size(12.dp),
                            tint = Color.White.copy(alpha = 0.55f)
                        )
                        Text(
                            text       = vehicleText,
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color      = Color.White.copy(alpha = 0.80f),
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Data discreta
            Text(
                text      = todayDateString(),
                color     = Color.White.copy(alpha = 0.38f),
                fontSize  = 11.sp,
                textAlign = TextAlign.Center,
                letterSpacing = 0.3.sp
            )
        }
    }
}

// ── Approval banner ─────────────────────────────────────────────────────────────

@Composable
private fun ApprovalBanner(
    status: String,
    reason: String?,
    onSupport: () -> Unit,
    onLogout: () -> Unit
) {
    val isPending = status == "PENDING"
    val accent    = if (isPending) AppAmber else AppRed
    val icon      = if (isPending) Icons.Filled.Schedule else Icons.Filled.Cancel
    val title     = if (isPending) "Cadastro em análise" else "Cadastro reprovado"
    val body      = if (isPending)
        "Seu perfil está sendo revisado. Você será notificado após a aprovação."
    else
        reason?.takeIf { it.isNotBlank() } ?: "Seu cadastro foi reprovado. Entre em contato com o suporte."

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.10f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment      = Alignment.CenterVertically,
                horizontalArrangement  = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, null, Modifier.size(20.dp), tint = accent)
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = accent)
            }
            Text(body, style = MaterialTheme.typography.bodySmall, color = accent.copy(alpha = 0.85f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onSupport) {
                    Text("Suporte", color = accent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                TextButton(onClick = onLogout) {
                    Text("Sair", color = accent.copy(alpha = 0.65f), fontSize = 13.sp)
                }
            }
        }
    }
}

// ── Daily goal card ──────────────────────────────────────────────────────────────

private const val PREF_GOAL_KEY = "driver_daily_goal_cents"
private const val DEFAULT_GOAL_CENTS = 10000

@Composable
private fun DailyGoalCard(todayEarnedCents: Int) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE) }

    var goalCents by remember { mutableStateOf(prefs.getInt(PREF_GOAL_KEY, DEFAULT_GOAL_CENTS)) }
    var showEditDialog by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    val progress = (todayEarnedCents.toFloat() / goalCents).coerceIn(0f, 1f)
    val goalReached = todayEarnedCents >= goalCents

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Definir meta do dia", fontWeight = FontWeight.Bold)
            },
            text = {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
                    label = { Text("Valor (R$)") },
                    prefix = { Text("R$ ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val v = inputText.replace(',', '.').toDoubleOrNull()
                        if (v != null && v > 0) {
                            val cents = (v * 100).toInt()
                            goalCents = cents
                            prefs.edit().putInt(PREF_GOAL_KEY, cents).apply()
                        }
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppPurple),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = if (goalReached)
            CardDefaults.cardColors(containerColor = AppGreenLight)
        else
            CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (goalReached) {
                        Icon(Icons.Filled.CheckCircle, null, Modifier.size(18.dp), tint = AppGreen)
                    }
                    Text(
                        if (goalReached) "Meta atingida! 🎉" else "Meta do dia",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (goalReached) AppGreen else Slate900
                    )
                }
                IconButton(onClick = { inputText = "%.2f".format(goalCents / 100.0); showEditDialog = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Edit, "Editar meta", Modifier.size(16.dp), tint = Slate500)
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(100.dp)),
                color = if (goalReached) AppGreen else AppPurple,
                trackColor = if (goalReached) AppGreen.copy(alpha = 0.2f) else Slate200
            )

            Text(
                "R$ ${"%.2f".format(todayEarnedCents / 100.0)} / R$ ${"%.2f".format(goalCents / 100.0)}",
                style = MaterialTheme.typography.labelMedium,
                color = if (goalReached) AppGreenDark else Slate500,
                fontWeight = if (goalReached) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

// ── Today stats row ─────────────────────────────────────────────────────────────

@Composable
private fun TodayStatsRow(
    history: DriverRideHistoryResponse?,
    ratings: DriverRatingSummary?,
    loading: Boolean
) {
    val animEarnings by animateIntAsState(history?.todayEarned ?: 0, tween(800), label = "te")
    val animRides    by animateIntAsState(history?.todayCount  ?: 0, tween(600), label = "tr")

    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatMiniCard(
            modifier  = Modifier.weight(1f),
            label     = "Hoje",
            value     = if (loading) "—" else formatCents(animEarnings),
            icon      = Icons.Filled.AttachMoney,
            iconColor = AppPurple
        )
        StatMiniCard(
            modifier  = Modifier.weight(1f),
            label     = "Corridas",
            value     = if (loading) "—" else "$animRides",
            icon      = Icons.Filled.DirectionsCar,
            iconColor = AppVioletDark
        )
        StatMiniCard(
            modifier  = Modifier.weight(1f),
            label     = "Avaliação",
            value     = if (loading) "—" else ratings?.average?.let { "★ ${"%.1f".format(it)}" } ?: "—",
            icon      = Icons.Filled.Star,
            iconColor = RatingStarYellow
        )
    }
}

@Composable
private fun StatMiniCard(
    modifier: Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    iconColor: Color
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier              = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.spacedBy(7.dp)
        ) {
            Surface(shape = CircleShape, color = iconColor.copy(alpha = 0.12f), modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(18.dp), tint = iconColor)
                }
            }
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                color = Slate900, textAlign = TextAlign.Center, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelSmall, color = Slate500, textAlign = TextAlign.Center)
        }
    }
}

// ── Week stats row ──────────────────────────────────────────────────────────────

@Composable
private fun WeekStatsRow(history: DriverRideHistoryResponse?, loading: Boolean) {
    val animWeek by animateIntAsState(history?.weekReceivable ?: 0, tween(900), label = "we")

    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            modifier  = Modifier.weight(1.15f),
            shape     = RoundedCornerShape(16.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Esta semana", style = MaterialTheme.typography.labelSmall, color = Slate500)
                Text(
                    if (loading) "—" else formatCents(animWeek),
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color      = AppPurple
                )
                Text("líquido estimado", style = MaterialTheme.typography.labelSmall, color = Slate400)
            }
        }
        Card(
            modifier  = Modifier.weight(0.85f),
            shape     = RoundedCornerShape(16.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StatLineSmall(
                    icon  = Icons.Filled.CheckCircle,
                    label = if (loading) "—" else "${history?.acceptedCount ?: 0} aceitas",
                    tint  = AppPurple
                )
                StatLineSmall(
                    icon  = Icons.Filled.Cancel,
                    label = if (loading) "—" else "${history?.rejectedCount ?: 0} rejeitadas",
                    tint  = Slate400
                )
            }
        }
    }
}

@Composable
private fun StatLineSmall(icon: ImageVector, label: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, null, Modifier.size(14.dp), tint = tint)
        Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Slate900)
    }
}

// ── Balance card ────────────────────────────────────────────────────────────────

@Composable
private fun BalanceCard(balance: DriverBalanceResponse?, loading: Boolean) {
    val balanceCents = balance?.balanceCents ?: 0
    val limitCents   = balance?.limitCents?.takeIf { it > 0 } ?: 5000
    val isBlocked    = balance?.isBlocked ?: false
    val fraction     = (balanceCents.toFloat() / limitCents).coerceIn(0f, 1f)

    val accent = when {
        isBlocked || fraction >= 0.8f -> AppRed
        balanceCents > 0              -> AppAmber
        else                          -> AppGreen
    }
    val statusLabel = when {
        isBlocked          -> "Bloqueado"
        fraction >= 0.8f   -> "Crítico"
        balanceCents > 0   -> "Pendente"
        else               -> "Em dia"
    }

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Plataforma", style = MaterialTheme.typography.labelSmall, color = Slate500)
                Surface(shape = RoundedCornerShape(50), color = accent.copy(alpha = 0.12f)) {
                    Text(
                        statusLabel,
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        style      = MaterialTheme.typography.labelSmall,
                        color      = accent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (loading) "—" else formatCents(balanceCents),
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color      = accent
                )
                if (!loading) {
                    Text(
                        "devidos",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = Slate500,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress  = { fraction },
                modifier  = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color     = accent,
                trackColor = Slate200
            )

            Text(
                "Limite: ${formatCents(limitCents)}  ·  ${(fraction * 100).toInt()}% utilizado",
                style = MaterialTheme.typography.labelSmall,
                color = Slate500
            )
        }
    }
}

// ── Quick actions ───────────────────────────────────────────────────────────────

@Composable
private fun QuickActionsRow(
    onProfile: () -> Unit,
    onFinance: () -> Unit,
    onBilling: () -> Unit,
    onSupport: () -> Unit
) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickAction(Modifier.weight(1f), Icons.Filled.Person,       "Perfil",    onProfile)
        QuickAction(Modifier.weight(1f), Icons.Filled.AttachMoney,  "Finanças",  onFinance)
        QuickAction(Modifier.weight(1f), Icons.Filled.CalendarToday,"Cobranças", onBilling)
        QuickAction(Modifier.weight(1f), Icons.Filled.Help,         "Suporte",   onSupport)
    }
}

@Composable
private fun QuickAction(modifier: Modifier, icon: ImageVector, label: String, onClick: () -> Unit) {
    Card(
        modifier  = modifier.clickable(onClick = onClick),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier              = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, Modifier.size(22.dp), tint = AppPurple)
            Text(label, style = MaterialTheme.typography.labelSmall, color = Slate700, textAlign = TextAlign.Center)
        }
    }
}

