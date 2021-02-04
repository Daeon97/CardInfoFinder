package com.engelsimmanuel.cardinfofinder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var viewFinder: PreviewView
    private lateinit var positionInstruction: TextView
    private lateinit var imagePreview: ImageView
    private lateinit var capture: Button
    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    companion object {
        private const val TAG = "cardInfFndrByEngels"
        private const val CAMERA_REQUEST_CODE: Int = 1997
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewFinder = findViewById(R.id.main_view_camera_preview)
        positionInstruction = findViewById(R.id.main_view_position_instruction)
        imagePreview = findViewById(R.id.main_view_image_preview)
        capture = findViewById(R.id.main_view_capture_button)

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

        capture.setOnClickListener {
            capturePhoto()
        }

        requestCameraPermission()
    }

    private fun requestCameraPermission() {
        when (ContextCompat.checkSelfPermission(
            this@MainActivity,
            Manifest.permission.CAMERA
        ) != PackageManager.PERMISSION_GRANTED) {
            true -> {
                positionInstruction.text = getString(R.string.accept_camera_permission)
                positionInstruction.setTextColor(resources.getColor(android.R.color.holo_red_dark))

                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_REQUEST_CODE
                )
            }
            false -> {
                showCamera()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode == CAMERA_REQUEST_CODE) {
            true -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showCamera()
                } else {
                    Snackbar.make(
                        findViewById(R.id.main_view),
                        getString(R.string.camera_permission_required),
                        Snackbar.LENGTH_INDEFINITE
                    )
                        .setAction(
                            getString(R.string.ask_again)
                        ) {
                            requestCameraPermission()
                        }.show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //when(requestCode == )
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }

    private fun showCamera() {
        capture.isEnabled = true
        positionInstruction.text = getString(R.string.instruction)
        positionInstruction.setTextColor(resources.getColor(android.R.color.holo_green_dark))

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this@MainActivity)

        cameraProviderFuture.addListener(
            {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(viewFinder.surfaceProvider)
                    }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(this@MainActivity, cameraSelector, preview)
                } catch (e: Exception) {
                    log("error: $e")
                }
            },
            ContextCompat.getMainExecutor(this@MainActivity)
        )
    }

    private fun capturePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            outputDirectory,
            "${DateFormat.format(" dd-MMMM-yyyy hh:mm:ss a", System.currentTimeMillis())}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this@MainActivity),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    viewFinder.visibility = View.GONE
                    imagePreview.visibility = View.VISIBLE
                    imagePreview.setImageURI(savedUri)
                    val message = "Photo capture succeeded: $savedUri"
                    log(messageToLog = message)
                }

                override fun onError(exception: ImageCaptureException) {
                    log("Photo capture failed: ${exception.message}")
                }

            })
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun log(messageToLog: String) {
        Log.wtf(TAG, messageToLog)
    }
}