package com.example.beefound.api

data class User(
    val id: Int,
    val username: String,
    val email: String,
    val phone: String,
    val user_role: String,
)

data class Hive(
    val id: Int,
    val created : String,
    val updated : String,
    val longitude : String,
    val latitude : String,
    val userid : Int,
    val type : String,
    var name :String = "",
    val email: String,
)
