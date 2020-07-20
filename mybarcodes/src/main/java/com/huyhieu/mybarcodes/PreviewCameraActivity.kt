package com.huyhieu.mybarcodes

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.android.synthetic.main.activity_preview_camera.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.Executors


class PreviewCameraActivity : AppCompatActivity() {

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private var executors = Executors.newSingleThreadExecutor()
    private var isFlash = false
    private var camera: Camera? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var imageCapture: ImageCapture? = null

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 11111
        const val TAG = ">>>>>>>>>>>>>>>"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview_camera)

        val displayMetrics = DisplayMetrics()
        this.windowManager.defaultDisplay.getMetrics(displayMetrics)

        val params: FrameLayout.LayoutParams = prvCamera.layoutParams as FrameLayout.LayoutParams
        params.height = displayMetrics.widthPixels
        params.width = displayMetrics.widthPixels
        cvBack.setOnClickListener {
            finish()
        }
        setFlash()
        // Every time the orientation of device changes, update rotation for use cases
        if (checkAllPermission(CAMERA_REQUIRED_PERMISSIONS)) {
            prvCamera.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                this,
                CAMERA_REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun setFlash() {
        val isFlashAvailable =
            applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
        if (isFlashAvailable) {
            imgFlash.setOnClickListener {
                if (isFlash) {
                    /*OFF flash*/
                    camera?.cameraControl?.enableTorch(false)
                    imgFlash.setImageDrawable(
                        ContextCompat.getDrawable(
                            this,
                            R.drawable.ic_baseline_flash_on_24
                        )
                    )
                    isFlash = false
                } else {
                    /*ON flash*/
                    camera?.cameraControl?.enableTorch(true)
                    imgFlash.setImageDrawable(
                        ContextCompat.getDrawable(
                            this,
                            R.drawable.ic_baseline_flash_off_24
                        )
                    )
                    isFlash = true
                }
                try {

                } catch (e: java.lang.Exception) {
                }
            }
        } else {
            Toast.makeText(
                this,
                "Thiết bị không hỗ trợ flash hoặc đã bị hỏng. Vui lòng kiểm tra lại!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(this))
    }

    lateinit var overlay: Bitmap
    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val preview: Preview = Preview.Builder().build()
        val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        imageCapture = ImageCapture.Builder().setFlashMode(ImageCapture.FLASH_MODE_ON).build()

        /* ImageAnalysis.Analyzer */
        imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build().also {
                it.setAnalyzer(
                    executors,
                    ImageAnalysis.Analyzer { imageProxy: ImageProxy ->
                        /*Scan image*/
                        scanImage(imageProxy)
                    })
            }
        // Unbind use cases before rebinding
        cameraProvider.unbindAll()
        try {
            // Bind use cases to camera
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                imageAnalyzer,
                imageCapture,
                preview
            )
            preview.setSurfaceProvider(prvCamera.createSurfaceProvider())
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private var isShow = false

    @SuppressLint("UnsafeExperimentalUsageError")
    private fun scanImage(imageProxy: ImageProxy) {
        /*ML Kit module*/
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE
            )
            .build()
        val scanner = BarcodeScanning.getClient(options)
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        it.result?.forEach { barcode ->
                            isShow = true
                            val bounds = barcode.boundingBox
                            val corners = barcode.cornerPoints
                            val rawValue = barcode.rawValue
                            when (barcode.valueType) {
                                Barcode.TYPE_WIFI -> {
                                    val ssid = barcode.wifi!!.ssid
                                    val password = barcode.wifi!!.password
                                    val type = barcode.wifi!!.encryptionType
                                    showDialogAlert("SSID: ${ssid}\nPassword: ${password}\nType: $type",
                                        DialogInterface.OnClickListener { p0, _ ->
                                            isShow = false
                                            p0.dismiss()
                                            finish()
                                        })
                                }
                                Barcode.TYPE_URL -> {
                                    val title = barcode.url!!.title
                                    val url = barcode.url!!.url
                                    showDialogAlert("Title: ${title}\nUrl: $url",
                                        DialogInterface.OnClickListener { p0, _ ->
                                            isShow = false
                                            p0.dismiss()
                                            val browserIntent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(url)
                                            )
                                            //startActivity(browserIntent)
                                            finish()
                                        })
                                }
                                else -> {
                                    showDialogAlert(rawValue.toString(),
                                        DialogInterface.OnClickListener { p0, _ ->
                                            isShow = false
                                            p0.dismiss()
                                            finish()
                                        })
                                }
                            }
                        }
                    } else {
                        Log.e(TAG, "Bị lỗi rồi nè...: 1 -> ", it.exception)
                    }
                    if (!isShow) {
                        imageProxy.close()
                    }
                }
                .addOnFailureListener {
                    Log.e(TAG, "Bị lỗi rồi nè...: 2 -> ", it)
                    imageProxy.close()
                }
        }
    }

    private fun drawShape(
        bounds: Rect,
        corners: Array<Point>?
    ) {
        val bitmap = prvCamera.bitmap ?: return
        overlay = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)

        GlobalScope.launch(Dispatchers.Unconfined) {
            val paint = Paint().apply {
                //isAntiAlias = true
                style = Paint.Style.STROKE
                color = Color.GREEN
                strokeWidth = 5f
            }
            val canvas = Canvas(overlay)
            /*canvas.drawRect(
                convertDpToPixel(bounds.left.toFloat()),
                convertDpToPixel(bounds.top.toFloat()),
                convertDpToPixel(bounds.right.toFloat()),
                convertDpToPixel(bounds.bottom.toFloat()),
                paint
            )*/
            canvas.drawRect(
                bounds.left.toFloat(),
                bounds.top.toFloat(),
                bounds.right.toFloat(),
                bounds.bottom.toFloat(),
                paint
            )
            Canvas(overlay).apply {
                canvas
            }
        }

        runOnUiThread {
            imageShape.setImageBitmap(overlay)
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (checkAllPermission(CAMERA_REQUIRED_PERMISSIONS)) {
                prvCamera.post { startCamera() }
            } else {
                Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Shut down our background executor
        executors.shutdown()
    }
}