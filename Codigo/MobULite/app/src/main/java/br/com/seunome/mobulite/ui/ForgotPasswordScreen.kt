package br.com.seunome.mobulite.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import br.com.seunome.mobulite.data.remote.ConfirmPasswordResetRequest
import br.com.seunome.mobulite.data.remote.LoginRequest
import br.com.seunome.mobulite.data.remote.RequestPasswordResetRequest
import br.com.seunome.mobulite.data.remote.RetrofitClient
import br.com.seunome.mobulite.ui.theme.AppGreen
import br.com.seunome.mobulite.ui.theme.AppGreenBg
import br.com.seunome.mobulite.ui.theme.AppGreenDarker
import br.com.seunome.mobulite.ui.theme.AppLilacSoft
import br.com.seunome.mobulite.ui.theme.AppPurple
import br.com.seunome.mobulite.ui.theme.AppPurpleLight
import br.com.seunome.mobulite.ui.theme.AppRed
import br.com.seunome.mobulite.ui.theme.AppRedLight
import br.com.seunome.mobulite.ui.theme.AppVioletDarker
import br.com.seunome.mobulite.ui.theme.Slate100
import br.com.seunome.mobulite.ui.theme.Slate200
import br.com.seunome.mobulite.ui.theme.Slate400
import br.com.seunome.mobulite.ui.theme.Slate500
import br.com.seunome.mobulite.ui.theme.Slate600
import br.com.seunome.mobulite.ui.theme.Slate700
import br.com.seunome.mobulite.ui.theme.Slate900
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.HttpException

private enum class PasswordResetStep {
    REQUEST_CODE,
    CONFIRM_CODE,
    DONE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var step by remember { mutableStateOf(PasswordResetStep.REQUEST_CODE) }
    var phone by remember { mutableStateOf(textFieldValueAtEnd("")) }
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var sentMessage by remember { mutableStateOf<String?>(null) }
    var devHint by remember { mutableStateOf<String?>(null) }
    var resendSeconds by remember { mutableIntStateOf(0) }

    val phoneDigits = onlyDigits(phone.text)
    val codeDigits = onlyDigits(code).take(6)
    val passwordScore = passwordStrength(newPassword)
    val passwordIsStrong = isStrongPassword(newPassword)
    val passwordsMatch = newPassword == confirmPassword
    val canRequestCode = !loading && phoneDigits.length in 10..11
    val canConfirm = !loading &&
        phoneDigits.length in 10..11 &&
        codeDigits.length == 6 &&
        passwordIsStrong &&
        passwordsMatch

    LaunchedEffect(resendSeconds) {
        if (resendSeconds > 0) {
            delay(1_000)
            resendSeconds -= 1
        }
    }

    fun requestCode() {
        scope.launch {
            loading = true
            error = null
            try {
                val ack = RetrofitClient.api.requestPasswordReset(
                    RequestPasswordResetRequest(phoneDigits)
                )
                sentMessage = ack.message ?: "Código enviado por SMS."
                devHint = ack.devHint
                resendSeconds = 60
                step = PasswordResetStep.CONFIRM_CODE
            } catch (e: Exception) {
                error = friendlyPasswordResetError(e)
            } finally {
                loading = false
            }
        }
    }

    fun confirmReset() {
        scope.launch {
            loading = true
            error = null
            try {
                RetrofitClient.api.confirmPasswordReset(
                    ConfirmPasswordResetRequest(
                        phone = phoneDigits,
                        code = codeDigits,
                        newPassword = newPassword
                    )
                )
                RetrofitClient.api.login(LoginRequest(phoneDigits, newPassword))
                step = PasswordResetStep.DONE
            } catch (e: Exception) {
                error = friendlyPasswordResetError(e)
            } finally {
                loading = false
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Recuperar senha", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppLilacSoft,
                    titleContentColor = Slate900,
                    navigationIconContentColor = Slate700
                )
            )
        },
        containerColor = AppLilacSoft
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ResetProgress(step = step)

