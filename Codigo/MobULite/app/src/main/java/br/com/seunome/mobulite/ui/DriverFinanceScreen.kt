package br.com.seunome.mobulite.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
// KeyboardOptions removed — no longer needed here
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
// KeyboardType removed — no longer needed here
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.seunome.mobulite.data.remote.DriverBalanceResponse
import br.com.seunome.mobulite.data.remote.DriverMeResponse
import br.com.seunome.mobulite.data.remote.DriverProfileUpdateRequest
import br.com.seunome.mobulite.data.remote.DriverRideHistoryItem
import br.com.seunome.mobulite.data.remote.DriverRideHistoryResponse
import br.com.seunome.mobulite.data.remote.RetrofitClient
import br.com.seunome.mobulite.ui.theme.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.ImageBitmap

// ─── Entry point ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverFinanceScreen(onBack: () -> Unit, onBillingCycles: (() -> Unit)? = null) {
    val scope = rememberCoroutineScope()

    var profile  by remember { mutableStateOf<DriverMeResponse?>(null) }
    var history  by remember { mutableStateOf<DriverRideHistoryResponse?>(null) }
    var balance  by remember { mutableStateOf<DriverBalanceResponse?>(null) }
    var loading  by remember { mutableStateOf(true) }
    var error    by remember { mutableStateOf<String?>(null) }

    suspend fun load() {
        loading = true; error = null
        try {
            profile = RetrofitClient.driverApi.getDriverMe()
            history = RetrofitClient.driverApi.getRideHistory()
            balance = try { RetrofitClient.driverApi.getFinancialBalance() } catch (_: Exception) { null }
        } catch (e: Exception) {
            error = "Não foi possível carregar os dados financeiros."
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Financeiro", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { scope.launch { load() } }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                    }
                }
            )
        },
        containerColor = Slate150
    ) { padding ->
        when {
            loading -> FinanceLoading(Modifier.padding(padding))
            error != null -> FinanceError(error!!, Modifier.padding(padding)) { scope.launch { load() } }
            else -> FinanceContent(
                profile         = profile,
                history         = history,
                balance         = balance,
                modifier        = Modifier.padding(padding),
                onProfileSaved  = { scope.launch { load() } },
                onBillingCycles = onBillingCycles
            )
        }
    }
}

// ─── Main content ────────────────────────────────────────────────────────────

private enum class FinancePeriod(val label: String) {
    WEEK("Semana"), MONTH("Mês"), ALL("Total")
}

@Composable
private fun FinanceContent(
    profile: DriverMeResponse?,
    history: DriverRideHistoryResponse?,
    balance: DriverBalanceResponse?,
    modifier: Modifier = Modifier,
    onProfileSaved: () -> Unit,
    onBillingCycles: (() -> Unit)? = null
) {
    var period by remember { mutableStateOf(FinancePeriod.WEEK) }

    val earned = when (period) {
        FinancePeriod.WEEK  -> history?.weekEarned ?: 0
        FinancePeriod.MONTH -> history?.monthEarned ?: 0
        FinancePeriod.ALL   -> history?.totalEarned ?: 0
    }
    val receivable = when (period) {
        FinancePeriod.WEEK  -> history?.weekReceivable ?: 0
        FinancePeriod.MONTH -> history?.monthReceivable ?: 0
        FinancePeriod.ALL   -> history?.totalReceivable ?: 0
    }
    val fee = (earned - receivable).coerceAtLeast(0)

    val periodRides = remember(period, history) {
        val all = history?.rides?.filter { it.status == "FINISHED" } ?: emptyList()
        val cal = Calendar.getInstance()
        when (period) {
            FinancePeriod.ALL -> all
            FinancePeriod.WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                all.filter { parseTs(it.createdAt) >= start }
            }
            FinancePeriod.MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                all.filter { parseTs(it.createdAt) >= start }
            }
        }
    }

    val totalKm = remember(periodRides) {
        periodRides.sumOf { (it.distanceMeters ?: 0) } / 1000.0
    }
    val avgMinutes = remember(periodRides) {
        if (periodRides.isEmpty()) 0.0
        else periodRides.map { (it.durationSeconds ?: 0) / 60.0 }.average()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Period selector
        item { PeriodSelector(selected = period, onSelect = { period = it }) }

        // Hero earnings card
        item {
            EarningsHeroCard(
                earnedCents    = earned,
                receivableCents = receivable,
                feeCents       = fee
            )
        }

        // Quick stats
        item {
            QuickStatsRow(
                rides    = periodRides.size,
                totalKm  = totalKm,
                avgMin   = avgMinutes,
                acceptRate = run {
                    val a = history?.acceptedCount ?: 0
                    val r = history?.rejectedCount ?: 0
                    if (a + r > 0) (a.toFloat() / (a + r) * 100).toInt() else null
                }
            )
        }

        // Platform balance
        item { SectionTitle(icon = Icons.Default.AccountBalanceWallet, text = "Saldo com a plataforma") }
        item { PlatformBalanceCard(balance = balance, onGoToBilling = onBillingCycles) }

        // PIX management
        item { SectionTitle(icon = Icons.Default.AccountBalance, text = "Recebimento via Pix") }
        item { PixManagementCard(profile = profile, onSaved = onProfileSaved) }

        // Statement
        if (periodRides.isNotEmpty()) {
            item { SectionTitle(icon = Icons.Default.Receipt, text = "Extrato — ${period.label}") }
            items(periodRides, key = { it.id }) { ride ->
                StatementRideCard(ride = ride)
            }
        } else {
            item { EmptyStatement(period = period.label) }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ─── Period selector ─────────────────────────────────────────────────────────

@Composable
private fun PeriodSelector(selected: FinancePeriod, onSelect: (FinancePeriod) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FinancePeriod.entries.forEach { p ->
            val isSelected = p == selected
            val bgColor by animateColorAsState(
                if (isSelected) AppVioletDark else Color.Transparent,
                animationSpec = tween(200), label = "tab_bg"
            )
            val textColor by animateColorAsState(
                if (isSelected) Color.White else Slate600,
                animationSpec = tween(200), label = "tab_text"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgColor)
                    .clickable { onSelect(p) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = p.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = textColor
                )
            }
        }
    }
}

