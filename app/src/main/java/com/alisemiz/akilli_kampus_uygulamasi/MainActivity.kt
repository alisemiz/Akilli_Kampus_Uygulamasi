package com.alisemiz.akilli_kampus_uygulamasi

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.alisemiz.akilli_kampus_uygulamasi.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Date

class MainActivity : AppCompatActivity() {

    // Görünüm bağlama ve veritabanı bağlantılarımızı en başta tanımlıyoruz.
    private lateinit var binding: ActivityMainBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Dinleyicileri (Listener) değişken olarak tutuyoruz ki uygulama kapanınca durdurabilelim.
    private var followListener: ListenerRegistration? = null
    private var emergencyListener: ListenerRegistration? = null

    private val appStartTime = Date()
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    // Android 13 ve üzeri için bildirim izni isteme mekanizması.
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Bildirim izni verilmedi.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Tam Ekran Ayarları
        //Durum çubuğu ve navigasyon tuşlarını gizledim.
        window.decorView.post {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.let { controller ->
                    controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        )
            }
        }

        // Bildirim iznini kontrol et ve gerekirse iste.
        askNotificationPermission()

        // Alt menüyü  navigasyon kontrolcüsüne bağlıyoruz.
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigationView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment, R.id.mapFragment, R.id.profileFragment -> {
                    binding.bottomNavigationView.visibility = View.VISIBLE
                }
                else -> binding.bottomNavigationView.visibility = View.GONE
            }
        }
        //Gerekli olan bildirim kanallarını oluşturuyoruz.
        createNotificationChannel()

        // Kullanıcı giriş yapmışsa bildirim takibini başlat,çıkış yapmışsa dinleyicileri kapat.
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                baslatBildirimTakibi()
            } else {
                durdurDinleyiciler()
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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

    override fun onResume() {
        super.onResume()
        if (auth.currentUser != null) baslatBildirimTakibi()
    }

    private fun baslatBildirimTakibi() {
        durdurDinleyiciler()
        val uid = auth.currentUser?.uid ?: return

        // 1. TAKİP BİLDİRİMLERİ: Sadece kullanıcının 'takip et' dediği olaylardaki değişiklikleri dinler.
        followListener = db.collection("incidents")
            .whereArrayContains("followers", uid)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                for (doc in snapshots!!.documentChanges) {
                    // Sadece var olan bir olay güncellendiğinde bildirim gönderiyoruz.
                    if (doc.type == DocumentChange.Type.MODIFIED) {
                        val title = doc.document.getString("title") ?: "Olay"
                        val newStatus = doc.document.getString("status") ?: "Güncellendi"

                        // False -> Normal Bildirim
                        bildirimGonder("Durum Güncellemesi", "$title durumu '$newStatus' oldu.", false)
                    }
                }
            }

        // 2. ACİL DURUMLAR: Takip etsin etmesin, tüm kullanıcılar için 'ACİL' olanları dinler.
        emergencyListener = db.collection("incidents")
            .whereEqualTo("status", "ACİL")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                for (doc in snapshots!!.documentChanges) {
                    if (doc.type == DocumentChange.Type.ADDED) {
                        val timestamp = doc.document.getTimestamp("timestamp")?.toDate()
                        if (timestamp != null && timestamp.after(appStartTime)) {
                            val title = doc.document.getString("title") ?: "ACİL DURUM"
                            val desc = doc.document.getString("description") ?: "Kampüste acil durum!"

                            // True -> Acil Bildirim
                            bildirimGonder("⚠️ $title", desc, true)
                        }
                    }
                }
            }
    }

    private fun durdurDinleyiciler() {
        followListener?.remove()
        emergencyListener?.remove()
    }

    // YEREL BİLDİRİM GÖNDERME FONKSİYONU
    private fun bildirimGonder(baslik: String, icerik: String, isEmergency: Boolean) {
        // Kullanıcının profil sayfasından yaptığı bildirim tercihlerini kontrol ediyoruz.
        val sharedPref = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val allowStatusUpdates = sharedPref.getBoolean("pref_status_updates", true)
        val allowEmergency = sharedPref.getBoolean("pref_emergency", true)

        // AYAR KONTROLÜ
        if (isEmergency) {
            if (!allowEmergency) return
        } else {
            if (!allowStatusUpdates) return
        }

        // İZİN KONTROLÜ
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        // KANAL AYARLARI (GÜNCELLENDİ: Kanal ID'si değiştiği için ayarlar sıfırlanacak ve sesli gelecek)
        val channelId = if (isEmergency) "EMERGENCY_CHANNEL" else "DEFAULT_CHANNEL_V2" // V2 ekledik ki ayar yenilensin

        // ÖNEMLİ: Normal bildirimleri de HIGH yaptık ki ekrana düşsün
        val priority = NotificationCompat.PRIORITY_MAX
        val vibratePattern = longArrayOf(0, 500, 200, 500)

        val icon = if (isEmergency) android.R.drawable.stat_sys_warning else android.R.drawable.ic_popup_reminder

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(icon)
            .setContentTitle(baslik)
            .setContentText(icerik)
            .setStyle(NotificationCompat.BigTextStyle().bigText(icerik))
            .setPriority(priority)
            .setVibrate(vibratePattern)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Acil Durum Kanalı
            val emergencyChannel = NotificationChannel("EMERGENCY_CHANNEL", "Acil Durumlar", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Yüksek öncelikli acil durum bildirimleri"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(emergencyChannel)

            // GÜNCELLENDİ: Normal Kanal (V2) - Artık bu da IMPORTANCE_HIGH (Sesli ve Pop-up)
            val defaultChannel = NotificationChannel("DEFAULT_CHANNEL_V2", "Takip Bildirimleri", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Olay durum güncellemeleri"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(defaultChannel)
        }
    }
}