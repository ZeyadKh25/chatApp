package com.example.kotlinnchappapp.Adapters

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlinnchappapp.Adapters.ResentConversationAdapter.ConversionViewHolder
import com.example.kotlinnchappapp.databinding.ItemContanerRecentConvertionrBinding
import com.example.kotlinnchappapp.listeners.ConversationLesteners
import com.example.kotlinnchappapp.models.ChatMessage
import com.example.kotlinnchappapp.models.user

class ResentConversationAdapter(
    private val chatMessages: List<ChatMessage>,
    private val conversionListener: ConversationLesteners
) : RecyclerView.Adapter<ConversionViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversionViewHolder {
        return ConversionViewHolder(
            ItemContanerRecentConvertionrBinding.inflate(
                LayoutInflater.from(parent.context), parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ConversionViewHolder, position: Int) {
        holder.setData(chatMessages[position])
    }

    override fun getItemCount(): Int {
        return chatMessages.size
    }

    inner class ConversionViewHolder(var binding: ItemContanerRecentConvertionrBinding) :
        RecyclerView.ViewHolder(
            binding.root
        ) {
        fun setData(chatMessage: ChatMessage) {
            binding.imageProfile.setImageBitmap(chatMessage.conversationImage?.let {
                getConversionImage(
                    it
                )
            })
            binding.textName.text = chatMessage.conversationName
            binding.textRecentMessage.text = chatMessage.message
            binding.textlastMsTime.text = chatMessage.dateTime?.substring(5) ?: "2:04 pm"
            binding.root.setOnClickListener {
                val user = user()
                user.id = chatMessage.conversationId
                user.name = chatMessage.conversationName
                user.image = chatMessage.conversationImage
                conversionListener.onConversionClicked(user)
            }
        }

        private fun getConversionImage(encodedImage: String): Bitmap {
            val bytes = Base64.decode(encodedImage, Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }
}