// ─── Hero card ───────────────────────────────────────────────────────────────

@Composable
private fun EarningsHeroCard(earnedCents: Int, receivableCents: Int, feeCents: Int) {
    val feePercent = if (earnedCents > 0) feeCents.toFloat() / earnedCents else 0f
    val netPercent = 1f - feePercent
    val animatedNet by animateFloatAsState(netPercent, animationSpec = tween(600), label = "bar")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AppPurple)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Label + icon
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                Text("Seu ganho líquido", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge)
            }

            // Main number
            Text(
                text = fmtMoney(receivableCents),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            // Visual breakdown bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    // Net segment
                    Box(
                        Modifier
                            .weight(animatedNet.coerceIn(0.05f, 1f))
                            .height(8.dp)
                            .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = if (feeCents == 0) 8.dp else 0.dp, bottomEnd = if (feeCents == 0) 8.dp else 0.dp))
                            .background(Color.White.copy(alpha = 0.9f))
                    )
                    if (feeCents > 0) {
                        Box(
                            Modifier
                                .weight((1f - animatedNet).coerceIn(0.05f, 1f))
                                .height(8.dp)
                                .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                                .background(Color.White.copy(alpha = 0.3f))
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Líquido ${(netPercent * 100).toInt()}%",
                        color = Color.White.copy(0.9f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    if (feeCents > 0) {
                        Text(
                            "Taxa ${(feePercent * 100).toInt()}%",
                            color = Color.White.copy(0.5f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            // Divider
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

            // Breakdown rows
            BreakdownRow(label = "Valor bruto (corridas)", value = fmtMoney(earnedCents), highlight = false)
            BreakdownRow(label = "Taxa da plataforma", value = "- ${fmtMoney(feeCents)}", highlight = false, negative = true)
            BreakdownRow(label = "Seu ganho líquido", value = fmtMoney(receivableCents), highlight = true)
        }
    }
}

@Composable
private fun BreakdownRow(label: String, value: String, highlight: Boolean, negative: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (highlight) Color.White else Color.White.copy(alpha = 0.7f),
            fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (highlight) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (negative) Color.White.copy(0.55f) else Color.White
        )
    }
}

// ─── Quick stats ─────────────────────────────────────────────────────────────

@Composable
private fun QuickStatsRow(rides: Int, totalKm: Double, avgMin: Double, acceptRate: Int?) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MiniStatCard(
            icon = Icons.Default.DirectionsCar,
            value = rides.toString(),
            label = "Corridas",
            modifier = Modifier.weight(1f)
        )
        MiniStatCard(
            icon = Icons.Default.Route,
            value = "${"%.0f".format(totalKm)} km",
            label = "Distância",
            modifier = Modifier.weight(1f)
        )
        if (acceptRate != null) {
            MiniStatCard(
                icon = Icons.Default.CheckCircle,
                value = "$acceptRate%",
                label = "Aceite",
                modifier = Modifier.weight(1f)
            )
        } else {
            MiniStatCard(
                icon = Icons.Default.Timer,
                value = "${"%.0f".format(avgMin)} min",
                label = "Média/corrida",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MiniStatCard(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = AppVioletDark, modifier = Modifier.size(20.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Slate850, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, style = MaterialTheme.typography.labelSmall, color = Slate500, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ─── Platform balance card ────────────────────────────────────────────────────
// Read-only balance overview. Payment actions live exclusively in DriverBillingScreen.

@Composable
private fun PlatformBalanceCard(balance: DriverBalanceResponse?, onGoToBilling: (() -> Unit)? = null) {
    val pendingCents      = balance?.balanceCents ?: 0
    val totalFeeCents     = balance?.totalFeeCents ?: 0
    val totalSettledCents = balance?.totalSettledCents ?: 0
    val limitCents        = balance?.limitCents ?: 5000
    val isBlocked         = balance?.isBlocked ?: false
    val pendingRequest    = balance?.paymentRequests?.firstOrNull { it.status == "PENDING" }

    val balanceColor = when {
        balance == null    -> Slate500
        pendingCents == 0  -> AppGreen
        isBlocked          -> AppRed
        pendingCents > 500 -> AppAmberBright
        else               -> AppGreen
    }

    if (isBlocked) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(AppRedDark)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Block, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Conta bloqueada!", fontWeight = FontWeight.Black, color = Color.White, fontSize = 15.sp)
                Text(
                    "Débito de ${fmtMoney(limitCents)} atingido. Acesse Cobranças para regularizar.",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(Modifier.height(4.dp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("O que você deve", style = MaterialTheme.typography.labelMedium, color = Slate500, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (balance == null) "Carregando..." else fmtMoney(pendingCents),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = balanceColor
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(balanceColor.copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        when {
                            pendingCents == 0 -> "Em dia ✓"
                            isBlocked         -> "Bloqueado ✗"
                            else              -> "Pendente"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = balanceColor
                    )
                }
            }

            HorizontalDivider(color = Slate50)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BalanceMiniStat(label = "Total de taxas", value = fmtMoney(totalFeeCents), color = Slate850, modifier = Modifier.weight(1f))
                BalanceMiniStat(label = "Total já pago", value = fmtMoney(totalSettledCents), color = AppGreen, modifier = Modifier.weight(1f))
            }

            if (pendingRequest != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppAmberLight)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.HourglassTop, contentDescription = null, tint = AppAmberBright, modifier = Modifier.size(20.dp))
                    Column {
                        Text("Pagamento enviado!", fontWeight = FontWeight.Black, color = AppAmberBright, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Aguardando confirmação do admin · ${fmtMoney(pendingRequest.amountCents)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500
                        )
                    }
                }
            }

            if (!balance?.settlements.isNullOrEmpty()) {
                Text("Pagamentos confirmados pelo admin", style = MaterialTheme.typography.labelSmall, color = Slate500, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    balance!!.settlements.take(3).forEach { st ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Slate50)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AppGreen))
                                Column {
                                    Text(fmtMoney(st.amountCents), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = AppGreen)
                                    Text(st.notes ?: st.method, style = MaterialTheme.typography.labelSmall, color = Slate500, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            Text(fmtDate(st.settledAt), style = MaterialTheme.typography.labelSmall, color = Slate400)
                        }
                    }
                }
            }

            // Direct link to payment flow — wizard lives in BillingScreen
            if (pendingCents > 0 && pendingRequest == null && onGoToBilling != null) {
                val ctaColor = if (isBlocked) AppRed else AppVioletDark
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.5.dp, ctaColor, RoundedCornerShape(14.dp))
                        .clickable { onGoToBilling() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = ctaColor, modifier = Modifier.size(18.dp))
                    Text("Pagar via Cobranças Semanais", fontWeight = FontWeight.Black, color = ctaColor, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = ctaColor)
                }
            }
        }
    }
}

