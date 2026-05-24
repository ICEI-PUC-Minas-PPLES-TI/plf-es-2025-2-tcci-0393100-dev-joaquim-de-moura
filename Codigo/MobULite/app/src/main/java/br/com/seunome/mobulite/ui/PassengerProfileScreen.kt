package br.com.seunome.mobulite.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.seunome.mobulite.data.local.SessionStore
import br.com.seunome.mobulite.data.remote.*
import br.com.seunome.mobulite.ui.theme.*
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ─── Filter ──────────────────────────────────────────────────────────────────

private enum class RideHistoryFilter(val label: String) {
    ALL("Todas"), FINISHED("Concluídas"), CANCELED("Canceladas")
}

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerProfileScreen(
    onBack: () -> Unit,
    onSupport: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val sessionStore = remember { SessionStore(context) }

    // ── Data state ──────────────────────────────────────────────────────────
    var user            by remember { mutableStateOf<AuthUser?>(null) }
    var rides           by remember { mutableStateOf<List<RideHistoryItem>>(emptyList()) }
    var loadingData     by remember { mutableStateOf(true) }
    var loadError       by remember { mutableStateOf<String?>(null) }
    var profilePhotoPath by remember { mutableStateOf<String?>(null) }
    var uploadingPhoto  by remember { mutableStateOf(false) }

    // ── Name edit ───────────────────────────────────────────────────────────
    var editingName by remember { mutableStateOf(false) }
    var nameField   by remember { mutableStateOf("") }
    var savingName  by remember { mutableStateOf(false) }
    var nameError   by remember { mutableStateOf<String?>(null) }

    // ── Verification ───────────────────────────────────────────────────────
    var emailField by remember { mutableStateOf("") }
    var phoneCode by remember { mutableStateOf("") }
    var emailCode by remember { mutableStateOf("") }
    var phoneVerificationBusy by remember { mutableStateOf(false) }
    var emailVerificationBusy by remember { mutableStateOf(false) }
    var verificationMessage by remember { mutableStateOf<String?>(null) }

    // ── Password change ─────────────────────────────────────────────────────
    var showChangePassword by remember { mutableStateOf(false) }
    var currentPass  by remember { mutableStateOf("") }
    var newPass      by remember { mutableStateOf("") }
    var confirmPass  by remember { mutableStateOf("") }
    var showCurVis   by remember { mutableStateOf(false) }
    var showNewVis   by remember { mutableStateOf(false) }
    var changingPass by remember { mutableStateOf(false) }
    var passError    by remember { mutableStateOf<String?>(null) }
    var passSuccess  by remember { mutableStateOf(false) }

    // ── Legal screens ───────────────────────────────────────────────────────
    var showLegal by remember { mutableStateOf<LegalType?>(null) }

    // ── UI state ────────────────────────────────────────────────────────────
    var historyFilter   by remember { mutableStateOf(RideHistoryFilter.ALL) }
    var historyPage     by remember { mutableStateOf(1) }
    val historyPageSize = 10
    var notifRides      by remember { mutableStateOf(true) }
    var notifPromos     by remember { mutableStateOf(false) }
    var showDeleteDlg   by remember { mutableStateOf(false) }
    var deletingAccount by remember { mutableStateOf(false) }

    // ── Photo picker ────────────────────────────────────────────────────────
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) scope.launch {
            uploadingPhoto = true
            try {
                val original = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                    ?: return@launch
                val maxDim = 800
                val scale = minOf(1f, minOf(maxDim.toFloat() / original.width, maxDim.toFloat() / original.height))
                val scaled = if (scale < 1f)
                    Bitmap.createScaledBitmap(original, (original.width * scale).toInt(), (original.height * scale).toInt(), true)
                else original
                val dest = File(context.filesDir, "profile_photo.jpg")
                FileOutputStream(dest).use { o -> scaled.compress(Bitmap.CompressFormat.JPEG, 80, o) }
                if (scaled !== original) scaled.recycle()
                original.recycle()
                profilePhotoPath = dest.absolutePath
                sessionStore.saveProfilePhotoPath(dest.absolutePath)
                val part = MultipartBody.Part.createFormData(
                    "photo", dest.name, dest.asRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                user = RetrofitClient.api.uploadProfilePhoto(part)
            } catch (_: Exception) {
            } finally { uploadingPhoto = false }
        }
    }

    fun loadData() = scope.launch {
        loadingData = true; loadError = null
        try {
            val me = RetrofitClient.api.me()
            user = me; nameField = me.name ?: ""; emailField = me.email ?: ""
            rides = RetrofitClient.api.getMyRides()
        } catch (e: Exception) {
            loadError = e.message ?: "Erro ao carregar perfil"
        } finally { loadingData = false }
    }

    val notifPrefs = remember { context.getSharedPreferences("passenger_notif_prefs", Context.MODE_PRIVATE) }

    LaunchedEffect(Unit) {
        val path = sessionStore.profilePhotoPathFlow.first()
        if (path != null && File(path).exists()) profilePhotoPath = path
        else if (path != null) sessionStore.saveProfilePhotoPath(null)
        notifRides  = notifPrefs.getBoolean("notif_rides", true)
        notifPromos = notifPrefs.getBoolean("notif_promos", false)
        loadData()
    }

    LaunchedEffect(historyFilter) { historyPage = 1 }

    // ── Derived stats ────────────────────────────────────────────────────────
    val initial       = user?.name?.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
    val finishedRides = rides.filter { it.status == "FINISHED" }
    val finishedCount = finishedRides.size
    val canceledCount = rides.count { it.status == "CANCELED" }
    val totalCents    = finishedRides.sumOf { it.estimatedFareCents ?: 0 }
    val totalKm       = finishedRides.sumOf { it.distanceMeters ?: 0 } / 1000.0
    val filteredRides = when (historyFilter) {
        RideHistoryFilter.ALL      -> rides
        RideHistoryFilter.FINISHED -> finishedRides
        RideHistoryFilter.CANCELED -> rides.filter { it.status == "CANCELED" }
    }

    // ── Legal screens ────────────────────────────────────────────────────────
    showLegal?.let { legalType ->
        LegalScreen(type = legalType, onBack = { showLegal = null })
        return
    }

    // ── Delete dialog ────────────────────────────────────────────────────────
    if (showDeleteDlg) {
        DeleteAccountDialog(
            userName = user?.name,
            onDismiss = { showDeleteDlg = false },
            onConfirmed = {
                scope.launch {
                    deletingAccount = true
                    try {
                        RetrofitClient.api.deleteAccount()
                        sessionStore.clear()
                        showDeleteDlg = false
                        onLogout()
                    } catch (e: Exception) {
                        deletingAccount = false
                    }
                }
            },
            deleting = deletingAccount
        )
    }

    // ── Root layout ──────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize().background(AppLilacSoft)) {

        LazyColumn(modifier = Modifier.fillMaxSize()) {

            // ── Hero header ─────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppPurple)
                ) {
                    Column {
                        // Top bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                            }
                            Text("Meu Perfil", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                            IconButton(onClick = { loadData() }, enabled = !loadingData) {
                                Icon(Icons.Default.Refresh, null, tint = Color.White.copy(if (loadingData) 0.35f else 1f))
                            }
                        }

                        // Avatar
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(CircleShape)
                                        .background(AppPurpleLight)
                                        .border(3.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                                        .clickable(enabled = !uploadingPhoto) { photoLauncher.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    val serverUrl = RetrofitClient.photoUrl(user?.profilePhotoUrl)
                                    when {
                                        uploadingPhoto  -> CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 3.dp, color = AppPurple)
                                        serverUrl != null -> AsyncImage(serverUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                                        profilePhotoPath != null -> AsyncImage(File(profilePhotoPath!!), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                                        else -> Text(initial, color = AppPurple, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = AppPurple,
                                    modifier = Modifier.size(30.dp).border(2.dp, Color.White, CircleShape)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(15.dp))
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    user?.name ?: if (loadingData) "" else "—",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (!user?.phone.isNullOrBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Phone, null, tint = Color.White.copy(0.65f), modifier = Modifier.size(13.dp))
                                        Text(formatPhoneBr(user?.phone ?: ""), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.8f))
                                    }
                                }
                            }

                            // Header stat chips
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HeroChip(Icons.Default.DirectionsCar, "$finishedCount", "Corridas", Modifier.weight(1f))
                                HeroChip(Icons.Default.Route, "${"%.0f".format(totalKm)} km", "Percorridos", Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // ── Error banner ────────────────────────────────────────────────
            if (loadError != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppRedLight)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, null, tint = AppRed, modifier = Modifier.size(20.dp))
                        Text(loadError!!, color = AppRedDark, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        TextButton(onClick = { loadData() }, contentPadding = PaddingValues(0.dp)) {
                            Text("Tentar", color = AppRedDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── Stats grid ──────────────────────────────────────────────────
            item {
                PCard(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(AppPurpleLight), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.BarChart, null, tint = AppPurple, modifier = Modifier.size(17.dp))
                        }
                        Text("Resumo da conta", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate700)
                    }
                    HorizontalDivider(color = Slate150)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCell(Icons.Default.CheckCircle, AppGreen, "$finishedCount", "Concluídas", Modifier.weight(1f))
                        StatCell(Icons.Default.Cancel, AppRed, "$canceledCount", "Canceladas", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCell(Icons.Default.AccountBalanceWallet, AppPurple, "R$ ${"%.0f".format(totalCents / 100.0)}", "Total gasto", Modifier.weight(1f))
                        StatCell(Icons.Default.Route, AppBlue, "${"%.1f".format(totalKm)} km", "Distância", Modifier.weight(1f))
                    }
                }
            }

            // ── Conta ───────────────────────────────────────────────────────
            item { SectionLabel("Conta", Icons.Default.ManageAccounts, Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp)) }

            // Dados pessoais
            item {
                PCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(AppPurpleLight), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, null, tint = AppPurple, modifier = Modifier.size(17.dp))
                            }
                            Text("Dados pessoais", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Slate700)
                        }
                        if (!editingName) {
                            TextButton(onClick = { editingName = true; nameField = user?.name ?: "" }) {
                                Text("Editar", color = AppPurple, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    HorizontalDivider(color = Slate150)

                    if (editingName) {
                        OutlinedTextField(
                            value = nameField, onValueChange = { nameField = it },
                            label = { Text("Nome completo") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            shape = RoundedCornerShape(12.dp), enabled = !savingName,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppPurple, focusedLabelColor = AppPurple)
                        )
                        nameError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { editingName = false; nameError = null }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                                Text("Cancelar")
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        savingName = true; nameError = null
                                        try {
                                            user = RetrofitClient.api.updateProfile(UpdateProfileRequest(nameField.trim()))
                                            editingName = false
                                        } catch (e: Exception) {
                                            nameError = e.message ?: "Erro ao salvar"
                                        } finally { savingName = false }
                                    }
                                },
                                enabled = !savingName && nameField.isNotBlank(),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AppPurple)
                            ) { Text(if (savingName) "Salvando…" else "Salvar") }
                        }
                    } else {
                        PInfoRow(Icons.Default.Person, "Nome completo", user?.name ?: "—")
                        HorizontalDivider(color = Slate150)
                        PInfoRow(Icons.Default.Phone, "Telefone", formatPhoneBr(user?.phone ?: "—"))
                        HorizontalDivider(color = Slate150)
                        VerificationStatusRow(
                            icon = Icons.Default.Phone,
                            title = "Telefone",
                            value = if (user?.phoneVerifiedAt != null) "Verificado" else "Pendente",
                            verified = user?.phoneVerifiedAt != null,
                        )
                        if (user?.phoneVerifiedAt == null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = phoneCode,
                                    onValueChange = { phoneCode = it.filter(Char::isDigit).take(6) },
                                    label = { Text("Código SMS") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = !phoneVerificationBusy,
                                )
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            phoneVerificationBusy = true
                                            verificationMessage = null
                                            try {
                                                val ack = RetrofitClient.api.requestPhoneVerification()
                                                verificationMessage = ack.devHint?.let { "Código SMS: $it" } ?: ack.message ?: "Código enviado"
                                            } catch (e: Exception) {
                                                verificationMessage = e.message ?: "Falha ao enviar código"
                                            } finally { phoneVerificationBusy = false }
                                        }
                                    },
                                    enabled = !phoneVerificationBusy,
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("Enviar") }
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        phoneVerificationBusy = true
                                        verificationMessage = null
                                        try {
                                            RetrofitClient.api.confirmPhoneVerification(ConfirmCodeBody(phoneCode))
                                            user = RetrofitClient.api.me()
                                            phoneCode = ""
                                            verificationMessage = "Telefone verificado"
                                        } catch (e: Exception) {
                                            verificationMessage = e.message ?: "Código inválido"
                                        } finally { phoneVerificationBusy = false }
                                    }
                                },
                                enabled = !phoneVerificationBusy && phoneCode.length == 6,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AppPurple)
                            ) { Text(if (phoneVerificationBusy) "Aguarde..." else "Confirmar telefone") }
                        }
                        HorizontalDivider(color = Slate150)
                        VerificationStatusRow(
                            icon = Icons.Default.Email,
                            title = "E-mail",
                            value = user?.email ?: "Não informado",
                            verified = user?.emailVerifiedAt != null,
                        )
                        OutlinedTextField(
                            value = emailField,
                            onValueChange = { emailField = it.trim() },
                            label = { Text("E-mail") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            enabled = !emailVerificationBusy,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = emailCode,
                                onValueChange = { emailCode = it.filter(Char::isDigit).take(6) },
                                label = { Text("Código e-mail") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                enabled = !emailVerificationBusy,
                            )
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        emailVerificationBusy = true
                                        verificationMessage = null
                                        try {
                                            if (emailField != user?.email) {
                                                RetrofitClient.api.setEmail(SetEmailBody(emailField))
                                            }
                                            val ack = RetrofitClient.api.requestEmailVerification()
                                            user = RetrofitClient.api.me()
                                            verificationMessage = ack.devHint?.let { "Código e-mail: $it" } ?: ack.message ?: "Código enviado"
                                        } catch (e: Exception) {
                                            verificationMessage = e.message ?: "Falha ao enviar código"
                                        } finally { emailVerificationBusy = false }
                                    }
                                },
                                enabled = !emailVerificationBusy && emailField.isNotBlank(),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Enviar") }
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    emailVerificationBusy = true
                                    verificationMessage = null
                                    try {
                                        RetrofitClient.api.confirmEmailVerification(ConfirmCodeBody(emailCode))
                                        user = RetrofitClient.api.me()
                                        emailCode = ""
                                        verificationMessage = "E-mail verificado"
                                    } catch (e: Exception) {
                                        verificationMessage = e.message ?: "Código inválido"
                                    } finally { emailVerificationBusy = false }
                                }
                            },
                            enabled = !emailVerificationBusy && emailCode.length == 6,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppPurple)
                        ) { Text(if (emailVerificationBusy) "Aguarde..." else "Confirmar e-mail") }
                        verificationMessage?.let {
                            Text(it, color = Slate600, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Segurança
            item {
                PCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(AppGreenBg), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Lock, null, tint = AppGreen, modifier = Modifier.size(17.dp))
                            }
                            Text("Segurança", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Slate700)
                        }
                        TextButton(onClick = {
                            showChangePassword = !showChangePassword
                            passError = null; passSuccess = false
                            currentPass = ""; newPass = ""; confirmPass = ""
                        }) {
                            Text(if (showChangePassword) "Cancelar" else "Alterar senha", color = AppPurple, style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    AnimatedVisibility(showChangePassword) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            HorizontalDivider(color = Slate150)
                            if (passSuccess) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.CheckCircle, null, tint = AppGreen, modifier = Modifier.size(18.dp))
                                    Text("Senha alterada com sucesso!", color = AppGreen, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                }
                            } else {
                                OutlinedTextField(
                                    value = currentPass, onValueChange = { currentPass = it },
                                    label = { Text("Senha atual") },
                                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                                    trailingIcon = {
                                        IconButton(onClick = { showCurVis = !showCurVis }) {
                                            Icon(if (showCurVis) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                                        }
                                    },
                                    visualTransformation = if (showCurVis) VisualTransformation.None else PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !changingPass,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppPurple, focusedLabelColor = AppPurple)
                                )
                                OutlinedTextField(
                                    value = newPass, onValueChange = { newPass = it },
                                    label = { Text("Nova senha (mín. 6 caracteres)") },
                                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                                    trailingIcon = {
                                        IconButton(onClick = { showNewVis = !showNewVis }) {
                                            Icon(if (showNewVis) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                                        }
                                    },
                                    visualTransformation = if (showNewVis) VisualTransformation.None else PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !changingPass,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppPurple, focusedLabelColor = AppPurple)
                                )
                                OutlinedTextField(
                                    value = confirmPass, onValueChange = { confirmPass = it },
                                    label = { Text("Confirmar nova senha") },
                                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !changingPass,
                                    shape = RoundedCornerShape(12.dp),
                                    isError = confirmPass.isNotEmpty() && confirmPass != newPass,
                                    supportingText = {
                                        if (confirmPass.isNotEmpty() && confirmPass != newPass)
                                            Text("As senhas não coincidem", color = MaterialTheme.colorScheme.error)
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppPurple, focusedLabelColor = AppPurple)
                                )
                                passError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                                Button(
                                    onClick = {
                                        scope.launch {
                                            changingPass = true; passError = null
                                            try {
                                                RetrofitClient.api.changePassword(ChangePasswordRequest(currentPass, newPass))
                                                passSuccess = true; currentPass = ""; newPass = ""; confirmPass = ""
                                            } catch (e: HttpException) {
                                                val body = e.response()?.errorBody()?.string()
                                                passError = try { org.json.JSONObject(body ?: "").optString("message", "Erro ${e.code()}") } catch (_: Exception) { "Erro ${e.code()}" }
                                            } catch (e: Exception) {
                                                passError = e.message ?: "Erro"
                                            } finally { changingPass = false }
                                        }
                                    },
                                    enabled = !changingPass && currentPass.isNotBlank() && newPass.length >= 6 && newPass == confirmPass,
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AppPurple)
                                ) { Text(if (changingPass) "Salvando…" else "Salvar nova senha", fontWeight = FontWeight.SemiBold) }
                            }
                        }
                    }
                }
            }

            // ── Preferências ─────────────────────────────────────────────────
            item { SectionLabel("Preferências", Icons.Default.Tune, Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)) }
            item {
                PCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(AppAmberLight), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Notifications, null, tint = AppAmber, modifier = Modifier.size(17.dp))
                        }
                        Text("Notificações", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Slate700)
                    }
                    HorizontalDivider(color = Slate150)
                    PrefToggle(
                        icon = Icons.Default.NotificationsActive,
                        title = "Atualizações de corrida",
                        subtitle = "Motorista a caminho, chegada e conclusão",
                        checked = notifRides,
                        onToggle = {
                            notifRides = it
                            notifPrefs.edit().putBoolean("notif_rides", it).apply()
                        }
                    )
                    HorizontalDivider(color = Slate150)
                    PrefToggle(
                        icon = Icons.Default.LocalOffer,
                        title = "Promoções e novidades",
                        subtitle = "Descontos e novidades exclusivos",
                        checked = notifPromos,
                        onToggle = {
                            notifPromos = it
                            notifPrefs.edit().putBoolean("notif_promos", it).apply()
                        }
                    )
                }
            }

            // ── Histórico ────────────────────────────────────────────────────
            item { SectionLabel("Histórico de corridas", Icons.Default.History, Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp)) }

            item {
                // Filter tabs
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RideHistoryFilter.entries.forEach { f ->
                        val isSelected = f == historyFilter
                        val bg by animateColorAsState(if (isSelected) AppVioletDark else Color.Transparent, tween(200), label = "tab")
                        val tc by animateColorAsState(if (isSelected) Color.White else Slate500, tween(200), label = "tabt")
                        val count = when (f) {
                            RideHistoryFilter.ALL -> rides.size
                            RideHistoryFilter.FINISHED -> finishedCount
                            RideHistoryFilter.CANCELED -> canceledCount
                        }
                        Box(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(bg).clickable { historyFilter = f }.padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${f.label} ($count)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = tc
                            )
                        }
                    }
                }
            }

            if (filteredRides.isEmpty() && !loadingData) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(Modifier.size(64.dp).clip(CircleShape).background(Slate150), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.DirectionsCar, null, tint = Slate400, modifier = Modifier.size(32.dp))
                        }
                        Text(
                            if (historyFilter == RideHistoryFilter.ALL) "Nenhuma corrida ainda" else "Nenhuma corrida ${historyFilter.label.lowercase()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate500
                        )
                    }
                }
            } else {
                val paginatedRides = filteredRides.take(historyPage * historyPageSize)
                items(paginatedRides, key = { it.id }) { ride ->
                    PassengerRideCard(ride = ride, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }
                if (filteredRides.size > paginatedRides.size) {
                    item {
                        val remaining = filteredRides.size - paginatedRides.size
                        TextButton(
                            onClick = { historyPage++ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ExpandMore, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ver mais $remaining corrida${if (remaining > 1) "s" else ""}")
                        }
                    }
                }
            }

            // ── Sobre ────────────────────────────────────────────────────────
            item { SectionLabel("Sobre o app", Icons.Default.Info, Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp)) }
            item {
                PCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(AppBlueBg), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Info, null, tint = AppBlue, modifier = Modifier.size(17.dp))
                        }
                        Text("Informações", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Slate700)
                    }
                    HorizontalDivider(color = Slate150)
                    AboutRow(Icons.Default.HeadsetMic, "Suporte ao cliente", "Dúvidas, reclamações e sugestões", onSupport)
                    HorizontalDivider(color = Slate150)
                    AboutRow(Icons.Default.Description, "Termos de uso", "Leia os termos de serviço do app") {
                        showLegal = LegalType.TERMS
                    }
                    HorizontalDivider(color = Slate150)
                    AboutRow(Icons.Default.Shield, "Política de privacidade", "Como tratamos seus dados pessoais") {
                        showLegal = LegalType.PRIVACY
                    }
                    HorizontalDivider(color = Slate150)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PhoneAndroid, null, tint = Slate400, modifier = Modifier.size(18.dp))
                            Column {
                                Text("Versão do aplicativo", style = MaterialTheme.typography.bodySmall, color = Slate700)
                                Text("1.0.0 (build 1)", style = MaterialTheme.typography.labelSmall, color = Slate400)
                            }
                        }
                        Surface(shape = RoundedCornerShape(8.dp), color = AppPurpleLight) {
                            Text("Atual", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = AppPurple, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── Ações finais ──────────────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppRed)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sair da conta", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                    TextButton(
                        onClick = { showDeleteDlg = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteForever, null, tint = Slate400, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Excluir minha conta", color = Slate400, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }

        // Loading overlay (only on first load, user == null)
        if (loadingData && user == null) {
            Box(Modifier.fillMaxSize().background(Color.White.copy(0.6f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = AppPurple)
                    Text("Carregando perfil…", style = MaterialTheme.typography.bodyMedium, color = Slate500)
                }
            }
        }
    }
}

