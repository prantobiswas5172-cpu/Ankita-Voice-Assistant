package com.example.engine.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    private val TAG = "SpeechManager"
    private val mainHandler = Handler(Looper.getMainLooper())

    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private var isBengaliSupported = false

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListeningToCommand = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _lastRecognizedText = MutableStateFlow("")
    val lastRecognizedText: StateFlow<String> = _lastRecognizedText.asStateFlow()

    var speechRate: Float = 1.0f
    var speechPitch: Float = 1.05f

    private var onCommandResultCallback: ((String) -> Unit)? = null
    private var onSpeechDoneCallback: (() -> Unit)? = null

    init {
        initTts()
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true

            // Test Bengali locale availability (prefer Bangladesh or India variant)
            val bengaliLocale = Locale("bn", "BD")
            val resultBd = textToSpeech?.isLanguageAvailable(bengaliLocale) ?: TextToSpeech.LANG_NOT_SUPPORTED
            val bengaliInLocale = Locale("bn", "IN")
            val resultIn = textToSpeech?.isLanguageAvailable(bengaliInLocale) ?: TextToSpeech.LANG_NOT_SUPPORTED

            isBengaliSupported = (resultBd >= TextToSpeech.LANG_AVAILABLE || resultIn >= TextToSpeech.LANG_AVAILABLE)
            Log.d(TAG, "TTS Initialized. Bengali supported: $isBengaliSupported (BD=$resultBd, IN=$resultIn)")

            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    mainHandler.post {
                        _isSpeaking.value = true
                    }
                }

                override fun onDone(utteranceId: String?) {
                    mainHandler.post {
                        _isSpeaking.value = false
                        onSpeechDoneCallback?.invoke()
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    mainHandler.post {
                        _isSpeaking.value = false
                    }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    mainHandler.post {
                        _isSpeaking.value = false
                        Log.w(TAG, "TTS Error code: $errorCode for utterance $utteranceId")
                    }
                }
            })
        } else {
            Log.e(TAG, "TTS Initialization failed with status: $status")
        }
    }

    fun speak(text: String, preferBengali: Boolean = true, onDone: (() -> Unit)? = null) {
        if (!isTtsInitialized || textToSpeech == null) {
            Log.w(TAG, "TTS not ready yet: $text")
            mainHandler.post { onDone?.invoke() }
            return
        }

        this.onSpeechDoneCallback = onDone

        try {
            textToSpeech?.setSpeechRate(speechRate)
            textToSpeech?.setPitch(speechPitch)

            if (preferBengali && isBengaliSupported) {
                val locale = if (textToSpeech?.isLanguageAvailable(Locale("bn", "BD")) ?: -1 >= TextToSpeech.LANG_AVAILABLE) {
                    Locale("bn", "BD")
                } else {
                    Locale("bn", "IN")
                }
                textToSpeech?.language = locale
            } else if (preferBengali && !isBengaliSupported) {
                // If Bengali TTS voice pack is not installed on device, log warning and set English
                Log.w(TAG, "Bengali TTS voice pack not installed on device, falling back to English voice")
                textToSpeech?.language = Locale.US
            } else {
                textToSpeech?.language = Locale.US
            }

            val utteranceId = "ankita_${System.currentTimeMillis()}"
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } catch (e: Exception) {
            Log.e(TAG, "Error during speech synthesis: ${e.message}", e)
            mainHandler.post {
                _isSpeaking.value = false
                onDone?.invoke()
            }
        }
    }

    fun stopSpeaking() {
        try {
            textToSpeech?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping TTS: ${e.message}")
        }
        mainHandler.post {
            _isSpeaking.value = false
        }
    }

    fun isBengaliAvailable(): Boolean = isBengaliSupported

    /**
     * Starts listening for user's follow-up voice command after wake-up.
     * Enforces execution on the application's main thread as required by SpeechRecognizer.
     */
    fun startListeningForCommand(
        preferBengali: Boolean = true,
        onResult: (String) -> Unit
    ) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post {
                startListeningForCommand(preferBengali, onResult)
            }
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition is not available on this device")
            mainHandler.post { onResult("") }
            return
        }

        this.onCommandResultCallback = onResult

        // Release previous instance if any
        stopListeningForCommand()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        mainHandler.post {
                            _isListening.value = true
                            Log.d(TAG, "SpeechRecognizer: Ready for speech")
                        }
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d(TAG, "SpeechRecognizer: Beginning of speech")
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Sound level updates
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        mainHandler.post {
                            _isListening.value = false
                            Log.d(TAG, "SpeechRecognizer: End of speech")
                        }
                    }

                    override fun onError(error: Int) {
                        mainHandler.post {
                            _isListening.value = false
                            Log.w(TAG, "SpeechRecognizer Error: $error")
                            onCommandResultCallback?.invoke("")
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        mainHandler.post {
                            _isListening.value = false
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val spokenText = matches?.firstOrNull() ?: ""
                            Log.i(TAG, "SpeechRecognizer Result: '$spokenText'")
                            _lastRecognizedText.value = spokenText
                            onCommandResultCallback?.invoke(spokenText)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        matches?.firstOrNull()?.let {
                            mainHandler.post { _lastRecognizedText.value = it }
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

                if (preferBengali) {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("bn-IN", "en-US"))
                } else {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("bn-BD"))
                }
            }

            speechRecognizer?.startListening(intent)
            isListeningToCommand = true
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to start speech recognition: ${t.message}", t)
            mainHandler.post {
                _isListening.value = false
                onResult("")
            }
        }
    }

    fun stopListeningForCommand() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post {
                stopListeningForCommand()
            }
            return
        }

        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error destroying speech recognizer: ${e.message}")
        } finally {
            speechRecognizer = null
            isListeningToCommand = false
            _isListening.value = false
        }
    }

    fun release() {
        stopSpeaking()
        stopListeningForCommand()
        try {
            textToSpeech?.shutdown()
        } catch (e: Exception) {
            Log.w(TAG, "Error shutting down TTS: ${e.message}")
        }
        textToSpeech = null
        isTtsInitialized = false
    }
}
