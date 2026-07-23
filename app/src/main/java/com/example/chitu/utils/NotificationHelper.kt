package com.example.chitu.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.chitu.MainActivity

object NotificationHelper {

    private const val FATIGUE_CHANNEL_ID = "fatigue_channel"
    private const val FATIGUE_NOTIFICATION_ID = 2001

    fun createFatigueNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FATIGUE_CHANNEL_ID,
                "疲劳驾驶提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "疲劳驾驶超时提醒"
                enableVibration(true)
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    fun sendFatigueAlert(context: Context) {
        // 1. 震动
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(1000)
        }

        // 2. 通知
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, FATIGUE_CHANNEL_ID)
            .setContentTitle("⚠️ 疲劳驾驶提醒")
            .setContentText("您已连续驾驶超过设定时间，请立即停车休息！")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

       // NotificationManagerCompat.from(context).notify(FATIGUE_NOTIFICATION_ID, notification)
    }
}