package com.example.visionedge

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var textureView: TextureView
    private lateinit var processedImage: ImageView
    private lateinit var modeLabel: TextView

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null

    private var currentFilter = Filter.EDGE

    // JNI: native function implemented in C++
    external fun processFrameJNI(
        data: ByteArray,
        width: Int,
        height: Int,
        mode: Int
    ): ByteArray

    enum class Filter { ORIGINAL, GRAY, EDGE }

    companion object {
        init {
            // Must match CMake project's target name: "visionedge"
            System.loadLibrary("visionedge")
        }

        private const val CAMERA_REQUEST = 101
        private const val TAG = "VisionEdge"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")
        setContentView(R.layout.activity_main)

        // View references
        modeLabel = findViewById(R.id.txtMode)
        textureView = findViewById(R.id.textureView)
        processedImage = findViewById(R.id.processedImage)

        // Default mode
        modeLabel.text = "Mode: EDGE"

        // Filter buttons
        findViewById<Button>(R.id.btnOriginal).setOnClickListener {
            currentFilter = Filter.ORIGINAL
            modeLabel.text = "Mode: ORIGINAL"
        }

        findViewById<Button>(R.id.btnGray).setOnClickListener {
            currentFilter = Filter.GRAY
            modeLabel.text = "Mode: GRAYSCALE"
        }

        findViewById<Button>(R.id.btnEdge).setOnClickListener {
            currentFilter = Filter.EDGE
            modeLabel.text = "Mode: EDGE"
        }

        // Capture button
        findViewById<Button>(R.id.btnCapture).setOnClickListener {
            captureCurrentFrame()
        }

        // Permission check
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "Requesting camera permission")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_REQUEST
            )
        } else {
            Log.d(TAG, "Camera permission already granted, starting preview")
            startCameraPreview()
        }
    }

    private fun startCameraPreview() {
        Log.d(TAG, "startCameraPreview called")
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                Log.d(TAG, "SurfaceTexture available, opening camera")
                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                // No-op
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                Log.d(TAG, "SurfaceTexture destroyed")
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                val bitmap = textureView.bitmap ?: return

                // ORIGINAL mode: just show camera frame, no JNI call
                if (currentFilter == Filter.ORIGINAL) {
                    processedImage.setImageBitmap(bitmap)
                    return
                }

                val width = bitmap.width
                val height = bitmap.height

                val bufferSize = width * height * 4
                val buffer = java.nio.ByteBuffer.allocate(bufferSize)
                bitmap.copyPixelsToBuffer(buffer)
                val inputBytes = buffer.array()

                val mode = when (currentFilter) {
                    Filter.GRAY -> 1
                    Filter.EDGE -> 2
                    else -> 0
                }

                val outputBytes = processFrameJNI(inputBytes, width, height, mode)

                val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                outBitmap.copyPixelsFromBuffer(java.nio.ByteBuffer.wrap(outputBytes))
                processedImage.setImageBitmap(outBitmap)
            }
        }
    }

    private fun openCamera() {
        Log.d(TAG, "openCamera called")
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        val cameraId = manager.cameraIdList[0]

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Camera permission not granted at openCamera")
            return
        }

        manager.openCamera(
            cameraId,
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Log.d(TAG, "Camera opened")
                    cameraDevice = camera
                    createPreviewSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    Log.e(TAG, "Camera disconnected")
                    camera.close()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(TAG, "Camera error: $error")
                    camera.close()
                }
            },
            null
        )
    }

    private fun createPreviewSession() {
        Log.d(TAG, "createPreviewSession called")
        val texture = textureView.surfaceTexture
        if (texture == null) {
            Log.e(TAG, "SurfaceTexture is null")
            return
        }

        texture.setDefaultBufferSize(textureView.width, textureView.height)
        val surface = Surface(texture)

        val camera = cameraDevice
        if (camera == null) {
            Log.e(TAG, "cameraDevice is null in createPreviewSession")
            return
        }

        val requestBuilder =
            camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(surface)
            }

        camera.createCaptureSession(
            listOf(surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    Log.d(TAG, "CaptureSession configured")
                    captureSession = session
                    try {
                        captureSession?.setRepeatingRequest(requestBuilder.build(), null, null)
                    } catch (e: CameraAccessException) {
                        Log.e(TAG, "CameraAccessException in setRepeatingRequest: ${e.message}")
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "CaptureSession configuration failed")
                }
            },
            null
        )
    }

    private fun captureCurrentFrame() {
        // Grab bitmap from the processedImage ImageView
        processedImage.isDrawingCacheEnabled = true
        processedImage.buildDrawingCache()
        val bitmap = processedImage.drawingCache

        if (bitmap == null) {
            Toast.makeText(this, "No frame to capture yet", Toast.LENGTH_SHORT).show()
            processedImage.isDrawingCacheEnabled = false
            return
        }

        try {
            val picturesDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
            if (picturesDir == null) {
                Toast.makeText(this, "Cannot access pictures directory", Toast.LENGTH_SHORT).show()
                processedImage.isDrawingCacheEnabled = false
                return
            }

            val fileName = "visionedge_${System.currentTimeMillis()}.png"
            val file = java.io.File(picturesDir, fileName)

            val outputStream = java.io.FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            Log.d(TAG, "Saved frame to: ${file.absolutePath}")
            Toast.makeText(this, "Saved: ${file.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving frame: ${e.message}")
            Toast.makeText(this, "Error saving frame", Toast.LENGTH_SHORT).show()
        } finally {
            processedImage.isDrawingCacheEnabled = false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "onRequestPermissionsResult called")

        if (requestCode == CAMERA_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Camera permission granted, starting preview")
                startCameraPreview()
            } else {
                Log.e(TAG, "Camera permission denied")
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called, closing camera")
        captureSession?.close()
        cameraDevice?.close()
    }
}