@Composable
private fun BalanceMiniStat(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Slate50)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Slate500, fontWeight = FontWeight.SemiBold)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Black, color = color)
    }
}

// ─── PIX management ──────────────────────────────────────────────────────────

@Composable
private fun PixManagementCard(profile: DriverMeResponse?, onSaved: () -> Unit) {
    val scope = rememberCoroutineScope()
    var editing  by remember { mutableStateOf(false) }
    var pixKey   by remember(profile) { mutableStateOf(profile?.pixKey.orEmpty()) }
    var payload  by remember(profile) { mutableStateOf(profile?.pixQrPayload.orEmpty()) }
    var saving   by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }

    val configured = !profile?.pixKey.isNullOrBlank() || !profile?.pixQrPayload.isNullOrBlank()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (configured) AppGreenLight else AppAmberLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Payments,
                            contentDescription = null,
                            tint = if (configured) AppGreenDarker else AppAmber,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text("Chave Pix", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate850)
                        Text(
                            if (configured) "Configurado" else "Não configurado",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (configured) AppGreenDarker else AppAmber
                        )
                    }
                }
                TextButton(onClick = { editing = !editing; feedback = null }) {
                    Text(if (editing) "Cancelar" else "Editar", color = AppVioletDark, fontWeight = FontWeight.SemiBold)
                }
            }

            if (editing) {
                // Edit mode
                OutlinedTextField(
                    value = pixKey,
                    onValueChange = { pixKey = it },
                    label = { Text("Chave Pix") },
                    placeholder = { Text("CPF, telefone, e-mail ou aleatória") },
                    leadingIcon = { Icon(Icons.Default.Key, null, tint = AppVioletDark) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = payload,
                    onValueChange = { payload = it },
                    label = { Text("Pix Copia e Cola (payload)") },
                    placeholder = { Text("Cole aqui o payload gerado pelo seu banco") },
                    leadingIcon = { Icon(Icons.Default.ContentPaste, null, tint = AppVioletDark) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )

                feedback?.let {
                    Text(it, color = if (it.startsWith("✓")) AppGreenDarker else AppRedDark, style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = {
                        scope.launch {
                            saving = true
                            runCatching {
                                RetrofitClient.driverApi.updateProfile(
                                    DriverProfileUpdateRequest(
                                        pixKey = pixKey.ifBlank { null },
                                        pixQrPayload = payload.ifBlank { null }
                                    )
                                )
                            }.onSuccess {
                                feedback = "✓ Pix salvo com sucesso"
                                editing = false
                                onSaved()
                            }.onFailure {
                                feedback = "Erro ao salvar. Tente novamente."
                            }
                            saving = false
                        }
                    },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppVioletDark)
                ) {
                    if (saving) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("Salvar Pix", fontWeight = FontWeight.SemiBold)
                }
            } else {
                // View mode
                if (configured) {
                    PixInfoRow(icon = Icons.Default.Key, label = "Chave", value = profile?.pixKey ?: "—")
                    PixInfoRow(
                        icon = Icons.Default.QrCode,
                        label = "Payload",
                        value = if (profile?.pixQrPayload.isNullOrBlank()) "Não informado"
                                else "Configurado (${profile?.pixQrPayload?.take(20)}…)"
                    )

                    if (!profile?.pixQrPayload.isNullOrBlank()) {
                        val bmp = remember(profile?.pixQrPayload) {
                            runCatching { generateQrCodeBitmap(profile?.pixQrPayload.orEmpty(), 220) }.getOrNull()
                        }
                        bmp?.let {
                            HorizontalDivider(color = Slate200)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("QR Code de recebimento", style = MaterialTheme.typography.labelSmall, color = Slate500)
                                    Image(
                                        bitmap = it.asImageBitmap(),
                                        contentDescription = "QR Code Pix",
                                        modifier = Modifier.size(140.dp).clip(RoundedCornerShape(10.dp))
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppAmberLight)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = AppAmber, modifier = Modifier.size(18.dp))
                        Text(
                            "Configure sua chave Pix para que o passageiro possa efetuar o pagamento.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppAmberDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PixInfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, contentDescription = null, tint = Slate500, modifier = Modifier.size(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Slate500)
            Text(value, style = MaterialTheme.typography.bodySmall, color = Slate850, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ─── Statement ride card ─────────────────────────────────────────────────────

@Composable
private fun StatementRideCard(ride: DriverRideHistoryItem) {
    var expanded by remember { mutableStateOf(false) }
    val chevron  by animateFloatAsState(if (expanded) 180f else 0f, animationSpec = tween(200), label = "chevron")

    val gross      = ride.estimatedFareCents ?: 0
    val fee        = ride.platformFeeCents ?: 0
    val net        = ride.driverReceivableCents ?: (gross - fee)
    val hasFeeData = ride.platformFeeCents != null
    val km         = (ride.distanceMeters ?: 0) / 1000.0
    val minutes    = (ride.durationSeconds ?: 0) / 60

    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {

            // Always-visible summary
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        ride.passengerName ?: "Passageiro",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate850,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(fmtDate(ride.createdAt), style = MaterialTheme.typography.labelSmall, color = Slate500)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppGreenLight)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "+ ${fmtMoney(net)}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = AppGreenDarker
                        )
                    }
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Slate500,
                        modifier = Modifier.size(20.dp).rotate(chevron)
                    )
                }
            }

            // Expandable details
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.TripOrigin, null, tint = AppVioletDark, modifier = Modifier.size(12.dp))
                            Text(ride.originAddress ?: "Origem não informada", style = MaterialTheme.typography.bodySmall, color = Slate600, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.LocationOn, null, tint = AppRed, modifier = Modifier.size(12.dp))
                            Text(ride.destinationAddress ?: "Destino não informado", style = MaterialTheme.typography.bodySmall, color = Slate600, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoChip(Icons.Default.Route, "${"%.1f".format(km)} km")
                        InfoChip(Icons.Default.Timer, "$minutes min")
                        ride.ratingScore?.let { InfoChip(Icons.Default.Star, "$it ★") }
                    }

                    if (hasFeeData && fee > 0) {
                        HorizontalDivider(color = Slate200)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            StatementLine(label = "Valor bruto", value = fmtMoney(gross), valueColor = Slate700)
                            StatementLine(label = "Taxa plataforma", value = "- ${fmtMoney(fee)}", valueColor = AppRedDark)
                            HorizontalDivider(color = Slate200, thickness = 0.5.dp)
                            StatementLine(label = "Seu ganho", value = fmtMoney(net), valueColor = AppGreenDarker, bold = true)
                        }
                    } else if (!hasFeeData) {
                        HorizontalDivider(color = Slate200)
                        StatementLine(label = "Valor da corrida", value = fmtMoney(gross), valueColor = Slate700)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Info, null, tint = Slate500, modifier = Modifier.size(12.dp))
                            Text("Corrida anterior à implementação de taxa", style = MaterialTheme.typography.labelSmall, color = Slate500)
                        }
                    } else {
                        HorizontalDivider(color = Slate200)
                        StatementLine(label = "Valor bruto / líquido", value = fmtMoney(gross), valueColor = AppGreenDarker, bold = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatementLine(label: String, value: String, valueColor: Color, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Slate600)
        Text(value, style = MaterialTheme.typography.bodySmall, color = valueColor, fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Medium)
    }
}

