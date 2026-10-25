package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.database.MessageVaultDatabase
import com.example.data.repository.MessageVaultRepository

class MessageVaultApp : Application() {

    lateinit var database: MessageVaultDatabase
        private set
    lateinit var repository: MessageVaultRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = MessageVaultDatabase.getInstance(this)
        repository = MessageVaultRepository(database)

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_SERVICE,
                "خدمة حفظ الرسائل",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعارات مراقبة وحفظ الرسائل في الخلفية"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_SERVICE = "vault_service_channel"

        @Volatile
        private var instance: MessageVaultApp? = null

        fun get(): MessageVaultApp = instance ?: error("MessageVaultApp not initialized")
    }
}
