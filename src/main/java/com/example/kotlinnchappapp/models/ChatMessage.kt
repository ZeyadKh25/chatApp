package com.example.kotlinnchappapp.models

import java.util.*

class ChatMessage {
    @JvmField
    var senderId: String? = null
    @JvmField
    var receiverId: String? = null
    @JvmField
    var message: String? = null
    @JvmField
    var dateTime: String? = null
    @JvmField
    var dateObject: Date? = null
    var conversationId: String? = null
    var conversationName: String? = null
    var conversationImage: String? = null
}