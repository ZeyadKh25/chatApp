package com.example.kotlinnchappapp.activites

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Toast
import com.example.kotlinnchappapp.Adapters.ResentConversationAdapter
import com.example.kotlinnchappapp.databinding.ActivityMainBinding
import com.example.kotlinnchappapp.listeners.ConversationLesteners
import com.example.kotlinnchappapp.models.ChatMessage
import com.example.kotlinnchappapp.models.user
import com.example.kotlinnchappapp.utilites.Constants
import com.example.kotlinnchappapp.utilites.PreferanceManeger
import com.google.firebase.firestore.*
import com.google.firebase.messaging.FirebaseMessaging
import java.util.ArrayList
import java.util.HashMap

class MainActivity : BaseActivity(), ConversationLesteners {
    private var binding: ActivityMainBinding? = null
    private var preferanceManeger: PreferanceManeger? = null
    private var conversations: MutableList<ChatMessage>? = null
    private var conversationsAdapter: ResentConversationAdapter? = null
    private var database: FirebaseFirestore? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferanceManeger = PreferanceManeger(applicationContext)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        init()
        loadUserDetails()
        token
        setListeners()
        listenConversations()
    }

    private fun init() {
        conversations = ArrayList()
        conversationsAdapter = ResentConversationAdapter(conversations as ArrayList<ChatMessage>, this)
        binding!!.conversationsRecyclerView.adapter = conversationsAdapter
        database = FirebaseFirestore.getInstance()
    }

    private fun setListeners() {
        binding!!.imageSignOut.setOnClickListener { v: View? -> signOut() }
        binding!!.fabNewChat.setOnClickListener { v: View? ->
            startActivity(
                Intent(
                    applicationContext, UsersActivity::class.java
                )
            )
        }
    }

    private fun listenConversations() {
        database!!.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
            .whereEqualTo(
                Constants.KEY_SENDER_ID,
                preferanceManeger!!.getString(Constants.KEY_USER_ID)
            ).addSnapshotListener(eventListener)
        database!!.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
            .whereEqualTo(
                Constants.KEY_RECEIVER_ID,
                preferanceManeger!!.getString(Constants.KEY_USER_ID)
            ).addSnapshotListener(eventListener)
    }

    private fun showToast(m: String) {
        Toast.makeText(applicationContext, m, Toast.LENGTH_LONG).show()
    }

    private fun updateToken(token: String) {
        val database = FirebaseFirestore.getInstance()
        val documentReference = database.collection(Constants.KEY_COLLECTION_USERS).document(
            preferanceManeger!!.getString(Constants.KEY_USER_ID)!!
        )
        documentReference
            .update(
                Constants.KEY_FCM_TOKEN,
                token
            ) //                .addOnSuccessListener(unused -> showToast("Token updated successfully"))
            .addOnFailureListener { e: Exception? -> showToast("Unable to update token") }
    }

    private val eventListener =
        label@ EventListener { value: QuerySnapshot?, error: FirebaseFirestoreException? ->
            if (error != null) {
                return@EventListener
            }
            if (value != null) {
                for (documentChange in value.documentChanges) {
                    if (documentChange.type == DocumentChange.Type.ADDED) {
                        val senderId = documentChange.document.getString(Constants.KEY_SENDER_ID)
                        val receiverId =
                            documentChange.document.getString(Constants.KEY_RECEIVER_ID)
                        val chatMessage = ChatMessage()
                        chatMessage.senderId = senderId
                        chatMessage.receiverId = receiverId
                        if (preferanceManeger!!.getString(Constants.KEY_USER_ID) == senderId) {
                            chatMessage.conversationImage =
                                documentChange.document.getString(Constants.KEY_RECEIVER_IMAGE)
                            chatMessage.conversationName =
                                documentChange.document.getString(Constants.KEY_RECEIVER_NAME)
                            chatMessage.conversationId =
                                documentChange.document.getString(Constants.KEY_RECEIVER_ID)
                        } else {
                            chatMessage.conversationImage =
                                documentChange.document.getString(Constants.KEY_SENDER_IMAGE)
                            chatMessage.conversationName =
                                documentChange.document.getString(Constants.KEY_SENDER_NAME)
                            chatMessage.conversationId =
                                documentChange.document.getString(Constants.KEY_SENDER_ID)
                        }
                        chatMessage.message =
                            documentChange.document.getString(Constants.KEY_LAST_MESSAGE)
                        chatMessage.dateObject =
                            documentChange.document.getDate(Constants.KEY_TIMESTAMP)
                        conversations!!.add(chatMessage)
                    } else if (documentChange.type == DocumentChange.Type.MODIFIED) {
                        var i = 0
                        while (i < conversations!!.size) {
                            val senderId =
                                documentChange.document.getString(Constants.KEY_SENDER_ID)
                            val receiverId =
                                documentChange.document.getString(Constants.KEY_RECEIVER_ID)
                            if (conversations!![i].senderId == senderId && conversations!![i].receiverId == receiverId) {
                                conversations!![i].message =
                                    documentChange.document.getString(Constants.KEY_LAST_MESSAGE)
                                conversations!![i].dateObject =
                                    documentChange.document.getDate(Constants.KEY_TIMESTAMP)
                                break
                            }
                            i++
                        }
                    }
                }
                conversations!!.sortWith(Comparator { obj1: ChatMessage, obj2: ChatMessage ->
                    obj2.dateObject!!.compareTo(
                        obj1.dateObject
                    )
                })
                conversationsAdapter!!.notifyDataSetChanged()
                binding!!.conversationsRecyclerView.smoothScrollToPosition(0)
                binding!!.conversationsRecyclerView.visibility = View.VISIBLE
                binding!!.progressBar.visibility = View.GONE
            }
        }
    private val token: Unit
        private get() {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token: String ->
                updateToken(
                    token
                )
            }
        }

    private fun loadUserDetails() {
        binding!!.textName.text =
        preferanceManeger?.getString(Constants.KEY_NAME)
        val bytes =
            Base64.decode(preferanceManeger?.getString(Constants.KEY_IMAGE) ?: "no ", Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        binding!!.imageProfile.setImageBitmap(bitmap)
    }

    private fun signOut() {
        showToast("Signing out...")
        val database = FirebaseFirestore.getInstance()
        val documentReference = database.collection(Constants.KEY_COLLECTION_USERS).document(
            preferanceManeger!!.getString(Constants.KEY_USER_ID).toString()
        )
        val updates = HashMap<String, Any>()
        updates[Constants.KEY_FCM_TOKEN] = FieldValue.delete()
        documentReference.update(updates)
            .addOnSuccessListener { unused: Void? ->
                preferanceManeger!!.clear()
                startActivity(Intent(applicationContext, signInActivity::class.java))
                finish()
            }
            .addOnFailureListener { e: Exception? -> showToast("Unable to sign out") }
    }

    override fun onConversionClicked(user: user?) {
        val intent = Intent(applicationContext, ChatActivity::class.java)
        intent.putExtra(Constants.KEY_USER, user)
        startActivity(intent)
    }
}