package com.alisemiz.akilli_kampus_uygulamasi.data.model

import com.google.firebase.Timestamp

data class Incident(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: String = "",
    val status: String = "Açık",
    val imageUrl: String? = null,
    val userId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Timestamp? = null,
    val followers: List<String> = emptyList() //
)