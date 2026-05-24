package br.com.seunome.mobulite.ui

import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.seunome.mobulite.R
import br.com.seunome.mobulite.data.remote.DriverProfileUpdateRequest
import br.com.seunome.mobulite.data.remote.DriverRegisterRequest
import br.com.seunome.mobulite.data.remote.RetrofitClient
import br.com.seunome.mobulite.ui.theme.AppGreen
import br.com.seunome.mobulite.ui.theme.AppGreenLight
import br.com.seunome.mobulite.ui.theme.AppLilacSoft
import br.com.seunome.mobulite.ui.theme.AppPurple
import br.com.seunome.mobulite.ui.theme.AppPurpleLight
import br.com.seunome.mobulite.ui.theme.AppRed
import br.com.seunome.mobulite.ui.theme.AppRedLight
import br.com.seunome.mobulite.ui.theme.AppVioletDark
import br.com.seunome.mobulite.ui.theme.Slate100
import br.com.seunome.mobulite.ui.theme.Slate200
import br.com.seunome.mobulite.ui.theme.Slate500
import br.com.seunome.mobulite.ui.theme.Slate700
import br.com.seunome.mobulite.ui.theme.Slate900
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import retrofit2.HttpException
import java.io.File
import java.io.FileOutputStream

private fun isValidCpf(cpf: String): Boolean {
    val digits = cpf.filter { it.isDigit() }
    if (digits.length != 11) return false
    if (digits.all { it == digits[0] }) return false
    fun calcDigit(sub: String, weights: IntArray): Int {
        val sum = sub.mapIndexed { i, c -> c.digitToInt() * weights[i] }.sum()
        val rem = sum % 11
        return if (rem < 2) 0 else 11 - rem
    }
    val w1 = intArrayOf(10, 9, 8, 7, 6, 5, 4, 3, 2)
    val w2 = intArrayOf(11, 10, 9, 8, 7, 6, 5, 4, 3, 2)
    val d1 = calcDigit(digits.substring(0, 9), w1)
    val d2 = calcDigit(digits.substring(0, 10), w2)
    return digits[9].digitToInt() == d1 && digits[10].digitToInt() == d2
}

