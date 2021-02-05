package com.engelsimmanuel.cardinfofinder

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
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
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var viewFinder: PreviewView
    private lateinit var positionInstruction: TextView
    private lateinit var gottenCardDetailsTextInputLayout: TextInputLayout
    private lateinit var gottenCardDetails: TextInputEditText
    private lateinit var analyze: Button
    private lateinit var cardBrand: TextView
    private lateinit var cardType: TextView
    private lateinit var bank: TextView
    private lateinit var country: TextView

    private lateinit var cardBrandShimmer: ShimmerFrameLayout
    private lateinit var cardTypeShimmer: ShimmerFrameLayout
    private lateinit var bankShimmer: ShimmerFrameLayout
    private lateinit var countryShimmer: ShimmerFrameLayout

    private var imageCapture: ImageCapture? = null
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
        gottenCardDetailsTextInputLayout =
            findViewById(R.id.main_view_gotten_card_details_text_input_layout)
        gottenCardDetails = findViewById(R.id.main_view_gotten_card_details)
        analyze = findViewById(R.id.main_view_analyze_button)
        cardBrand = findViewById(R.id.main_view_card_brand)
        cardType = findViewById(R.id.main_view_card_type)
        bank = findViewById(R.id.main_view_bank)
        country = findViewById(R.id.main_view_country)
        cardBrandShimmer = findViewById(R.id.main_view_card_brand_shimmer)
        cardTypeShimmer = findViewById(R.id.main_view_card_type_shimmer)
        bankShimmer = findViewById(R.id.main_view_bank_shimmer)
        countryShimmer = findViewById(R.id.main_view_country_shimmer)

        cameraExecutor = Executors.newSingleThreadExecutor()

        analyze.setOnClickListener {
            analyzePhoto()
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

    private fun showCamera() {
        log("showCamera is called")

        positionInstruction.text = getString(R.string.opening_camera)
        positionInstruction.setTextColor(resources.getColor(android.R.color.holo_blue_dark))

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this@MainActivity)

        cameraProviderFuture.addListener(
            {
                log("cameraProviderFuture.addListener is called")

                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder()
                    .build()
                    .also {
                        log("preview surface provider")
                        it.setSurfaceProvider(viewFinder.surfaceProvider)
                    }

                imageCapture = ImageCapture.Builder()
                    .build()
                log("imageCapture was just assigned")

                val imageAnalyzer = ImageAnalysis.Builder()
                    .build()
                    .also {
                        it.setAnalyzer(
                            cameraExecutor,
                            Analyzer()
                        )
                    }
                log("imageAnalyzer was just assigned")

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

                    positionInstruction.text = getString(R.string.instruction)
                    positionInstruction.setTextColor(resources.getColor(android.R.color.holo_green_dark))
                    log("cameraProvider was just bound to lifecycle")

                } catch (e: Exception) {
                    positionInstruction.text =
                        getString(R.string.error_occurred_while_opening_camera)
                    positionInstruction.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                    log("error: $e")
                }
            },
            ContextCompat.getMainExecutor(this@MainActivity)
        )
    }

    private fun analyzePhoto() {
        when (analyze.text.toString()) {
            getString(R.string.analyze) -> {
                // get text from text input edit text here
                // make request with text as BIN
                if (analyze.text.trim().toString().isEmpty()) {
                    Snackbar.make(
                        findViewById(R.id.main_view),
                        getString(R.string.empty_text),
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    makeRequest(gottenCardDetails.text?.trim().toString())
                }
            }
            getString(R.string.enter_manually) -> {
                positionInstruction.text = getString(R.string.enter_card_details)
                positionInstruction.setTextColor(resources.getColor(android.R.color.holo_green_dark))
                gottenCardDetailsTextInputLayout.visibility = View.VISIBLE
                viewFinder.visibility = View.GONE
                cameraExecutor.shutdown()
                analyze.text = getString(R.string.analyze)
            }
        }
    }

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

        cardBrandShimmer.showShimmer(true)
        cardTypeShimmer.showShimmer(true)
        bankShimmer.showShimmer(true)
        countryShimmer.showShimmer(true)
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

        cardBrandShimmer.hideShimmer()
        cardTypeShimmer.hideShimmer()
        bankShimmer.hideShimmer()
        countryShimmer.hideShimmer()
    }

    private fun stopShimmer() {
        cardBrandShimmer.hideShimmer()
        cardTypeShimmer.hideShimmer()
        bankShimmer.hideShimmer()
        countryShimmer.hideShimmer()
    }

    private fun makeRequest(bin: String) {
        showLoadingCardDetails()
        val request = JsonObjectRequest(Request.Method.GET, String.format(ENDPOINT, bin), null, {
            log("response: $it")
            stopShimmerOnLoadSuccessful(
                cardBrandText = when (it.isNull("brand") || it.getString("brand").isNullOrEmpty()) {
                    true -> "Card brand: UNKNOWN"
                    false -> "Card brand: ${it.getString("brand")}"
                },
                cardTypeText = when (it.isNull("type") || it.getString("type").isNullOrEmpty()) {
                    true -> "Card type: UNKNOWN"
                    false -> "Card type: ${it.getString("type")}"
                },
                bankText = when (it.isNull("bank") || it.getJSONObject("bank")
                    .isNull("name") || it.getJSONObject("bank")
                    .getString("name").isNullOrEmpty()) {
                    true -> "Bank: UNKNOWN"
                    false -> "Bank: ${it.getJSONObject("bank").getString("name")}"
                },
                countryText = when (it.isNull("country") || it.getJSONObject("country")
                    .isNull("name") || it.getJSONObject("country")
                    .getString("name").isNullOrEmpty()) {
                    true -> "Country: UNKNOWN"
                    false -> "Country: ${it.getJSONObject("country").getString("name")}"
                }
            )
        }) {
            stopShimmer()
            Snackbar.make(
                findViewById(R.id.main_view),
                getString(R.string.an_error_occurred_analyze_again),
                Snackbar.LENGTH_SHORT
            ).show()
        }

        Volley.newRequestQueue(this@MainActivity).add(request)
    }

    inner class Analyzer : ImageAnalysis.Analyzer {
        private fun degreesToFirebaseRotation(degrees: Int): Int = when (degrees) {
            0 -> FirebaseVisionImageMetadata.ROTATION_0
            90 -> FirebaseVisionImageMetadata.ROTATION_90
            180 -> FirebaseVisionImageMetadata.ROTATION_180
            270 -> FirebaseVisionImageMetadata.ROTATION_270
            else -> Log.wtf(TAG, "Rotation must be 0, 90, 180 or 270")
        }

        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(image: ImageProxy) {

            log("analyze image is called")

            val mediaImage = image.image
            val imageRotation = degreesToFirebaseRotation(image.imageInfo.rotationDegrees)

            log("imageRotation was just assigned")

            if (mediaImage != null) {
                val firebaseVisionImage =
                    FirebaseVisionImage.fromMediaImage(mediaImage, imageRotation)
                val detector = FirebaseVision.getInstance().cloudTextRecognizer

                log("inside mediaImage != null, detector was just assigned")

                detector.processImage(firebaseVisionImage).addOnSuccessListener {
                    log("analyze image onSuccessListener")

                    positionInstruction.text = getString(R.string.correct_card_details_mistakes)
                    positionInstruction.setTextColor(resources.getColor(R.color.purple_500))
                    gottenCardDetails.setText(it.text)
                    gottenCardDetailsTextInputLayout.visibility = View.VISIBLE
                    viewFinder.visibility = View.GONE
                    cameraExecutor.shutdown()
                    analyze.isEnabled = true

                }.addOnFailureListener {
                    log("analyze image onFailureListener")
                    positionInstruction.text = getString(R.string.exception_while_scanning_card)
                    positionInstruction.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                    analyze.text = getString(R.string.enter_manually)
                    analyze.isEnabled = true
                }
            }
        }
    }
}