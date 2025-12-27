package com.alisemiz.akilli_kampus_uygulamasi.ui.add

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.alisemiz.akilli_kampus_uygulamasi.databinding.FragmentAddIncidentBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Yeni Olay Bildirme Ekranı
 * Güncellemeler:
 * 1. Olayı ekleyen kişi otomatik olarak takipçi listesine (followers) eklenir.
 * 2. Harita üzerinde dokunarak konum değiştirme özelliği eklendi.
 */
class AddIncidentFragment : Fragment() {

    private var _binding: FragmentAddIncidentBinding? = null
    private val binding get() = _binding!!

    // Harita ve İşaretçi Tanımları
    private lateinit var map: MapView
    private var marker: Marker? = null

    // Konum verilerini çekmek için gerekli istemci
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLat: Double = 39.9048 // Varsayılan: Erzurum Atatürk Üni
    private var currentLng: Double = 41.2572

    private var secilenGorselUri: Uri? = null
    private var photoURI: Uri? = null

    // Firebase Servisleri
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // Galeriden resim seçme işlemi sonucu
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data != null && data.data != null) {
                secilenGorselUri = data.data
                binding.ivSelectedImage.setImageURI(secilenGorselUri)
                binding.ivSelectedImage.visibility = View.VISIBLE
            }
        }
    }

    // Kamera ile resim çekme işlemi sonucu
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoURI != null) {
            secilenGorselUri = photoURI
            binding.ivSelectedImage.setImageURI(photoURI)
            binding.ivSelectedImage.visibility = View.VISIBLE
        }
    }

    // İzinlerin (Kamera ve Konum) toplu kontrolü
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true) {
            kamerayiAc()
        }
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            cihazKonumunuAl()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // osmdroid harita ayarlarını yükle
        Configuration.getInstance().load(context, android.preference.PreferenceManager.getDefaultSharedPreferences(context))

        _binding = FragmentAddIncidentBinding.inflate(inflater, container, false)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // İlk başta haritayı hazırla
        haritayiHazirla()

        // Konum iznini kontrol et ve varsa konumu güncelle
        konumIzniKontrolEt()

        // Kategorileri spinner'a yükle
        kategorileriDoldur()

        // Buton tıklama olayları
        binding.btnAddPhoto.setOnClickListener { resimSecimDialoguGoster() }
        binding.btnSubmit.setOnClickListener { olayiKaydet() }
    }

    private fun haritayiHazirla() {
        map = binding.mapAdd
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        val baslangicNoktasi = GeoPoint(currentLat, currentLng)
        map.controller.setZoom(16.0)
        map.controller.setCenter(baslangicNoktasi)

        // Harita üzerine konumu gösteren pin ekle
        marker = Marker(map)
        marker?.position = baslangicNoktasi
        marker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker?.title = "Olay Konumu"
        map.overlays.add(marker)

        // YENİ ÖZELLİK: Haritaya dokunarak pini taşıma
        val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                if (p != null) {
                    konumuGuncelle(p.latitude, p.longitude)
                }
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                // Uzun basınca da çalışsın istersen burayı da açabilirsin
                if (p != null) {
                    konumuGuncelle(p.latitude, p.longitude)
                }
                return true
            }
        })
        map.overlays.add(mapEventsOverlay)
    }

    // Haritadaki pini ve değişkenleri güncelleyen yardımcı fonksiyon
    private fun konumuGuncelle(lat: Double, lng: Double) {
        currentLat = lat
        currentLng = lng
        val nokta = GeoPoint(lat, lng)
        marker?.position = nokta
        map.invalidate() // Haritayı yeniden çiz
    }

    private fun konumIzniKontrolEt() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            cihazKonumunuAl()
        } else {
            requestPermissionsLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    private fun cihazKonumunuAl() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    // Cihazın gerçek konumuna git
                    konumuGuncelle(location.latitude, location.longitude)
                    map.controller.animateTo(GeoPoint(location.latitude, location.longitude))
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun kategorileriDoldur() {
        val list = arrayOf("Güvenlik", "Sağlık", "Teknik", "Temizlik", "Kayıp Eşya", "Diğer")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, list)
        binding.spinnerCategory.adapter = adapter
    }

    private fun resimSecimDialoguGoster() {
        val options = arrayOf("Fotoğraf Çek", "Galeriden Seç")
        AlertDialog.Builder(requireContext())
            .setTitle("Olay Fotoğrafı")
            .setItems(options) { _, i ->
                if (i == 0) {
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        kamerayiAc()
                    } else {
                        requestPermissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                    }
                } else {
                    val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    galleryLauncher.launch(intent)
                }
            }.show()
    }

    private fun kamerayiAc() {
        try {
            val photoFile = createImageFile()
            photoURI = FileProvider.getUriForFile(
                requireContext(),
                "com.alisemiz.akilli_kampus_uygulamasi.fileprovider", // AndroidManifest'teki authority ile aynı olmalı
                photoFile
            )
            cameraLauncher.launch(photoURI)
        } catch (ex: IOException) {
            Toast.makeText(context, "Kamera hatası!", Toast.LENGTH_SHORT).show()
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("OLAY_${timeStamp}_", ".jpg", storageDir)
    }

    private fun olayiKaydet() {
        val baslik = binding.etTitle.text.toString().trim()
        val aciklama = binding.etDescription.text.toString().trim()
        val kategori = binding.spinnerCategory.selectedItem.toString()

        if (baslik.isEmpty() || aciklama.isEmpty()) {
            Toast.makeText(context, "Lütfen boş alanları doldurun!", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSubmit.isEnabled = false

        // Eğer resim seçildiyse önce Storage'a yükle
        if (secilenGorselUri != null) {
            val dosyaAdi = "${UUID.randomUUID()}.jpg"
            val referans = storage.reference.child("olay_resimleri/$dosyaAdi")

            referans.putFile(secilenGorselUri!!)
                .addOnSuccessListener {
                    referans.downloadUrl.addOnSuccessListener { url ->
                        veriyiFirestoreKaydet(baslik, aciklama, kategori, url.toString())
                    }
                }
                .addOnFailureListener {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmit.isEnabled = true
                    Toast.makeText(context, "Resim yüklenemedi!", Toast.LENGTH_SHORT).show()
                }
        } else {
            veriyiFirestoreKaydet(baslik, aciklama, kategori, null)
        }
    }

    private fun veriyiFirestoreKaydet(baslik: String, aciklama: String, kategori: String, gorselUrl: String?) {
        val uid = auth.currentUser?.uid ?: return

        // DÜZELTME BURADA:
        // Olayı oluşturan kişiyi (uid) followers listesine ekliyoruz.
        // Böylece MainActivity'deki listener bunu yakalayıp bildirim gönderebilecek.
        val takipciler = listOf(uid)

        val yeniOlay = hashMapOf(
            "title" to baslik,
            "description" to aciklama,
            "type" to kategori,
            "status" to "Beklemede", // Yeni olaylar beklemede başlar
            "timestamp" to Timestamp(Date()),
            "userId" to uid,
            "imageUrl" to gorselUrl,
            "followers" to takipciler, // <-- KRİTİK GÜNCELLEME
            "latitude" to currentLat,
            "longitude" to currentLng
        )

        db.collection("incidents").add(yeniOlay)
            .addOnSuccessListener {
                if (_binding != null) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "Olay başarıyla kaydedildi.", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            }
            .addOnFailureListener {
                if (_binding != null) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmit.isEnabled = true
                    Toast.makeText(context, "Firestore hatası: ${it.message}", Toast.LENGTH_SHORT).show()
                }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}