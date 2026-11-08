package com.example.alarm

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

enum class AlarmTone(val displayName: String, val description: String) {
    SIREN("Siren (Fast Sweep)", "High-speed rapid frequency siren sweep (800Hz - 2400Hz)"),
    HARSH_PULSE("Harsh Pulse (Square)", "Intense square wave pulse bursts at full gain"),
    HIGH_BEEP("High Beep (Rapid)", "Sharp fast electronic alert beeps at 2200Hz")
}

class SynthesizedSoundEngine {

    @Volatile
    private var isPlaying = false
    private var audioTrack: AudioTrack? = null
    private var playbackThread: Thread? = null

    fun startLoop(tone: AlarmTone) {
        stop()
        isPlaying = true

        playbackThread = thread(start = true, name = "DoseLock-SynthAudio") {
            try {
                val sampleRate = 44100
                var minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                // Check for ERROR or ERROR_BAD_VALUE from getMinBufferSize
                if (minBufferSize <= 0) {
                    minBufferSize = 4096
                }

                // Ensure buffer size is a multiple of frame size (2 bytes for 16-bit mono)
                val bufferSize = (minBufferSize * 4).coerceAtLeast(8192)

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val audioFormat = AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()

                val track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    AudioTrack.Builder()
                        .setAudioAttributes(audioAttributes)
                        .setAudioFormat(audioFormat)
                        .setBufferSizeInBytes(bufferSize)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    AudioTrack(
                        AudioManager.STREAM_ALARM,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize,
                        AudioTrack.MODE_STREAM
                    )
                }

                audioTrack = track
                track.play()

                // Generate 1-second cycling PCM buffer at full amplitude
                val samplesPerCycle = sampleRate
                val pcmBuffer = ShortArray(samplesPerCycle)

                while (isPlaying) {
                    generateToneBuffer(tone, sampleRate, pcmBuffer)
                    var written = 0
                    while (written < pcmBuffer.size && isPlaying) {
                        val res = track.write(pcmBuffer, written, pcmBuffer.size - written)
                        if (res > 0) {
                            written += res
                        } else {
                            break
                        }
                    }
                }

                try {
                    track.stop()
                    track.release()
                } catch (e: Exception) {
                    Log.w(TAG, "AudioTrack release note: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Audio playback exception: ${e.message}", e)
            }
        }
    }

    fun playPreview(tone: AlarmTone, durationMs: Long = 3000L, onComplete: () -> Unit = {}) {
        startLoop(tone)
        thread(start = true) {
            try {
                Thread.sleep(durationMs)
            } catch (_: InterruptedException) {
            } finally {
                stop()
                onComplete()
            }
        }
    }

    fun stop() {
        isPlaying = false
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {
        }
        audioTrack = null
        playbackThread?.interrupt()
        playbackThread = null
    }

    private fun generateToneBuffer(tone: AlarmTone, sampleRate: Int, buffer: ShortArray) {
        val totalSamples = buffer.size
        when (tone) {
            AlarmTone.SIREN -> {
                // High-speed frequency modulation between 800 Hz and 2400 Hz (3 Hz cycle)
                val sweepFreq = 3.0
                var phase = 0.0
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val currentFreq = 1600.0 + 800.0 * sin(2.0 * PI * sweepFreq * t)
                    phase += 2.0 * PI * currentFreq / sampleRate
                    val sample = sin(phase) * 0.98 // full volume gain 0.98
                    buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
                }
            }

            AlarmTone.HARSH_PULSE -> {
                // Harsh square wave bursts (1000 Hz, 8 bursts per sec)
                val pulseRate = 8.0
                val freq = 1000.0
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val inPulse = (t * pulseRate) % 1.0 < 0.65
                    if (inPulse) {
                        val phase = (t * freq) % 1.0
                        val sample = if (phase < 0.5) 0.95 else -0.95
                        buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
                    } else {
                        buffer[i] = 0
                    }
                }
            }

            AlarmTone.HIGH_BEEP -> {
                // Fast 2200 Hz electronic beep (100ms on, 100ms off)
                val period = 0.2
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val inBeep = (t % period) < 0.1
                    if (inBeep) {
                        val sample = sin(2.0 * PI * 2200.0 * t) * 0.95
                        buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
                    } else {
                        buffer[i] = 0
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "SynthesizedSoundEngine"
    }
}
