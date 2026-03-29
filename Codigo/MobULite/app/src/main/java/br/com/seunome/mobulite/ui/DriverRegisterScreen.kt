package br.com.seunome.mobulite.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import br.com.seunome.mobulite.data.remote.DriverRegisterRequest
import br.com.seunome.mobulite.data.remote.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException

@Composable
fun DriverRegisterScreen(
    onBackToLogin: () -> Unit,
    onRegistered: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var cnhNumber by remember { mutableStateOf("") }
    var cnhCategory by remember { mutableStateOf("") }
    var cnhImageUrl by remember { mutableStateOf("") }
    var hasEar by remember { mutableStateOf(false) }

    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Cadastro de Motorista",
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

        OutlinedTextField(
            value = cnhNumber,
            onValueChange = { cnhNumber = it },
            label = { Text("Número da CNH") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = cnhCategory,
            onValueChange = { cnhCategory = it },
            label = { Text("Categoria da CNH") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = cnhImageUrl,
            onValueChange = { cnhImageUrl = it },
            label = { Text("URL da imagem da CNH") },
            modifier = Modifier.fillMaxWidth()
        )

        androidx.compose.foundation.layout.Row {
            Checkbox(
                checked = hasEar,
                onCheckedChange = { hasEar = it }
            )
            Text("Minha CNH possui EAR")
        }

        Button(
            onClick = {
                scope.launch {
                    loading = true
                    message = null

                    try {
                        RetrofitClient.authApi.registerDriver(
                            DriverRegisterRequest(
                                phone = phone,
                                password = password,
                                name = name,
                                cnhImageUrl = cnhImageUrl.ifBlank { null },
                                cnhNumber = cnhNumber.ifBlank { null },
                                cnhCategory = cnhCategory.ifBlank { null },
                                hasEar = hasEar
                            )
                        )
                        message = "Cadastro enviado para análise."
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
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (loading) "Enviando..." else "Cadastrar como motorista")
        }

        message?.let {
            Text(it)
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onBackToLogin) {
            Text("Voltar para login")
        }
    }
}