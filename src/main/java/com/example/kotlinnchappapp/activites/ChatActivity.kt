package com.example.kotlinnchappapp.activites

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import com.example.kotlinnchappapp.Adapters.ChatAdapter
import com.example.kotlinnchappapp.databinding.ActivityChatBinding
import com.example.kotlinnchappapp.models.ChatMessage
import com.example.kotlinnchappapp.models.user
import com.example.kotlinnchappapp.utilites.Constants
import com.example.kotlinnchappapp.utilites.PreferanceManeger
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : BaseActivity() {

    private var binding: ActivityChatBinding? = null
    private var receiverUser: user? = null
    private var chatMessages: MutableList<ChatMessage>? = null
    private var chatAdapter: ChatAdapter? = null
    private var preferanceManeger: PreferanceManeger? = null
    private var database: FirebaseFirestore? = null
    private var conversationId: String? = null
    private var isReceiverAvailable = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        loadReceiverDetails()
        setListeners()
        init()
        listenMessages()
    }

    private fun init() {
        preferanceManeger = PreferanceManeger(applicationContext)
        chatMessages = ArrayList()
        chatAdapter = ChatAdapter(
            chatMessages as ArrayList<ChatMessage>,
            getBitmapFromEncodedString(receiverUser!!.image),
            preferanceManeger!!.getString(Constants.KEY_USER_ID)!!
        )
        binding!!.chatRecyclerView.adapter = chatAdapter
        database = FirebaseFirestore.getInstance()
    }

    private fun listenAvailabilityOfReceiver() {
        database!!.collection(Constants.KEY_COLLECTION_USERS).document(
            receiverUser!!.id!!
        )
            .addSnapshotListener(this@ChatActivity) { value: DocumentSnapshot?, error: FirebaseFirestoreException? ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (value != null) {
                    if (value.getLong(Constants.KEY_AVAILABILITY) != null) {
                        val availability = Objects.requireNonNull(
                            value.getLong(Constants.KEY_AVAILABILITY)
                        )!!.toInt()
                        isReceiverAvailable = availability == 1
                    }
                    receiverUser!!.token = value.getString(Constants.KEY_FCM_TOKEN)
                }
                if (isReceiverAvailable) {
                    binding!!.textAvailability.visibility = View.VISIBLE
                } else {
                    binding!!.textAvailability.visibility = View.GONE
                }
            }
    }

    private fun listenMessages() {
        database!!.collection(Constants.KEY_COLLECTION_CHAT)
            .whereEqualTo(
                Constants.KEY_SENDER_ID,
                preferanceManeger!!.getString(Constants.KEY_USER_ID)
            )
            .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverUser!!.id)
            .addSnapshotListener(eventListener)
        database!!.collection(Constants.KEY_COLLECTION_CHAT)
            .whereEqualTo(Constants.KEY_SENDER_ID, receiverUser!!.id)
            .whereEqualTo(
                Constants.KEY_RECEIVER_ID,
                preferanceManeger!!.getString(Constants.KEY_USER_ID)
            ).addSnapshotListener(eventListener)
    }

    private val eventListener =
        label@ EventListener { value: QuerySnapshot?, error: FirebaseFirestoreException? ->
            if (error != null) {
                return@EventListener
            }
            if (value != null) {
                val count = chatMessages!!.size
                for (documentChange in value.documentChanges) {
                    if (documentChange.type == DocumentChange.Type.ADDED) {
                        val chatMessage = ChatMessage()
                        chatMessage.senderId =
                            documentChange.document.getString(Constants.KEY_SENDER_ID)
                        chatMessage.receiverId =
                            documentChange.document.getString(Constants.KEY_RECEIVER_ID)
                        chatMessage.message =
                            documentChange.document.getString(Constants.KEY_MESSAGE)
                        chatMessage.dateTime =
                            getReadableDateTime(documentChange.document.getDate(Constants.KEY_TIMESTAMP))
                        chatMessage.dateObject =
                            documentChange.document.getDate(Constants.KEY_TIMESTAMP)
                        chatMessages!!.add(chatMessage)
                    }
                }
                Collections.sort(chatMessages) { obj1: ChatMessage, obj2: ChatMessage ->
                    obj1.dateObject!!.compareTo(
                        obj2.dateObject
                    )
                }
                if (count == 0) {
                    chatAdapter!!.notifyDataSetChanged()
                } else {
                    chatAdapter!!.notifyItemRangeInserted(chatMessages!!.size, chatMessages!!.size)
                    binding!!.chatRecyclerView.smoothScrollToPosition(chatMessages!!.size - 1)
                }
                binding!!.chatRecyclerView.visibility = View.VISIBLE
            }
            binding!!.progressBar.visibility = View.GONE
            if (conversationId == null) {
                checkForConversion()
            }
        }

    private fun sendMessage() {
        val message = HashMap<String, Any?>()
        message[Constants.KEY_RECEIVER_ID] = receiverUser!!.id
        message[Constants.KEY_TIMESTAMP] = Date()
        message[Constants.KEY_SENDER_ID] =
            preferanceManeger!!.getString(Constants.KEY_USER_ID)
        message[Constants.KEY_MESSAGE] = binding!!.inputMessage.text.toString()
        database!!.collection(Constants.KEY_COLLECTION_CHAT).add(message)
        if (conversationId != null) {
            updateConversion(binding!!.inputMessage.text.toString())
        } else {
            val conversion = HashMap<String, Any?>()
            conversion[Constants.KEY_SENDER_ID] =
                preferanceManeger!!.getString(Constants.KEY_USER_ID)
            conversion[Constants.KEY_SENDER_IMAGE] =
                preferanceManeger!!.getString(Constants.KEY_IMAGE)
            conversion[Constants.KEY_RECEIVER_ID] = receiverUser!!.id
            conversion[Constants.KEY_RECEIVER_IMAGE] = receiverUser!!.image
            conversion[Constants.KEY_LAST_MESSAGE] =
                binding!!.inputMessage.text.toString()
            conversion[Constants.KEY_SENDER_NAME] =
                preferanceManeger!!.getString(Constants.KEY_NAME)
            conversion[Constants.KEY_RECEIVER_NAME] = receiverUser!!.name
            conversion[Constants.KEY_TIMESTAMP] = Date()
            addConversion(conversion)
        }
        binding!!.inputMessage.text = null
    }

    private fun getBitmapFromEncodedString(encodedImage: String?): Bitmap {
        val bytes = Base64.decode(encodedImage, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun loadReceiverDetails() {
        receiverUser = intent.getSerializableExtra(Constants.KEY_USER) as user?
        binding!!.textName.text = receiverUser!!.name
    }

    private fun setListeners() {
        binding!!.imageBack.setOnClickListener { v: View? -> onBackPressed() }
        binding!!.layoutSend.setOnClickListener { v: View? -> sendMessage() }
    }

    private fun getReadableDateTime(date: Date?): String {
        return SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date)
    }

    private fun addConversion(conversion: HashMap<String, Any?>) {
        database!!.collection(Constants.KEY_COLLECTION_CONVERSATIONS).add(conversion)
            .addOnSuccessListener { documentReference: DocumentReference ->
                conversationId = documentReference.id
            }
    }

    private fun updateConversion(message: String) {
        val documentReference =
            database!!.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(
                conversationId!!
            )
        documentReference.update(
            Constants.KEY_LAST_MESSAGE, message,
            Constants.KEY_TIMESTAMP, Date()
        )
    }

    private fun checkForConversion() {
        if (chatMessages!!.size != 0) {
            checkForConversionRemotely(
                preferanceManeger!!.getString(Constants.KEY_USER_ID),
                receiverUser!!.id
            )
            checkForConversionRemotely(
                receiverUser!!.id,
                preferanceManeger!!.getString(Constants.KEY_USER_ID)
            )
        }
    }

    private fun checkForConversionRemotely(senderId: String?, receiverId: String?) {
        database!!.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
            .whereEqualTo(Constants.KEY_SENDER_ID, senderId)
            .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverId)
            .get()
            .addOnCompleteListener(conversionOnCompleteListener)
    }

    private val conversionOnCompleteListener = OnCompleteListener { task: Task<QuerySnapshot?> ->
        if (task.isSuccessful && task.result != null && task.result!!
                .documents.size > 0
        ) {
            val documentSnapshot = task.result!!.documents[0]
            conversationId = documentSnapshot.id
        }
    }

    override fun onResume() {
        super.onResume()
        listenAvailabilityOfReceiver()
    }
}