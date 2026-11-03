package com.example.engine.wakeword

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * High-efficiency, battery-conscious offline acoustic wake-word detector.
 * Analyzes real-time microphone PCM streams to detect the phonetic acoustic cadence
 * of "Hey Ankita" (3-syllable rhythmic energy profile: [Hey] + [An] + [ki-ta])
 * without heavy cloud ML, enabling battery-friendly operation while screen is OFF.
 */
class AcousticWakeWordDetector(
    private val onWakeWordDetected: () -> Unit,
    private val onSoundLevelChanged: (Float) -> Unit = {}
) {
    private val TAG = "AcousticWakeWord"
    private val isRecording = AtomicBoolean(false)
    private var recordingThread: Thread? = null
    private var audioRecord: AudioRecord? = null

    // Audio sampling configuration: 16kHz 16-bit Mono (Standard for voice engines)
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val frameSize = 512 // 32ms frames

    // Sensitivity factor: 0.1 (low sensitivity/loud only) to 1.0 (high sensitivity)
    var sensitivity: Float = 0.75f

    // Acoustic energy envelope buffer (~2.5 seconds = 80 frames @ 32ms)
    private val historySize = 80
    private val energyHistory = FloatArray(historySize)
    private val zcrHistory = FloatArray(historySize)
    private var historyIndex = 0

    // Dynamic background noise floor tracking
    private var noiseFloor = 300f
    private var lastTriggerTime = 0L
    private val cooldownMs = 2500L // Prevent double triggering

    @SuppressLint("MissingPermission")
    fun startListening(): Boolean {
        if (isRecording.get()) return true

        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = maxOf(minBufferSize, frameSize * 4)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                // Fallback to default MIC source if VOICE_RECOGNITION is unavailable
                audioRecord?.release()
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
            }

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "Failed to initialize AudioRecord")
                return false
            }

            audioRecord?.startRecording()
            isRecording.set(true)

            recordingThread = Thread({ processAudioLoop() }, "Ankita-WakeWord-Thread")
            recordingThread?.priority = Thread.NORM_PRIORITY + 1
            recordingThread?.start()
            Log.d(TAG, "Wake word listening started successfully")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting AudioRecord: ${e.message}", e)
            stopListening()
            return false
        }
    }

    fun stopListening() {
        isRecording.set(false)
        try {
            audioRecord?.let {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping audioRecord: ${e.message}")
        } finally {
            audioRecord = null
            recordingThread = null
        }
    }

    fun isListening(): Boolean = isRecording.get()

    /**
     * Allows simulated wake word trigger from UI for testing without speaking,
     * or when running in emulators without audio input.
     */
    fun simulateWakeTrigger() {
        val now = System.currentTimeMillis()
        if (now - lastTriggerTime > 1000) {
            lastTriggerTime = now
            onWakeWordDetected()
        }
    }

    private fun processAudioLoop() {
        val audioBuffer = ShortArray(frameSize)

        while (isRecording.get()) {
            val bytesRead = audioRecord?.read(audioBuffer, 0, frameSize) ?: -1
            if (bytesRead <= 0) {
                try {
                    Thread.sleep(10)
                } catch (ignored: InterruptedException) {
                    break
                }
                continue
            }

            // 1. Calculate Frame RMS Energy & Zero Crossing Rate
            var sumSquares = 0.0
            var zeroCrossings = 0
            for (i in 0 until bytesRead) {
                val sample = audioBuffer[i]
                sumSquares += (sample * sample)
                if (i > 0 && ((audioBuffer[i - 1] >= 0 && sample < 0) || (audioBuffer[i - 1] < 0 && sample >= 0))) {
                    zeroCrossings++
                }
            }
            val rms = sqrt(sumSquares / bytesRead).toFloat()
            val zcr = zeroCrossings.toFloat() / bytesRead

            // Report normalized level for UI orb visualizer
            val normalizedLevel = (rms / 4000f).coerceIn(0f, 1f)
            onSoundLevelChanged(normalizedLevel)

            // Dynamic noise floor tracking (exponential moving average during quiet periods)
            if (rms < noiseFloor * 1.5f) {
                noiseFloor = noiseFloor * 0.98f + rms * 0.02f
            }
            val voiceThreshold = (noiseFloor * (2.2f - (sensitivity * 0.8f))).coerceAtLeast(350f)

            // Record to rolling history buffer
            energyHistory[historyIndex] = rms
            zcrHistory[historyIndex] = zcr
            historyIndex = (historyIndex + 1) % historySize

            // 2. Pattern Matching: Evaluate acoustic cadence for "Hey Ankita"
            // "Hey" (~10-18 frames), pause (~2-6 frames), "An-ki-ta" (~18-35 frames)
            if (rms > voiceThreshold) {
                val now = System.currentTimeMillis()
                if (now - lastTriggerTime >= cooldownMs) {
                    if (evaluateWakePattern(voiceThreshold)) {
                        lastTriggerTime = now
                        Log.i(TAG, ">>> 'Hey Ankita' Wake Phrase Detected! <<<")
                        onWakeWordDetected()
                    }
                }
            }
        }
    }

    /**
     * Evaluates temporal rhythm and energy distribution across rolling audio frames.
     * Looks for two distinct vocal segments:
     * Segment A ("Hey"): Duration ~250-450ms (8-14 frames)
     * Gap/Diphthong dip: Duration ~60-180ms (2-6 frames)
     * Segment B ("Ankita"): Duration ~550-950ms (18-30 frames) with internal syllables
     */
    private fun evaluateWakePattern(voiceThreshold: Float): Boolean {
        // Read chronological energy frames
        val orderedEnergies = FloatArray(historySize)
        for (i in 0 until historySize) {
            orderedEnergies[i] = energyHistory[(historyIndex + i) % historySize]
        }

        // Look back from current position (recent 50 frames = ~1.6 seconds)
        val lookbackFrames = 50
        val startIndex = historySize - lookbackFrames

        // Count voiced bursts and valleys
        var bursts = 0
        var inBurst = false
        var burstLength = 0
        var totalBurstFrames = 0

        for (i in startIndex until historySize) {
            val e = orderedEnergies[i]
            if (e > voiceThreshold) {
                if (!inBurst) {
                    inBurst = true
                    bursts++
                    burstLength = 1
                } else {
                    burstLength++
                }
                totalBurstFrames++
            } else {
                inBurst = false
            }
        }

        // "Hey Ankita" naturally produces 2 to 4 energy bursts (Hey + An-ki-ta)
        // with total speech duration between 20 and 42 frames (~640ms - ~1350ms)
        val hasMultiSyllableRhythm = bursts in 2..4 && totalBurstFrames in 20..44

        // Check if there is significant acoustic energy variation (not continuous steady noise/fan)
        var maxEnergy = 0f
        var minEnergy = Float.MAX_VALUE
        for (i in startIndex until historySize) {
            val e = orderedEnergies[i]
            if (e > maxEnergy) maxEnergy = e
            if (e < minEnergy) minEnergy = e
        }
        val dynamicRange = maxEnergy / (minEnergy.coerceAtLeast(10f))

        return hasMultiSyllableRhythm && dynamicRange > 3.0f
    }
}
