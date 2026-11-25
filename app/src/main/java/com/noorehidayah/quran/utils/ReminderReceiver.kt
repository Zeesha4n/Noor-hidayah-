package com.noorehidayah.quran.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.noorehidayah.quran.data.model.AsrMethod
import com.noorehidayah.quran.data.model.CityLocation
import com.noorehidayah.quran.data.model.PrayerCalcMethod
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra("prayer_name") ?: "Namaz"
        val prayerTime = intent.getStringExtra("prayer_time") ?: ""
        val cityName = intent.getStringExtra("city_name") ?: ""
        val notifId = intent.getIntExtra("notif_id", 2001)

        val title = "$prayerName Azan Time ($prayerTime)"
        val body = if (cityName.isNotBlank()) {
            "$cityName mein $prayerName ki namaz ka waqt ho gaya hai."
        } else {
            "$prayerName ki namaz ka waqt ho gaya hai."
        }

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "noor_azan_alarms"
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val channel = NotificationChannel(
                channelId,
                "Azan & Prayer Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                setSound(soundUri, audioAttr)
            }
            nm.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setAutoCancel(true)
            .build()

        nm.notify(notifId, notification)
    }
}

object ReminderScheduler {

    private data class PrayerSlot(val id: Int, val name: String, val time12h: String)

    fun schedulePrayerAlarms(
        context: Context,
        enabled: Boolean,
        city: CityLocation,
        method: PrayerCalcMethod,
        asrMethod: AsrMethod
    ) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val times = PrayerTimeCalculator.calculate(city, method, asrMethod)
        val slots = listOf(
            PrayerSlot(2001, "Fajr", times.fajr),
            PrayerSlot(2002, "Dhuhr", times.dhuhr),
            PrayerSlot(2003, "Asr", times.asr),
            PrayerSlot(2004, "Maghrib", times.maghrib),
            PrayerSlot(2005, "Isha", times.isha)
        )

        for (slot in slots) {
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("prayer_name", slot.name)
                putExtra("prayer_time", slot.time12h)
                putExtra("city_name", city.name)
                putExtra("notif_id", slot.id)
            }
            val pi = PendingIntent.getBroadcast(
                context,
                slot.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (!enabled) {
                am.cancel(pi)
                continue
            }

            val cal = parseTimeToNextCalendar(slot.time12h) ?: continue
            am.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pi
            )
        }
    }

    private fun parseTimeToNextCalendar(time12h: String): Calendar? {
        val parts = time12h.trim().split(" ")
        if (parts.size != 2) return null
        val hm = parts[0].split(":")
        if (hm.size != 2) return null
        var hour = hm[0].toIntOrNull() ?: return null
        val minute = hm[1].toIntOrNull() ?: return null
        val isPm = parts[1].equals("PM", ignoreCase = true)
        if (isPm && hour != 12) hour += 12
        if (!isPm && hour == 12) hour = 0

        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
    }
}
