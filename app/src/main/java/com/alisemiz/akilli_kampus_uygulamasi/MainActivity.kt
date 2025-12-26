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

    // Bildirim Dinleyicileri
    private var followListener: ListenerRegistration? = null
    private var emergencyListener: ListenerRegistration? = null
    private val appStartTime = Date()

    // Auth Durum Dinleyicisi
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        val navController = navHostFragment.navController

        // Alt menüyü bağla
        binding.bottomNavigationView.setupWithNavController(navController)

        // HANGİ SAYFALARDA MENÜ GÖRÜNSÜN/GİZLENSİN?
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                // Menünün GÖRÜNECEĞİ sayfalar (Ana Sekmeler)
                R.id.homeFragment,
                R.id.mapFragment,
                R.id.profileFragment,
                R.id.notificationsFragment -> { // Takip sekmesi burada
                    binding.bottomNavigationView.visibility = View.VISIBLE
                }

                // Diğer tüm sayfalarda (Login, Register, Detay, Ekleme vb.) GİZLE
                else -> {
                    binding.bottomNavigationView.visibility = View.GONE
                }
            }
        }

        // Bildirim Kanalını Oluştur
        createNotificationChannel()

        // Auth Listener: Kullanıcı giriş/çıkışını dinle
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // Kullanıcı giriş yaptıysa menüyü ayarla ve bildirimleri aç
                checkRoleAndAdjustMenu(user.uid)
                baslatBildirimTakibi()
            } else {
                // Çıkış yaptıysa dinleyicileri temizle
                followListener?.remove()
                emergencyListener?.remove()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(authStateListener)
    }

    // --- MENÜ AYARLARI ---
    private fun checkRoleAndAdjustMenu(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // DÜZELTME: Artık Admin dahil herkes Takip sekmesini görebilecek.
                    // Eski kodda admin için gizliyorduk, şimdi herkese görünür yapıyoruz.
                    val menu = binding.bottomNavigationView.menu
                    menu.findItem(R.id.notificationsFragment)?.isVisible = true

                    // İleride Admin'e özel başka bir buton gizlemek istersen burayı kullanabilirsin.
                }
            }
    }

    override fun onResume() {
        super.onResume()
        if (auth.currentUser != null) {
            baslatBildirimTakibi()
        }
    }

    override fun onPause() {
        super.onPause()
    }

    private fun baslatBildirimTakibi() {
        val uid = auth.currentUser?.uid ?: return
        val sharedPref = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        if (!sharedPref.getBoolean("notifications_enabled", true)) return

        followListener?.remove()
        emergencyListener?.remove()

        // 1. Takip Edilen Olayların Durum Güncellemeleri
        followListener = db.collection("incidents")
            .whereArrayContains("followers", uid)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                for (doc in snapshots!!.documentChanges) {
                    if (doc.type == DocumentChange.Type.MODIFIED) {
                        val title = doc.document.getString("title") ?: "Olay"
                        val newStatus = doc.document.getString("status") ?: "Güncellendi"
                        // Sadece durum değiştiyse bildirim atabiliriz veya her güncellemede
                        bildirimGonder("Durum Güncellemesi", "$title durumu '$newStatus' oldu.")
                    }
                }
            }

        // 2. Yeni Gelen ACİL Durumlar
        emergencyListener = db.collection("incidents")
            .whereEqualTo("status", "ACİL")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                for (doc in snapshots!!.documentChanges) {
                    if (doc.type == DocumentChange.Type.ADDED) {
                        val timestamp = doc.document.getTimestamp("timestamp")?.toDate()
                        // Sadece uygulama açıldıktan SONRA gelenleri bildir (Eskileri bildirme)
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
        // Android 13+ (Tiramisu) için İzin Kontrolü
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // İzin yoksa gönderme (İzin isteme işlemi ayrı yapılmalı)
                return
            }
        }

        val builder = NotificationCompat.Builder(this, "CHANNEL_ID")
            .setSmallIcon(android.R.drawable.stat_sys_warning) // İkonu değiştirebilirsin
            .setContentTitle(baslik)
            .setContentText(icerik)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("CHANNEL_ID", "Olay Bildirimleri", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Acil durum ve takip bildirimleri"
                enableVibration(true)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}