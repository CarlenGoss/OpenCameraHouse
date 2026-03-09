package com.carlengosez.open_camera_server

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var btnRecord: Button
    private lateinit var cameraExecutor: ExecutorService

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var imageAnalyzer: ImageAnalysis? = null

    // Estados del sistema
    private enum class SystemState { OFF, ARMED, RECORDING }
    private var currentState = SystemState.OFF

    // 1 HORA en milisegundos
    private val chunkDurationMillis = 60L * 60L * 1000L

    private val autoStopHandler = Handler(Looper.getMainLooper())
    private val autoStopRunnable = Runnable {
        recording?.stop() // Al pasar la hora, detiene el video
    }

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) startCamera()
            else Toast.makeText(this, "Permisos denegados.", Toast.LENGTH_LONG).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ESTA ES LA LÍNEA MÁGICA: Mantiene la pantalla encendida y la app activa
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_main)

        viewFinder = findViewById(R.id.viewFinder)
        btnRecord = findViewById(R.id.btnRecord)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) startCamera() else requestPermissions()

        btnRecord.setOnClickListener { toggleSystem() }
    }

    private fun toggleSystem() {
        when (currentState) {
            SystemState.OFF -> {
                // Encendemos el sistema: Pasa a modo ARMADO (Vigilando)
                currentState = SystemState.ARMED
                btnRecord.text = "Sistema Armado (Detectando...)"
                btnRecord.setBackgroundColor(android.graphics.Color.parseColor("#FFA500")) // Naranja
                Toast.makeText(this, "Sensor de movimiento activado", Toast.LENGTH_SHORT).show()
            }
            SystemState.ARMED, SystemState.RECORDING -> {
                // Apagamos todo el sistema
                currentState = SystemState.OFF
                recording?.stop()
                autoStopHandler.removeCallbacks(autoStopRunnable)
                btnRecord.text = "Iniciar Sistema"
                btnRecord.setBackgroundColor(android.graphics.Color.RED)
            }
        }
    }

    // Esta función es llamada por nuestro detector de movimiento
    private fun onMotionDetected() {
        // Solo iniciamos grabación si el sistema está armado y NO está grabando ya
        if (currentState == SystemState.ARMED) {
            Log.d(TAG, "¡MOVIMIENTO DETECTADO! Iniciando grabación...")
            runOnUiThread {
                currentState = SystemState.RECORDING
                btnRecord.text = "Grabando por Movimiento..."
                btnRecord.setBackgroundColor(android.graphics.Color.parseColor("#008000")) // Verde
            }
            startRecordingChunk()
        }
    }

    private fun startRecordingChunk() {
        val videoCapture = this.videoCapture ?: return

        val name = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "Motion_$name")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraServer-Videos")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions.Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues).build()

        recording = videoCapture.output
            .prepareRecording(this, mediaStoreOutputOptions)
            .apply { if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) withAudioEnabled() }
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when(recordEvent) {
                    is VideoRecordEvent.Start -> {
                        // Inicia el cronómetro de 1 hora
                        autoStopHandler.postDelayed(autoStopRunnable, chunkDurationMillis)
                    }
                    is VideoRecordEvent.Finalize -> {
                        autoStopHandler.removeCallbacks(autoStopRunnable)
                        recording = null

                        // Si el sistema no fue apagado manualmente, vuelve a estado ARMADO para seguir vigilando
                        if (currentState != SystemState.OFF) {
                            runOnUiThread {
                                currentState = SystemState.ARMED
                                btnRecord.text = "Sistema Armado (Detectando...)"
                                btnRecord.setBackgroundColor(android.graphics.Color.parseColor("#FFA500"))
                            }
                        }
                    }
                }
            }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(viewFinder.surfaceProvider) }

            // 1. Configuramos Video (en HD o FHD según prefieras)
            val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD)).build()
            videoCapture = VideoCapture.withOutput(recorder)

            // 2. Configuramos el Analizador de Imagen (NUEVO)
            imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // Evita que el teléfono se congele
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, LuminosityMotionAnalyzer {
                        onMotionDetected()
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                // 3. Vinculamos Preview, Video y Análisis al mismo tiempo
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture, imageAnalyzer)
            } catch(exc: Exception) {
                Log.e(TAG, "Error al vincular la cámara", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun requestPermissions() = activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all { ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        autoStopHandler.removeCallbacks(autoStopRunnable)
    }

    companion object {
        private const val TAG = "CameraServerApp"
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    }

    // --- ALGORITMO DE DETECCIÓN DE MOVIMIENTO ---
    private inner class LuminosityMotionAnalyzer(private val listener: () -> Unit) : ImageAnalysis.Analyzer {
        private var previousPixels: ByteArray? = null
        private var lastAnalyzedTimestamp = 0L

        override fun analyze(image: ImageProxy) {
            val currentTimestamp = System.currentTimeMillis()
            // Solo analizamos 2 fotogramas por segundo para ahorrar batería y CPU
            if (currentTimestamp - lastAnalyzedTimestamp >= 500) {
                val buffer = image.planes[0].buffer // Plano Y (Luminosidad/Blanco y Negro)
                val data = ByteArray(buffer.remaining())
                buffer.get(data)

                if (previousPixels != null) {
                    var motionPixels = 0
                    // Comparamos 1 de cada 10 píxeles para ser más rápidos
                    for (i in data.indices step 10) {
                        val currentPixel = data[i].toInt() and 0xFF
                        val previousPixel = previousPixels!![i].toInt() and 0xFF

                        // Si la diferencia de luz en este píxel es mayor a 50, es "movimiento"
                        if (abs(currentPixel - previousPixel) > 50) {
                            motionPixels++
                        }
                    }

                    // Si más del 2% de los píxeles analizados cambiaron, disparamos la alerta
                    val threshold = (data.size / 10) * 0.02
                    if (motionPixels > threshold) {
                        listener()
                    }
                }
                previousPixels = data.clone()
                lastAnalyzedTimestamp = currentTimestamp
            }
            image.close() // ¡MUY IMPORTANTE cerrar la imagen para no bloquear la cámara!
        }
    }
}