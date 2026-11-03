package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.AnkitaApplication
import com.example.MainActivity
import com.example.R
import com.example.data.preferences.AssistantPreferences
import com.example.engine.nlp.CommandResponse
import com.example.engine.nlp.VoiceCommandProcessor
import com.example.engine.speech.SpeechManager
import com.example.engine.wakeword.AcousticWakeWordDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AssistantVisualState {
    SLEEPING,
    LISTENING_WAKE,
    WAKE_DETECTED,
    LISTENING_COMMAND,
    PROCESSING,
    SPEAKING
}

class AnkitaVoiceService : Service() {

    private val TAG = "AnkitaVoiceService"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val binder = LocalBinder()
    private var wakeLock: PowerManager.WakeLock? = null
    private var screenWakeLock: PowerManager.WakeLock? = null

    private lateinit var wakeWordDetector: AcousticWakeWordDetector
    private lateinit var speechManager: SpeechManager
    private lateinit var commandProcessor: VoiceCommandProcessor
    private lateinit var preferences: AssistantPreferences

    inner class LocalBinder : Binder() {
        fun getService(): AnkitaVoiceService = this@AnkitaVoiceService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    companion object {
        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_TRIGGER_WAKE = "com.example.service.ACTION_TRIGGER_WAKE"
        const val ACTION_SPEAK_TIME = "com.example.service.ACTION_SPEAK_TIME"
        const val ACTION_PROCESS_TEXT = "com.example.service.ACTION_PROCESS_TEXT"
        const val EXTRA_TEXT = "extra_text"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _assistantState = MutableStateFlow(AssistantVisualState.SLEEPING)
        val assistantState: StateFlow<AssistantVisualState> = _assistantState.asStateFlow()

        private val _audioLevel = MutableStateFlow(0f)
        val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

        private val _lastTranscript = MutableStateFlow("")
        val lastTranscript: StateFlow<String> = _lastTranscript.asStateFlow()

        private val _lastResponse = MutableStateFlow("")
        val lastResponse: StateFlow<String> = _lastResponse.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Creating AnkitaVoiceService...")

        preferences = AssistantPreferences(applicationContext)
        speechManager = SpeechManager(applicationContext)
        commandProcessor = VoiceCommandProcessor(applicationContext)

        wakeWordDetector = AcousticWakeWordDetector(
            onWakeWordDetected = { handleWakeWordDetected() },
            onSoundLevelChanged = { level ->
                if (_assistantState.value == AssistantVisualState.LISTENING_WAKE) {
                    _audioLevel.value = level
                }
            }
        )

        acquirePartialWakeLock()

        serviceScope.launch {
            preferences.languageFlow.collect { lang ->
                currentLanguage = lang
                if (_isRunning.value) {
                    val isBn = lang != "en"
                    updateNotification(if (isBn) "অঙ্কিতা শুনছে: 'Hey Ankita' বলুন..." else "Ankita is listening: Say 'Hey Ankita'...")
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        Log.d(TAG, "onStartCommand received action: $action")

        when (action) {
            ACTION_STOP -> {
                stopForegroundService()
                return START_NOT_STICKY
            }
            ACTION_TRIGGER_WAKE -> {
                handleWakeWordDetected()
                return START_STICKY
            }
            ACTION_SPEAK_TIME -> {
                speakCurrentTimeImmediate()
                return START_STICKY
            }
            ACTION_PROCESS_TEXT -> {
                val text = intent?.getStringExtra(EXTRA_TEXT) ?: ""
                if (text.isNotBlank()) {
                    executeTextCommand(text)
                }
                return START_STICKY
            }
            ACTION_START -> {
                startForegroundWithNotification()
                startListeningIfEnabled()
                return START_STICKY
            }
        }
        return START_STICKY
    }

    private var currentLanguage = "bn"

    private fun startForegroundWithNotification() {
        val isBn = currentLanguage != "en"
        val initialStatus = if (isBn) "অঙ্কিতা শুনছে: 'Hey Ankita' বলুন..." else "Ankita is listening: Say 'Hey Ankita'..."
        val notification = buildServiceNotification(initialStatus)
        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            0
        }

        try {
            ServiceCompat.startForeground(
                this,
                AnkitaApplication.SERVICE_NOTIFICATION_ID,
                notification,
                serviceType
            )
            _isRunning.value = true
            _assistantState.value = AssistantVisualState.LISTENING_WAKE
            Log.d(TAG, "Foreground service started with MICROPHONE type")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service: ${e.message}", e)
        }
    }

    private fun buildServiceNotification(statusText: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, AnkitaVoiceService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isBn = currentLanguage != "en"
        val title = if (isBn) "অঙ্কিতা ভয়েস সহকারী সক্রিয়" else "Ankita Voice Assistant Active"
        val actionStopText = if (isBn) "সহকারী বন্ধ করুন" else "Stop Assistant"

        return NotificationCompat.Builder(this, AnkitaApplication.CHANNEL_VOICE_SERVICE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(statusText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(android.R.drawable.ic_media_pause, actionStopText, stopIntent)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val notification = buildServiceNotification(statusText)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(AnkitaApplication.SERVICE_NOTIFICATION_ID, notification)
    }

    private fun startListeningIfEnabled() {
        serviceScope.launch {
            val isMasterEnabled = preferences.assistantEnabledFlow.first()
            val isWakeWordEnabled = preferences.wakeWordEnabledFlow.first()
            val sensitivity = preferences.wakeSensitivityFlow.first()

            wakeWordDetector.sensitivity = sensitivity

            if (isMasterEnabled && isWakeWordEnabled) {
                val started = wakeWordDetector.startListening()
                if (started) {
                    _assistantState.value = AssistantVisualState.LISTENING_WAKE
                    Log.i(TAG, "Wake word acoustic listener is active")
                } else {
                    Log.w(TAG, "Wake word detector failed to start (check permissions)")
                }
            } else {
                wakeWordDetector.stopListening()
                _assistantState.value = AssistantVisualState.SLEEPING
            }
        }
    }

    /**
     * Called when "Hey Ankita" acoustic pattern is detected (even with screen off).
     */
    private fun handleWakeWordDetected() {
        Log.i(TAG, "Handle Wake Word triggered!")
        serviceScope.launch(Dispatchers.Main) {
            _assistantState.value = AssistantVisualState.WAKE_DETECTED

            // 1. Vibrate device
            vibrateBriefly()

            // 2. Wake up screen if locked or off
            wakeScreenUp()

            // 3. Temporarily pause acoustic detector while speech dialog takes place
            wakeWordDetector.stopListening()

            // 4. Prompt user: "শুনছি, বলুন!" or "I'm listening!"
            val language = preferences.languageFlow.first()
            val isBengali = language == "bn" || language == "auto"

            val promptText = if (isBengali) "হ্যাঁ, বলুন!" else "Yes, I'm listening!"
            _lastResponse.value = promptText
            updateNotification("শুনছি: নির্দেশ দিন...")

            speechManager.speak(promptText, preferBengali = isBengali) {
                serviceScope.launch(Dispatchers.Main) {
                    listenForFollowUpCommand(isBengali)
                }
            }
        }
    }

    private fun listenForFollowUpCommand(isBengali: Boolean) {
        _assistantState.value = AssistantVisualState.LISTENING_COMMAND
        speechManager.startListeningForCommand(preferBengali = isBengali) { transcript ->
            serviceScope.launch(Dispatchers.Main) {
                if (transcript.isNotBlank()) {
                    _lastTranscript.value = transcript
                    _assistantState.value = AssistantVisualState.PROCESSING
                    processVoiceCommand(transcript, isBengali)
                } else {
                    // Empty or timeout: revert to wake listening
                    _assistantState.value = AssistantVisualState.LISTENING_WAKE
                    updateNotification("অঙ্কিতা শুনছে: 'Hey Ankita' বলুন...")
                    startListeningIfEnabled()
                }
            }
        }
    }

    private suspend fun processVoiceCommand(commandText: String, isBengali: Boolean) {
        val response = withContext(Dispatchers.IO) {
            commandProcessor.processCommand(commandText, if (isBengali) "bn" else "en")
        }

        _lastResponse.value = response.spokenResponse
        _assistantState.value = AssistantVisualState.SPEAKING
        updateNotification(response.displayText)

        speechManager.speak(response.spokenResponse, preferBengali = response.isBengali) {
            // Once Ankita finishes speaking, resume background wake-word listening
            serviceScope.launch(Dispatchers.Main) {
                _assistantState.value = AssistantVisualState.LISTENING_WAKE
                updateNotification("অঙ্কিতা শুনছে: 'Hey Ankita' বলুন...")
                startListeningIfEnabled()
            }
        }
    }

    fun executeTextCommand(text: String) {
        serviceScope.launch {
            val language = preferences.languageFlow.first()
            val isBengali = language == "bn" || language == "auto"
            _lastTranscript.value = text
            processVoiceCommand(text, isBengali)
        }
    }

    fun speakCurrentTimeImmediate() {
        serviceScope.launch {
            val language = preferences.languageFlow.first()
            val isBengali = language == "bn" || language == "auto"
            val text = if (isBengali) "এখন সময় কত?" else "What time is it?"
            processVoiceCommand(text, isBengali)
        }
    }

    fun simulateWake() {
        wakeWordDetector.simulateWakeTrigger()
    }

    @SuppressLint("WakelockTimeout")
    private fun acquirePartialWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ankita:service_wakelock").apply {
                setReferenceCounted(false)
                acquire()
            }
            Log.d(TAG, "Partial WakeLock acquired for screen-off detection")
        }
    }

    @SuppressLint("WakelockTimeout")
    private fun wakeScreenUp() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            screenWakeLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "ankita:screen_wake"
            )
            screenWakeLock?.acquire(7000L) // Release after 7 seconds
        } catch (e: Exception) {
            Log.w(TAG, "Failed to wake screen: ${e.message}")
        }
    }

    private fun vibrateBriefly() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                v?.vibrate(120)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to vibrate: ${e.message}")
        }
    }

    private fun stopForegroundService() {
        Log.i(TAG, "Stopping AnkitaVoiceService...")
        wakeWordDetector.stopListening()
        speechManager.stopSpeaking()
        speechManager.stopListeningForCommand()

        _isRunning.value = false
        _assistantState.value = AssistantVisualState.SLEEPING
        _audioLevel.value = 0f

        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AnkitaVoiceService onDestroy")
        stopForegroundService()
        speechManager.release()
        serviceScope.cancel()
    }
}
