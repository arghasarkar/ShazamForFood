package com.example.arghasarkar.shazamforfood

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.widget.Toast

import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    val CAMERA_REQUEST_CODE = 0
    lateinit var imageFilePath : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraButton.setOnClickListener {
            try {
                val imageFile = createImageFile()

                val callCameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (callCameraIntent.resolveActivity(packageManager) != null) {
                    val authorities = packageName + ".fileprovider"
                    val imageUri = FileProvider.getUriForFile(this, authorities, imageFile)
                    callCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)

                    startActivityForResult(callCameraIntent, CAMERA_REQUEST_CODE)
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Cannot create image file." + e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode) {
            CAMERA_REQUEST_CODE -> {
//                if (resultCode == Activity.RESULT_OK && data != null) {
//                    photoImageView.setImageBitmap(data.extras.get("data") as Bitmap)
//                }
                if (resultCode == Activity.RESULT_OK) {
                    photoImageView.setImageBitmap(setScaledBitmap())
                }

            }
            else -> {
                Toast.makeText(this, "Unrecognised request code.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Throws(IOException::class)
    fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.UK).format(Date())
        val imageFileName = "JPEG_" + timestamp + "_"
        val storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        if (!storageDirectory.exists()) {
            // mkdirs() function used instead of mkdir() to create any parent directory that does
            // not exist.
            storageDirectory.mkdirs()
        }

        val imageFile = createTempFile(imageFileName, ".jpg", storageDirectory)
        imageFilePath = imageFile.absolutePath

        return imageFile
    }

    fun setScaledBitmap(): Bitmap {
        val imageViewWidth = photoImageView.width
        val imageViewHeight = photoImageView.height

        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true
        BitmapFactory.decodeFile(imageFilePath, bmOptions)

        val bitmapWidth = bmOptions.outWidth
        val bitmapHeight = bmOptions.outHeight

        val scaleFactor = Math.min(bitmapWidth / imageViewWidth, bitmapHeight / imageViewHeight)
        bmOptions.inSampleSize = scaleFactor
        bmOptions.inJustDecodeBounds = false

        return BitmapFactory.decodeFile(imageFilePath, bmOptions)
    }
}
