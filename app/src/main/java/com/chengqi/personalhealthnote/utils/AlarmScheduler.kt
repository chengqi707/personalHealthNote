package com.chengqi.personalhealthnote.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.chengqi.personalhealthnote.entity.MedicineReminder
import com.chengqi.personalhealthnote.receiver.MedicineReminderReceiver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object AlarmScheduler {

    private const val ACTION_REMINDER = "com.chengqi.personalhealthnote.ACTION_MEDICINE_REMINDER"

    fun scheduleReminder(context: Context, reminder: MedicineReminder) {
        if (!reminder.isEnabled) {
            cancelReminder(context, reminder)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val times = reminder.getAllRemindTimes()

        times.forEachIndexed { index, timeStr ->
            val requestCode = getRequestCode(reminder.id, index)
            val pendingIntent = createPendingIntent(context, reminder, index)

            val triggerMillis = calculateTriggerMillis(reminder, timeStr)
            if (triggerMillis > System.currentTimeMillis()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                }
            }
        }
    }

    fun cancelReminder(context: Context, reminder: MedicineReminder) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val times = reminder.getAllRemindTimes()

        times.forEachIndexed { index, _ ->
            val pendingIntent = createPendingIntent(context, reminder, index)
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun rescheduleReminder(context: Context, oldReminder: MedicineReminder, newReminder: MedicineReminder) {
        cancelReminder(context, oldReminder)
        scheduleReminder(context, newReminder)
    }

    fun scheduleAllEnabledReminders(context: Context, reminders: List<MedicineReminder>) {
        reminders.filter { it.isEnabled }.forEach { scheduleReminder(context, it) }
    }

    private fun calculateTriggerMillis(reminder: MedicineReminder, timeStr: String): Long {
        val parts = timeStr.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()

        val now = Calendar.getInstance()
        val trigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 如果今天的时间已过，设为明天
        if (trigger.before(now)) {
            trigger.add(Calendar.DAY_OF_YEAR, 1)
        }

        // 检查是否在有效期内
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startDate = dateFormat.parse(reminder.startDate)
        if (startDate != null && trigger.time.before(startDate)) {
            trigger.time = startDate
            trigger.set(Calendar.HOUR_OF_DAY, hour)
            trigger.set(Calendar.MINUTE, minute)
        }

        if (reminder.endDate != null) {
            val endDate = dateFormat.parse(reminder.endDate)
            if (endDate != null) {
                val endCal = Calendar.getInstance().apply { time = endDate }
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                if (trigger.after(endCal)) {
                    return 0L
                }
            }
        }

        return trigger.timeInMillis
    }

    private fun createPendingIntent(context: Context, reminder: MedicineReminder, timeIndex: Int): PendingIntent {
        val intent = Intent(context, MedicineReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra("reminder_id", reminder.id)
            putExtra("medicine_name", reminder.medicineName)
            putExtra("dosage", reminder.getFullDosage())
            putExtra("meal_time", reminder.getMealTimeText())
            putExtra("time_index", timeIndex)
            putExtra("time_str", reminder.getAllRemindTimes().getOrNull(timeIndex) ?: "")
        }
        return PendingIntent.getBroadcast(
            context,
            getRequestCode(reminder.id, timeIndex),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getRequestCode(reminderId: Long, timeIndex: Int): Int {
        return (reminderId * 10 + timeIndex).toInt()
    }
}
