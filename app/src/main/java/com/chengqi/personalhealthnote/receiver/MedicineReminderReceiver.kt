package com.chengqi.personalhealthnote.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.chengqi.personalhealthnote.R
import com.chengqi.personalhealthnote.database.DatabaseHelper
import com.chengqi.personalhealthnote.utils.AlarmScheduler

class MedicineReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "medicine_reminder_channel"
        private const val CHANNEL_NAME = "用药提醒"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra("reminder_id", 0)
        val medicineName = intent.getStringExtra("medicine_name") ?: "药品"
        val dosage = intent.getStringExtra("dosage") ?: ""
        val mealTime = intent.getStringExtra("meal_time") ?: ""

        createNotificationChannel(context)

        val title = "用药提醒"
        val content = buildString {
            append(medicineName)
            if (dosage.isNotEmpty()) append(" $dosage")
            if (mealTime.isNotEmpty() && mealTime != "任意时间") append("（$mealTime）")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_medicine)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(reminderId.toInt(), notification)

        // 设置下一天的闹钟
        val dbHelper = DatabaseHelper(context)
        val reminder = dbHelper.getMedicineReminderById(reminderId)
        if (reminder != null && reminder.isEnabled) {
            AlarmScheduler.scheduleReminder(context, reminder)
        }
        dbHelper.close()
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "用药提醒通知"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
