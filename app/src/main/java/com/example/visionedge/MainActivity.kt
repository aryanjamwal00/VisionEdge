package com.example.visionedge

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var textureView: TextureView
    private lateinit var processedImage: ImageView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null

    external fun processFrameJNI(data: ByteArray, width: Int, height: Int): ByteArray

    companion object {
        init {
            // ⚠️ Library name must match CMake target (usually "native-lib")
            System.loadLibrary("visionedge")
        }
        private const val CAMERA_REQUEST = 101
        private const val TAG = "VisionEdge"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")
        setContentView(R.layout.activity_main)

        textureView = findViewById(R.id.textureView)
        processedImage = findViewById(R.id.processedImage)

        if (!::textureView.isInitialized) {
            Toast.makeText(this, "textureView not found in layout!", Toast.LENGTH_LONG).show()
            Log.e(TAG, "textureView is not initialized. Check activity_main.xml IDs.")
            return
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
            ) {}

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                Log.d(TAG, "SurfaceTexture destroyed")
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                // Later we'll grab frame here for processing
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

        manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
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
        }, null)
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

        val requestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        requestBuilder.addTarget(surface)

        camera.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
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
        }, null)
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
                // Don’t finish() for now, so we can see logs
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
