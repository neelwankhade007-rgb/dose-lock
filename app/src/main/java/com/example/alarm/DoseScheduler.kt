package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.model.Medication
import com.example.ui.lockscreen.AlarmLockActivity
import java.util.Calendar

class DoseScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleMedicationAlarm(medication: Medication) {
        if (!medication.isActive) return

        val triggerAtMillis = calculateNextTriggerMillis(medication.scheduledHour, medication.scheduledMinute)

        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            action = MedicationAlarmReceiver.ACTION_TRIGGER_DOSE_ALARM
            putExtra(MedicationAlarmReceiver.EXTRA_MED_ID, medication.id)
            putExtra(MedicationAlarmReceiver.EXTRA_MED_NAME, medication.name)
            putExtra(MedicationAlarmReceiver.EXTRA_MED_DOSAGE, medication.dosage)
            putExtra(MedicationAlarmReceiver.EXTRA_REFERENCE_PHOTO, medication.referencePhotoPath)
            putExtra(MedicationAlarmReceiver.EXTRA_ALARM_TONE, medication.alarmTone.name)
            putExtra(MedicationAlarmReceiver.EXTRA_GRACE_MINUTES, medication.gracePeriodMinutes)
            putExtra(MedicationAlarmReceiver.EXTRA_SCHEDULED_TIME, triggerAtMillis)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medication.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            Log.d("DoseScheduler", "Scheduled alarm for ${medication.name} at timestamp $triggerAtMillis")
        } catch (e: SecurityException) {
            Log.e("DoseScheduler", "Failed to schedule exact alarm: ${e.message}", e)
        }
    }

    fun cancelMedicationAlarm(medicationId: Long) {
        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            action = MedicationAlarmReceiver.ACTION_TRIGGER_DOSE_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicationId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d("DoseScheduler", "Cancelled alarm for medId: $medicationId")
    }

    /**
     * Immediately triggers the full unignorable alarm lockout flow with working audio
     */
    fun triggerInstantTestAlarm(medication: Medication) {
        val scheduledTime = System.currentTimeMillis()

        // 1. Launch Foreground Service playing synthesized alarm tone
        val serviceIntent = Intent(context, AggressiveAlarmService::class.java).apply {
            action = AggressiveAlarmService.ACTION_START_ALARM
            putExtra(MedicationAlarmReceiver.EXTRA_MED_ID, medication.id)
            putExtra(MedicationAlarmReceiver.EXTRA_MED_NAME, medication.name)
            putExtra(MedicationAlarmReceiver.EXTRA_MED_DOSAGE, medication.dosage)
            putExtra(MedicationAlarmReceiver.EXTRA_REFERENCE_PHOTO, medication.referencePhotoPath)
            putExtra(MedicationAlarmReceiver.EXTRA_ALARM_TONE, medication.alarmTone.name)
            putExtra(MedicationAlarmReceiver.EXTRA_GRACE_MINUTES, medication.gracePeriodMinutes)
            putExtra(MedicationAlarmReceiver.EXTRA_SCHEDULED_TIME, scheduledTime)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e("DoseScheduler", "Failed to startForegroundService: ${e.message}", e)
        }

        // 2. Immediately launch the Full-Screen Lockout Activity
        val lockIntent = Intent(context, AlarmLockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra(MedicationAlarmReceiver.EXTRA_MED_ID, medication.id)
            putExtra(MedicationAlarmReceiver.EXTRA_MED_NAME, medication.name)
            putExtra(MedicationAlarmReceiver.EXTRA_MED_DOSAGE, medication.dosage)
            putExtra(MedicationAlarmReceiver.EXTRA_REFERENCE_PHOTO, medication.referencePhotoPath)
            putExtra(MedicationAlarmReceiver.EXTRA_ALARM_TONE, medication.alarmTone.name)
            putExtra(MedicationAlarmReceiver.EXTRA_GRACE_MINUTES, medication.gracePeriodMinutes)
            putExtra(MedicationAlarmReceiver.EXTRA_SCHEDULED_TIME, scheduledTime)
        }
        context.startActivity(lockIntent)
    }

    private fun calculateNextTriggerMillis(hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return calendar.timeInMillis
    }
}
