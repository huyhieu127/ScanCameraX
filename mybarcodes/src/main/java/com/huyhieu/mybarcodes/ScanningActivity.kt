package com.huyhieu.mybarcodes

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.android.synthetic.main.activity_scanning.*
import java.io.IOException


class ScanningActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 1
        private const val REQUEST_CODE_TAKE_PHOTO = 11
        private const val REQUEST_CODE_CHOOSE_PHOTO = 12
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanning)

        /*Scanning with take photo*/
        buttonTake.setOnClickListener {
            if (checkAllPermission(CAMERA_REQUIRED_PERMISSIONS)) {
                setTakePhoto()
            } else {
                ActivityCompat.requestPermissions(
                    this, CAMERA_REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
                )
            }
        }

        /*Preview camera*/
        buttonCamera.setOnClickListener {
            startActivity(Intent(this, PreviewCameraActivity::class.java))
        }
        /*Scanning with choose photo in collection*/
        button.setOnClickListener {
            setChooseImage()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (checkAllPermission(CAMERA_REQUIRED_PERMISSIONS)) {
                setTakePhoto()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setTakePhoto() {
        startActivityForResult(Intent(MediaStore.ACTION_IMAGE_CAPTURE), REQUEST_CODE_TAKE_PHOTO)
    }

    private fun setChooseImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(
            Intent.createChooser(intent, "Select Picture"),
            REQUEST_CODE_CHOOSE_PHOTO
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            /*Take image success*/
            if (requestCode == REQUEST_CODE_TAKE_PHOTO) {
                val imageBitmap = data?.extras?.get("data") as Bitmap
                imageView.setImageBitmap(imageBitmap)
                scanPhoto(imageBitmap)
            }
            /*Get image success*/
            if (requestCode == REQUEST_CODE_CHOOSE_PHOTO) {
                val selectedImageUri: Uri? = data?.data
                var bitmap: Bitmap? = null
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(
                        this.contentResolver,
                        selectedImageUri
                    )
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                imageView.setImageURI(selectedImageUri)
                scanPhoto(bitmap)
            }
        }
    }

    /*Scan photo with bitmap*/
    private fun scanPhoto(bitmap: Bitmap?) {
        textView.visibility = View.GONE
        imageView.visibility = View.VISIBLE

        var image: InputImage? = null
        if (bitmap != null) {
            image = InputImage.fromBitmap(bitmap, 0)
        }
        val scanner = BarcodeScanning.getClient()
        scanner.process(image!!)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    /*val bounds = barcode.boundingBox
                    val corners = barcode.cornerPoints*/

                    val rawValue = barcode.rawValue

                    // See API reference for complete list of supported types
                    when (barcode.valueType) {
                        Barcode.TYPE_WIFI -> {
                            val ssid = barcode.wifi!!.ssid
                            val password = barcode.wifi!!.password
                            val type = barcode.wifi!!.encryptionType
                            showDialogAlert("SSID: ${ssid}\nPassword: ${password}\nType: $type",
                                DialogInterface.OnClickListener { p0, _ ->
                                    p0.dismiss()
                                })
                        }
                        Barcode.TYPE_URL -> {
                            val title = barcode.url!!.title
                            val url = barcode.url!!.url
                            showDialogAlert("Title: ${title}\nUrl: $url",
                                DialogInterface.OnClickListener { p0, _ ->
                                    p0.dismiss()
                                    val browserIntent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(url)
                                    )
                                    startActivity(browserIntent)
                                })
                        }
                        else -> {
                            showDialogAlert(rawValue.toString(),
                                DialogInterface.OnClickListener { p0, _ ->
                                    p0.dismiss()
                                })
                        }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
            }
    }
}