@Composable
private fun InfoChip(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Slate150)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = Slate500, modifier = Modifier.size(12.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = Slate700)
    }
}

// ─── Section title ────────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = AppVioletDark, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate700)
    }
}

// ─── Empty state ─────────────────────────────────────────────────────────────

@Composable
private fun EmptyStatement(period: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Receipt, null, tint = Slate300, modifier = Modifier.size(40.dp))
            Text("Nenhuma corrida em $period", style = MaterialTheme.typography.bodyMedium, color = Slate500, textAlign = TextAlign.Center)
        }
    }
}

// ─── Loading / Error ─────────────────────────────────────────────────────────

@Composable
private fun FinanceLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(color = AppVioletDark)
            Text("Carregando financeiro…", style = MaterialTheme.typography.bodyMedium, color = Slate500)
        }
    }
}

@Composable
private fun FinanceError(message: String, modifier: Modifier = Modifier, onRetry: () -> Unit) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.ErrorOutline, null, tint = AppRedDark, modifier = Modifier.size(48.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = Slate700, textAlign = TextAlign.Center)
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = AppVioletDark), shape = RoundedCornerShape(12.dp)) {
                Text("Tentar novamente")
            }
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun fmtMoney(cents: Int): String {
    val fmt = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    return fmt.format(cents / 100.0)
}

private fun fmtDate(iso: String): String = runCatching {
    val src = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
    val dst = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    dst.format(src.parse(iso)!!)
}.getOrElse { iso.take(10) }

private fun parseTs(iso: String): Long = runCatching {
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .parse(iso)?.time ?: 0L
}.getOrElse { 0L }
