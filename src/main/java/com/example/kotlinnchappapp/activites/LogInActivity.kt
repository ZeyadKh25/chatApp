package com.example.kotlinnchappapp.activites

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.kotlinnchappapp.databinding.ActivityLogInBinding
import com.example.kotlinnchappapp.utilites.Constants
import com.example.kotlinnchappapp.utilites.PreferanceManeger
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException

class LogInActivity : AppCompatActivity() {
    private var binding: ActivityLogInBinding? = null
    private var encodedImage: String? = null
    private var preferanceManeger: PreferanceManeger? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogInBinding.inflate(layoutInflater)
        preferanceManeger = PreferanceManeger(applicationContext)
        setContentView(binding!!.root)
        setListener()
    }

    private fun setListener() {
        binding!!.signIn.setOnClickListener { onBackPressed() }
        binding!!.btnSiginUp.setOnClickListener {
            if (isValidSignUpDetails) {
                signUp()
            }
        }
        binding!!.imageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            pickImage.launch(intent)
        }
    }

    private fun showToast(m: String?) {
        Toast.makeText(applicationContext, m, Toast.LENGTH_LONG).show()
    }

    private fun signUp() {
        loading(true)
        val database = FirebaseFirestore.getInstance()
        val user = HashMap<String, Any?>()
        user[Constants.KEY_NAME] = binding!!.editTextTextPersonName.text.toString()
        user[Constants.KEY_EMAIL] = binding!!.edEmailSignIn.text.toString()
        user[Constants.KEY_PASSWORD] = binding!!.edPasswordSignIn.text.toString()
        user[Constants.KEY_IMAGE] = encodedImage
        database.collection(Constants.KEY_COLLECTION_USERS).add(user)
            .addOnSuccessListener { decRef: DocumentReference ->
                loading(false)
                preferanceManeger!!.putString(Constants.KEY_USER_ID, decRef.id)
                preferanceManeger!!.putString(
                    Constants.KEY_NAME,
                    binding!!.editTextTextPersonName.text.toString()
                )
                preferanceManeger!!.putString(Constants.KEY_IMAGE, encodedImage)
                val intent = Intent(applicationContext, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
            }.addOnFailureListener { exception: Exception ->
                loading(false)
                showToast(exception.message)
            }
    }

    private fun encodeImage(bitmap: Bitmap): String {
        val previewWidth = 150
        val previewHeight = bitmap.height * previewWidth / bitmap.width
        val previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false)
        val byteArrayOutputStream = ByteArrayOutputStream()
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
        val bytes = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                if (result.data != null) {
                    val imageUri = result.data!!.data
                    try {
                        val inputStream = contentResolver.openInputStream(imageUri!!)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        binding!!.imageView.setImageBitmap(bitmap)
                        binding!!.textAddImage.visibility = View.GONE
                        encodedImage = encodeImage(bitmap)
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    private val isValidSignUpDetails: Boolean
        get() = if (encodedImage == null) {
            showToast("Select profile image")
            false
        } else if (binding!!.editTextTextPersonName.text.toString().trim { it <= ' ' }.isEmpty()) {
            showToast("Enter name")
            false
        } else if (binding!!.edEmailSignIn.text.toString().trim { it <= ' ' }.isEmpty()) {
            showToast("Enter email")
            false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding!!.edEmailSignIn.text.toString())
                .matches()
        ) {
            showToast("Enter valid email")
            false
        } else if (binding!!.edPasswordSignIn.text.toString().trim { it <= ' ' }.isEmpty()) {
            showToast("Enter password")
            false
        } else if (binding!!.edRePasswordSignIn.text.toString().trim { it <= ' ' }.isEmpty()) {
            showToast("Confirm your password")
            false
        } else if (binding!!.edPasswordSignIn.text.toString() != binding!!.edRePasswordSignIn.text.toString()) {
            showToast("Password & confirm password must be same")
            false
        } else {
            true
        }

    private fun loading(isLoading: Boolean) {
        if (isLoading) {
            binding!!.btnSiginUp.visibility = View.INVISIBLE
            binding!!.progressBar.visibility = View.VISIBLE
        } else {
            binding!!.progressBar.visibility = View.INVISIBLE
            binding!!.btnSiginUp.visibility = View.VISIBLE
        }
    }

}