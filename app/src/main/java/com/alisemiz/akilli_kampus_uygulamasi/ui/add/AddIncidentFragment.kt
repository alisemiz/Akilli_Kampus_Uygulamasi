package com.alisemiz.akilli_kampus_uygulamasi.ui.add

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.alisemiz.akilli_kampus_uygulamasi.databinding.FragmentAddIncidentBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.Date
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager

class AddIncidentFragment : Fragment() {

    private var _binding: FragmentAddIncidentBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    //Konum değişkenleri
    private lateinit var locationOverlay: MyLocationNewOverlay
    private var currentLat: Double = 39.9048 // Varsayılan Erzurum
    private var currentLng: Double = 41.2572

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //OSM Ayarları
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        _binding = FragmentAddIncidentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            setupMap()//İzin varsa haritayı kur
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
        }

        setupSpinner()
        binding.btnSubmit.setOnClickListener { gonder() }
    }

    private fun setupMap() {
        binding.mapAdd.setMultiTouchControls(true)
        val mapController = binding.mapAdd.controller
        mapController.setZoom(17.0)

        //Başlangıçta Erzurum kampüsüne odakla
        mapController.setCenter(GeoPoint(currentLat, currentLng))

        //Kendi Konumum Katmanı
        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), binding.mapAdd)
        locationOverlay.enableMyLocation()
        locationOverlay.enableFollowLocation() // Konum bulunduğunda haritayı oraya kaydır

        //Konum ilk bulunduğunda koordinatları güncelle
        locationOverlay.runOnFirstFix {
            val myLocation = locationOverlay.myLocation
            if (myLocation != null) {
                currentLat = myLocation.latitude
                currentLng = myLocation.longitude
            }
        }

        binding.mapAdd.overlays.add(locationOverlay)
    }

    private fun setupSpinner() {
        val types = arrayOf("Genel", "Yangın", "Sağlık", "Güvenlik", "Teknik")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, types)
        binding.spinnerType.adapter = adapter
    }

    private fun gonder() {
        val title = binding.etIncidentTitle.text.toString().trim()
        val desc = binding.etIncidentDesc.text.toString().trim()
        val type = binding.spinnerType.selectedItem.toString()

        if (title.isEmpty() || desc.isEmpty()) {
            Toast.makeText(context, "Lütfen başlık ve açıklama giriniz.", Toast.LENGTH_SHORT).show()
            return
        }

        //Firestore'a gidecek veri paketi
        val incidentData = hashMapOf(
            "title" to title,
            "description" to desc,
            "type" to type,
            "status" to "Açık",
            "timestamp" to Timestamp(Date()),
            "creatorUid" to (auth.currentUser?.uid ?: "Anonymous"),
            "latitude" to currentLat, //GPS'ten gelen gerçek koordinat
            "longitude" to currentLng, //GPS'ten gelen gerçek koordinat
            "followers" to emptyList<String>(),
            "imageUrl" to ""
        )

        binding.btnSubmit.isEnabled = false
        binding.btnSubmit.text = "Gönderiliyor..."

        db.collection("incidents")
            .add(incidentData)
            .addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(context, "Bildirim başarıyla gönderildi!", Toast.LENGTH_LONG).show()
                    findNavController().popBackStack()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnSubmit.isEnabled = true
                binding.btnSubmit.text = "BİLDİRİMİ GÖNDER"
            }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupMap() //İzin verildi,haritayı şimdi kur
        } else {
            Toast.makeText(context, "Konum izni olmadan konumunuz gözükmez.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapAdd.onResume()
        locationOverlay.enableMyLocation()
    }

    override fun onPause() {
        super.onPause()
        binding.mapAdd.onPause()
        locationOverlay.disableMyLocation()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}