package br.com.seunome.mobulite.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.seunome.mobulite.data.remote.CreateTicketRequest
import br.com.seunome.mobulite.data.remote.RetrofitClient
import br.com.seunome.mobulite.data.remote.SupportTicketResponse
import br.com.seunome.mobulite.ui.theme.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ─── Category model ───────────────────────────────────────────────────────────

private data class SupportCategory(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val bg: Color
)

private val PASSENGER_CATEGORIES = listOf(
    SupportCategory("PAYMENT",           "Pagamento",            Icons.Default.AccountBalance, AppPurple,      AppPurpleLight),
    SupportCategory("RIDE_CANCELLATION", "Cancelamento",         Icons.Default.DirectionsCar,  AppAmberOrange, AppAmberLight),
    SupportCategory("SAFETY",            "Segurança",            Icons.Default.Security,       AppRed,         AppRedLight),
    SupportCategory("APP_ISSUE",         "Problema no app",      Icons.Default.PhoneAndroid,   AppBlue,        AppBlueLight),
    SupportCategory("OTHER",             "Outro",                Icons.Default.HelpOutline,    Slate500,       Slate100)
)

private val DRIVER_CATEGORIES = listOf(
    SupportCategory("PAYMENT",           "Repasse financeiro",   Icons.Default.AttachMoney,    AppPurple,      AppPurpleLight),
    SupportCategory("RIDE_CANCELLATION", "Corridas",             Icons.Default.DirectionsCar,  AppAmberOrange, AppAmberLight),
    SupportCategory("SAFETY",            "Segurança",            Icons.Default.Security,       AppRed,         AppRedLight),
    SupportCategory("APP_ISSUE",         "Cadastro e conta",     Icons.Default.ManageAccounts, AppBlue,        AppBlueLight),
    SupportCategory("OTHER",             "Outro",                Icons.Default.HelpOutline,    Slate500,       Slate100)
)

private fun categoryFor(key: String?, isDriver: Boolean): SupportCategory? {
    val list = if (isDriver) DRIVER_CATEGORIES else PASSENGER_CATEGORIES
    return list.find { it.key == key } ?: list.lastOrNull()
}

// ─── Date helper ──────────────────────────────────────────────────────────────

private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault())

private fun formatTicketDate(iso: String?): String = if (iso.isNullOrBlank()) "—" else
    try { dateFormatter.format(Instant.parse(iso)) } catch (_: Exception) { iso }

