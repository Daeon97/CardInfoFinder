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
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

typealias LumaListener = (luma: Double) -> Unit

class MainActivity : AppCompatActivity() {
    private lateinit var viewFinder: PreviewView
    private lateinit var positionInstruction: TextView
    private lateinit var imagePreview: ImageView
    private lateinit var capture: Button
    private lateinit var cardBrand: TextView
    private lateinit var cardType: TextView
    private lateinit var bank: TextView
    private lateinit var country: TextView

    private lateinit var cardBrandShimmer: ShimmerFrameLayout
    private lateinit var cardTypeShimmer: ShimmerFrameLayout
    private lateinit var bankShimmer: ShimmerFrameLayout
    private lateinit var countryShimmer: ShimmerFrameLayout

    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    companion object {
        private const val TAG = "cardInfFndrByEngels"
        private const val CAMERA_REQUEST_CODE: Int = 1997
        private const val ENDPOINT = "https://lookup.binlist.net/%1s"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewFinder = findViewById(R.id.main_view_camera_preview)
        positionInstruction = findViewById(R.id.main_view_position_instruction)
        imagePreview = findViewById(R.id.main_view_image_preview)
        capture = findViewById(R.id.main_view_capture_button)
        cardBrand = findViewById(R.id.main_view_card_brand)
        cardType = findViewById(R.id.main_view_card_type)
        bank = findViewById(R.id.main_view_bank)
        country = findViewById(R.id.main_view_country)
        cardBrandShimmer = findViewById(R.id.main_view_card_brand_shimmer)
        cardTypeShimmer = findViewById(R.id.main_view_card_type_shimmer)
        bankShimmer = findViewById(R.id.main_view_bank_shimmer)
        countryShimmer = findViewById(R.id.main_view_country_shimmer)

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

        capture.setOnClickListener {
            //capturePhoto()
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
        // is this still necessary?
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

                imageCapture = ImageCapture.Builder()
                    .build()

                val imageAnalyzer = ImageAnalysis.Builder()
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                            log("Average luminosity: $luma")
                        })
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        this@MainActivity,
                        cameraSelector,
                        preview,
                        imageCapture,
                        imageAnalyzer
                    )
                } catch (e: Exception) {
                    log("error: $e")
                }
            },
            ContextCompat.getMainExecutor(this@MainActivity)
        )
    }

    /*private fun capturePhoto() {
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

    private fun analyzePhoto() {
        //.
    }*/

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun log(messageToLog: String) {
        Log.wtf(TAG, messageToLog)
    }

    private fun showLoadingCardDetails() {
        startShimmerOnLoadStarted()
        cardBrandShimmer.visibility = View.VISIBLE
        cardTypeShimmer.visibility = View.VISIBLE
        bankShimmer.visibility = View.VISIBLE
        countryShimmer.visibility = View.VISIBLE
    }

    private fun hideLoadingCardDetails() {
        stopShimmer()
        cardBrandShimmer.visibility = View.GONE
        cardTypeShimmer.visibility = View.GONE
        bankShimmer.visibility = View.GONE
        countryShimmer.visibility = View.GONE
    }

    private fun startShimmerOnLoadStarted() {
        cardBrand.setBackgroundColor(resources.getColor(R.color.default_font_color))
        cardType.setBackgroundColor(resources.getColor(R.color.default_font_color))
        bank.setBackgroundColor(resources.getColor(R.color.default_font_color))
        country.setBackgroundColor(resources.getColor(R.color.default_font_color))

        cardBrandShimmer.startShimmer()
        cardTypeShimmer.startShimmer()
        bankShimmer.startShimmer()
        countryShimmer.startShimmer()
    }

    private fun stopShimmerOnLoadSuccessful(
        cardBrandText: String,
        cardTypeText: String,
        bankText: String,
        countryText: String
    ) {
        cardBrand.text = cardBrandText
        cardType.text = cardTypeText
        bank.text = bankText
        country.text = countryText

        cardBrand.setBackgroundColor(resources.getColor(android.R.color.transparent))
        cardType.setBackgroundColor(resources.getColor(android.R.color.transparent))
        bank.setBackgroundColor(resources.getColor(android.R.color.transparent))
        country.setBackgroundColor(resources.getColor(android.R.color.transparent))

        cardBrandShimmer.stopShimmer()
        cardTypeShimmer.stopShimmer()
        bankShimmer.stopShimmer()
        countryShimmer.stopShimmer()
    }

    private fun stopShimmer() {
        cardBrandShimmer.stopShimmer()
        cardTypeShimmer.stopShimmer()
        bankShimmer.stopShimmer()
        countryShimmer.stopShimmer()
    }

    private fun makeRequest(bin: Int) {
        startShimmerOnLoadStarted()
        val request = JsonObjectRequest(Request.Method.GET, String.format(ENDPOINT, bin), null, {
            log("response: $it")
        }) {
            stopShimmer()
            Snackbar.make(
                findViewById(R.id.main_view),
                getString(R.string.an_error_occurred),
                Snackbar.LENGTH_INDEFINITE
            ).setAction(
                getString(R.string.retry)
            ) { makeRequest(bin) }.show()
        }

        Volley.newRequestQueue(this@MainActivity).add(request)
    }

    private class LuminosityAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {
        private fun degreesToFirebaseRotation(degrees: Int): Int = when (degrees) {
            0 -> FirebaseVisionImageMetadata.ROTATION_0
            90 -> FirebaseVisionImageMetadata.ROTATION_90
            180 -> FirebaseVisionImageMetadata.ROTATION_180
            270 -> FirebaseVisionImageMetadata.ROTATION_270
            else -> Log.wtf(TAG, "Rotation must be 0, 90, 180 or 270")
        }

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()
            val data = ByteArray(remaining())
            get(data)
            return data
        }

        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(image: ImageProxy) {
            val mediaImage = image.image
            val imageRotation = degreesToFirebaseRotation(image.imageInfo.rotationDegrees)
            if (mediaImage != null){
                val firebaseVisionImage = FirebaseVisionImage.fromMediaImage(mediaImage, imageRotation)
                val detector = FirebaseVision.getInstance().cloudTextRecognizer
                detector.processImage(firebaseVisionImage).addOnSuccessListener {

                    val resultText = it.text
                    Log.wtf(TAG, "Task completed successfully $resultText")

                }.addOnFailureListener {
                    Log.wtf(TAG, "Task failed with an exception ${it.message}")
                }
            }

            val buffer = image.planes[0].buffer
            val data = buffer.toByteArray()
            val pixels = data.map { it.toInt() and 0xFF }
            val luma = pixels.average()

            listener(luma)

            image.close()
        }
    }
}