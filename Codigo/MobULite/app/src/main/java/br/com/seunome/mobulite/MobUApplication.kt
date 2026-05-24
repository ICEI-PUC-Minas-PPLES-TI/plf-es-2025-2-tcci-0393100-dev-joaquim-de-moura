package br.com.seunome.mobulite

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import br.com.seunome.mobulite.data.remote.RetrofitClient

class MobUApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MobU",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações do MobU"
                enableVibration(true)
                enableLights(true)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "mobu_notifications"
    }
}
