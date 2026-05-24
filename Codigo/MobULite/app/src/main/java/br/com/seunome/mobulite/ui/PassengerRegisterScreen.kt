package br.com.seunome.mobulite.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import br.com.seunome.mobulite.R
import br.com.seunome.mobulite.data.remote.RegisterRequest
import br.com.seunome.mobulite.data.remote.RetrofitClient
import br.com.seunome.mobulite.ui.theme.AppGreen
import br.com.seunome.mobulite.ui.theme.AppGreenDarker
import br.com.seunome.mobulite.ui.theme.AppLilacSoft
import br.com.seunome.mobulite.ui.theme.AppPurple
import br.com.seunome.mobulite.ui.theme.AppPurpleLight
import br.com.seunome.mobulite.ui.theme.AppRed
import br.com.seunome.mobulite.ui.theme.AppRedLight
import br.com.seunome.mobulite.ui.theme.AppVioletDarker
import br.com.seunome.mobulite.ui.theme.Slate200
import br.com.seunome.mobulite.ui.theme.Slate400
import br.com.seunome.mobulite.ui.theme.Slate500
import br.com.seunome.mobulite.ui.theme.Slate600
import br.com.seunome.mobulite.ui.theme.Slate700
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.HttpException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerRegisterScreen(
    onBackToLogin: () -> Unit,
    onRegistered: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var name by remember { mutableStateOf("") }
    var cpf by remember { mutableStateOf(textFieldValueAtEnd("")) }
    var birthDate by remember { mutableStateOf(textFieldValueAtEnd("")) }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf(textFieldValueAtEnd("")) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var acceptedTerms by remember { mutableStateOf(false) }
    var acceptedPrivacy by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var showLegal by remember { mutableStateOf<LegalType?>(null) }

    val phoneDigits = onlyDigits(phone.text)
    val cpfDigits = onlyDigits(cpf.text)
    val birthDateIso = birthDateToIso(birthDate.text)
    val nameValid = isFullName(name)
    val phoneValid = phoneDigits.length in 10..11
    val cpfValid = isValidCpf(cpfDigits)
    val emailValid = isValidEmail(email)
    val birthDateValid = birthDateIso != null && isValidPassengerAge(birthDateIso)
    val passwordStrong = isPassengerPasswordStrong(password)
    val passwordsMatch = password == confirmPassword
    val passwordScore = passengerPasswordStrength(password)

    val canRegister = !loading &&
        nameValid &&
        cpfValid &&
        birthDateValid &&
        emailValid &&
        phoneValid &&
        passwordStrong &&
        passwordsMatch &&
        acceptedTerms &&
        acceptedPrivacy

    showLegal?.let { legalType ->
        LegalScreen(type = legalType, onBack = { showLegal = null })
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppLilacSoft)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo1),
                contentDescription = "Logo MobU",
                modifier = Modifier.height(92.dp),
                contentScale = ContentScale.Fit
            )
            Text(
                text = "Cadastro de passageiro",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Slate700
            )
            Text(
                text = "Dados essenciais para identificação, contato e recuperação da conta.",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate600
            )
        }

        FormSection(title = "Identificação") {
            AppTextField(
                value = name,
                onValueChange = { name = it },
                label = "Nome completo",
                icon = { Icon(Icons.Default.Person, contentDescription = null) },
                enabled = !loading,
                capitalization = KeyboardCapitalization.Words,
                isError = name.isNotBlank() && !nameValid,
                supportingText = if (name.isNotBlank() && !nameValid) "Informe nome e sobrenome." else null
            )

            MaskedAppTextField(
                value = cpf,
                onValueChange = { cpf = maskedTextFieldValue(it, ::formatCpfBr) },
                label = "CPF",
                icon = { Icon(Icons.Default.Info, contentDescription = null) },
                enabled = !loading,
                keyboardType = KeyboardType.Number,
                isError = cpf.text.isNotBlank() && !cpfValid,
                supportingText = if (cpf.text.isNotBlank() && !cpfValid) "Informe um CPF válido." else null
            )

            MaskedAppTextField(
                value = birthDate,
                onValueChange = { birthDate = maskedTextFieldValue(it, ::formatDateBr) },
                label = "Data de nascimento",
                icon = { Icon(Icons.Default.Info, contentDescription = null) },
                enabled = !loading,
                keyboardType = KeyboardType.Number,
                isError = birthDate.text.isNotBlank() && !birthDateValid,
                supportingText = "Formato DD/MM/AAAA"
            )
        }

        FormSection(title = "Contato") {
            AppTextField(
                value = email,
                onValueChange = { email = it.trim() },
                label = "E-mail",
                icon = { Icon(Icons.Default.Email, contentDescription = null) },
                enabled = !loading,
                keyboardType = KeyboardType.Email,
                isError = email.isNotBlank() && !emailValid,
                supportingText = if (email.isNotBlank() && !emailValid) "Informe um e-mail válido." else null
            )

            MaskedAppTextField(
                value = phone,
                onValueChange = { phone = maskedTextFieldValue(it, ::formatPhoneBr) },
                label = "Telefone com DDD",
                icon = { Icon(Icons.Default.Phone, contentDescription = null) },
                enabled = !loading,
                keyboardType = KeyboardType.Phone,
                isError = phone.text.isNotBlank() && !phoneValid,
                supportingText = if (phone.text.isNotBlank() && !phoneValid) "Exemplo: (31) 99999-9999" else null
            )
        }

        FormSection(title = "Acesso") {
            PasswordField(
                value = password,
                onValueChange = { password = it },
                label = "Senha",
                visible = showPassword,
                onToggleVisibility = { showPassword = !showPassword },
                enabled = !loading
            )

            PassengerPasswordStrength(
                score = passwordScore,
                password = password,
                passwordStrong = passwordStrong
            )

            PasswordField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "Confirmar senha",
                visible = showConfirmPassword,
                onToggleVisibility = { showConfirmPassword = !showConfirmPassword },
                enabled = !loading,
                isError = confirmPassword.isNotBlank() && !passwordsMatch,
                supportingText = if (confirmPassword.isNotBlank() && !passwordsMatch) "As senhas não coincidem." else null
            )

            ConsentRow(
                checked = acceptedTerms,
                onCheckedChange = { acceptedTerms = it },
                text = "Li e aceito os termos de uso do MobU.",
                actionText = "Ler termos",
                onOpenLegal = { showLegal = LegalType.TERMS },
                enabled = !loading
            )

            ConsentRow(
                checked = acceptedPrivacy,
                onCheckedChange = { acceptedPrivacy = it },
                text = "Li e aceito a política de privacidade.",
                actionText = "Ler política",
                onOpenLegal = { showLegal = LegalType.PRIVACY },
                enabled = !loading
            )
        }

        message?.let {
            Text(
                text = it,
                color = if (it.startsWith("Cadastro realizado")) AppGreenDarker else AppRed,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (it.startsWith("Cadastro realizado")) AppPurpleLight else AppRedLight,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            )
        }

        Button(
            onClick = {
                scope.launch {
                    loading = true
                    message = null
                    try {
                        RetrofitClient.authApi.register(
                            RegisterRequest(
                                phone = phoneDigits,
                                password = password,
                                name = name.trim(),
                                role = "PASSENGER",
                                email = email.trim().lowercase(),
                                cpf = cpfDigits,
                                birthDate = birthDateIso,
                                acceptedTerms = acceptedTerms,
                                acceptedPrivacy = acceptedPrivacy
                            )
                        )

                        message = "Cadastro realizado com sucesso."
                        onRegistered()
                    } catch (e: Exception) {
                        message = friendlyRegisterError(e)
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = canRegister,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppPurple)
        ) {
            Text(
                text = if (loading) "Cadastrando..." else "Cadastrar passageiro",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        OutlinedButton(
            onClick = onBackToLogin,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            enabled = !loading
        ) {
            Text("Voltar para login")
        }
    }
}

@Composable
private fun FormSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Slate700
            )
            content()
        }
    }
}

