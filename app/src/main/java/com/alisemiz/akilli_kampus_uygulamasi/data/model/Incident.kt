package com.alisemiz.akilli_kampus_uygulamasi.data.model

import com.google.firebase.Timestamp

data class Incident(
    val id: String = "",              // Her bildirimin benzersiz kimliği
    val title: String = "",           // Olay başlığı [cite: 36, 55, 66]
    val description: String = "",     // Olay açıklaması [cite: 36, 55, 67]
    val type: String = "",            // Tür (Güvenlik, Sağlık, Teknik vb.) [cite: 36, 40, 55, 65]
    val status: String = "Açık",      // Durum (Açık, İnceleniyor, Çözüldü) [cite: 36, 37, 56, 58]
    val timestamp: Timestamp? = null, // Oluşturulma zamanı (Sıralama için) [cite: 36, 38, 55]
    val imageUrl: String? = null,     // Olay fotoğrafı (Opsiyonel) [cite: 55, 69]
    val creatorUid: String = ""       // Olayı bildiren kullanıcının ID'si (User.kt ile bağlanacak)
)