package com.alisemiz.akilli_kampus_uygulamasi.data.model

import com.google.firebase.Timestamp

data class Incident(
    val id: String = "",                 // Olayın benzersiz doküman kimliği.
    val title: String = "",              // Bildirilen olayın kısa başlığı.
    val description: String = "",        // Olayın detaylı açıklama metni.
    val type: String = "",               // Kategori bilgisi (Yangın, Teknik vb.).
    val status: String = "Açık",         // Olayın güncel çözüm durumu.
    val imageUrl: String? = null,        // Varsa olaya ait fotoğrafın linki.
    val userId: String = "",             // Paylaşımı yapan kullanıcının ID'si.
    val latitude: Double = 0.0,          // Haritadaki konumun enlem bilgisi.
    val longitude: Double = 0.0,         // Haritadaki konumun boylam bilgisi.
    val timestamp: Timestamp? = null,    // Paylaşımın yapıldığı tarih ve saat.
    val followers: List<String> = emptyList() // Olayı takip edenlerin UID listesi.
)