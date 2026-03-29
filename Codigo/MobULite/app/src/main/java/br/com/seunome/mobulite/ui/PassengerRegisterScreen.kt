package br.com.seunome.mobulite.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.seunome.mobulite.data.remote.RegisterRequest
import br.com.seunome.mobulite.data.remote.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException

@Composable
fun PassengerRegisterScreen(
    onBackToLogin: () -> Unit,
    onRegistered: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Cadastro de Passageiro",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nome") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Telefone") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Senha") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                scope.launch {
                    loading = true
                    message = null

                    try {
                        RetrofitClient.authApi.register(
                            RegisterRequest(
                                phone = phone,
                                password = password,
                                name = name,
                                role = "PASSENGER"
                            )
                        )

                        message = "Cadastro realizado com sucesso."
                        onRegistered()
                    } catch (e: HttpException) {
                        val errorBody = e.response()?.errorBody()?.string()
                        message = "Erro: HTTP ${e.code()} - ${errorBody ?: e.message()}"
                    } catch (e: Exception) {
                        message = "Erro: ${e.message}"
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = !loading && name.isNotBlank() && phone.isNotBlank() && password.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(if (loading) "Cadastrando..." else "Cadastrar como passageiro")
        }

        message?.let {
            Text(it)
        }

        TextButton(onClick = onBackToLogin) {
            Text("Voltar para login")
        }
    }
}