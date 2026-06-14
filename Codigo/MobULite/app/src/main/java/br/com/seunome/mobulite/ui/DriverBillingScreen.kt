package br.com.seunome.mobulite.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.seunome.mobulite.data.remote.BillingCycle
import br.com.seunome.mobulite.data.remote.BillingCyclesResponse
import br.com.seunome.mobulite.data.remote.CreatePaymentRequestBody
import br.com.seunome.mobulite.data.remote.RetrofitClient
import br.com.seunome.mobulite.ui.theme.*
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

// ─── Colours ─────────────────────────────────────────────────────────────────

private val CyclePaid    = AppGreen
private val CyclePending = AppAmberBright
private val CycleOverdue = AppRed
private val CyclePartial = AppAmberOrange
private val CycleOpen    = AppVioletDark

// ─── Entry point ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverBillingScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var data    by remember { mutableStateOf<BillingCyclesResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error   by remember { mutableStateOf<String?>(null) }

    suspend fun load() {
        loading = true; error = null
        try { data = RetrofitClient.driverApi.getBillingCycles() }
        catch (_: Exception) { error = "Não foi possível carregar. Verifique sua internet e tente de novo." }
        finally { loading = false }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Minha Taxa Semanal", fontWeight = FontWeight.Black, fontSize = 17.sp)
                        Text("feche sua conta toda semana", style = MaterialTheme.typography.labelSmall, color = Slate500)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { scope.launch { load() } }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Slate150
    ) { padding ->
        when {
            loading -> BillingLoading(Modifier.padding(padding))
            error != null -> BillingError(error!!, Modifier.padding(padding)) { scope.launch { load() } }
            else -> BillingContent(
                data = data!!,
                modifier = Modifier.padding(padding),
                onRequestSent = { scope.launch { load() } }
            )
        }
    }
}

// ─── Main content ─────────────────────────────────────────────────────────────

@Composable
private fun BillingContent(
    data: BillingCyclesResponse,
    modifier: Modifier = Modifier,
    onRequestSent: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentCycle  = data.cycles.firstOrNull { it.isCurrentWeek }
    val historyCycles = data.cycles.filter { !it.isCurrentWeek }
    val overdueCount  = historyCycles.count { it.status == "OVERDUE" }

    var payTarget by remember { mutableStateOf<BillingCycle?>(null) }
    var payBusy   by remember { mutableStateOf(false) }
    var payErr    by remember { mutableStateOf<String?>(null) }

    suspend fun submitRequest(cycle: BillingCycle, receiptFile: File?) {
        payBusy = true; payErr = null
        try {
            val created = RetrofitClient.driverApi.createPaymentRequest(
                CreatePaymentRequestBody(
                    amountCents = cycle.balanceCents,
                    notes = "Pagamento semana ${fmtWeekShort(cycle.weekStart)}"
                )
            )
            if (receiptFile != null && receiptFile.exists()) {
                try {
                    val requestFile = receiptFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("receipt", receiptFile.name, requestFile)
                    RetrofitClient.driverApi.uploadPaymentReceipt(created.id, part)
                } catch (_: Exception) { /* receipt upload failed silently */ }
            }
            payTarget = null
            onRequestSent()
        } catch (e: HttpException) {
            val body = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
            val msg = try { JSONObject(body ?: "").getString("message") } catch (_: Exception) { null }
            payErr = msg ?: "Erro ${e.code()}. Tente de novo."
        } catch (e: Exception) {
            payErr = e.message?.take(120) ?: "Erro ao enviar. Tente de novo."
        } finally { payBusy = false }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Status banner ─────────────────────────────────────────────────
        when {
            data.hasPendingRequest -> item { SentBanner() }
            data.pendingCount > 0  -> item {
                PendingAlertBanner(
                    count       = data.pendingCount,
                    isOverdue   = overdueCount > 0,
                    pixKey      = data.platformPixKey
                )
            }
        }

        // ── Current week card ─────────────────────────────────────────────
        if (currentCycle != null) {
            item {
                CurrentWeekCard(
                    cycle             = currentCycle,
                    hasPendingRequest = data.hasPendingRequest,
                    onPay             = if (currentCycle.balanceCents > 0 && !data.hasPendingRequest) {{
                        payTarget = currentCycle
                        payErr    = null
                    }} else null
                )
            }
        }

        // ── Past cycles section ───────────────────────────────────────────
        if (historyCycles.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.padding(top = 4.dp, start = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = Slate400, modifier = Modifier.size(14.dp))
                    Text(
                        "SEMANAS ANTERIORES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = Slate400,
                        letterSpacing = 1.5.sp
                    )
                }
            }
            items(historyCycles, key = { it.weekStart }) { cycle ->
                PastCycleCard(
                    cycle            = cycle,
                    hasPendingRequest = data.hasPendingRequest,
                    onPay = {
                        payTarget = cycle
                        payErr    = null
                    }
                )
            }
        }

        // ── Empty state ───────────────────────────────────────────────────
        if (historyCycles.isEmpty() && (currentCycle == null || currentCycle.rideCount == 0)) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(72.dp).clip(CircleShape).background(Slate100),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SentimentSatisfied, contentDescription = null, tint = Slate300, modifier = Modifier.size(40.dp))
                    }
                    Text("Nenhuma corrida ainda", fontWeight = FontWeight.Bold, color = Slate500, fontSize = 15.sp)
                    Text("Quando você fizer corridas,\nelas aparecem aqui.", style = MaterialTheme.typography.bodySmall, color = Slate400, textAlign = TextAlign.Center)
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    // ── Payment wizard modal ──────────────────────────────────────────────────
    payTarget?.let { cycle ->
        PaymentWizardModal(
            cycle     = cycle,
            pixKey    = data.platformPixKey,
            error     = payErr,
            busy      = payBusy,
            onConfirm = { receiptFile -> scope.launch { submitRequest(cycle, receiptFile) } },
            onDismiss = { if (!payBusy) { payTarget = null; payErr = null } }
        )
    }
}

