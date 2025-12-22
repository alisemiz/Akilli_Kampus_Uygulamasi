package com.alisemiz.akilli_kampus_uygulamasi.data.model

import com.google.firebase.Timestamp

data class Incident(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: String = "",
    val status: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val creatorUid: String = "",
    val timestamp: Timestamp? = null,
    val imageUrl: String = "",
    val followers: List<String> = emptyList()
)