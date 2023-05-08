package com.example.kotlinnchappapp.activites

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import com.example.kotlinnchappapp.databinding.ActivitySignInBinding
import com.example.kotlinnchappapp.utilites.Constants
import com.example.kotlinnchappapp.utilites.PreferanceManeger
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class signInActivity : AppCompatActivity() {
    var binding: ActivitySignInBinding? = null
    private var preferanceManeger: PreferanceManeger? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferanceManeger = PreferanceManeger(applicationContext)
        if (preferanceManeger!!.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            val i = Intent(applicationContext, MainActivity::class.java)
            startActivity(i)
            finish()
        }
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        setLestiner()
    }

    private fun setLestiner() {
        binding!!.txtCreateNewAccount.setOnClickListener {
            val i = Intent(applicationContext, LogInActivity::class.java)
            startActivity(i)
        }
        binding!!.btnSignIn.setOnClickListener { v: View? ->
            if (isValidSignInDetails) {
                signIn()
            }
        }
    }

    private fun signIn() {
        loading(true)
        val database = FirebaseFirestore.getInstance()
        database.collection(Constants.KEY_COLLECTION_USERS)
            .whereEqualTo(Constants.KEY_EMAIL, binding!!.edEmailSignIn.text.toString())
            .whereEqualTo(Constants.KEY_PASSWORD, binding!!.edPasswordSignIn.text.toString())
            .get()
            .addOnCompleteListener { task: Task<QuerySnapshot?> ->
                if (task.isSuccessful && task.result != null && task.result!!.documents.size > 0) {
                    val documentSnapshot = task.result!!.documents[0]
                    preferanceManeger!!.putBoolean(Constants.KEY_IS_SIGNED_IN, true)
                    preferanceManeger!!.putString(Constants.KEY_USER_ID, documentSnapshot.id)
                    preferanceManeger!!.putString(
                        Constants.KEY_NAME,
                        documentSnapshot.getString(Constants.KEY_NAME)
                    )
                    preferanceManeger!!.putString(
                        Constants.KEY_IMAGE,
                        documentSnapshot.getString(Constants.KEY_IMAGE)
                    )
                    val intent = Intent(applicationContext, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                } else {
                    loading(false)
                    showToast("Unable to sign in")
                }
            }
    }

    private fun loading(isLoading: Boolean) {
        if (isLoading) {
            binding!!.btnSignIn.visibility = View.INVISIBLE
            binding!!.progressBar2.visibility = View.VISIBLE
        } else {
            binding!!.progressBar2.visibility = View.INVISIBLE
            binding!!.btnSignIn.visibility = View.VISIBLE
        }
    }

    private fun showToast(m: String) {
        Toast.makeText(applicationContext, m, Toast.LENGTH_LONG).show()
    }

    private val isValidSignInDetails: Boolean
        private get() = if (binding!!.edEmailSignIn.text.toString().trim { it <= ' ' }.isEmpty()) {
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
        } else {
            true
        }
}