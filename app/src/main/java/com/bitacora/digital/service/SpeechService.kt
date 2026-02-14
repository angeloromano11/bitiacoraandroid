package com.bitacora.digital.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Speech recognition service using Android SpeechRecognizer.
 * Provides real-time transcript with partial and final results.
 */
@Singleton
class SpeechService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var shouldRestart = false

    companion object {
        private const val TAG = "SpeechService"
    }

    /**
     * Check if speech recognition is available.
     */
    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    /**
     * Start listening for speech.
     * Must be called from main thread.
     */
    fun startListening() {
        if (!isAvailable()) {
            Log.e(TAG, "Speech recognition not available")
            return
        }

        shouldRestart = true
        _isListening.value = true

        createRecognizer()
        startRecognition()
    }

    /**
     * Stop listening and return final transcript.
     */
    fun stopListening(): String {
        shouldRestart = false
        _isListening.value = false

        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null

        // Combine partial with final
        val finalText = buildFinalTranscript()
        _partialTranscript.value = ""

        return finalText
    }

    /**
     * Clear transcript buffer.
     */
    fun clearTranscript() {
        _transcript.value = ""
        _partialTranscript.value = ""
    }

    /**
     * Release resources.
     */
    fun destroy() {
        shouldRestart = false
        speechRecognizer?.destroy()
        speechRecognizer = null
        _isListening.value = false
    }

    private fun createRecognizer() {
        speechRecognizer?.destroy()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d(TAG, "Ready for speech")
                }

                override fun onBeginningOfSpeech() {
                    Log.d(TAG, "Speech started")
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // Audio level changed - could update UI
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                    // Raw audio buffer
                }

                override fun onEndOfSpeech() {
                    Log.d(TAG, "Speech ended")
                }

                override fun onError(error: Int) {
                    Log.e(TAG, "Recognition error: ${getErrorMessage(error)}")
                    handleError(error)
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""

                    if (text.isNotEmpty()) {
                        // Append to transcript
                        val current = _transcript.value
                        _transcript.value = if (current.isEmpty()) {
                            text
                        } else {
                            "$current $text"
                        }
                        _partialTranscript.value = ""
                    }

                    // Auto-restart if still listening
                    if (shouldRestart && _isListening.value) {
                        startRecognition()
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    _partialTranscript.value = text
                }

                override fun onEvent(eventType: Int, params: Bundle?) {
                    // Additional events
                }
            })
        }
    }

    private fun startRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // Longer silence detection
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening", e)
        }
    }

    private fun handleError(error: Int) {
        when (error) {
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                // No speech detected - restart if still listening
                if (shouldRestart && _isListening.value) {
                    startRecognition()
                }
            }
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                // Wait and retry
                if (shouldRestart && _isListening.value) {
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        if (shouldRestart && _isListening.value) {
                            createRecognizer()
                            startRecognition()
                        }
                    }, 500)
                }
            }
            SpeechRecognizer.ERROR_CLIENT,
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                // Fatal errors - stop listening
                _isListening.value = false
                shouldRestart = false
            }
            else -> {
                // Try to restart for other errors
                if (shouldRestart && _isListening.value) {
                    createRecognizer()
                    startRecognition()
                }
            }
        }
    }

    private fun getErrorMessage(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
            else -> "Unknown error: $error"
        }
    }

    private fun buildFinalTranscript(): String {
        val final = _transcript.value
        val partial = _partialTranscript.value

        return if (partial.isNotEmpty() && final.isNotEmpty()) {
            "$final $partial"
        } else if (partial.isNotEmpty()) {
            partial
        } else {
            final
        }
    }
}
