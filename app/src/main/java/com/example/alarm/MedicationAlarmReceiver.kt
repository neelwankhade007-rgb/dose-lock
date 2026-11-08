package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.db.DoseLockDatabase
import com.example.ui.lockscreen.AlarmLockActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MedicationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "onReceive triggered with action: $action")

        if (Intent.ACTION_BOOT_COMPLETED == action ||
            Intent.ACTION_MY_PACKAGE_REPLACED == action
        ) {
            // Device rebooted -> reschedule all active medications
            rescheduleAllMedications(context)
            return
        }

        if (ACTION_TRIGGER_DOSE_ALARM == action) {
            val medId = intent.getLongExtra(EXTRA_MED_ID, -1L)
            val medName = intent.getStringExtra(EXTRA_MED_NAME) ?: "Prescription Dose"
            val dosage = intent.getStringExtra(EXTRA_MED_DOSAGE) ?: ""
            val referencePhoto = intent.getStringExtra(EXTRA_REFERENCE_PHOTO) ?: ""
            val alarmTone = intent.getStringExtra(EXTRA_ALARM_TONE) ?: AlarmTone.SIREN.name
            val graceMinutes = intent.getIntExtra(EXTRA_GRACE_MINUTES, 3)
            val scheduledTime = intent.getLongExtra(EXTRA_SCHEDULED_TIME, System.currentTimeMillis())

            // 1. Launch Foreground Service for continuous synthesized audio loop & wake lock
            val serviceIntent = Intent(context, AggressiveAlarmService::class.java).apply {
                setAction(AggressiveAlarmService.ACTION_START_ALARM)
                putExtra(EXTRA_MED_ID, medId)
                putExtra(EXTRA_MED_NAME, medName)
                putExtra(EXTRA_MED_DOSAGE, dosage)
                putExtra(EXTRA_REFERENCE_PHOTO, referencePhoto)
                putExtra(EXTRA_ALARM_TONE, alarmTone)
                putExtra(EXTRA_GRACE_MINUTES, graceMinutes)
                putExtra(EXTRA_SCHEDULED_TIME, scheduledTime)
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "startForegroundService error: ${e.message}", e)
            }

            // 2. Direct full screen activity launch
            val activityIntent = Intent(context, AlarmLockActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                putExtra(EXTRA_MED_ID, medId)
                putExtra(EXTRA_MED_NAME, medName)
                putExtra(EXTRA_MED_DOSAGE, dosage)
                putExtra(EXTRA_REFERENCE_PHOTO, referencePhoto)
                putExtra(EXTRA_ALARM_TONE, alarmTone)
                putExtra(EXTRA_GRACE_MINUTES, graceMinutes)
                putExtra(EXTRA_SCHEDULED_TIME, scheduledTime)
            }
            context.startActivity(activityIntent)
        }
    }

    private fun rescheduleAllMedications(context: Context) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = DoseLockDatabase.getDatabase(context)
                val activeList = db.medicationDao().getActiveMedicationsSync()
                val scheduler = DoseScheduler(context)
                for (med in activeList) {
                    scheduler.scheduleMedicationAlarm(med)
                }
                Log.d(TAG, "Successfully restored ${activeList.size} medication alarms after boot")
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling alarms on boot: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val TAG = "MedAlarmReceiver"
        const val ACTION_TRIGGER_DOSE_ALARM = "com.example.doselock.action.TRIGGER_DOSE_ALARM"

        const val EXTRA_MED_ID = "extra_med_id"
        const val EXTRA_MED_NAME = "extra_med_name"
        const val EXTRA_MED_DOSAGE = "extra_med_dosage"
        const val EXTRA_REFERENCE_PHOTO = "extra_reference_photo"
        const val EXTRA_ALARM_TONE = "extra_alarm_tone"
        const val EXTRA_GRACE_MINUTES = "extra_grace_minutes"
        const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
    }
}
