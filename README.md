#  Akıllı Kampüs ve Güvenlik Uygulaması

Bu proje, üniversite kampüslerinde güvenliği artırmak, teknik ve idari sorunları hızlıca raporlamak ve acil durumlarda tüm kullanıcılara anlık bildirim göndermek amacıyla geliştirilmiş **Kotlin** tabanlı bir Android uygulamasıdır.

##  Özellikler

###  Kullanıcı Özellikleri
* **Olay Bildirimi:** Kullanıcılar kampüs içindeki sorunları (Güvenlik, Temizlik, Teknik, Sağlık vb.) bildirebilir.
* **Fotoğraf Kanıtı:** Olay bildirirken **Kamera** ile anlık fotoğraf çekebilir veya **Galeriden** seçebilir.
* **Durum Takibi:** Bildirilen olayların durumu ("Beklemede", "İnceleniyor", "Çözüldü") takip edilebilir.
* **Kısıtlı Silme:** Kullanıcılar kendi bildirimlerini yalnızca **"Beklemede"** durumundayken silebilir. Yönetim incelemeye aldıktan sonra silme işlemi engellenir.
* **Bildirim Sistemi:** Takip edilen olayların durumu değiştiğinde bildirim alınır.
* **Filtreleme & Arama:** Olaylar kategorilere veya duruma göre filtrelenebilir.

### Yönetici (Admin) Özellikleri
* **Durum Güncelleme:** Olayların durumunu (Örn: İnceleniyor -> Çözüldü) değiştirebilir.
* ** Acil Durum Yayını:** Kampüs genelinde **Acil Durum (Emergency)** mesajı yayınlayabilir. Bu mesaj, uygulamayı kullanan herkese sesli ve titreşimli **yüksek öncelikli bildirim** olarak gider.
* **İçerik Yönetimi:** Uygunsuz içerikleri veya çözülen olayları yönetebilir.

## Kullanılan Teknolojiler

* **Dil:** Kotlin
* **Mimari:** Single Activity & Navigation Component
* **Backend & Veritabanı:** Firebase Firestore
* **Kimlik Doğrulama:** Firebase Authentication (Email/Password & Rol Yönetimi)
* **Depolama:** Firebase Storage (Olay fotoğrafları için)
* **Harita:** Google Maps SDK (Konum bazlı gösterim)
* **Bildirimler:** Android Notification Channel & Firebase Listeners
* **Görsel Yükleme:** Glide Library
