package com.alisemiz.akilli_kampus_uygulamasi

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.alisemiz.akilli_kampus_uygulamasi.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // İki farklı dinleyiciye ihtiyacımız var:
    private var followListener: ListenerRegistration? = null    // Takip ettiklerim için
    private var emergencyListener: ListenerRegistration? = null // Genel acil durumlar için

    // Uygulama açılış zamanını tutuyoruz (Eski acil durumlar bildirim atmasın diye)
    private val appStartTime = Date()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        val navController = navHostFragment.navController

        // Alt menüyü navigasyona bağla
        binding.bottomNavigationView.setupWithNavController(navController)

        // Giriş ve Kayıt ekranlarında alt barı gizle
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment, R.id.registerFragment -> {
                    binding.bottomNavigationView.visibility = View.GONE
                }
                else -> {
                    binding.bottomNavigationView.visibility = View.VISIBLE
                }
            }
        }

        createNotificationChannel()
    }

    override fun onResume() {
        super.onResume()
        baslatBildirimTakibi()
    }

    override fun onPause() {
        super.onPause()
        // Uygulama arka plana atıldığında dinlemeyi durduruyoruz
        // (Pil tasarrufu için. Gerçek zamanlı istersen burayı silebilirsin.)
        followListener?.remove()
        emergencyListener?.remove()
    }

    private fun baslatBildirimTakibi() {
        val uid = auth.currentUser?.uid ?: return
        val sharedPref = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        // Kullanıcı ayarlardan bildirimleri kapattıysa çık
        if (!sharedPref.getBoolean("notifications_enabled", true)) {
            return
        }

        // Önceki dinleyicileri temizle
        followListener?.remove()
        emergencyListener?.remove()

        // 1. DİNLEYİCİ: Takip Ettiğim Olayların Durum Değişikliği
        followListener = db.collection("incidents")
            .whereArrayContains("followers", uid)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                for (doc in snapshots!!.documentChanges) {
                    // Sadece durum DEĞİŞTİĞİNDE (Modified) bildirim at
                    if (doc.type == DocumentChange.Type.MODIFIED) {
                        val title = doc.document.getString("title") ?: "Olay"
                        val newStatus = doc.document.getString("status") ?: "Güncellendi"

                        bildirimGonder("Durum Güncellemesi", "$title durumu '$newStatus' oldu.")
                    }
                }
            }

        // 2. DİNLEYİCİ: Genel ACİL Durum Bildirimleri (Rubrik 8. Madde)
        emergencyListener = db.collection("incidents")
            .whereEqualTo("status", "ACİL") // Sadece ACİL olanları dinle
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                for (doc in snapshots!!.documentChanges) {
                    // Yeni bir acil durum EKLENDİĞİNDE (Added)
                    if (doc.type == DocumentChange.Type.ADDED) {
                        val timestamp = doc.document.getTimestamp("timestamp")?.toDate()

                        // Sadece uygulama açıldıktan SONRA eklenenleri bildir
                        // (Yoksa geçmişteki tüm acil durumlar her açılışta öter)
                        if (timestamp != null && timestamp.after(appStartTime)) {
                            val title = doc.document.getString("title") ?: "ACİL DURUM"
                            val desc = doc.document.getString("description") ?: "Kampüste acil durum!"

                            bildirimGonder("⚠️ $title", desc)
                        }
                    }
                }
            }
    }

    private fun bildirimGonder(baslik: String, icerik: String) {
        // Android 13+ İzin Kontrolü
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val builder = NotificationCompat.Builder(this, "CHANNEL_ID")
            .setSmallIcon(android.R.drawable.stat_sys_warning) // İkonu uyarı ikonu yaptık
            .setContentTitle(baslik)
            .setContentText(icerik)
            .setPriority(NotificationCompat.PRIORITY_MAX) // Acil durum için en yüksek öncelik
            .setVibrate(longArrayOf(0, 500, 200, 500)) // Titreşim ekledik
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Olay Bildirimleri"
            val descriptionText = "Acil durum ve takip bildirimleri"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("CHANNEL_ID", name, importance).apply {
                description = descriptionText
                enableVibration(true) // Kanalda titreşimi açtık
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}