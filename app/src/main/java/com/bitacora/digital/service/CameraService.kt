package com.bitacora.digital.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * CameraX-based camera and video recording service.
 */
@Singleton
class CameraService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storageService: StorageService
) {
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private var cameraProvider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var currentOutputPath: String? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var currentCameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

    companion object {
        private const val TAG = "CameraService"

        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }

    /**
     * Check if all required permissions are granted.
     */
    fun hasPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Get required permissions array.
     */
    fun getRequiredPermissions(): Array<String> = REQUIRED_PERMISSIONS

    /**
     * Initialize camera with preview view.
     */
    suspend fun initialize(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) = suspendCoroutine { continuation ->
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(lifecycleOwner, previewView)
                _isInitialized.value = true
                continuation.resume(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Camera initialization failed", e)
                continuation.resumeWithException(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        val provider = cameraProvider ?: return

        // Unbind existing use cases
        provider.unbindAll()

        // Preview
        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        // Video capture
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD))
            .build()

        videoCapture = VideoCapture.withOutput(recorder)

        try {
            provider.bindToLifecycle(
                lifecycleOwner,
                currentCameraSelector,
                preview,
                videoCapture
            )
            Log.d(TAG, "Camera use cases bound successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind camera use cases", e)
            throw e
        }
    }

    /**
     * Start recording video.
     */
    fun startRecording(sessionId: String): String {
        val videoCapture = videoCapture ?: throw IllegalStateException("Camera not initialized")

        val outputPath = storageService.sessionFilePath(sessionId)
        currentOutputPath = outputPath

        // Ensure sessions directory exists
        storageService.initialize()

        val outputFile = File(outputPath)
        val outputOptions = FileOutputOptions.Builder(outputFile).build()

        recording = videoCapture.output
            .prepareRecording(context, outputOptions)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        _isRecording.value = true
                        Log.d(TAG, "Recording started: $outputPath")
                    }
                    is VideoRecordEvent.Finalize -> {
                        _isRecording.value = false
                        if (event.hasError()) {
                            Log.e(TAG, "Recording error: ${event.error}")
                        } else {
                            Log.d(TAG, "Recording saved: ${event.outputResults.outputUri}")
                        }
                    }
                }
            }

        return outputPath
    }

    /**
     * Stop recording and return the output path.
     */
    fun stopRecording(): String {
        recording?.stop()
        recording = null
        return currentOutputPath ?: throw IllegalStateException("No recording in progress")
    }

    /**
     * Switch between front and back camera.
     */
    fun switchCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        currentCameraSelector = if (currentCameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }

        bindCameraUseCases(lifecycleOwner, previewView)
    }

    /**
     * Release camera resources.
     */
    fun release() {
        recording?.stop()
        recording = null
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        _isInitialized.value = false
    }
}