// ─── Banners ──────────────────────────────────────────────────────────────────

@Composable
private fun SentBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppGreen)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.HourglassTop, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Pagamento enviado!", fontWeight = FontWeight.Black, color = Color.White, fontSize = 15.sp)
            Text("O administrador vai confirmar em breve. Você pode continuar trabalhando.", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PendingAlertBanner(count: Int, isOverdue: Boolean, pixKey: String?) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pulse = rememberInfiniteTransition(label = "pulse")
    val alpha by pulse.animateFloat(
        initialValue = 0.85f, targetValue = 1f, label = "a",
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse)
    )

    val bgColor = if (isOverdue) AppRedDark else AppAmberBright

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Title row
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (isOverdue) Icons.Default.ErrorOutline else Icons.Default.Warning,
                contentDescription = null,
                tint = Color.White.copy(alpha = alpha),
                modifier = Modifier.size(26.dp)
            )
            Column {
                Text(
                    if (isOverdue) "Taxa VENCIDA — pague agora!"
                    else if (count == 1) "Você tem 1 semana para pagar"
                    else "Você tem $count semanas para pagar",
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 15.sp
                )
                Text(
                    "Não pagar pode bloquear sua conta.",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // How to pay steps
        HorizontalDivider(color = Color.White.copy(alpha = 0.25f))
        Text("Como pagar:", fontWeight = FontWeight.Black, color = Color.White, fontSize = 12.sp)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HowToPayStep(number = "1", text = "Copie a chave PIX da plataforma abaixo")
            HowToPayStep(number = "2", text = "Abra seu banco e envie o valor da taxa")
            HowToPayStep(number = "3", text = "Volte aqui → toque em \"Pagar taxa\" e confirme")
        }

        if (pixKey != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Pix, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text(pixKey, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.weight(1f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.clickable {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("PIX", pixKey))
                        copied = true
                        scope.launch { delay(2000); copied = false }
                    }
                ) {
                    Text(
                        if (copied) "Copiado ✓" else "Copiar",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun HowToPayStep(number: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(20.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Text(number, fontWeight = FontWeight.Black, color = Color.White, fontSize = 11.sp)
        }
        Text(text, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
    }
}

// ─── Current week hero card ───────────────────────────────────────────────────

@Composable
private fun CurrentWeekCard(
    cycle: BillingCycle,
    hasPendingRequest: Boolean = false,
    onPay: (() -> Unit)? = null
) {
    val dayNames      = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")
    val progressFrac  = (cycle.daysElapsed / cycle.daysTotal.toFloat()).coerceIn(0f, 1f)
    val daysLeft      = (cycle.daysTotal - cycle.daysElapsed).coerceAtLeast(0)
    val animProg by animateFloatAsState(targetValue = progressFrac, animationSpec = tween(1000, easing = EaseOut), label = "prog")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppPurple)
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {

                // Header
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("SEMANA ATUAL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AppGreenLight, letterSpacing = 2.sp)
                        Text("${fmtWeekShort(cycle.weekStart)} – ${fmtWeekShort(cycle.weekEnd)}", fontWeight = FontWeight.Black, color = Color.White, fontSize = 18.sp)
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = if (daysLeft == 0) 0.3f else 0.15f)
                    ) {
                        Text(
                            when {
                                daysLeft == 0 -> "Fecha hoje!"
                                daysLeft == 1 -> "Amanhã fecha"
                                else -> "$daysLeft dias restantes"
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                // Day dots
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    dayNames.forEachIndexed { idx, name ->
                        val isPast  = idx < cycle.daysElapsed
                        val isToday = idx == cycle.daysElapsed - 1
                        DayDot(name = name, isPast = isPast, isToday = isToday)
                    }
                }

                // Progress bar
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearProgressIndicator(
                        progress = { animProg },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                        color = AppPurpleLight,
                        trackColor = Color.White.copy(alpha = 0.18f),
                        strokeCap = StrokeCap.Round
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Dia ${cycle.daysElapsed} de 7", color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.labelSmall)
                        Text("${(progressFrac * 100).toInt()}% da semana", color = AppPurpleLight, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

                // Stats
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    when {
                        cycle.balanceCents > 0 ->
                            WeekStat(label = "Taxa restante", value = fmtMoney(cycle.balanceCents), highlight = true)
                        cycle.paidCents > 0 ->
                            WeekStat(label = "Já adiantado", value = fmtMoney(cycle.paidCents), highlight = false)
                        else ->
                            WeekStat(label = "Taxa acumulada", value = fmtMoney(cycle.totalFeeCents), highlight = false)
                    }
                    WeekStatDivider()
                    WeekStat(label = "Total que ganhei", value = fmtMoney(cycle.totalGrossCents))
                    WeekStatDivider()
                    WeekStat(label = "Corridas", value = "${cycle.rideCount}")
                }

                // Already paid indicator (when advance payment was made)
                if (cycle.paidCents > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppPurpleLight.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AppPurpleLight, modifier = Modifier.size(16.dp))
                        Text(
                            if (cycle.balanceCents == 0) "Taxa adiantada — tudo certo!"
                            else "${fmtMoney(cycle.paidCents)} já adiantado",
                            color = AppPurpleLight,
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Zero-rides nudge
                if (cycle.rideCount == 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                        Text("Fique online para começar a receber corridas!", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Advance payment action
                if (cycle.balanceCents > 0) {
                    if (hasPendingRequest) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.12f))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.HourglassTop, contentDescription = null, tint = AppPurpleLight, modifier = Modifier.size(18.dp))
                            Column {
                                Text("Pagamento enviado!", fontWeight = FontWeight.Black, color = AppPurpleLight, style = MaterialTheme.typography.bodySmall)
                                Text("Aguardando confirmação do administrador.", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    } else if (onPay != null) {
                        Button(
                            onClick = onPay,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppVioletDark),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppPurpleLight.copy(alpha = 0.4f)),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Icon(Icons.Default.Pix, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Adiantar pagamento", fontWeight = FontWeight.Black, fontSize = 14.sp, lineHeight = 16.sp)
                                Text("Pague ${fmtMoney(cycle.balanceCents)} antes de fechar a semana", style = MaterialTheme.typography.labelSmall, color = AppPurpleLight)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayDot(name: String, isPast: Boolean, isToday: Boolean) {
    val pulse = rememberInfiniteTransition(label = "dot_pulse")
    val dotScale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.35f, label = "dot_s",
        animationSpec = infiniteRepeatable(tween(if (isToday) 650 else 60_000), RepeatMode.Reverse)
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(
            modifier = Modifier
                .size(if (isToday) 16.dp else 11.dp)
                .scale(if (isToday) dotScale else 1f)
                .clip(CircleShape)
                .background(
                    when {
                        isToday -> AppPurpleLight
                        isPast  -> Color.White.copy(alpha = 0.8f)
                        else    -> Color.White.copy(alpha = 0.18f)
                    }
                )
        )
        Text(
            name,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = if (isPast || isToday) Color.White else Color.White.copy(alpha = 0.35f),
            fontWeight = if (isToday) FontWeight.Black else FontWeight.Normal
        )
    }
}

@Composable
private fun WeekStat(label: String, value: String, highlight: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, fontWeight = FontWeight.Black, color = if (highlight) AppPurpleLight else Color.White, fontSize = if (highlight) 20.sp else 16.sp)
        Text(label, color = Color.White.copy(alpha = 0.55f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, lineHeight = 14.sp)
    }
}

@Composable
private fun WeekStatDivider() {
    Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.White.copy(alpha = 0.13f)))
}

// ─── Past cycle card ──────────────────────────────────────────────────────────

@Composable
private fun PastCycleCard(
    cycle: BillingCycle,
    hasPendingRequest: Boolean,
    onPay: () -> Unit
) {
    val isPaid        = cycle.status == "PAID"
    val isOverdue     = cycle.status == "OVERDUE"
    val needsPayment  = cycle.status in listOf("PENDING_PAYMENT", "OVERDUE", "PARTIAL")
    val balanceCents  = cycle.balanceCents

    val statusTriple: Triple<Color, String, ImageVector> = when (cycle.status) {
        "PAID"            -> Triple(CyclePaid,    "Pago",                    Icons.Default.CheckCircle)
        "OVERDUE"         -> Triple(CycleOverdue, "Vencido",                 Icons.Default.Error)
        "PARTIAL"         -> Triple(CyclePartial, "Parcialmente pago",       Icons.Default.Schedule)
        "PENDING_PAYMENT" -> Triple(CyclePending, "Aguardando pagamento",    Icons.Default.HourglassEmpty)
        else              -> Triple(Slate400,     "Aberto",                  Icons.Default.Circle)
    }
    val (statusColor, statusLabel, statusIcon) = statusTriple

    val cardBg = when {
        isOverdue -> AppRedLight
        needsPayment && !isPaid -> AppAmberLight
        else -> Color.White
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(if (needsPayment) 3.dp else 1.dp),
        border = if (isOverdue) androidx.compose.foundation.BorderStroke(1.5.dp, CycleOverdue.copy(alpha = 0.3f)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${fmtWeekShort(cycle.weekStart)} – ${fmtWeekShort(cycle.weekEnd)}", fontWeight = FontWeight.Black, color = Slate850, fontSize = 15.sp)
                    Text("${cycle.rideCount} corrida${if (cycle.rideCount != 1) "s" else ""} · ${fmtKm(cycle.totalDistanceMeters)}", style = MaterialTheme.typography.labelSmall, color = Slate500)
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(13.dp))
                    Text(statusLabel, color = statusColor, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall)
                }
            }

            // Debt highlight for unpaid cycles
            if (needsPayment && balanceCents > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusColor.copy(alpha = 0.08f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Você deve pagar:", style = MaterialTheme.typography.labelSmall, color = Slate500)
                        Text(fmtMoney(balanceCents), fontWeight = FontWeight.Black, color = statusColor, fontSize = 22.sp)
                    }
                    if (cycle.paidCents > 0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("já pago:", style = MaterialTheme.typography.labelSmall, color = Slate400)
                            Text(fmtMoney(cycle.paidCents), fontWeight = FontWeight.Bold, color = CyclePaid, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Progress bar
            if (cycle.totalFeeCents > 0) {
                val paid = (cycle.paidCents.toFloat() / cycle.totalFeeCents).coerceIn(0f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { paid },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = CyclePaid,
                        trackColor = if (isOverdue) CycleOverdue.copy(alpha = 0.2f) else Slate200
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total da taxa: ${fmtMoney(cycle.totalFeeCents)}", style = MaterialTheme.typography.labelSmall, color = Slate400)
                        Text("${(paid * 100).toInt()}% pago", style = MaterialTheme.typography.labelSmall, color = if (isPaid) CyclePaid else Slate400, fontWeight = if (isPaid) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            // Earnings info (paid cycles — collapsed, just chips)
            if (isPaid || cycle.totalGrossCents > 0) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CycleMiniChip("Ganho: ${fmtMoney(cycle.totalGrossCents)}", Modifier.weight(1f))
                    CycleMiniChip("Líquido: ${fmtMoney(cycle.totalReceivableCents)}", Modifier.weight(1f))
                }
            }

            // Paid celebration
            if (isPaid) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyclePaid.copy(alpha = 0.08f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyclePaid, modifier = Modifier.size(18.dp))
                    Text("Tudo certo! Esta semana está paga.", fontWeight = FontWeight.Bold, color = CyclePaid, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Action area
            if (needsPayment && balanceCents > 0) {
                if (hasPendingRequest) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyclePending.copy(alpha = 0.1f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.HourglassTop, contentDescription = null, tint = CyclePending, modifier = Modifier.size(18.dp))
                        Column {
                            Text("Pagamento enviado!", fontWeight = FontWeight.Black, color = CyclePending, style = MaterialTheme.typography.bodySmall)
                            Text("Aguardando confirmação do administrador.", color = Slate500, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                } else {
                    Button(
                        onClick = onPay,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isOverdue) CycleOverdue else AppVioletDark
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = if (isOverdue) 4.dp else 2.dp)
                    ) {
                        Icon(Icons.Default.Pix, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isOverdue) "PAGAR AGORA — ${fmtMoney(balanceCents)}"
                            else "Pagar taxa — ${fmtMoney(balanceCents)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CycleMiniChip(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Slate100)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = Slate600, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ─── Payment wizard modal ─────────────────────────────────────────────────────

@Composable
private fun PaymentWizardModal(
    cycle: BillingCycle,
    pixKey: String?,
    error: String?,
    busy: Boolean,
    onConfirm: (File?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    var copied      by remember { mutableStateOf(false) }
    var receiptFile by remember { mutableStateOf<File?>(null) }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val dest = File(context.filesDir, "receipt_upload_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(dest).use { output -> input.copyTo(output) }
                    }
                    receiptFile = dest
                } catch (_: Exception) { }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {

                // Title
                Text("Como pagar sua taxa", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Slate850)
                Text(
                    "Semana ${fmtWeekShort(cycle.weekStart)} – ${fmtWeekShort(cycle.weekEnd)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // ── Step 1 ──────────────────────────────────────────────
                WizardStep(
                    number = "1",
                    title  = "Copie a chave PIX",
                    color  = AppBlue
                ) {
                    if (pixKey != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(AppBlueBg)
                                .border(1.dp, AppBlueLight, RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Pix, contentDescription = null, tint = AppBlue, modifier = Modifier.size(20.dp))
                            Text(pixKey, fontWeight = FontWeight.Black, color = AppBlueDark, modifier = Modifier.weight(1f), fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (copied) AppGreen else AppBlue,
                                modifier = Modifier.clickable(enabled = !copied) {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("PIX", pixKey))
                                    copied = true
                                    scope.launch { delay(2500); copied = false }
                                }
                            ) {
                                Text(
                                    if (copied) "Copiado ✓" else "Copiar",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(AppAmberLight).padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = CyclePending, modifier = Modifier.size(18.dp))
                            Text("Chave PIX não configurada.\nEntre em contato com o administrador.", style = MaterialTheme.typography.bodySmall, color = AppAmberDark)
                        }
                    }
                }

                WizardStepConnector()

                // ── Step 2 ──────────────────────────────────────────────
                WizardStep(
                    number = "2",
                    title  = "Envie este valor pelo seu banco",
                    color  = CyclePartial
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppAmberLight)
                            .border(1.dp, AppAmberBright, RoundedCornerShape(14.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(fmtMoney(cycle.balanceCents), fontWeight = FontWeight.Black, color = CyclePartial, fontSize = 32.sp)
                            Text("envie exatamente este valor", style = MaterialTheme.typography.labelSmall, color = Slate500)
                        }
                    }
                }

                WizardStepConnector()

                // ── Step 3 ──────────────────────────────────────────────
                WizardStep(
                    number = "3",
                    title  = "Anexe o comprovante de pagamento",
                    color  = AppVioletDark
                ) {
                    if (receiptFile == null) {
                        OutlinedButton(
                            onClick = { imageLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppVioletDark),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppVioletDark)
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Selecionar imagem do comprovante", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF3E8FF))
                                .border(1.dp, AppVioletDark, RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AsyncImage(
                                    model = receiptFile,
                                    contentDescription = "Comprovante",
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Comprovante selecionado", fontWeight = FontWeight.Black, color = AppVioletDark, fontSize = 13.sp)
                                    TextButton(
                                        onClick = { imageLauncher.launch("image/*") },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Trocar imagem", fontSize = 12.sp, color = Slate500)
                                    }
                                }
                                IconButton(onClick = { receiptFile = null }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Remover", tint = Slate500, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                error?.let {
                    Text(it, color = CycleOverdue, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                }

                Button(
                    onClick = { onConfirm(receiptFile) },
                    enabled = !busy && pixKey != null,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppVioletDark)
                ) {
                    if (busy) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Enviar solicitação", fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), enabled = !busy) {
                    Text("Cancelar", color = Slate500)
                }
            }
        }
    }
}

@Composable
private fun WizardStep(
    number: String,
    title: String,
    color: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // Circle number
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(number, fontWeight = FontWeight.Black, color = Color.White, fontSize = 14.sp)
        }
        Column(modifier = Modifier.weight(1f).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Black, color = Slate800, fontSize = 14.sp)
            content()
        }
    }
}

@Composable
private fun WizardStepConnector() {
    Row(modifier = Modifier.padding(start = 15.dp, top = 4.dp, bottom = 4.dp)) {
        Box(modifier = Modifier.width(2.dp).height(20.dp).background(Slate200))
    }
}

// ─── Billing mini card (called from DriverScreen) ─────────────────────────────

@Composable
fun BillingMiniCard(
    data: BillingCyclesResponse?,
    loading: Boolean,
    onClick: () -> Unit
) {
    val urgentCount   = data?.cycles?.count { it.status == "OVERDUE" } ?: 0
    val pendingCount  = data?.pendingCount ?: 0
    val currentCycle  = data?.cycles?.firstOrNull { it.isCurrentWeek }
    val progressFrac  = currentCycle?.let { it.daysElapsed / it.daysTotal.toFloat() } ?: 0f
    val hasSent       = data?.hasPendingRequest ?: false

    val cardBg = when {
        urgentCount > 0 -> AppRedDark
        pendingCount > 0 && !hasSent -> AppAmberBright
        hasSent -> AppGreen
        else -> AppVioletDarker
    }

    val pulse = rememberInfiniteTransition(label = "mini_pulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.7f, targetValue = 1f, label = "pa",
        animationSpec = infiniteRepeatable(tween(if (urgentCount > 0) 600 else 60_000), RepeatMode.Reverse)
    )

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(if (urgentCount > 0 || (pendingCount > 0 && !hasSent)) 6.dp else 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBg)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {

                // Icon
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        when {
                            urgentCount > 0 -> Icons.Default.Error
                            pendingCount > 0 && !hasSent -> Icons.Default.Warning
                            hasSent -> Icons.Default.HourglassTop
                            else -> Icons.Default.CalendarToday
                        },
                        contentDescription = null,
                        tint = Color.White.copy(alpha = if (urgentCount > 0) pulseAlpha else 1f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    when {
                        loading || data == null -> {
                            Text("Taxa semanal", fontWeight = FontWeight.Black, color = Color.White, fontSize = 14.sp)
                            Text("Carregando...", color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.labelSmall)
                        }
                        urgentCount > 0 -> {
                            Text("TAXA VENCIDA — pague agora!", fontWeight = FontWeight.Black, color = Color.White, fontSize = 14.sp)
                            Text("$urgentCount semana${if (urgentCount > 1) "s" else ""} em atraso · toque para pagar", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelSmall)
                        }
                        pendingCount > 0 && !hasSent -> {
                            Text("Taxa pendente", fontWeight = FontWeight.Black, color = Color.White, fontSize = 14.sp)
                            Text("$pendingCount semana${if (pendingCount > 1) "s" else ""} aguardando pagamento · toque para ver", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelSmall)
                        }
                        hasSent -> {
                            Text("Pagamento enviado!", fontWeight = FontWeight.Black, color = Color.White, fontSize = 14.sp)
                            Text("Aguardando confirmação do admin", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelSmall)
                        }
                        else -> {
                            // Normal week in progress
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(12.dp))
                                Text(
                                    if (currentCycle != null) "${fmtWeekShort(currentCycle.weekStart)} – ${fmtWeekShort(currentCycle.weekEnd)}"
                                    else "Taxa semanal",
                                    fontWeight = FontWeight.Black, color = Color.White, fontSize = 14.sp
                                )
                            }
                            if (currentCycle != null) {
                                LinearProgressIndicator(
                                    progress = { progressFrac },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                    color = Color.White,
                                    trackColor = Color.White.copy(alpha = 0.2f)
                                )
                                Text(
                                    when {
                                        currentCycle.balanceCents == 0 && currentCycle.paidCents > 0 ->
                                            "Taxa adiantada — tudo certo! · ${currentCycle.rideCount} corridas"
                                        currentCycle.paidCents > 0 ->
                                            "Restante: ${fmtMoney(currentCycle.balanceCents)} · ${currentCycle.rideCount} corridas"
                                        else ->
                                            "Taxa acumulada: ${fmtMoney(currentCycle.totalFeeCents)} · ${currentCycle.rideCount} corridas"
                                    },
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                Icon(Icons.Default.ChevronRight, contentDescription = "Abrir", tint = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

// ─── Loading / Error ──────────────────────────────────────────────────────────

@Composable
private fun BillingLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(color = AppVioletDark, strokeWidth = 3.dp)
            Text("Buscando suas informações...", color = Slate500, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun BillingError(msg: String, modifier: Modifier = Modifier, onRetry: () -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier.size(72.dp).clip(CircleShape).background(CycleOverdue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.WifiOff, contentDescription = null, tint = CycleOverdue, modifier = Modifier.size(36.dp))
            }
            Text("Algo deu errado", fontWeight = FontWeight.Black, color = Slate800, fontSize = 18.sp)
            Text(msg, color = Slate500, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = AppVioletDark),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Tentar de novo", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private val DATE_IN  = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale("pt", "BR")).apply { timeZone = TimeZone.getTimeZone("UTC") }
private val DATE_OUT = SimpleDateFormat("dd MMM", Locale("pt", "BR"))

private fun fmtWeekShort(iso: String): String = try {
    DATE_OUT.format(DATE_IN.parse(iso)!!)
} catch (_: Exception) { iso.take(10) }

private fun fmtKm(meters: Int): String =
    if (meters < 1000) "${meters} m" else "${"%.1f".format(meters / 1000.0).replace(".", ",")} km"

private fun fmtMoney(cents: Int): String =
    "R$ ${"%.2f".format(cents / 100.0).replace(".", ",")}"