// ─── Main screen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportTicketScreen(onBack: () -> Unit, isDriver: Boolean = false, initialRideId: String? = null) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var tickets       by remember { mutableStateOf<List<SupportTicketResponse>>(emptyList()) }
    var loading       by remember { mutableStateOf(true) }
    var loadError     by remember { mutableStateOf<String?>(null) }
    var showCreate    by remember { mutableStateOf(initialRideId != null) }
    var preSelected   by remember { mutableStateOf("OTHER") }
    var selectedTicket by remember { mutableStateOf<SupportTicketResponse?>(null) }

    fun loadTickets() = scope.launch {
        loading = true; loadError = null
        try { tickets = RetrofitClient.api.getMyTickets() }
        catch (e: Exception) { loadError = e.message }
        finally { loading = false }
    }

    LaunchedEffect(Unit) { loadTickets() }

    // ── Ticket detail overlay ────────────────────────────────────────────────
    selectedTicket?.let { ticket ->
        TicketDetailScreen(ticket = ticket, isDriver = isDriver, onBack = { selectedTicket = null })
        return
    }

    // ── Create ticket screen ─────────────────────────────────────────────────
    if (showCreate) {
        CreateTicketSheet(
            isDriver = isDriver,
            initialType = preSelected,
            initialRideId = initialRideId,
            onDismiss = { showCreate = false; preSelected = "OTHER" },
            onCreated = { showCreate = false; preSelected = "OTHER"; loadTickets() }
        )
        return
    }

    val categories  = if (isDriver) DRIVER_CATEGORIES else PASSENGER_CATEGORIES
    val openTickets = tickets.filter { it.status == "OPEN" || it.status == "IN_REVIEW" }
    val doneTickets = tickets.filter { it.status == "RESOLVED" || it.status == "CLOSED" }

    Scaffold(containerColor = Slate100) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            // ── Hero header ──────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(AppVioletDarker, AppPurple)))
                ) {
                    Column {
                        // Top bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                            }
                        }

                        // Content
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 22.dp)
                                .padding(bottom = 28.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    if (isDriver) "Suporte ao Motorista" else "Central de Suporte",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    "Estamos aqui para ajudar você",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.78f)
                                )
                                Spacer(Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.AccessTime, null, tint = Color.White.copy(0.85f), modifier = Modifier.size(13.dp))
                                        Text("Respondemos em até 48h", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.85f))
                                    }
                                }
                            }
                            // Open tickets badge
                            if (openTickets.isNotEmpty()) {
                                Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = 0.15f)) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            "${openTickets.size}",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                        Text("em aberto", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.75f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Quick help — categories ──────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(top = 20.dp)) {
                    Text(
                        "Como podemos ajudar?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate700,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    )
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        categories.forEach { cat ->
                            QuickHelpChip(cat) {
                                preSelected = cat.key
                                showCreate = true
                            }
                        }
                    }
                }
            }

            // ── Direct contact card ──────────────────────────────────────────
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(shape = CircleShape, color = AppPurpleLight, modifier = Modifier.size(46.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.HeadsetMic, null, tint = AppPurple, modifier = Modifier.size(22.dp))
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Contato direto", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate900)
                            Text("suporte@mobulite.com.br", style = MaterialTheme.typography.bodySmall, color = Slate500)
                        }
                        val ctx = LocalContext.current
                        OutlinedButton(
                            onClick = {
                                ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:suporte@mobulite.com.br")))
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppPurple),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Email, null, tint = AppPurple, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("E-mail", color = AppPurple, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ── Loading ──────────────────────────────────────────────────────
            if (loading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppPurple, strokeWidth = 3.dp)
                    }
                }
            }

            // ── Error ────────────────────────────────────────────────────────
            if (loadError != null) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = AppRedLight
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Error, null, tint = AppRed, modifier = Modifier.size(20.dp))
                            Text("Erro ao carregar chamados", color = AppRedDark, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            TextButton(onClick = { loadTickets() }, contentPadding = PaddingValues(0.dp)) {
                                Text("Tentar", color = AppRedDark, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ── Empty state ──────────────────────────────────────────────────
            if (!loading && loadError == null && tickets.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(shape = CircleShape, color = AppPurpleLight, modifier = Modifier.size(80.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.HeadsetMic, null, tint = AppPurple, modifier = Modifier.size(38.dp))
                            }
                        }
                        Text("Nenhum chamado ainda", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Slate700)
                        Text(
                            "Abra um chamado se tiver dúvidas ou precisar de ajuda. Nossa equipe responderá em até 48 horas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { showCreate = true },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppPurple),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Abrir primeiro chamado", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ── Open tickets ─────────────────────────────────────────────────
            if (openTickets.isNotEmpty()) {
                item { TicketSectionHeader(title = "Chamados em aberto", count = openTickets.size, onNew = { showCreate = true }) }
                items(openTickets, key = { it.id }) { ticket ->
                    TicketCard(
                        ticket = ticket,
                        isDriver = isDriver,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        onClick = { selectedTicket = ticket }
                    )
                }
            }

            // ── Resolved / closed tickets ────────────────────────────────────
            if (doneTickets.isNotEmpty()) {
                item { TicketSectionHeader(title = "Histórico", count = doneTickets.size) }

                items(doneTickets, key = { it.id }) { ticket ->
                    TicketCard(
                        ticket = ticket,
                        isDriver = isDriver,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        onClick = { selectedTicket = ticket }
                    )
                }
            }
        }
    }
}

// ─── Quick help chip ──────────────────────────────────────────────────────────

@Composable
private fun QuickHelpChip(cat: SupportCategory, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier
            .width(110.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(shape = CircleShape, color = cat.bg, modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(cat.icon, null, tint = cat.color, modifier = Modifier.size(20.dp))
                }
            }
            Text(
                cat.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = Slate700,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─── Section header ───────────────────────────────────────────────────────────

@Composable
private fun TicketSectionHeader(title: String, count: Int, onNew: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate700)
        Spacer(Modifier.width(8.dp))
        Surface(shape = RoundedCornerShape(50), color = AppPurpleLight) {
            Text(
                "$count",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = AppPurple,
                fontWeight = FontWeight.Bold
            )
        }
        if (onNew != null) {
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = onNew,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Add, null, tint = AppPurple, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(3.dp))
                Text("Novo", color = AppPurple, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─── Ticket card ─────────────────────────────────────────────────────────────

@Composable
private fun TicketCard(
    ticket: SupportTicketResponse,
    isDriver: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val cat = categoryFor(ticket.type, isDriver)
    val (statusColor, statusLabel, statusBg) = when (ticket.status) {
        "OPEN"      -> Triple(AppBlue,   "Aberto",     AppBlueLight)
        "IN_REVIEW" -> Triple(AppAmber,  "Em análise", AppAmberLight)
        "RESOLVED"  -> Triple(AppGreen,  "Resolvido",  AppGreenLight)
        "CLOSED"    -> Triple(Slate500,  "Encerrado",  Slate100)
        else        -> Triple(Slate500,  ticket.status, Slate100)
    }

    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // Top row: category + status
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category chip
                cat?.let {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = CircleShape, color = it.bg, modifier = Modifier.size(28.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(it.icon, null, tint = it.color, modifier = Modifier.size(14.dp))
                            }
                        }
                        Text(it.label, style = MaterialTheme.typography.labelSmall, color = Slate500, fontWeight = FontWeight.Medium)
                    }
                }
                // Status badge
                Surface(shape = RoundedCornerShape(50), color = statusBg) {
                    Text(
                        statusLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Subject
            Text(
                ticket.subject,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Slate900,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Description preview
            Text(
                ticket.description,
                style = MaterialTheme.typography.bodySmall,
                color = Slate500,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Resolution highlight
            if (!ticket.resolution.isNullOrBlank()) {
                Surface(shape = RoundedCornerShape(10.dp), color = AppGreenBg) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = AppGreen, modifier = Modifier.size(14.dp).padding(top = 1.dp))
                        Text(ticket.resolution!!, style = MaterialTheme.typography.labelSmall, color = AppGreenDark, modifier = Modifier.weight(1f))
                    }
                }
            }

            // Footer: date + ticket ID
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    formatTicketDate(ticket.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400
                )
                Text(
                    "#${ticket.id.take(8).uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400
                )
            }
        }
    }
}

