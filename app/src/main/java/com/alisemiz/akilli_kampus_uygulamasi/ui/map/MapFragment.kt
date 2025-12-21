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
import com.alisemiz.akilli_kampus_uygulamasi.data.model.Incident
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
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        map = view.findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        // --- HEDEF GÜNCELLEMESİ: Erzurum Atatürk Üniversitesi Kampüsü ---
        val kampüsMerkezi = GeoPoint(39.9048, 41.2572)
        map.controller.setZoom(16.0) // Kampüs binalarını görmek için ideal seviye
        map.controller.setCenter(kampüsMerkezi)

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
                    val type = document.getString("type") ?: "Genel"
                    val timestamp = document.getTimestamp("timestamp")

                    if (lat != null && lng != null) {
                        val point = GeoPoint(lat, lng)
                        val marker = Marker(map)
                        marker.position = point
                        marker.title = title

                        val timeAgo = zamanFarkiHesapla(timestamp)
                        marker.snippet = "Tür: $type\n$timeAgo\n(Detay için tekrar tıkla)"

                        val iconDrawable = getIconForType(requireContext(), type)
                        if (iconDrawable != null) {
                            marker.icon = iconDrawable
                        }

                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                        marker.setOnMarkerClickListener { m, _ ->
                            if (m.isInfoWindowShown) {
                                val bundle = Bundle().apply {
                                    putString("incidentId", document.id)
                                }
                                findNavController().navigate(R.id.incidentDetailFragment, bundle)
                            } else {
                                m.showInfoWindow()
                            }
                            true
                        }
                        map.overlays.add(marker)
                    }
                }
                map.invalidate()
            }
            .addOnFailureListener {
                if (isAdded) {
                    Toast.makeText(context, "Veriler yüklenemedi", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun getIconForType(context: Context, type: String): Drawable? {
        val iconRes = when (type) {
            "Yangın" -> android.R.drawable.ic_dialog_alert
            "Sağlık" -> android.R.drawable.ic_menu_add
            "Güvenlik" -> android.R.drawable.ic_lock_lock
            "Teknik" -> android.R.drawable.ic_menu_manage
            else -> android.R.drawable.ic_dialog_info
        }
        val drawable = ContextCompat.getDrawable(context, iconRes)

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

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}