            when (step) {
                PasswordResetStep.REQUEST_CODE -> RequestCodeSection(
                    phone = phone,
                    onPhoneChange = { phone = maskedTextFieldValue(it, ::formatPhoneBr) },
                    loading = loading,
                    canRequestCode = canRequestCode,
                    error = error,
                    onRequestCode = { requestCode() }
                )

                PasswordResetStep.CONFIRM_CODE -> ConfirmCodeSection(
                    phone = phone.text,
                    code = codeDigits,
                    onCodeChange = { code = onlyDigits(it).take(6) },
                    newPassword = newPassword,
                    onNewPasswordChange = { newPassword = it },
                    confirmPassword = confirmPassword,
                    onConfirmPasswordChange = { confirmPassword = it },
                    showNewPassword = showNewPassword,
                    onToggleNewPassword = { showNewPassword = !showNewPassword },
                    showConfirmPassword = showConfirmPassword,
                    onToggleConfirmPassword = { showConfirmPassword = !showConfirmPassword },
                    passwordScore = passwordScore,
                    passwordIsStrong = passwordIsStrong,
                    passwordsMatch = passwordsMatch,
                    loading = loading,
                    canConfirm = canConfirm,
                    error = error,
                    sentMessage = sentMessage,
                    devHint = devHint,
                    resendSeconds = resendSeconds,
                    onConfirmReset = { confirmReset() },
                    onResend = { if (!loading && resendSeconds == 0) requestCode() },
                    onEditPhone = {
                        error = null
                        step = PasswordResetStep.REQUEST_CODE
                    }
                )

                PasswordResetStep.DONE -> DoneSection(onBack = onBack)
            }
        }
    }
}

@Composable
private fun RequestCodeSection(
    phone: TextFieldValue,
    onPhoneChange: (TextFieldValue) -> Unit,
    loading: Boolean,
    canRequestCode: Boolean,
    error: String?,
    onRequestCode: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Vamos confirmar que a conta é sua",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Text(
                    text = "Informe o telefone cadastrado. Enviaremos um código de 6 dígitos para continuar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate600
                )
            }

            OutlinedTextField(
                value = phone,
                onValueChange = onPhoneChange,
                label = { Text("Telefone cadastrado") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                enabled = !loading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                supportingText = {
                    Text("Use o mesmo número que você utiliza para entrar no MobU.")
                }
            )

            SecurityNotice(
                text = "Por segurança, a resposta será a mesma mesmo que o telefone não esteja cadastrado."
            )

            ErrorText(error)

            Button(
                onClick = onRequestCode,
                enabled = canRequestCode,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppPurple)
            ) {
                ButtonLoadingText(loading = loading, text = "Enviar código")
            }
        }
    }
}

@Composable
private fun ConfirmCodeSection(
    phone: String,
    code: String,
    onCodeChange: (String) -> Unit,
    newPassword: String,
    onNewPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    showNewPassword: Boolean,
    onToggleNewPassword: () -> Unit,
    showConfirmPassword: Boolean,
    onToggleConfirmPassword: () -> Unit,
    passwordScore: Int,
    passwordIsStrong: Boolean,
    passwordsMatch: Boolean,
    loading: Boolean,
    canConfirm: Boolean,
    error: String?,
    sentMessage: String?,
    devHint: String?,
    resendSeconds: Int,
    onConfirmReset: () -> Unit,
    onResend: () -> Unit,
    onEditPhone: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Digite o código e crie uma nova senha",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Text(
                    text = sentMessage ?: "Confira o SMS enviado para $phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate600
                )
            }

            AnimatedVisibility(devHint != null) {
                DevCodeNotice(code = devHint.orEmpty())
            }

            OutlinedTextField(
                value = code,
                onValueChange = onCodeChange,
                label = { Text("Código de 6 dígitos") },
                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                enabled = !loading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
            )

            PasswordField(
                value = newPassword,
                onValueChange = onNewPasswordChange,
                label = "Nova senha",
                visible = showNewPassword,
                onToggleVisibility = onToggleNewPassword,
                enabled = !loading
            )

            PasswordStrength(
                score = passwordScore,
                passwordIsStrong = passwordIsStrong,
                password = newPassword
            )

            PasswordField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = "Confirmar nova senha",
                visible = showConfirmPassword,
                onToggleVisibility = onToggleConfirmPassword,
                enabled = !loading,
                isError = confirmPassword.isNotEmpty() && !passwordsMatch,
                supportingText = if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                    "As senhas não coincidem."
                } else {
                    null
                }
            )

            ErrorText(error)

            Button(
                onClick = onConfirmReset,
                enabled = canConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppPurple)
            ) {
                ButtonLoadingText(loading = loading, text = "Redefinir senha")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onEditPhone, enabled = !loading) {
                    Text("Trocar telefone")
                }
                OutlinedButton(
                    onClick = onResend,
                    enabled = !loading && resendSeconds == 0,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (resendSeconds > 0) "Reenviar em ${resendSeconds}s" else "Reenviar código")
                }
            }
        }
    }
}