// ─── Header chip ──────────────────────────────────────────────────────────────

@Composable
private fun HeroChip(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = 0.15f), modifier = modifier) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(icon, null, tint = Color.White.copy(0.8f), modifier = Modifier.size(16.dp))
            Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleSmall)
            Text(label, color = Color.White.copy(0.7f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

// ─── Stat cell ────────────────────────────────────────────────────────────────

@Composable
private fun StatCell(icon: ImageVector, color: Color, value: String, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.07f)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(17.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = Slate850, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, style = MaterialTheme.typography.labelSmall, color = Slate500)
        }
    }
}

// ─── Section label ────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(title: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, null, tint = AppVioletDark, modifier = Modifier.size(15.dp))
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Slate600)
    }
}

// ─── Card container ───────────────────────────────────────────────────────────

@Composable
private fun PCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 2.dp) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

// ─── Info row ─────────────────────────────────────────────────────────────────

@Composable
private fun PInfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(AppLilacSoft), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = AppPurple, modifier = Modifier.size(16.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Slate500)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Slate850)
        }
    }
}

@Composable
private fun VerificationStatusRow(
    icon: ImageVector,
    title: String,
    value: String,
    verified: Boolean,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(if (verified) AppGreenBg else AppAmberLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = if (verified) AppGreen else AppAmber, modifier = Modifier.size(16.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = Slate500)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Slate850)
        }
        Surface(shape = RoundedCornerShape(999.dp), color = if (verified) AppGreenBg else AppAmberLight) {
            Text(
                if (verified) "Verificado" else "Pendente",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (verified) AppGreen else AppAmber,
            )
        }
    }
}