// ─── Ticket detail screen ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TicketDetailScreen(
    ticket: SupportTicketResponse,
    isDriver: Boolean,
    onBack: () -> Unit
) {
    val cat = categoryFor(ticket.type, isDriver)
    val (statusColor, statusLabel) = when (ticket.status) {
        "OPEN"      -> AppBlue  to "Aberto"
        "IN_REVIEW" -> AppAmber to "Em análise"
        "RESOLVED"  -> AppGreen to "Resolvido"
        "CLOSED"    -> Slate500 to "Encerrado"
        else        -> Slate500 to ticket.status
    }

    val statusStep = when (ticket.status) {
        "OPEN"      -> 0
        "IN_REVIEW" -> 1
        "RESOLVED"  -> 2
        "CLOSED"    -> 3
        else        -> 0
    }

    Scaffold(containerColor = Slate100) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(AppVioletDarker, AppPurple)))
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                        }
                        Text("Detalhe do chamado", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(ticket.subject, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 3)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            cat?.let {
                                Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.2f)) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(it.icon, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                        Text(it.label, style = MaterialTheme.typography.labelSmall, color = Color.White)
                                    }
                                }
                            }
                            Surface(shape = RoundedCornerShape(50), color = statusColor.copy(alpha = 0.25f)) {
                                Text(
                                    statusLabel,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Status timeline
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Andamento do chamado", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate700)
                    StatusTimeline(currentStep = statusStep)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Info card
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Informações", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate700)
                    HorizontalDivider(color = Slate150)
                    DetailInfoRow(Icons.Default.Tag, "Protocolo", "#${ticket.id.take(12).uppercase()}")
                    HorizontalDivider(color = Slate150)
                    DetailInfoRow(Icons.Default.CalendarToday, "Data de abertura", formatTicketDate(ticket.createdAt))
                    if (!ticket.updatedAt.isNullOrBlank() && ticket.updatedAt != ticket.createdAt) {
                        HorizontalDivider(color = Slate150)
                        DetailInfoRow(Icons.Default.Update, "Última atualização", formatTicketDate(ticket.updatedAt))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Description
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Descrição", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate700)
                    HorizontalDivider(color = Slate150)
                    Text(ticket.description, style = MaterialTheme.typography.bodyMedium, color = Slate700, lineHeight = 22.sp)
                }
            }

            // Resolution
            if (!ticket.resolution.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = AppGreenBg,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = AppGreen, modifier = Modifier.size(18.dp))
                            Text("Resolução", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = AppGreenDark)
                        }
                        HorizontalDivider(color = AppGreen.copy(alpha = 0.2f))
                        Text(ticket.resolution!!, style = MaterialTheme.typography.bodyMedium, color = AppGreenDark, lineHeight = 22.sp)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatusTimeline(currentStep: Int) {
    val steps = listOf("Aberto", "Em análise", "Resolvido", "Encerrado")
    val stepColors = listOf(AppBlue, AppAmber, AppGreen, Slate500)

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        steps.forEachIndexed { index, label ->
            val isDone    = index <= currentStep
            val isCurrent = index == currentStep
            val color     = if (isDone) stepColors[index] else Slate300

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(2.dp)
                                .align(Alignment.CenterStart)
                                .background(if (index <= currentStep) stepColors[index - 1] else Slate200)
                        )
                    }
                    if (index < steps.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(2.dp)
                                .align(Alignment.CenterEnd)
                                .background(if (index < currentStep) stepColors[index] else Slate200)
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = if (isDone) color else Color.White,
                        border = androidx.compose.foundation.BorderStroke(2.dp, color),
                        modifier = Modifier.size(if (isCurrent) 22.dp else 18.dp)
                    ) {
                        if (isDone && !isCurrent) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(10.dp))
                            }
                        }
                    }
                }
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDone) color else Slate400,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun DetailInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, tint = Slate400, modifier = Modifier.size(16.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = Slate500, modifier = Modifier.width(130.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = Slate800)
    }
}