@Composable
private fun DoneSection(onBack: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .background(AppGreenBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = AppGreen,
                    modifier = Modifier.size(34.dp)
                )
            }
            Text(
                text = "Senha redefinida",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
            Text(
                text = "Sua nova senha já está ativa. Volte ao login para entrar com segurança.",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate600
            )
            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppPurple)
            ) {
                Text("Ir para login", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ResetProgress(step: PasswordResetStep) {
    val activeIndex = when (step) {
        PasswordResetStep.REQUEST_CODE -> 0
        PasswordResetStep.CONFIRM_CODE -> 1
        PasswordResetStep.DONE -> 2
    }
    val labels = listOf("Telefone", "Código", "Concluído")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            labels.forEachIndexed { index, label ->
                StepPill(label = label, selected = index <= activeIndex)
                if (index < labels.lastIndex) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(if (index < activeIndex) AppPurple else Slate200)
                    )
                }
            }
        }
    }
}

@Composable
private fun StepPill(label: String, selected: Boolean) {
    Row(
        modifier = Modifier
            .background(
                color = if (selected) AppPurpleLight else Slate100,
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(if (selected) AppPurple else Slate400, CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) AppVioletDarker else Slate500
        )
    }
}

@Composable
private fun SecurityNotice(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppPurpleLight, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(AppPurple, CircleShape)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = AppVioletDarker
        )
    }
}

@Composable
private fun DevCodeNotice(code: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppGreenBg, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AppGreenDarker)
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Código de teste: $code",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = AppGreenDarker
        )
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggleVisibility: () -> Unit,
    enabled: Boolean,
    isError: Boolean = false,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "Ocultar senha" else "Mostrar senha"
                )
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        enabled = enabled,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        supportingText = {
            if (supportingText != null) {
                Text(supportingText, color = AppRed)
            }
        }
    )
}

@Composable
private fun PasswordStrength(
    score: Int,
    passwordIsStrong: Boolean,
    password: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LinearProgressIndicator(
            progress = { score / 4f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = if (passwordIsStrong) AppGreen else AppPurple,
            trackColor = Slate200
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PasswordRule("8+ caracteres", password.length >= 8)
            PasswordRule("Letra", password.any { it.isLetter() })
            PasswordRule("Número", password.any { it.isDigit() })
        }
    }
}

@Composable
private fun PasswordRule(label: String, ok: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(if (ok) AppGreen else Slate400, CircleShape)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (ok) AppGreenDarker else Slate500
        )
    }
}

@Composable
private fun ButtonLoadingText(loading: Boolean, text: String) {
    if (loading) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = Color.White
        )
    } else {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ErrorText(error: String?) {
    AnimatedVisibility(error != null) {
        Text(
            text = error.orEmpty(),
            color = AppRed,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .background(AppRedLight, RoundedCornerShape(12.dp))
                .padding(12.dp)
        )
    }
}

private fun passwordStrength(password: String): Int {
    var score = 0
    if (password.length >= 8) score++
    if (password.any { it.isLetter() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    return score
}

private fun isStrongPassword(password: String): Boolean {
    return password.length >= 8 && password.any { it.isLetter() } && password.any { it.isDigit() }
}

private fun friendlyPasswordResetError(error: Exception): String {
    if (error is HttpException) {
        val fallback = when (error.code()) {
            429 -> "Muitas tentativas. Aguarde alguns minutos e tente novamente."
            400 -> "Confira os dados informados e tente novamente."
            else -> "Não foi possível concluir agora. Tente novamente em instantes."
        }
        val body = error.response()?.errorBody()?.string().orEmpty()
        if (body.isBlank()) return fallback

        return try {
            val json = JSONObject(body)
            when (val message = json.opt("message")) {
                is JSONArray -> (0 until message.length()).joinToString("\n") { index ->
                    message.optString(index)
                }.ifBlank { fallback }
                is String -> message.ifBlank { fallback }
                else -> fallback
            }
        } catch (_: Exception) {
            fallback
        }
    }

    return "Verifique sua conexão e tente novamente."
}
