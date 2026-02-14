package com.bitacora.digital.ui.recording

import android.util.Log
import androidx.camera.view.PreviewView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitacora.digital.data.repository.SessionRepository
import com.bitacora.digital.model.InterviewType
import com.bitacora.digital.model.PracticeSubcategory
import com.bitacora.digital.model.Session
import com.bitacora.digital.service.AIService
import com.bitacora.digital.service.CameraService
import com.bitacora.digital.service.KeychainHelper
import com.bitacora.digital.service.MemoryService
import com.bitacora.digital.service.SpeechService
import com.bitacora.digital.util.AudioRingBuffer
import com.bitacora.digital.util.Config
import com.bitacora.digital.util.generateSessionId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel for the recording screen.
 */
@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val cameraService: CameraService,
    private val sessionRepository: SessionRepository,
    private val keychainHelper: KeychainHelper,
    private val aiService: AIService,
    private val speechService: SpeechService,
    private val memoryService: MemoryService
) : ViewModel() {

    companion object {
        private const val TAG = "RecordingViewModel"
    }

    // Camera state
    val isRecording: StateFlow<Boolean> = cameraService.isRecording
    val isCameraInitialized: StateFlow<Boolean> = cameraService.isInitialized

    private val _isSetup = MutableStateFlow(false)
    val isSetup: StateFlow<Boolean> = _isSetup.asStateFlow()

    // Recording state
    var durationSeconds by mutableIntStateOf(0)
        private set
    var sessionId by mutableStateOf<String?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    // Interview state
    var interviewType by mutableStateOf(InterviewType.MEMORY)
    var subcategory by mutableStateOf<PracticeSubcategory?>(null)
    var userName by mutableStateOf("")
    var isInterviewActive by mutableStateOf(false)
        private set
    var isGenerating by mutableStateOf(false)
        private set
    var currentQuestion by mutableStateOf("")
        private set
    var questionNumber by mutableIntStateOf(0)
        private set
    var questionsAsked by mutableStateOf<List<String>>(emptyList())
        private set
    var showTypeSelector by mutableStateOf(true)
        private set

    // AI state
    var aiReady by mutableStateOf(false)
        private set

    // Speech state - exposed from SpeechService
    val transcript: StateFlow<String> = speechService.transcript
    val partialTranscript: StateFlow<String> = speechService.partialTranscript
    val isListening: StateFlow<Boolean> = speechService.isListening

    // Audio ring buffer for AI context
    private val audioRingBuffer = AudioRingBuffer()

    // Auto-questions
    var autoQuestionsEnabled by mutableStateOf(false)
    var autoQuestionInterval by mutableIntStateOf(Config.AUTO_QUESTION_DEFAULT_INTERVAL)
    var countdown by mutableIntStateOf(0)
        private set

    // Timers
    private var durationJob: Job? = null
    private var autoQuestionJob: Job? = null

    /**
     * Check if camera has required permissions.
     */
    fun hasPermissions(): Boolean = cameraService.hasPermissions()

    /**
     * Get required permissions.
     */
    fun getRequiredPermissions(): Array<String> = cameraService.getRequiredPermissions()

    /**
     * Initialize camera, AI service, and check AI readiness.
     */
    suspend fun setup(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        try {
            cameraService.initialize(lifecycleOwner, previewView)
            aiService.initialize()
            aiReady = aiService.isReady
            _isSetup.value = true
        } catch (e: Exception) {
            error = e.message
        }
    }

    /**
     * Start recording.
     */
    fun startRecording() {
        val id = generateSessionId()
        sessionId = id
        error = null
        durationSeconds = 0

        try {
            cameraService.startRecording(id)
            startDurationTimer()
        } catch (e: Exception) {
            error = e.message
            sessionId = null
        }
    }

    /**
     * Stop recording and save session.
     */
    fun stopRecording() {
        val id = sessionId ?: return

        stopDurationTimer()
        val finalDuration = durationSeconds

        if (isInterviewActive) {
            endInterview()
        }

        try {
            val outputPath = cameraService.stopRecording()
            val filename = outputPath.substringAfterLast("/")

            val session = Session.create(
                id = id,
                filename = filename,
                durationSeconds = finalDuration,
                interviewType = interviewType,
                subcategory = subcategory,
                userName = userName,
                questionsCount = questionsAsked.size
            )

            viewModelScope.launch {
                sessionRepository.saveSession(session)
            }

            // Reset state
            sessionId = null
            durationSeconds = 0
            questionsAsked = emptyList()
            currentQuestion = ""
            questionNumber = 0
            showTypeSelector = true
            audioRingBuffer.clear()
        } catch (e: Exception) {
            error = e.message
        }
    }

    /**
     * Start interview (recording + AI if available).
     */
    fun startInterview() {
        if (!isRecording.value) {
            startRecording()
        }

        showTypeSelector = false

        if (aiReady) {
            val id = sessionId ?: return

            // Start AI interview
            val openingQuestion = aiService.startInterview(
                sessionId = id,
                type = interviewType,
                userName = userName,
                subcategory = subcategory
            )

            currentQuestion = openingQuestion
            questionNumber = 1
            questionsAsked = listOf(openingQuestion)
            isInterviewActive = true

            // Save opening question to memory
            viewModelScope.launch {
                memoryService.saveQuestion(id, openingQuestion)
            }

            // Start speech recognition
            speechService.clearTranscript()
            speechService.startListening()

            // Start auto-questions if enabled
            if (autoQuestionsEnabled) {
                startAutoQuestionTimer()
            }

            Log.d(TAG, "AI interview started with question: $openingQuestion")
        } else {
            // Recording-only mode
            isInterviewActive = false
            Log.d(TAG, "Recording started without AI (no API key)")
        }
    }

    /**
     * End interview without stopping recording.
     */
    fun endInterview() {
        stopAutoQuestionTimer()
        val finalTranscript = speechService.stopListening()

        if (isInterviewActive) {
            val id = sessionId
            // Save final response if there's any transcript
            if (finalTranscript.isNotEmpty() && id != null) {
                viewModelScope.launch {
                    memoryService.saveResponse(id, finalTranscript)
                }
            }

            val closingMessage = aiService.endInterview()
            Log.d(TAG, "Interview ended: $closingMessage")
        }

        isInterviewActive = false
        currentQuestion = ""
        questionNumber = 0
    }

    /**
     * Request next question from AI.
     */
    fun requestNextQuestion() {
        if (!isInterviewActive || isGenerating || !aiReady) return
        val id = sessionId ?: return

        isGenerating = true

        viewModelScope.launch {
            try {
                // Get transcript or fall back to audio buffer
                val text = transcript.value
                val question: String

                if (text.isNotEmpty()) {
                    // Save response to memory before generating new question
                    memoryService.saveResponse(id, text)

                    // Generate from text transcript
                    question = aiService.generateFollowUp(text)
                    speechService.clearTranscript()
                } else {
                    // Fall back to audio buffer
                    val audioBase64 = withContext(Dispatchers.Default) {
                        audioRingBuffer.exportToBase64(Config.AI_AUDIO_CONTEXT_SECONDS)
                    }
                    question = if (audioBase64.isNotEmpty()) {
                        aiService.generateFollowUpFromAudio(audioBase64)
                    } else {
                        aiService.generateFollowUp("")
                    }
                }

                currentQuestion = question
                questionNumber++
                questionsAsked = questionsAsked + question

                // Save question to memory
                memoryService.saveQuestion(id, question)

                // Reset auto-question timer
                if (autoQuestionsEnabled) {
                    countdown = autoQuestionInterval
                }

                Log.d(TAG, "Generated question #$questionNumber: $question")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate question", e)
                error = "Failed to generate question: ${e.message}"
            } finally {
                isGenerating = false
            }
        }
    }

    /**
     * Reset all state.
     */
    fun reset() {
        sessionId = null
        error = null
        durationSeconds = 0
        stopDurationTimer()
        isInterviewActive = false
        isGenerating = false
        currentQuestion = ""
        questionNumber = 0
        questionsAsked = emptyList()
        showTypeSelector = true
        countdown = 0
        stopAutoQuestionTimer()
        speechService.clearTranscript()
        audioRingBuffer.clear()
    }

    /**
     * Toggle auto-questions.
     */
    fun toggleAutoQuestions() {
        autoQuestionsEnabled = !autoQuestionsEnabled
        if (autoQuestionsEnabled && isInterviewActive) {
            startAutoQuestionTimer()
        } else {
            stopAutoQuestionTimer()
        }
    }

    // MARK: - Private

    private fun startDurationTimer() {
        durationJob?.cancel()
        durationJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                durationSeconds++
            }
        }
    }

    private fun stopDurationTimer() {
        durationJob?.cancel()
        durationJob = null
    }

    private fun startAutoQuestionTimer() {
        countdown = autoQuestionInterval
        stopAutoQuestionTimer()

        autoQuestionJob = viewModelScope.launch {
            while (isInterviewActive && autoQuestionsEnabled) {
                delay(1000)
                countdown--
                if (countdown <= 0) {
                    requestNextQuestion()
                    countdown = autoQuestionInterval
                }
            }
        }
    }

    private fun stopAutoQuestionTimer() {
        autoQuestionJob?.cancel()
        autoQuestionJob = null
        countdown = 0
    }

    override fun onCleared() {
        super.onCleared()
        cameraService.release()
        speechService.destroy()
    }
}
