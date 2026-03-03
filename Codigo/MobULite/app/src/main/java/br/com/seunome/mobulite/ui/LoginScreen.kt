package br.com.seunome.mobulite.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    onLogin: suspend (phone: String, password: String) -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Entrar", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Telefone") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it },
            label = { Text("Senha") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = {
                loading = true
                error = null
                // chama suspend via LaunchedEffect
            },
            enabled = !loading && phone.isNotBlank() && pass.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text(if (loading) "Entrando..." else "Entrar") }

        // executa o login suspend
        LaunchedEffect(loading) {
            if (loading) {
                try {
                    onLogin(phone, pass)
                    loading = false
                } catch (e: Exception) {
                    error = e.message ?: "Falha no login"
                    loading = false
                }
            }
        }
    }
}