// ─── Create ticket screen ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTicketSheet(
    isDriver: Boolean,
    initialType: String,
    initialRideId: String? = null,
    onDismiss: () -> Unit,
    onCreated: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var selectedType  by remember { mutableStateOf(initialType) }
    var subject       by remember { mutableStateOf("") }
    var description   by remember { mutableStateOf("") }
    var sending       by remember { mutableStateOf(false) }
    var sendError     by remember { mutableStateOf<String?>(null) }
    var sendSuccess   by remember { mutableStateOf(false) }

    val categories = if (isDriver) DRIVER_CATEGORIES else PASSENGER_CATEGORIES
    val canSend = subject.length >= 4 && description.length >= 10 && !sending

    if (sendSuccess) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1800)
            onCreated()
        }
    }

    Scaffold(containerColor = Slate100) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(AppVioletDarker, AppPurple)))
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss, enabled = !sending) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                        }
                        Text("Novo chamado", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                    Text(
                        "Descreva seu problema com detalhes para agilizarmos o atendimento.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.78f),
                        modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Success state
            if (sendSuccess) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(shape = CircleShape, color = AppGreenLight, modifier = Modifier.size(80.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CheckCircle, null, tint = AppGreen, modifier = Modifier.size(42.dp))
                        }
                    }
                    Text("Chamado enviado!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Slate900)
                    Text("Nossa equipe analisará sua solicitação e retornará em breve.", style = MaterialTheme.typography.bodyMedium, color = Slate500, textAlign = TextAlign.Center)
                }
                return@Column
            }

            // Linked ride row (if initialRideId is provided)
            if (initialRideId != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = AppPurpleLight,
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(shape = CircleShape, color = AppPurple.copy(alpha = 0.12f), modifier = Modifier.size(36.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.DirectionsCar, null, Modifier.size(18.dp), tint = AppVioletDarker)
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Corrida vinculada", style = MaterialTheme.typography.labelSmall, color = Slate500)
                            Text(
                                "#${initialRideId.take(8).uppercase()}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AppVioletDarker
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }

            // Category grid
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Categoria", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate700)
                    HorizontalDivider(color = Slate150)
                    val chunked = categories.chunked(2)
                    chunked.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { cat ->
                                val isSelected = selectedType == cat.key
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedType = cat.key }
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) cat.color else Slate200,
                                            shape = RoundedCornerShape(14.dp)
                                        ),
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) cat.bg else Color.White
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(shape = CircleShape, color = if (isSelected) cat.color.copy(0.15f) else Slate100, modifier = Modifier.size(32.dp)) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(cat.icon, null, tint = if (isSelected) cat.color else Slate400, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Text(
                                            cat.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) cat.color else Slate600,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Subject + description
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Detalhes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate700)
                    HorizontalDivider(color = Slate150)

                    OutlinedTextField(
                        value = subject,
                        onValueChange = { if (it.length <= 120) subject = it },
                        label = { Text("Assunto") },
                        placeholder = { Text("Descreva o problema em poucas palavras") },
                        singleLine = true,
                        isError = subject.isNotEmpty() && subject.length < 4,
                        supportingText = {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                if (subject.isNotEmpty() && subject.length < 4) Text("Mínimo de 4 caracteres", color = MaterialTheme.colorScheme.error)
                                else Spacer(Modifier.width(0.dp))
                                Text("${subject.length}/120", color = Slate400)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppPurple, focusedLabelColor = AppPurple),
                        enabled = !sending
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { if (it.length <= 1000) description = it },
                        label = { Text("Descrição") },
                        placeholder = { Text("Explique o que aconteceu com o máximo de detalhes") },
                        minLines = 5,
                        maxLines = 8,
                        isError = description.isNotEmpty() && description.length < 10,
                        supportingText = {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                if (description.isNotEmpty() && description.length < 10) Text("Mínimo de 10 caracteres", color = MaterialTheme.colorScheme.error)
                                else Spacer(Modifier.width(0.dp))
                                Text("${description.length}/1000", color = Slate400)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppPurple, focusedLabelColor = AppPurple),
                        enabled = !sending
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Error
            sendError?.let {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = AppRedLight
                ) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = AppRed, modifier = Modifier.size(18.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = AppRedDark, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Send button
            Button(
                onClick = {
                    scope.launch {
                        sending = true; sendError = null
                        try {
                            RetrofitClient.api.createTicket(
                                CreateTicketRequest(
                                    subject = subject.trim(),
                                    description = description.trim(),
                                    type = selectedType,
                                    rideId = initialRideId
                                )
                            )
                            sendSuccess = true
                        } catch (e: Exception) {
                            sendError = e.message ?: "Erro ao enviar chamado"
                        } finally {
                            sending = false
                        }
                    }
                },
                enabled = canSend,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppPurple)
            ) {
                if (sending) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.5.dp, color = Color.White)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Enviar chamado", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
