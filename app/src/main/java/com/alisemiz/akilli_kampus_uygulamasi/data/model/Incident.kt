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
    val userId: String = "",
    val timestamp: Timestamp? = null,
    // Takipçilerin ID listesi
    // Eğer veritabanında bu alan yoksa boş liste olarak gelir, çökme yapmaz.
    val followers: List<String> = emptyList()
)