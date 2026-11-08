package com.example.ui.lockscreen

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.example.alarm.AggressiveAlarmService
import com.example.alarm.AlarmTone
import com.example.alarm.MedicationAlarmReceiver
import com.example.data.db.DoseLockDatabase
import com.example.data.model.DoseStatus
import com.example.data.repository.MedicationRepository
import com.example.ui.theme.DoseLockTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmLockActivity : ComponentActivity() {

    private lateinit var repository: MedicationRepository

    private var medId: Long = -1L
    private var medName: String = ""
    private var medDosage: String = ""
    private var referencePhoto: String = ""
    private var alarmTone: String = AlarmTone.SIREN.name
    private var graceMinutes: Int = 3
    private var scheduledTime: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureLockScreenFlags()

        val db = DoseLockDatabase.getDatabase(this)
        repository = MedicationRepository(db.medicationDao())

        extractIntentExtras(intent)

        setContent {
            DoseLockTheme {
                AlarmLockScreen(
                    medName = medName,
                    medDosage = medDosage,
                    referencePhotoUri = referencePhoto,
                    alarmToneName = alarmTone,
                    graceMinutes = graceMinutes,
                    onSilenceForGrace = {
                        silenceAlarmForGraceTimer()
                    },
                    onEscalateBackOn = {
                        resumeAggressiveAlarm()
                    },
                    onVerifiedDismissal = { capturedPhotoUri ->
                        handleVerifiedDismissal(capturedPhotoUri)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractIntentExtras(intent)
    }

    private fun extractIntentExtras(intent: Intent) {
        medId = intent.getLongExtra(MedicationAlarmReceiver.EXTRA_MED_ID, -1L)
        medName = intent.getStringExtra(MedicationAlarmReceiver.EXTRA_MED_NAME) ?: "Prescription Dose"
        medDosage = intent.getStringExtra(MedicationAlarmReceiver.EXTRA_MED_DOSAGE) ?: ""
        referencePhoto = intent.getStringExtra(MedicationAlarmReceiver.EXTRA_REFERENCE_PHOTO) ?: ""
        alarmTone = intent.getStringExtra(MedicationAlarmReceiver.EXTRA_ALARM_TONE) ?: AlarmTone.SIREN.name
        graceMinutes = intent.getIntExtra(MedicationAlarmReceiver.EXTRA_GRACE_MINUTES, 3)
        scheduledTime = intent.getLongExtra(MedicationAlarmReceiver.EXTRA_SCHEDULED_TIME, System.currentTimeMillis())
    }

    private fun configureLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    // Intercept hardware volume keys to prevent muting without following protocol
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE -> {
                true // Consume volume events
            }
            KeyEvent.KEYCODE_BACK -> {
                true // Prevent exiting without completing verification
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun silenceAlarmForGraceTimer() {
        val silenceIntent = Intent(this, AggressiveAlarmService::class.java).apply {
            action = AggressiveAlarmService.ACTION_SILENCE_FOR_GRACE
        }
        startService(silenceIntent)
    }

    private fun resumeAggressiveAlarm() {
        val resumeIntent = Intent(this, AggressiveAlarmService::class.java).apply {
            action = AggressiveAlarmService.ACTION_RESUME_AGGRESSIVE_ALARM
        }
        startService(resumeIntent)

        // Log escalation
        if (medId > 0) {
            CoroutineScope(Dispatchers.IO).launch {
                repository.logDose(
                    medicationId = medId,
                    scheduledTimestamp = scheduledTime,
                    status = DoseStatus.ESCALATED,
                    verificationMetadata = "Grace timer expired - Siren re-engaged"
                )
            }
        }
    }

    private fun handleVerifiedDismissal(capturedPhotoUri: String) {
        // 1. Terminate the foreground audio loop and wake lock
        val stopServiceIntent = Intent(this, AggressiveAlarmService::class.java).apply {
            action = AggressiveAlarmService.ACTION_STOP_ALARM
        }
        startService(stopServiceIntent)

        // 2. Persist DoseLog in Room database
        if (medId > 0) {
            CoroutineScope(Dispatchers.IO).launch {
                repository.logDose(
                    medicationId = medId,
                    scheduledTimestamp = scheduledTime,
                    status = DoseStatus.TAKEN_VERIFIED,
                    verificationMetadata = "Photo Verified: $capturedPhotoUri"
                )
            }
        }

        // 3. Show clean success toast and close lockout
        Toast.makeText(this, "Dose Taken & Verified: $medName", Toast.LENGTH_LONG).show()
        finishAndRemoveTask()
    }
}
