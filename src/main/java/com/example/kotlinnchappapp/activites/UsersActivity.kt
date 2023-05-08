package com.example.kotlinnchappapp.activites

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.example.kotlinnchappapp.Adapters.userAdapter
import com.example.kotlinnchappapp.databinding.ActivityUsersBinding
import com.example.kotlinnchappapp.listeners.UserLesteners
import com.example.kotlinnchappapp.models.user
import com.example.kotlinnchappapp.utilites.Constants
import com.example.kotlinnchappapp.utilites.PreferanceManeger
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class UsersActivity : BaseActivity(), UserLesteners {
    private var binding: ActivityUsersBinding? = null
    private var preferanceManeger: PreferanceManeger? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsersBinding.inflate(layoutInflater)
        preferanceManeger = PreferanceManeger(applicationContext)
        setContentView(binding!!.root)
        users
        setListeners()
    }

    private fun setListeners() {
        binding!!.imageBack.setOnClickListener { v: View? -> onBackPressed() }
    }

    private fun showErrorMessage() {
        binding!!.textErrorMessage.text = String.format("%s", "No user available")
        binding!!.textErrorMessage.visibility = View.VISIBLE
    }

    private fun loading(isLoading: Boolean) {
        if (isLoading) {
            binding!!.progressBar.visibility = View.VISIBLE
        } else {
            binding!!.progressBar.visibility = View.INVISIBLE
        }
    }

    private val users: Unit
        get() {
            loading(true)
            val database = FirebaseFirestore.getInstance()
            database.collection(Constants.KEY_COLLECTION_USERS).get()
                .addOnCompleteListener { task: Task<QuerySnapshot?> ->
                    loading(false)
                    val currentUserId = preferanceManeger!!.getString(Constants.KEY_USER_ID)
                    if (task.isSuccessful && task.result != null) {
                        val users: MutableList<user> = ArrayList()
                        for (queryDocumentSnapshot in task.result!!) {
                            if (currentUserId == queryDocumentSnapshot.id) {
                                continue
                            }
                            val user = user()
                            user.name = queryDocumentSnapshot.getString(Constants.KEY_NAME)
                            user.email = queryDocumentSnapshot.getString(Constants.KEY_EMAIL)
                            user.image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE)
                            user.token = queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN)
                            user.id = queryDocumentSnapshot.id
                            users.add(user)
                        }
                        if (users.size > 0) {
                            val usersAdapter = userAdapter(users, this)
                            binding!!.usersRecyclerView.adapter = usersAdapter
                            binding!!.usersRecyclerView.visibility = View.VISIBLE
                        } else {
                            showErrorMessage()
                        }
                    } else {
                        showErrorMessage()
                    }
                }
        }

    override fun onUserClicked(user: user?) {
        val intent = Intent(applicationContext, ChatActivity::class.java)
        intent.putExtra(Constants.KEY_USER, user)
        startActivity(intent)
        finish()
    }
}