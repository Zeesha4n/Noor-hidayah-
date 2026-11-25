package com.noorehidayah.quran.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Noor-e-Hidayah Daily Reminder"
        val body = intent.getStringExtra("body")
            ?: "Fa-inna ma'al 'usri yusra — Har mushkil ke saath aasani hai. (Surah Ash-Sharh 94:5)"

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "noor_daily_reminders"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Daily Islamic Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nm.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .build()

        nm.notify(1001, notification)
    }
}

object ReminderScheduler {
    fun scheduleDailyReminder(context: Context, enabled: Boolean) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("title", "Noor-e-Hidayah • Daily Ayah & Azkar")
            putExtra("body", "Apne din ki shuruat Bismillah aur Subah ke Azkar ke saath karein.")
        }
        val pi = PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!enabled) {
            am.cancel(pi)
            return
        }

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        am.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pi
        )
    }
}
