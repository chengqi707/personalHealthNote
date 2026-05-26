package com.chengqi.personalhealthnote.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.chengqi.personalhealthnote.database.DatabaseHelper
import com.chengqi.personalhealthnote.utils.AlarmScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val dbHelper = DatabaseHelper(context)
            val enabledReminders = dbHelper.getEnabledMedicineReminders()
            AlarmScheduler.scheduleAllEnabledReminders(context, enabledReminders)
            dbHelper.close()
        }
    }
}
