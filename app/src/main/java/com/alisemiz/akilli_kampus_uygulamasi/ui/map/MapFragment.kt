package com.alisemiz.akilli_kampus_uygulamasi.ui.map

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
// YENİ EKLENDİ: Navigasyon için gerekli kütüphane
import androidx.navigation.fragment.findNavController
import com.alisemiz.akilli_kampus_uygulamasi.R
import com.google.firebase.firestore.FirebaseFirestore
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapFragment : Fragment() {

    private lateinit var map: MapView
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // OSM Konfigürasyonunu yükle (Burası önemli, haritanın önbellek yapması için)
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))

        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Haritayı Başlat
        map = view.findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK) // Standart harita görünümü
        map.setMultiTouchControls(true) // İki parmakla zoom yapabilsin

        // Başlangıç noktası (Ankara civarı, kendi okuluna göre değiştirebilirsin)
        val startPoint = GeoPoint(39.92077, 32.85411)
        map.controller.setZoom(15.0)
        map.controller.setCenter(startPoint)

        firestore = FirebaseFirestore.getInstance()
        verileriCekVeIsaretle()
    }

    private fun verileriCekVeIsaretle() {
        firestore.collection("incidents").get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val lat = document.getDouble("latitude")
                    val lng = document.getDouble("longitude")
                    val title = document.getString("title") ?: "Olay"
                    // Açıklama yerine kullanıcıyı yönlendiren bir mesaj yazdık
                    val description = "Detayı görmek için tekrar tıklayın"
                    val type = document.getString("type") ?: "Genel"

                    if (lat != null && lng != null) {
                        // OSM için Konum Noktası
                        val point = GeoPoint(lat, lng)

                        // Marker (Pin) Oluştur
                        val marker = Marker(map)
                        marker.position = point
                        marker.title = title
                        marker.snippet = description
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                        // --- YENİ EKLENEN KISIM: TIKLAMA MANTIĞI ---
                        marker.setOnMarkerClickListener { m, mapView ->
                            if (m.isInfoWindowShown) {
                                // Baloncuk zaten açıksa, kullanıcı DETAY'a gitmek istiyordur.
                                val bundle = Bundle().apply {
                                    putString("incidentId", document.id) // ID'yi paketle
                                }
                                // Detay sayfasına git
                                findNavController().navigate(R.id.incidentDetailFragment, bundle)
                            } else {
                                // Kapalıysa önce bilgi baloncuğunu aç
                                m.showInfoWindow()
                            }
                            true // Tıklama olayını tükettik (sistem başka bir şey yapmasın)
                        }
                        // ---------------------------------------------

                        // Haritaya ekle
                        map.overlays.add(marker)
                    }
                }
                // Haritayı yenile ki pinler gözüksün
                map.invalidate()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Veriler yüklenemedi", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        map.onResume() // Harita döngüsünü başlat
    }

    override fun onPause() {
        super.onPause()
        map.onPause() // Haritayı durdur (pil tasarrufu)
    }
}