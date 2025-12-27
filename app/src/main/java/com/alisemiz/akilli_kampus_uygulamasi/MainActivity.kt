package com.alisemiz.akilli_kampus_uygulamasi

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
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

        // 1. Önce binding ve tasarımı yükle (Çökme hatasını önlemek için ilk bu yapılmalı)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. Tam ekran kodunu tasarım yüklendikten SONRA çalıştır (Safe Immersive Mode)
        window.decorView.post {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.let { controller ->
                    controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        )
            }
        }

        // Navigasyon ayarları
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigationView.setupWithNavController(navController)

        // Menü görünürlük ayarları
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment,
                R.id.mapFragment,
                R.id.profileFragment,
                R.id.notificationsFragment -> {
                    binding.bottomNavigationView.visibility = View.VISIBLE
                }
                else -> {
                    binding.bottomNavigationView.visibility = View.GONE
                }
            }
        }

        createNotificationChannel()

        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                checkRoleAndAdjustMenu(user.uid)
                baslatBildirimTakibi()
            } else {
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

    private fun checkRoleAndAdjustMenu(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val menu = binding.bottomNavigationView.menu
                    menu.findItem(R.id.notificationsFragment)?.isVisible = true
                }
            }
    }

    override fun onResume() {
        super.onResume()
        if (auth.currentUser != null) {
            baslatBildirimTakibi()
        }
    }

    private fun baslatBildirimTakibi() {
        val uid = auth.currentUser?.uid ?: return
        val sharedPref = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        if (!sharedPref.getBoolean("notifications_enabled", true)) return

        followListener?.remove()
        emergencyListener?.remove()

        followListener = db.collection("incidents")
            .whereArrayContains("followers", uid)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                for (doc in snapshots!!.documentChanges) {
                    if (doc.type == DocumentChange.Type.MODIFIED) {
                        val title = doc.document.getString("title") ?: "Olay"
                        val newStatus = doc.document.getString("status") ?: "Güncellendi"
                        bildirimGonder("Durum Güncellemesi", "$title durumu '$newStatus' oldu.")
                    }
                }
            }

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
                            bildirimGonder("⚠️ $title", desc)
                        }
                    }
                }
            }
    }

    private fun bildirimGonder(baslik: String, icerik: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val builder = NotificationCompat.Builder(this, "CHANNEL_ID")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
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