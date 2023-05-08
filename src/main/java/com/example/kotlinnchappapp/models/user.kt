package com.example.kotlinnchappapp.models

import java.io.Serializable

class user : Serializable {
    @JvmField
    var name: String? = null

    @JvmField
    var image: String? = null

    @JvmField
    var email: String? = null

    @JvmField
    var token: String? = null

    @JvmField
    var id: String? = null
    var type: String? = null

}