private fun passwordStrength(password: String): Int {
    if (password.length < 6) return 0
    var score = 1
    if (password.length >= 8) score++
    if (password.any { it.isUpperCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    return score.coerceAtMost(4)
}

private fun formatCpf(raw: String): String {
    val digits = raw.filter { it.isDigit() }.take(11)
    return buildString {
        digits.forEachIndexed { i, c ->
            if (i == 3 || i == 6) append('.')
            if (i == 9) append('-')
            append(c)
        }
    }
}

private fun formatCnhDate(raw: String): String {
    val digits = raw.filter { it.isDigit() }.take(8)
    return buildString {
        digits.forEachIndexed { i, c ->
            if (i == 2 || i == 4) append('/')
            append(c)
        }
    }
}

private fun cnhDateToIso(ddMmYyyy: String): String? {
    val digits = ddMmYyyy.filter { it.isDigit() }
    if (digits.length < 8) return null
    return "${digits.substring(4, 8)}-${digits.substring(2, 4)}-${digits.substring(0, 2)}"
}

private fun formatPlate(raw: String): String {
    val clean = raw.uppercase().filter { it.isLetterOrDigit() }.take(7)
    return buildString {
        clean.forEachIndexed { i, c ->
            if (i == 3) append('-')
            append(c)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverRegisterScreen(
    onBackToLogin: () -> Unit,
    onRegistered: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // ── Step state ─────────────────────────────────────────────────────────────
    var step by remember { mutableStateOf(1) }

    // ── Step 1: Personal data ──────────────────────────────────────────────────
    var name by remember { mutableStateOf("") }
    var cpf by remember { mutableStateOf(textFieldValueAtEnd("")) }
    var phone by remember { mutableStateOf(textFieldValueAtEnd("")) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var acceptedTerms by remember { mutableStateOf(false) }
    var showLegal by remember { mutableStateOf<LegalType?>(null) }

    // ── Step 2: CNH ────────────────────────────────────────────────────────────
    var cnhNumber by remember { mutableStateOf("") }
    var cnhCategory by remember { mutableStateOf("") }
    var cnhExpiresAt by remember { mutableStateOf(textFieldValueAtEnd("")) }
    var hasEar by remember { mutableStateOf(false) }
    var showEarHelp by remember { mutableStateOf(false) }
    var cnhImageUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        cnhImageUri = uri
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let {
            val uri = Uri.parse(MediaStore.Images.Media.insertImage(context.contentResolver, it, "CNH", null))
            cnhImageUri = uri
        }
    }

    // ── Step 3: Vehicle ────────────────────────────────────────────────────────
    var vehicleModel by remember { mutableStateOf("") }
    var vehiclePlate by remember { mutableStateOf(textFieldValueAtEnd("")) }
    var vehicleColor by remember { mutableStateOf("") }
    var vehicleYear by remember { mutableStateOf("") }
    var vehicleCapacity by remember { mutableStateOf(4) }

    // ── Async state ────────────────────────────────────────────────────────────
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var registered by remember { mutableStateOf(false) }

    // ── Validation ─────────────────────────────────────────────────────────────
    val cpfDigits = cpf.text.filter { it.isDigit() }
    val cpfError = cpfDigits.isNotEmpty() && !isValidCpf(cpf.text)
    val step1Valid = name.isNotBlank() && phone.text.isNotBlank() &&
            password.length >= 6 && password == confirmPassword && acceptedTerms && !cpfError
    val step2Valid = cnhCategory.isNotBlank()
    val step3Valid = vehicleModel.isNotBlank() && vehiclePlate.text.length >= 7 &&
            vehicleColor.isNotBlank() && vehicleYear.length == 4 && !loading

    val pwStrength = passwordStrength(password)

    showLegal?.let { legalType ->
        LegalScreen(type = legalType, onBack = { showLegal = null })
        return
    }

    // ── Success screen ─────────────────────────────────────────────────────────
    if (registered) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppLilacSoft)
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Surface(shape = CircleShape, color = AppGreenLight, modifier = Modifier.size(88.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CheckCircle, null, tint = AppGreen, modifier = Modifier.size(48.dp))
                    }
                }
                Text("Cadastro enviado!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = AppGreen)
                Text(
                    "Seu cadastro está em análise.\nAnalisaremos suas informações e entraremos em contato em até 2 dias úteis.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate700,
                    textAlign = TextAlign.Center
                )
                HorizontalDivider(color = Slate200)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        "✓ Verifique seu cadastro no app após a aprovação",
                        "✓ Adicione sua foto de perfil nas configurações",
                        "✓ Configure sua chave Pix para receber pagamentos"
                    ).forEach { tip ->
                        Text(tip, style = MaterialTheme.typography.bodySmall, color = Slate700)
                    }
                }
                Button(
                    onClick = onRegistered,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppGreen)
                ) {
                    Text("Ir para o login", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }
        return
    }

    // ── Wizard ─────────────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppLilacSoft)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Logo + back
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (step > 1) step-- else onBackToLogin() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = AppPurple)
            }
            Spacer(Modifier.width(4.dp))
            Image(
                painter = painterResource(R.drawable.logo1),
                contentDescription = "MobU",
                modifier = Modifier.height(28.dp),
                contentScale = ContentScale.Fit
            )
        }

        // Step indicator
        StepIndicator(current = step, total = 3)

        // Step title + subtitle
        val (stepTitle, stepSubtitle) = when (step) {
            1 -> "Dados pessoais" to "Crie sua conta de motorista"
            2 -> "Habilitação (CNH)" to "Informe os dados do seu documento"
            else -> "Dados do veículo" to "Qual carro você vai usar?"
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stepTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Slate900)
            Text(stepSubtitle, style = MaterialTheme.typography.bodyMedium, color = Slate500)
        }

        // Step content card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (targetState > initialState)
                        (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
                    else
                        (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
                },
                label = "step_content"
            ) { currentStep ->
                when (currentStep) {
                    1 -> Step1Content(
                        name = name, onNameChange = { name = it },
                        cpf = cpf, onCpfChange = { cpf = maskedTextFieldValue(it, ::formatCpf) }, cpfError = cpfError,
                        phone = phone, onPhoneChange = { phone = maskedTextFieldValue(it, ::formatPhoneBr) },
                        password = password, onPasswordChange = { password = it },
                        confirmPassword = confirmPassword, onConfirmChange = { confirmPassword = it },
                        showPassword = showPassword, onTogglePassword = { showPassword = !showPassword },
                        showConfirmPassword = showConfirmPassword, onToggleConfirmPassword = { showConfirmPassword = !showConfirmPassword },
                        pwStrength = pwStrength,
                        acceptedTerms = acceptedTerms,
                        onTermsChange = { acceptedTerms = it },
                        onOpenLegal = { showLegal = it }
                    )
                    2 -> Step2Content(
                        cnhNumber = cnhNumber, onCnhNumberChange = { cnhNumber = it },
                        cnhCategory = cnhCategory, onCnhCategoryChange = { cnhCategory = it },
                        cnhExpiresAt = cnhExpiresAt, onCnhExpiresAtChange = { cnhExpiresAt = maskedTextFieldValue(it, ::formatCnhDate) },
                        hasEar = hasEar, onHasEarChange = { hasEar = it },
                        showEarHelp = showEarHelp, onToggleEarHelp = { showEarHelp = !showEarHelp },
                        cnhImageUri = cnhImageUri,
                        onCamera = { cameraLauncher.launch(null) },
                        onGallery = { galleryLauncher.launch("image/*") }
                    )
                    else -> Step3Content(
                        vehicleModel = vehicleModel, onVehicleModelChange = { vehicleModel = it },
                        vehiclePlate = vehiclePlate, onVehiclePlateChange = { vehiclePlate = maskedTextFieldValue(it, ::formatPlate) },
                        vehicleColor = vehicleColor, onVehicleColorChange = { vehicleColor = it },
                        vehicleYear = vehicleYear, onVehicleYearChange = { vehicleYear = it.filter { c -> c.isDigit() }.take(4) },
                        vehicleCapacity = vehicleCapacity, onVehicleCapacityChange = { vehicleCapacity = it }
                    )
                }
            }
        }

        // Error message
        errorMessage?.let { msg ->
            Surface(shape = RoundedCornerShape(12.dp), color = AppRedLight) {
                Text(
                    msg,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppRed,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Navigation buttons
        if (step < 3) {
            Button(
                onClick = {
                    errorMessage = null
                    step++
                },
                enabled = when (step) { 1 -> step1Valid; else -> step2Valid },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppPurple)
            ) {
                Text("Continuar", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        } else {
            Button(
                onClick = {
                    scope.launch {
                        loading = true
                        errorMessage = null
                        try {
                            val res = RetrofitClient.authApi.registerDriver(
                                DriverRegisterRequest(
                                    phone = onlyDigits(phone.text),
                                    password = password,
                                    name = name,
                                    cnhNumber = cnhNumber.ifBlank { null },
                                    cnhCategory = cnhCategory.ifBlank { null },
                                    cnhImageUrl = null,
                                    hasEar = hasEar
                                )
                            )
                            RetrofitClient.setToken(res.accessToken)
                            runCatching {
                                RetrofitClient.driverApi.updateProfile(
                                    DriverProfileUpdateRequest(
                                        cpf = cpf.text.ifBlank { null },
                                        cnhExpiresAt = cnhDateToIso(cnhExpiresAt.text),
                                        vehicleModel = vehicleModel.ifBlank { null },
                                        vehiclePlate = onlyAlphanumeric(vehiclePlate.text).ifBlank { null },
                                        vehicleColor = vehicleColor.ifBlank { null },
                                        vehicleYear = vehicleYear.toIntOrNull(),
                                        vehicleCapacity = vehicleCapacity
                                    )
                                )
                            }
                            val capturedCnhUri = cnhImageUri
                            if (capturedCnhUri != null) {
                                runCatching {
                                    val dest = File(context.filesDir, "cnh_upload_temp.jpg")
                                    context.contentResolver.openInputStream(capturedCnhUri)?.use { input ->
                                        FileOutputStream(dest).use { out -> input.copyTo(out) }
                                    }
                                    val requestFile = dest.asRequestBody("image/jpeg".toMediaTypeOrNull())
                                    val part = MultipartBody.Part.createFormData("photo", dest.name, requestFile)
                                    RetrofitClient.driverApi.uploadCnhPhoto(part)
                                }
                            }
                            RetrofitClient.setToken(null)
                            registered = true
                        } catch (e: HttpException) {
                            val body = e.response()?.errorBody()?.string() ?: ""
                            errorMessage = when {
                                body.contains("Telefone já cadastrado") -> "Este telefone já possui cadastro. Faça login ou recupere sua senha."
                                e.code() == 400 -> "Verifique os dados e tente novamente."
                                e.code() == 500 -> "Erro no servidor. Tente novamente mais tarde."
                                else -> "Erro ao cadastrar. Tente novamente."
                            }
                        } catch (e: Exception) {
                            errorMessage = "Sem conexão. Verifique sua internet e tente novamente."
                        } finally {
                            loading = false
                        }
                    }
                },
                enabled = step3Valid,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppPurple)
            ) {
                Text(if (loading) "Enviando cadastro..." else "Finalizar cadastro", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }

        if (step == 1) {
            TextButton(onClick = onBackToLogin, modifier = Modifier.fillMaxWidth()) {
                Text("Já tenho conta — Fazer login", color = AppVioletDark, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun StepIndicator(current: Int, total: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        (1..total).forEach { i ->
            val done = i < current
            val active = i == current
            Surface(
                shape = CircleShape,
                color = if (done || active) AppPurple else Slate200,
                modifier = Modifier.size(if (active) 32.dp else 26.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (done) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    } else {
                        Text(
                            "$i",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (active) Color.White else Slate500
                        )
                    }
                }
            }
            if (i < total) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(if (i < current) AppPurple else Slate200)
                )
            }
        }
    }
}

@Composable
private fun Step1Content(
    name: String, onNameChange: (String) -> Unit,
    cpf: TextFieldValue, onCpfChange: (TextFieldValue) -> Unit, cpfError: Boolean,
    phone: TextFieldValue, onPhoneChange: (TextFieldValue) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    confirmPassword: String, onConfirmChange: (String) -> Unit,
    showPassword: Boolean, onTogglePassword: () -> Unit,
    showConfirmPassword: Boolean, onToggleConfirmPassword: () -> Unit,
    pwStrength: Int,
    acceptedTerms: Boolean, onTermsChange: (Boolean) -> Unit,
    onOpenLegal: (LegalType) -> Unit
) {
    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        RegisterField(value = name, onValueChange = onNameChange, label = "Nome completo",
            leadingIcon = { Icon(Icons.Default.Person, null, tint = Slate500) })

        MaskedRegisterField(
            value = cpf, onValueChange = onCpfChange, label = "CPF",
            placeholder = "000.000.000-00",
            leadingIcon = { Icon(Icons.Default.Badge, null, tint = Slate500) },
            isError = cpfError,
            supportingText = if (cpfError) ({ Text("CPF inválido", color = AppRed) }) else null
        )

        MaskedRegisterField(value = phone, onValueChange = onPhoneChange, label = "Telefone",
            placeholder = "(00) 00000-0000",
            leadingIcon = { Icon(Icons.Default.Phone, null, tint = Slate500) })

        HorizontalDivider(color = Slate100)

        // Password with strength
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Senha") },
                leadingIcon = { Icon(Icons.Default.Lock, null, tint = Slate500) },
                trailingIcon = {
                    IconButton(onClick = onTogglePassword) {
                        Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = Slate500)
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppPurple)
            )
            if (password.isNotEmpty()) {
                PasswordStrengthBar(strength = pwStrength)
            }
        }

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = onConfirmChange,
            label = { Text("Confirmar senha") },
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = Slate500) },
            trailingIcon = {
                IconButton(onClick = onToggleConfirmPassword) {
                    Icon(if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = Slate500)
                }
            },
            visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (confirmPassword.isNotEmpty() && confirmPassword != password) AppRed else AppPurple,
                unfocusedBorderColor = if (confirmPassword.isNotEmpty() && confirmPassword != password) AppRed else Slate200
            ),
            isError = confirmPassword.isNotEmpty() && confirmPassword != password,
            supportingText = if (confirmPassword.isNotEmpty() && confirmPassword != password) {
                { Text("As senhas não coincidem", color = AppRed) }
            } else null
        )

        HorizontalDivider(color = Slate100)

        // Terms checkbox
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).padding(4.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Checkbox(
                checked = acceptedTerms,
                onCheckedChange = onTermsChange,
                colors = CheckboxDefaults.colors(checkedColor = AppPurple)
            )
            Column(modifier = Modifier.padding(top = 14.dp)) {
                Text(
                    "Li e aceito os Termos de Uso e a Política de Privacidade do MobU",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate700
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onOpenLegal(LegalType.TERMS) }) {
                        Text("Ler termos", color = AppPurple, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = { onOpenLegal(LegalType.PRIVACY) }) {
                        Text("Ler política", color = AppPurple, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PasswordStrengthBar(strength: Int) {
    val (label, color) = when (strength) {
        1 -> "Fraca" to AppRed
        2 -> "Razoável" to AppRed
        3 -> "Boa" to AppPurple
        4 -> "Forte" to AppGreen
        else -> "" to Slate200
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (1..4).forEach { i ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (i <= strength) color else Slate200)
                )
            }
        }
        if (label.isNotEmpty()) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Step2Content(
    cnhNumber: String, onCnhNumberChange: (String) -> Unit,
    cnhCategory: String, onCnhCategoryChange: (String) -> Unit,
    cnhExpiresAt: TextFieldValue, onCnhExpiresAtChange: (TextFieldValue) -> Unit,
    hasEar: Boolean, onHasEarChange: (Boolean) -> Unit,
    showEarHelp: Boolean, onToggleEarHelp: () -> Unit,
    cnhImageUri: Uri?,
    onCamera: () -> Unit,
    onGallery: () -> Unit
) {
    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        RegisterField(value = cnhNumber, onValueChange = onCnhNumberChange, label = "Número da CNH (opcional)",
            leadingIcon = { Icon(Icons.Default.Badge, null, tint = Slate500) })

        MaskedRegisterField(value = cnhExpiresAt, onValueChange = onCnhExpiresAtChange,
            label = "Validade da CNH (opcional)", placeholder = "DD/MM/AAAA",
            leadingIcon = { Icon(Icons.Default.Badge, null, tint = Slate500) })

        // CNH category
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Categoria da CNH *", style = MaterialTheme.typography.labelMedium, color = Slate700, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("A", "B", "AB").forEach { cat ->
                    FilterChip(
                        selected = cnhCategory == cat,
                        onClick = { onCnhCategoryChange(cat) },
                        label = { Text(cat, fontWeight = if (cnhCategory == cat) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppPurpleLight,
                            selectedLabelColor = AppVioletDark
                        )
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("C", "D", "E").forEach { cat ->
                    FilterChip(
                        selected = cnhCategory == cat,
                        onClick = { onCnhCategoryChange(cat) },
                        label = { Text(cat, fontWeight = if (cnhCategory == cat) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppPurpleLight,
                            selectedLabelColor = AppVioletDark
                        )
                    )
                }
            }
        }

        HorizontalDivider(color = Slate100)

        // EAR checkbox with help
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onHasEarChange(!hasEar) }.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Checkbox(checked = hasEar, onCheckedChange = onHasEarChange,
                    colors = CheckboxDefaults.colors(checkedColor = AppPurple))
                Text("Minha CNH possui EAR", style = MaterialTheme.typography.bodyMedium, color = Slate900, modifier = Modifier.weight(1f))
                IconButton(onClick = onToggleEarHelp, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Help, null, tint = Slate500, modifier = Modifier.size(18.dp))
                }
            }
            AnimatedVisibility(visible = showEarHelp, enter = fadeIn(), exit = fadeOut()) {
                Surface(shape = RoundedCornerShape(10.dp), color = AppLilacSoft) {
                    Text(
                        "EAR (Exerce Atividade Remunerada) indica que sua CNH autoriza o uso do veículo para transporte remunerado de passageiros, como motoristas de aplicativo. Se não souber, deixe desmarcado.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700
                    )
                }
            }
        }

        HorizontalDivider(color = Slate100)

        // CNH photo upload
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Foto da CNH (opcional)", style = MaterialTheme.typography.labelMedium, color = Slate700, fontWeight = FontWeight.SemiBold)
            if (cnhImageUri == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(2.dp, Slate200, RoundedCornerShape(14.dp))
                        .background(AppLilacSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Image, null, tint = Slate500, modifier = Modifier.size(32.dp))
                        Text("Nenhuma foto selecionada", style = MaterialTheme.typography.bodySmall, color = Slate500)
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(14.dp))) {
                    Image(
                        painter = rememberAsyncImagePainter(cnhImageUri),
                        contentDescription = "CNH",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AppGreen,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Text("Foto adicionada", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCamera, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Câmera", fontWeight = FontWeight.Medium)
                }
                OutlinedButton(
                    onClick = onGallery, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Image, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Galeria", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Step3Content(
    vehicleModel: String, onVehicleModelChange: (String) -> Unit,
    vehiclePlate: TextFieldValue, onVehiclePlateChange: (TextFieldValue) -> Unit,
    vehicleColor: String, onVehicleColorChange: (String) -> Unit,
    vehicleYear: String, onVehicleYearChange: (String) -> Unit,
    vehicleCapacity: Int, onVehicleCapacityChange: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(shape = RoundedCornerShape(12.dp), color = AppLilacSoft, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DirectionsCar, null, tint = AppVioletDark, modifier = Modifier.size(20.dp))
                Text("Informe o veículo que usará para fazer corridas.",
                    style = MaterialTheme.typography.bodySmall, color = Slate700)
            }
        }

        RegisterField(value = vehicleModel, onValueChange = onVehicleModelChange,
            label = "Marca e modelo *", placeholder = "Ex: Toyota Corolla",
            leadingIcon = { Icon(Icons.Default.DirectionsCar, null, tint = Slate500) })

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                MaskedRegisterField(value = vehiclePlate, onValueChange = onVehiclePlateChange,
                    label = "Placa *", placeholder = "ABC-1234")
            }
            Column(modifier = Modifier.weight(1f)) {
                RegisterField(value = vehicleYear, onValueChange = onVehicleYearChange,
                    label = "Ano *", placeholder = "2020")
            }
        }

        RegisterField(value = vehicleColor, onValueChange = onVehicleColorChange,
            label = "Cor *", placeholder = "Ex: Prata")

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Capacidade de passageiros *",
                style = MaterialTheme.typography.labelMedium,
                color = Slate700,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(2, 3, 4, 5, 6, 7).forEach { cap ->
                    FilterChip(
                        selected = vehicleCapacity == cap,
                        onClick = { onVehicleCapacityChange(cap) },
                        label = {
                            Text(
                                "$cap",
                                fontWeight = if (vehicleCapacity == cap) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppPurpleLight,
                            selectedLabelColor = AppVioletDark
                        )
                    )
                }
            }
            Text(
                "Número de passageiros que o veículo comporta",
                style = MaterialTheme.typography.labelSmall,
                color = Slate500
            )
        }
    }
}

@Composable
private fun RegisterField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    leadingIcon: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder.isNotEmpty()) { { Text(placeholder, color = Slate500) } } else null,
        leadingIcon = leadingIcon,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        isError = isError,
        supportingText = supportingText,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (isError) AppRed else AppPurple,
            unfocusedBorderColor = if (isError) AppRed else Slate200
        )
    )
}

@Composable
private fun MaskedRegisterField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    placeholder: String = "",
    leadingIcon: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder.isNotEmpty()) { { Text(placeholder, color = Slate500) } } else null,
        leadingIcon = leadingIcon,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        isError = isError,
        supportingText = supportingText,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (isError) AppRed else AppPurple,
            unfocusedBorderColor = if (isError) AppRed else Slate200
        )
    )
}

private fun onlyAlphanumeric(s: String) = s.filter { it.isLetterOrDigit() }
