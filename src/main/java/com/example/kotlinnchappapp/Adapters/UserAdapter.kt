package com.example.kotlinnchappapp.Adapters

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlinnchappapp.Adapters.userAdapter.UserViewHolder
import com.example.kotlinnchappapp.databinding.ItemContanerUserBinding
import com.example.kotlinnchappapp.listeners.UserLesteners
import com.example.kotlinnchappapp.models.user

class userAdapter(private val users: List<user>, private val userLesteners: UserLesteners) :
    RecyclerView.Adapter<UserViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val itemContainerUserBinding =
            ItemContanerUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(itemContainerUserBinding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.setUserData(users[position])
    }

    override fun getItemCount(): Int {
        return users.size
    }

    inner class UserViewHolder(var binding: ItemContanerUserBinding) :
        RecyclerView.ViewHolder(
            binding.root
        ) {
        fun setUserData(user: user) {
            binding.textName.text = user.name
            binding.textEmail.text = user.email
            binding.imageProfile.setImageBitmap(user.image?.let { getUserImage(it) })
            binding.root.setOnClickListener { v: View? -> userLesteners.onUserClicked(user) }
        }

        private fun getUserImage(encodedImage: String): Bitmap {
            val bytes = Base64.decode(encodedImage, Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }
}