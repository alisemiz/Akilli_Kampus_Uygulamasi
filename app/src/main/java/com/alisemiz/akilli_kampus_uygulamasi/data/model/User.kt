package com.alisemiz.akilli_kampus_uygulamasi.data.model

data class User(
    val uid: String = "",         // Kullanıcının benzersiz ID'si
    val name: String = "",        // Ad Soyad
    val email: String = "",       // E-posta
    val department: String = "",  // Birim bilgisi
    val role: String = "user"     // Varsayılan rol: User
)