package com.engelsimmanuel.cardinfofinder

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.engelsimmanuel.cardinfofinder.Constants.Companion.TAG
import com.engelsimmanuel.cardinfofinder.Constants.Companion.CAMERA_REQUEST_CODE
import com.engelsimmanuel.cardinfofinder.Constants.Companion.ENDPOINT
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var viewFinder: PreviewView
    private lateinit var analyzing: ProgressBar
    private lateinit var analysisError: ImageView
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

    private lateinit var commons: Commons

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewFinder = findViewById(R.id.main_view_camera_preview)
        analyzing = findViewById(R.id.main_view_analyzing)
        analysisError = findViewById(R.id.main_view_analysis_error);
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

        commons = Commons(
            activity = this@MainActivity,
            cardBrand = cardBrand,
            cardType = cardType,
            bank = bank,
            country = country,
            cardBrandShimmer = cardBrandShimmer,
            cardTypeShimmer = cardTypeShimmer,
            bankShimmer = bankShimmer,
            countryShimmer = countryShimmer
        )

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
        commons.log(tag = TAG, messageToLog = "showCamera is called")

        positionInstruction.text = getString(R.string.opening_camera)
        positionInstruction.setTextColor(resources.getColor(android.R.color.holo_blue_dark))

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this@MainActivity)

        cameraProviderFuture.addListener(
            {
                commons.log(tag = TAG, messageToLog = "cameraProviderFuture.addListener is called")

                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder()
                    .build()
                    .also {
                        commons.log(tag = TAG, messageToLog = "preview surface provider")
                        it.setSurfaceProvider(viewFinder.surfaceProvider)
                    }

                imageCapture = ImageCapture.Builder()
                    .build()
                commons.log(tag = TAG, messageToLog = "imageCapture was just assigned")

                val imageAnalyzer = ImageAnalysis.Builder()
                    .build()
                    .also {
                        it.setAnalyzer(
                            cameraExecutor,
                            Analyzer()
                        )
                    }
                commons.log(tag = TAG, messageToLog = "imageAnalyzer was just assigned")

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
                    commons.log(
                        tag = TAG,
                        messageToLog = "cameraProvider was just bound to lifecycle"
                    )

                } catch (e: Exception) {
                    positionInstruction.text =
                        getString(R.string.error_occurred_while_opening_camera)
                    positionInstruction.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                    commons.log(tag = TAG, messageToLog = "error: $e")
                }
            },
            ContextCompat.getMainExecutor(this@MainActivity)
        )
    }

    private fun analyzePhoto() {
        when (analyze.text.toString()) {
            getString(R.string.analyze) -> {
                if (gottenCardDetails.text?.trim().toString().isEmpty()) {
                    commons.showSnackBarWithoutAction(
                        view = findViewById(R.id.main_view),
                        message = getString(R.string.empty_text)
                    )
                } else {
                    makeRequest(gottenCardDetails.text?.trim().toString())
                }
            }
            getString(R.string.enter_manually) -> {
                positionInstruction.text = getString(R.string.enter_card_details)
                positionInstruction.setTextColor(resources.getColor(android.R.color.holo_green_dark))
                gottenCardDetailsTextInputLayout.visibility = View.VISIBLE
                analysisError.visibility = View.GONE
                cameraExecutor.shutdown()
                analyze.text = getString(R.string.analyze)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun makeRequest(bin: String) {
        commons.showLoadingCardDetails()
        val request = JsonObjectRequest(Request.Method.GET, String.format(ENDPOINT, bin), null, {
            commons.log(tag = TAG, messageToLog = "response: $it")
            commons.stopShimmerOnLoadSuccessful(
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
            commons.stopShimmer()
            commons.showSnackBarWithoutAction(
                view = findViewById(R.id.main_view),
                message = getString(R.string.an_error_occurred_analyze_again)
            )
        }

        Volley.newRequestQueue(this@MainActivity).add(request)
    }

    inner class Analyzer : ImageAnalysis.Analyzer {
        private fun degreesToFirebaseRotation(degrees: Int): Int = when (degrees) {
            0 -> FirebaseVisionImageMetadata.ROTATION_0
            90 -> FirebaseVisionImageMetadata.ROTATION_90
            180 -> FirebaseVisionImageMetadata.ROTATION_180
            270 -> FirebaseVisionImageMetadata.ROTATION_270
            else -> FirebaseVisionImageMetadata.ROTATION_0
        }

        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(image: ImageProxy) {

            commons.log(tag = TAG, messageToLog = "analyze image is called")

            val mediaImage = image.image
            val imageRotation = degreesToFirebaseRotation(image.imageInfo.rotationDegrees)

            commons.log(tag = TAG, messageToLog = "imageRotation was just assigned")

            if (mediaImage != null) {
                val firebaseVisionImage =
                    FirebaseVisionImage.fromMediaImage(mediaImage, imageRotation)
                val detector = FirebaseVision.getInstance().cloudTextRecognizer

                commons.log(
                    tag = TAG,
                    messageToLog = "inside mediaImage != null, detector was just assigned"
                )

                CoroutineScope(Dispatchers.Main).launch {
                    withContext(Dispatchers.Main) {
                        analyzing.visibility = View.VISIBLE
                        viewFinder.visibility = View.GONE
                        cameraExecutor.shutdown()
                        positionInstruction.text = getString(R.string.analyzing)
                        positionInstruction.setTextColor(resources.getColor(android.R.color.holo_blue_bright))
                    }
                }

                detector.processImage(firebaseVisionImage).addOnSuccessListener {
                    commons.log(tag = TAG, messageToLog = "analyze image onSuccessListener")

                    positionInstruction.text = getString(R.string.correct_card_details_mistakes)
                    positionInstruction.setTextColor(resources.getColor(R.color.purple_500))
                    gottenCardDetails.setText(it.text)
                    gottenCardDetailsTextInputLayout.visibility = View.VISIBLE
                    analyzing.visibility = View.GONE
                    cameraExecutor.shutdown()
                    analyze.isEnabled = true

                }.addOnFailureListener {
                    commons.log(tag = TAG, messageToLog = "analyze image onFailureListener")
                    positionInstruction.text = getString(R.string.exception_while_scanning_card)
                    positionInstruction.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                    analyzing.visibility = View.GONE
                    analysisError.visibility = View.VISIBLE
                    analyze.text = getString(R.string.enter_manually)
                    analyze.isEnabled = true
                }
            }
        }
    }
}