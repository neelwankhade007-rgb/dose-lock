package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.ui.lockscreen.AlarmLockActivity

class AggressiveAlarmService : Service() {

    private val soundEngine = SynthesizedSoundEngine()
    private var wakeLock: PowerManager.WakeLock? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var originalVolume: Int = -1
    private var currentTone: AlarmTone = AlarmTone.SIREN

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        try {
            createNotificationChannels()
            acquireWakeLock()
            initVibrator()
            audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        } catch (e: Exception) {
            Log.e(TAG, "Error during onCreate: ${e.message}", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        val action = intent.action
        val medId = intent.getLongExtra(MedicationAlarmReceiver.EXTRA_MED_ID, -1L)
        val medName = intent.getStringExtra(MedicationAlarmReceiver.EXTRA_MED_NAME) ?: "Prescription Dose"
        val dosage = intent.getStringExtra(MedicationAlarmReceiver.EXTRA_MED_DOSAGE) ?: ""
        val referencePhoto = intent.getStringExtra(MedicationAlarmReceiver.EXTRA_REFERENCE_PHOTO) ?: ""
        val toneName = intent.getStringExtra(MedicationAlarmReceiver.EXTRA_ALARM_TONE) ?: AlarmTone.SIREN.name
        val graceMinutes = intent.getIntExtra(MedicationAlarmReceiver.EXTRA_GRACE_MINUTES, 3)
        val scheduledTime = intent.getLongExtra(MedicationAlarmReceiver.EXTRA_SCHEDULED_TIME, System.currentTimeMillis())

        currentTone = try {
            AlarmTone.valueOf(toneName)
        } catch (_: Exception) {
            AlarmTone.SIREN
        }

        when (action) {
            ACTION_STOP_ALARM -> {
                Log.d(TAG, "Stop alarm command received")
                soundEngine.stop()
                try { vibrator?.cancel() } catch (_: Exception) {}
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SILENCE_FOR_GRACE -> {
                Log.d(TAG, "Muting alarm sound for grace period countdown")
                soundEngine.stop()
                try { vibrator?.cancel() } catch (_: Exception) {}
                return START_STICKY
            }
            ACTION_RESUME_AGGRESSIVE_ALARM -> {
                Log.w(TAG, "Grace period expired! Resuming aggressive alarm at maximum volume!")
                startAlarmPlayback(currentTone, isEscalated = true)
                return START_STICKY
            }
            else -> {
                startForegroundWithLockNotification(
                    medId = medId,
                    medName = medName,
                    dosage = dosage,
                    referencePhoto = referencePhoto,
                    toneName = currentTone.name,
                    graceMinutes = graceMinutes,
                    scheduledTime = scheduledTime
                )
                startAlarmPlayback(currentTone, isEscalated = false)
            }
        }

        return START_STICKY
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "DoseLock:AlarmWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(15 * 60 * 1000L) // 15 mins timeout safety
            }
        } catch (e: Exception) {
            Log.w(TAG, "WakeLock acquisition notice: ${e.message}")
        }
    }

    private fun initVibrator() {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibrator initialization notice: ${e.message}")
        }
    }

    private fun startAlarmPlayback(tone: AlarmTone, isEscalated: Boolean) {
        // Enforce maximum volume on Alarm stream safely
        audioManager?.let { am ->
            try {
                if (originalVolume == -1) {
                    originalVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
                }
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                am.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)
            } catch (e: Exception) {
                Log.w(TAG, "Could not set stream volume safely: ${e.message}")
            }
        }

        // Start synthesized loud audio loop
        soundEngine.startLoop(tone)

        // Trigger vibration pattern
        triggerVibration(isEscalated = isEscalated)
    }

    private fun triggerVibration(isEscalated: Boolean) {
        try {
            vibrator?.let { vib ->
                if (vib.hasVibrator()) {
                    val pattern = if (isEscalated) {
                        longArrayOf(0, 150, 100, 150, 100, 300, 150)
                    } else {
                        longArrayOf(0, 500, 500, 500, 500)
                    }
                    val amplitudes = if (isEscalated) {
                        intArrayOf(0, 255, 0, 255, 0, 255, 0)
                    } else {
                        intArrayOf(0, 200, 0, 200, 0)
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val effect = VibrationEffect.createWaveform(pattern, amplitudes, 0)
                        vib.vibrate(effect)
                    } else {
                        @Suppress("DEPRECATION")
                        vib.vibrate(pattern, 0)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibration execution note: ${e.message}")
        }
    }

    private fun startForegroundWithLockNotification(
        medId: Long,
        medName: String,
        dosage: String,
        referencePhoto: String,
        toneName: String,
        graceMinutes: Int,
        scheduledTime: Long
    ) {
        try {
            val fullScreenIntent = Intent(this, AlarmLockActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                putExtra(MedicationAlarmReceiver.EXTRA_MED_ID, medId)
                putExtra(MedicationAlarmReceiver.EXTRA_MED_NAME, medName)
                putExtra(MedicationAlarmReceiver.EXTRA_MED_DOSAGE, dosage)
                putExtra(MedicationAlarmReceiver.EXTRA_REFERENCE_PHOTO, referencePhoto)
                putExtra(MedicationAlarmReceiver.EXTRA_ALARM_TONE, toneName)
                putExtra(MedicationAlarmReceiver.EXTRA_GRACE_MINUTES, graceMinutes)
                putExtra(MedicationAlarmReceiver.EXTRA_SCHEDULED_TIME, scheduledTime)
            }

            val fullScreenPendingIntent = PendingIntent.getActivity(
                this,
                (medId.coerceAtLeast(0L) % 10000).toInt(),
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, CHANNEL_CRITICAL_ALARM)
                .setSmallIcon(R.drawable.ic_notification_lock)
                .setContentTitle("DOSE DUE: $medName ($dosage)")
                .setContentText("UNIGNORABLE ALERT: Proof of medication photo required.")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setContentIntent(fullScreenPendingIntent)
                .build()

            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "startForeground error: ${e.message}", e)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                CHANNEL_CRITICAL_ALARM,
                "DoseLock Critical Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Unignorable full-screen medication alerts"
                setBypassDnd(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                enableVibration(true)
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundEngine.stop()
        try {
            vibrator?.cancel()
        } catch (_: Exception) {}

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing wakeLock: ${e.message}")
        }
        Log.d(TAG, "AggressiveAlarmService destroyed cleanly.")
    }

    companion object {
        const val TAG = "AggressiveAlarmService"
        const val CHANNEL_CRITICAL_ALARM = "doselock_critical_alarm_channel"
        const val NOTIFICATION_ID = 9110

        const val ACTION_START_ALARM = "com.example.doselock.action.START_ALARM"
        const val ACTION_STOP_ALARM = "com.example.doselock.action.STOP_ALARM"
        const val ACTION_SILENCE_FOR_GRACE = "com.example.doselock.action.SILENCE_FOR_GRACE"
        const val ACTION_RESUME_AGGRESSIVE_ALARM = "com.example.doselock.action.RESUME_AGGRESSIVE_ALARM"
    }
}
