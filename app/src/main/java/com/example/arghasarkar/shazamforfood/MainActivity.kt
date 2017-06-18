package com.example.arghasarkar.shazamforfood

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.widget.Toast

import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.google.android.gms.tasks.OnSuccessListener
import android.support.annotation.NonNull
import clarifai2.api.ClarifaiBuilder
import clarifai2.dto.input.ClarifaiInput
import clarifai2.dto.input.image.ClarifaiImage
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream


class MainActivity : AppCompatActivity() {

    val CAMERA_REQUEST_CODE = 0
    lateinit var imageFilePath : String

    lateinit var bitmapImage : Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val policy = StrictMode.ThreadPolicy.Builder()
                .permitAll().build();
        StrictMode.setThreadPolicy(policy);

        nameOfFood.isFocusable = false
        nutrition.isFocusable = false

        cameraButton.setOnClickListener {
            try {
                val imageFile = createImageFile()

                val callCameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (callCameraIntent.resolveActivity(packageManager) != null) {
                    val authorities = packageName + ".fileprovider"
                    ///val imageUri = FileProvider.getUriForFile(this, authorities, imageFile.)
                    val imageUri = Uri.fromFile(imageFile);
                    val fileNameCleanAgain = sanitiseKey(imageUri.toString())

                    // Write a message to the database
                    val database = FirebaseDatabase.getInstance()
                    val myRef = database.getReference(fileNameCleanAgain.toString())
                    myRef.setValue(imageUri.toString())

                    storeFile(fileNameCleanAgain.toString() + ".jpg", imageUri)

                    callCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)

                    startActivityForResult(callCameraIntent, CAMERA_REQUEST_CODE)


                }
            } catch (e: Exception) {
                e.printStackTrace(System.out)

                println(e.stackTrace.toString())

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
                    bitmapImage = setScaledBitmap()
                    photoImageView.setImageBitmap(bitmapImage)

                    val baos = ByteArrayOutputStream()
                    bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                    val data = baos.toByteArray()
                    val storage = FirebaseStorage.getInstance()
                    val storageRef = storage.getReference()
                    // Create a reference to "mountains.jpg"
                    val mountainsRef = storageRef.child("mountains.jpg")
                    // Create a reference to 'images/mountains.jpg'
                    val mountainImagesRef = storageRef.child("images/mountains.jpg")
                    val uploadTask = mountainsRef.putBytes(data)
                    uploadTask.addOnFailureListener(OnFailureListener {
                        // Handle unsuccessful uploads
                    }).addOnSuccessListener(OnSuccessListener<UploadTask.TaskSnapshot> { taskSnapshot ->
                        // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                        val downloadUrl = taskSnapshot.downloadUrl
                        println(downloadUrl)
                        checkWithClarifai(downloadUrl.toString())
                    })
                }
            }
            else -> {
                Toast.makeText(this, "Unrecognised request code.", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun checkWithClarifai(downloadUrl : String) {
        val appID = "KFl7iXRRX0pFmub83mCUK5GjSMTG_D5jHT1lNw7W"
        val appSecret = "YW_KUzY_oQxQoe5vruIoIRc16Gofb6TwNz9UUuve"
        val client = ClarifaiBuilder(appID, appSecret).buildSync()


// NOTE: you probably won't have to handle this. The API client automatically refreshes your access token
// before making any network calls if it is expired
        val token = client.token

        val result = client.getDefaultModels().foodModel()// You can also do Clarifai.getModelByID("id") to get custom models
                .predict()
                .withInputs(
                        ClarifaiInput.forImage(ClarifaiImage.of(downloadUrl))
                )
                .executeSync() // optionally, pass a ClarifaiClient parameter to override the default client instance with another one
                .get();

        val data = result.first().data()
        val foodName = data[0].name()
        println("Food name")
        println(foodName)

        // Setting details on UI
        nameOfFood.setText("Food: ${foodName}")

        when (foodName) {
            "coffee" -> nutrition.setText("Coffee (per 100ml): Protein: 0.1g Fat: 0g Carb: 0g Calories: 0")
            "wine" -> nutrition.setText("Wine (per 100ml): Protein: 0.1g Fat: 0g Carb: 2.7g Calories: 83")
            "beer" -> nutrition.setText("Beer (100g): Protein: 0.5g Fat: 0g Carb: 3.6g Calories: 36")
            "lemon" -> nutrition.setText("Lemon (1 fruit): Protein: 0.6g Fat: 0.2g Carb: 5g Calories: 17")
            else -> nutrition.setText("No nutritional information found")
        }

    }

    @Throws(IOException::class)
    fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timestamp + "_"
        val storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        if (!storageDirectory.exists()) {
            // mkdirs() function used instead of mkdir() to create any parent directory that does
            // not exist.

            storageDirectory.mkdirs()
        } else {
            println("Directory exists!");
        }

        val imageFile = File.createTempFile(imageFileName, ".jpg", storageDirectory)
        imageFilePath = imageFile.absolutePath

        return imageFile
    }

    fun storeFile(key: String, fileUri : Uri) {
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.getReference(key)

        val uploadTask = storageRef.putFile(fileUri)

        uploadTask.addOnFailureListener {
            // Handle unsuccessful uploads
        }.addOnSuccessListener { taskSnapshot ->
            // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
            val downloadUrl = taskSnapshot.downloadUrl
        }

    }

    fun sanitiseKey(fileName: String) : String {
        val fileNameClean = fileName.split("/");
        val fileNameCleanAgain = fileNameClean[fileNameClean.size - 1].replace(".", "").replace("jpg", "")

        return fileNameCleanAgain
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
