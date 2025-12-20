package com.alisemiz.akilli_kampus_uygulamasi.ui.map

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.alisemiz.akilli_kampus_uygulamasi.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Date
import java.util.concurrent.TimeUnit

class MapFragment : Fragment() {

    private lateinit var map: MapView
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // OSM Konfigürasyonunu yükle (Harita önbelleği için gerekli)
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Harita Ayarları
        map = view.findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true) // İki parmakla zoom açıldı

        // Başlangıç noktası
        val startPoint = GeoPoint(39.92077, 32.85411)
        map.controller.setZoom(15.0)
        map.controller.setCenter(startPoint)

        firestore = FirebaseFirestore.getInstance()

        // Verileri veritabanından çek ve haritaya diz
        verileriCekVeIsaretle()
    }

    private fun verileriCekVeIsaretle() {
        firestore.collection("incidents").get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    // Verileri güvenli şekilde al
                    val lat = document.getDouble("latitude")
                    val lng = document.getDouble("longitude")
                    val title = document.getString("title") ?: "Olay"
                    val type = document.getString("type") ?: "Genel"
                    val timestamp = document.getTimestamp("timestamp")

                    if (lat != null && lng != null) {
                        val point = GeoPoint(lat, lng)

                        // Marker (Pin) Oluştur
                        val marker = Marker(map)
                        marker.position = point



                        // 1. Başlık
                        marker.title = title

                        // 2. Zaman Bilgisi ("Ne kadar önce?" formatında)
                        val timeAgo = zamanFarkiHesapla(timestamp)

                        // 3. Bilgi Kartı İçeriği (Snippet)
                        // Kartta Tür, Zaman ve Yönlendirme mesajı olacak
                        marker.snippet = "Tür: $type\n$timeAgo\n(Detay için tekrar tıkla)"

                        // 4. Renkli İkonlar (Türe göre farklılaşan pinler)
                        val iconDrawable = getIconForType(requireContext(), type)
                        if (iconDrawable != null) {
                            marker.icon = iconDrawable
                        }

                        // Baloncuğun konumu
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                        // 5. Tıklama ve Yönlendirme Mantığı
                        marker.setOnMarkerClickListener { m, mapView ->
                            if (m.isInfoWindowShown) {
                                // Eğer baloncuk zaten açıksa, kullanıcı DETAYA gitmek istiyordur.
                                // "Detayı Gör" işlevi burada çalışır.
                                val bundle = Bundle().apply {
                                    putString("incidentId", document.id)
                                }
                                findNavController().navigate(R.id.incidentDetailFragment, bundle)
                            } else {
                                // Baloncuk kapalıysa aç
                                m.showInfoWindow()
                            }
                            true
                        }

                        // Pini haritaya ekle
                        map.overlays.add(marker)
                    }
                }
                // Haritayı yenile
                map.invalidate()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Veriler yüklenemedi", Toast.LENGTH_SHORT).show()
            }
    }

    // Tura Göre Renkli İkon Seçimi
    private fun getIconForType(context: Context, type: String): Drawable? {
        val iconRes = when (type) {
            "Yangın" -> android.R.drawable.ic_dialog_alert // Kırmızı Ünlem
            "Sağlık" -> android.R.drawable.ic_menu_add    // Artı İşareti
            "Güvenlik" -> android.R.drawable.ic_lock_lock // Kilit
            "Teknik" -> android.R.drawable.ic_menu_manage // Tamir Aleti
            else -> android.R.drawable.ic_dialog_info     // Bilgi İkonu
        }
        val drawable = ContextCompat.getDrawable(context, iconRes)

        // İkonu boya (Tint)
        drawable?.setTint(
            when (type) {
                "Yangın" -> ContextCompat.getColor(context, android.R.color.holo_red_dark)
                "Sağlık" -> ContextCompat.getColor(context, android.R.color.holo_green_dark)
                "Güvenlik" -> ContextCompat.getColor(context, android.R.color.holo_blue_dark)
                "Teknik" -> ContextCompat.getColor(context, android.R.color.holo_orange_dark)
                else -> ContextCompat.getColor(context, android.R.color.black)
            }
        )
        return drawable
    }

    //  Zaman Farkı Hesaplama
    private fun zamanFarkiHesapla(timestamp: Timestamp?): String {
        if (timestamp == null) return ""

        val now = Date().time
        val time = timestamp.toDate().time
        val diff = now - time

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            minutes < 1 -> "Az önce"
            minutes < 60 -> "$minutes dk önce"
            hours < 24 -> "$hours saat önce"
            else -> "$days gün önce"
        }
    }

    // Lifecycle (Yaşam Döngüsü) Yönetimi
    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}