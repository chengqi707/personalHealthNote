package com.chengqi.personalhealthnote.utils

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import com.chengqi.personalhealthnote.entity.MedicineReminder
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object CalendarHelper {

    private val CALENDAR_PROJECTION = arrayOf(
        CalendarContract.Calendars._ID,
        CalendarContract.Calendars.IS_PRIMARY
    )

    private fun getDefaultCalendarId(context: Context): Long? {
        val resolver: ContentResolver = context.contentResolver
        val uri: Uri = CalendarContract.Calendars.CONTENT_URI
        val cursor = resolver.query(uri, CALENDAR_PROJECTION, null, null, null)
        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val isPrimary = it.getInt(1)
                if (isPrimary == 1) return id
            }
            if (it.moveToFirst()) return it.getLong(0)
        }
        return null
    }

    fun addCalendarEvents(context: Context, reminder: MedicineReminder): String {
        val calendarId = getDefaultCalendarId(context) ?: return ""
        val times = reminder.getAllRemindTimes()
        val eventIds = mutableListOf<Long>()

        times.forEachIndexed { index, timeStr ->
            val eventId = addSingleEvent(context, calendarId, reminder, timeStr, index)
            if (eventId > 0) {
                eventIds.add(eventId)
            }
        }

        val jsonArray = JSONArray()
        eventIds.forEach { jsonArray.put(it) }
        return jsonArray.toString()
    }

    fun deleteCalendarEvents(context: Context, calendarEventIds: String) {
        if (calendarEventIds.isEmpty()) return
        try {
            val jsonArray = JSONArray(calendarEventIds)
            val resolver = context.contentResolver
            for (i in 0 until jsonArray.length()) {
                val eventId = jsonArray.getLong(i)
                val eventUri = ContentUris.withAppendedId(
                    CalendarContract.Events.CONTENT_URI, eventId
                )
                resolver.delete(eventUri, null, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateCalendarEvents(context: Context, reminder: MedicineReminder, oldEventIds: String): String {
        deleteCalendarEvents(context, oldEventIds)
        return addCalendarEvents(context, reminder)
    }

    private fun addSingleEvent(
        context: Context,
        calendarId: Long,
        reminder: MedicineReminder,
        timeStr: String,
        timeIndex: Int
    ): Long {
        val resolver = context.contentResolver
        val parts = timeStr.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startDate = dateFormat.parse(reminder.startDate) ?: return -1

        val startCal = Calendar.getInstance().apply {
            time = startDate
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
        if (startCal.before(Calendar.getInstance())) {
            startCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val endCal = startCal.clone() as Calendar
        endCal.add(Calendar.MINUTE, 15)

        val title = "用药提醒：${reminder.medicineName}"
        val description = buildString {
            append("${reminder.medicineName} ${reminder.getFullDosage()}")
            append("\n${reminder.getMealTimeText()}")
            if (reminder.notes.isNotEmpty()) append("\n备注：${reminder.notes}")
        }

        val rrule = if (reminder.endDate != null) {
            val endDate = dateFormat.parse(reminder.endDate)
            if (endDate != null) {
                val endCalForRule = Calendar.getInstance().apply { time = endDate }
                "FREQ=DAILY;UNTIL=${String.format(Locale.US, "%04d%02d%02dT235959Z",
                    endCalForRule.get(Calendar.YEAR),
                    endCalForRule.get(Calendar.MONTH) + 1,
                    endCalForRule.get(Calendar.DAY_OF_MONTH))}"
            } else {
                "FREQ=DAILY"
            }
        } else {
            "FREQ=DAILY"
        }

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startCal.timeInMillis)
            put(CalendarContract.Events.DTEND, endCal.timeInMillis)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.RRULE, rrule)
        }

        val eventUri: Uri? = resolver.insert(CalendarContract.Events.CONTENT_URI, values)
        val eventId = eventUri?.lastPathSegment?.toLong() ?: -1L

        if (eventId > 0) {
            val reminderValues = ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID, eventId)
                put(CalendarContract.Reminders.MINUTES, 0)
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
            }
            resolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
        }

        return eventId
    }
}