@Composable
private fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: @Composable () -> Unit,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    isError: Boolean = false,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = icon,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        enabled = enabled,
        isError = isError,
        supportingText = {
            if (supportingText != null) {
                Text(supportingText)
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = capitalization
        )
    )
}

@Composable
private fun MaskedAppTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    icon: @Composable () -> Unit,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    isError: Boolean = false,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = icon,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        enabled = enabled,
        isError = isError,
        supportingText = {
            if (supportingText != null) {
                Text(supportingText)
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = capitalization
        )
    )
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
private fun ConsentRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: String,
    actionText: String,
    onOpenLegal: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = Slate600
            )
            TextButton(
                onClick = onOpenLegal,
                enabled = enabled
            ) {
                Text(actionText, color = AppPurple, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PassengerPasswordStrength(
    score: Int,
    password: String,
    passwordStrong: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LinearProgressIndicator(
            progress = { score / 4f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = if (passwordStrong) AppGreen else AppPurple,
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
        Spacer(Modifier.size(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (ok) AppGreenDarker else Slate500
        )
    }
}

private fun isFullName(value: String): Boolean {
    val parts = value.trim().split(Regex("\\s+")).filter { it.length >= 2 }
    return value.trim().length >= 5 && parts.size >= 2
}

private fun isValidEmail(value: String): Boolean {
    return Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE)
        .matches(value.trim())
}

private fun formatCpfBr(value: String): String {
    val digits = onlyDigits(value).take(11)
    return when {
        digits.length <= 3 -> digits
        digits.length <= 6 -> "${digits.take(3)}.${digits.drop(3)}"
        digits.length <= 9 -> "${digits.take(3)}.${digits.drop(3).take(3)}.${digits.drop(6)}"
        else -> "${digits.take(3)}.${digits.drop(3).take(3)}.${digits.drop(6).take(3)}-${digits.drop(9)}"
    }
}

private fun formatDateBr(value: String): String {
    val digits = onlyDigits(value).take(8)
    return when {
        digits.length <= 2 -> digits
        digits.length <= 4 -> "${digits.take(2)}/${digits.drop(2)}"
        else -> "${digits.take(2)}/${digits.drop(2).take(2)}/${digits.drop(4)}"
    }
}

private fun birthDateToIso(value: String): String? {
    val digits = onlyDigits(value)
    if (digits.length != 8) return null
    return try {
        val date = LocalDate.parse(value, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        date.toString()
    } catch (_: DateTimeParseException) {
        null
    }
}

private fun isValidPassengerAge(isoDate: String): Boolean {
    return try {
        val birth = LocalDate.parse(isoDate)
        val today = LocalDate.now()
        val age = today.year - birth.year - if (today.dayOfYear < birth.dayOfYear) 1 else 0
        !birth.isAfter(today) && age in 13..120
    } catch (_: Exception) {
        false
    }
}

private fun isValidCpf(cpf: String): Boolean {
    if (!Regex("^\\d{11}$").matches(cpf)) return false
    if (cpf.all { it == cpf.first() }) return false

    fun digit(baseLength: Int): Int {
        var sum = 0
        for (i in 0 until baseLength) {
            sum += cpf[i].digitToInt() * (baseLength + 1 - i)
        }
        val rest = (sum * 10) % 11
        return if (rest == 10) 0 else rest
    }

    return digit(9) == cpf[9].digitToInt() && digit(10) == cpf[10].digitToInt()
}

private fun passengerPasswordStrength(password: String): Int {
    var score = 0
    if (password.length >= 8) score++
    if (password.any { it.isLetter() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    return score
}

private fun isPassengerPasswordStrong(password: String): Boolean {
    return password.length >= 8 && password.any { it.isLetter() } && password.any { it.isDigit() }
}

private fun friendlyRegisterError(error: Exception): String {
    if (error is HttpException) {
        val fallback = when (error.code()) {
            400 -> "Confira os dados informados e tente novamente."
            409 -> "Já existe um cadastro com esses dados."
            else -> "Não foi possível concluir o cadastro agora."
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
