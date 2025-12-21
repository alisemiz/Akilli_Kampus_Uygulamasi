package com.alisemiz.akilli_kampus_uygulamasi

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View // Görünürlük ayarı için eklendi
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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var notificationListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        val navController = navHostFragment.navController

        // Alt menüyü navigasyona bağla
        binding.bottomNavigationView.setupWithNavController(navController)

        //Giriş ve Kayıt ekranlarında barı gizle
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                // nav_graph.xml dosyasındaki ID'lere göre kontrol yapıyoruz
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
        notificationListener?.remove()
    }

    private fun baslatBildirimTakibi() {
        val uid = auth.currentUser?.uid ?: return
        val sharedPref = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        if (!sharedPref.getBoolean("notifications_enabled", true)) {
            return
        }

        notificationListener?.remove()

        notificationListener = db.collection("incidents")
            .whereArrayContains("followers", uid)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                for (doc in snapshots!!.documentChanges) {
                    if (doc.type == DocumentChange.Type.MODIFIED) {
                        val title = doc.document.getString("title") ?: "Olay"
                        val newStatus = doc.document.getString("status") ?: "Güncellendi"

                        bildirimGonder("Durum Güncellemesi", "$title olayının durumu '$newStatus' oldu.")
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
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(baslik)
            .setContentText(icerik)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Olay Bildirimleri"
            val descriptionText = "Takip edilen olayların durum güncellemeleri"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("CHANNEL_ID", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}