// ─── Preference toggle row ────────────────────────────────────────────────────

@Composable
private fun PrefToggle(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Slate500, modifier = Modifier.size(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Slate850)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Slate500)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AppPurple)
        )
    }
}

// ─── About row ────────────────────────────────────────────────────────────────

@Composable
private fun AboutRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Slate500, modifier = Modifier.size(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = Slate850)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Slate500)
            }
        }
        Icon(Icons.Default.ChevronRight, null, tint = Slate400, modifier = Modifier.size(18.dp))
    }
}

// ─── Delete account dialog ────────────────────────────────────────────────────

@Composable
private fun DeleteAccountDialog(
    userName: String?,
    onDismiss: () -> Unit,
    onConfirmed: () -> Unit,
    deleting: Boolean
) {
    var step by remember { mutableStateOf(0) } // 0 = warning, 1 = confirm input
    var typed by remember { mutableStateOf("") }
    val confirmWord = "EXCLUIR"
    val canDelete = typed.trim() == confirmWord && !deleting

    AlertDialog(
        onDismissRequest = { if (!deleting) onDismiss() },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        icon = {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(AppRedLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (step == 0) Icons.Default.DeleteForever else Icons.Default.Warning,
                    null,
                    tint = AppRed,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                if (step == 0) "Excluir conta?" else "Tem certeza absoluta?",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = Slate900
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (step == 0) {
                    Text(
                        "Esta ação é permanente e não pode ser desfeita.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppRed
                    )
                    Spacer(Modifier.height(2.dp))
                    val consequences = listOf(
                        "Seus dados pessoais serão apagados",
                        "Você perderá o histórico de corridas",
                        "Preferências e configurações serão removidas",
                        "Não será possível recuperar a conta"
                    )
                    consequences.forEach { item ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Cancel,
                                null,
                                tint = AppRed,
                                modifier = Modifier.size(15.dp).padding(top = 2.dp)
                            )
                            Text(item, style = MaterialTheme.typography.bodySmall, color = Slate700)
                        }
                    }
                } else {
                    Text(
                        "Para confirmar, digite $confirmWord no campo abaixo:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate600
                    )
                    OutlinedTextField(
                        value = typed,
                        onValueChange = { typed = it.uppercase() },
                        placeholder = { Text(confirmWord, color = Slate300) },
                        singleLine = true,
                        enabled = !deleting,
                        isError = typed.isNotEmpty() && typed != confirmWord,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppRed,
                            focusedLabelColor = AppRed,
                            errorBorderColor = AppRed
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (typed.isNotEmpty() && typed != confirmWord) {
                        Text(
                            "Digite exatamente: $confirmWord",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppRed
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (step == 0) {
                Button(
                    onClick = { step = 1 },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppRed)
                ) {
                    Text("Continuar", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onConfirmed,
                    enabled = canDelete,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppRed,
                        disabledContainerColor = AppRedLight
                    )
                ) {
                    if (deleting) {
                        CircularProgressIndicator(
                            Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        if (deleting) "Excluindo…" else "Excluir permanentemente",
                        fontWeight = FontWeight.Bold,
                        color = if (canDelete) Color.White else AppRed
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = { if (step == 1) step = 0 else onDismiss() },
                enabled = !deleting
            ) {
                Text(
                    if (step == 1) "Voltar" else "Cancelar",
                    color = Slate500
                )
            }
        }
    )
}

// ─── Ride card ────────────────────────────────────────────────────────────────

@Composable
private fun PassengerRideCard(ride: RideHistoryItem, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val chevron  by animateFloatAsState(if (expanded) 180f else 0f, animationSpec = tween(200), label = "chevron")

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault())
    }
    val date = remember(ride.createdAt) {
        try { dateFormatter.format(Instant.parse(ride.createdAt)) } catch (_: Exception) { ride.createdAt }
    }

    val (statusLabel, statusColor) = when (ride.status) {
        "FINISHED"    -> "Concluída"    to AppGreen
        "CANCELED"    -> "Cancelada"    to AppRed
        "IN_PROGRESS" -> "Em andamento" to AppPurple
        else          -> ride.status    to Slate500
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier.fillMaxWidth().clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(date, style = MaterialTheme.typography.labelMedium, color = Slate500)
                    ride.destinationAddress?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = Slate700, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(50), color = statusColor.copy(alpha = 0.12f)) {
                        Text(
                            statusLabel,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(Icons.Default.ExpandMore, null, tint = Slate500, modifier = Modifier.size(18.dp).rotate(chevron))
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(thickness = 0.5.dp, color = Slate200)

                    ride.originAddress?.let {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = AppPurple, modifier = Modifier.size(14.dp))
                            Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2, color = Slate700, modifier = Modifier.weight(1f))
                        }
                    }
                    ride.destinationAddress?.let {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Flag, null, tint = AppVioletDark, modifier = Modifier.size(14.dp))
                            Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2, color = Slate700, modifier = Modifier.weight(1f))
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        ride.driverName?.let {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Person, null, tint = Slate400, modifier = Modifier.size(13.dp))
                                Text(it, style = MaterialTheme.typography.bodySmall, color = Slate500)
                            }
                        }
                        ride.estimatedFareCents?.let { cents ->
                            Surface(shape = RoundedCornerShape(8.dp), color = AppPurpleLight) {
                                Text(
                                    "R$ ${"%.2f".format(cents / 100.0)}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AppPurple
                                )
                            }
                        }
                    }

                    ride.rating?.let { score ->
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                            repeat(5) { i ->
                                Icon(
                                    if (i < score) Icons.Default.Star else Icons.Default.StarBorder,
                                    null,
                                    tint = if (i < score) RatingStarYellow else Slate200,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(" Sua avaliação", style = MaterialTheme.typography.labelSmall, color = Slate500)
                        }
                    }
                }
            }
        }
    }
}
