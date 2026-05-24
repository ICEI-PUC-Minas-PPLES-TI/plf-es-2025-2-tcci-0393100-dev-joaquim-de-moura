package br.com.seunome.mobulite.service

import android.content.Context
import br.com.seunome.mobulite.data.remote.FcmTokenRequest
import br.com.seunome.mobulite.data.remote.RetrofitClient
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MobUFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // Salva localmente para garantir reenvio após login
        getSharedPreferences("fcm", Context.MODE_PRIVATE)
            .edit().putString("pending_token", token).apply()

        // Tenta enviar agora (funciona se o usuário já estiver logado)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                RetrofitClient.authApi.saveFcmToken(FcmTokenRequest(token))
                // Enviou com sucesso — limpa o pendente
                getSharedPreferences("fcm", Context.MODE_PRIVATE)
                    .edit().remove("pending_token").apply()
            } catch (_: Exception) {
                // Usuário ainda não logado — o token será enviado no próximo login
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // Called only when the app is in the foreground — emit to the in-app bus so
        // active screens can react immediately without a system notification popup.
        val title = message.notification?.title
            ?: message.data["title"]
            ?: return
        val body = message.notification?.body
            ?: message.data["body"]
            ?: return

        FcmEventBus.post(FcmEvent(title = title, body = body, data = message.data))
    }

}
