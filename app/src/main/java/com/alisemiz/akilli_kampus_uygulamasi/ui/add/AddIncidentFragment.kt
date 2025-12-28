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

class AddIncidentFragment : Fragment() {

    private var _binding: FragmentAddIncidentBinding? = null
    private val binding get() = _binding!!

    // Harita bileşenlerini ve üzerindeki işaretçiyi burada tutuyoruz
    private lateinit var map: MapView
    private var marker: Marker? = null

    // Google Play Servislerini kullanarak cihazın GPS'ine erişmek için bu client lazım
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Uygulama Erzurum odaklı olduğu için başlangıç noktasını Atatürk Üniversitesi seçtik
    private var currentLat: Double = 39.9048
    private var currentLng: Double = 41.2572

    private var secilenGorselUri: Uri? = null
    private var photoURI: Uri? = null

    // Firebase'in üç farklı servisini de burada kullanıyoruz (Giriş, Veritabanı ve Depolama)
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // Galeriden fotoğraf seçince dönen sonucu burada yakalayıp ekranda gösteriyoruz
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data != null && data.data != null) {
                secilenGorselUri = data.data
                binding.ivSelectedImage.setImageURI(secilenGorselUri)
                binding.ivSelectedImage.visibility = View.VISIBLE // Önizleme resmini görünür yapıyoruz
            }
        }
    }

    // Kamera ile çekilen fotoğraf başarılıysa URI üzerinden ekrana basıyoruz
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoURI != null) {
            secilenGorselUri = photoURI
            binding.ivSelectedImage.setImageURI(photoURI)
            binding.ivSelectedImage.visibility = View.VISIBLE
        }
    }

    // Modern Android'de izinleri böyle toplu istemek en güvenli yol (Kamera ve Konum için)
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
        // osmdroid haritasının telefona düzgün yüklenmesi için bu ayarı en başta yapmak şart
        Configuration.getInstance().load(context, android.preference.PreferenceManager.getDefaultSharedPreferences(context))

        _binding = FragmentAddIncidentBinding.inflate(inflater, container, false)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Sayfa açılır açılmaz harita ve kategori listesini hazırlıyoruz
        haritayiHazirla()
        konumIzniKontrolEt()
        kategorileriDoldur()

        // Butonların tıklama aksiyonları
        binding.btnAddPhoto.setOnClickListener { resimSecimDialoguGoster() }
        binding.btnSubmit.setOnClickListener { olayiKaydet() }
    }

    private fun haritayiHazirla() {
        // XML tarafındaki mapAdd bileşenini koda bağladık
        map = binding.mapAdd
        map.setTileSource(TileSourceFactory.MAPNIK) // Standart görünüm
        map.setMultiTouchControls(true) // Kullanıcı iki parmakla zoom yapabilsin

        val baslangicNoktasi = GeoPoint(currentLat, currentLng)
        map.controller.setZoom(16.0)
        map.controller.setCenter(baslangicNoktasi)

        // Haritada seçilen yeri göstermek için bir pin oluşturup ekliyoruz
        marker = Marker(map)
        marker?.position = baslangicNoktasi
        marker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker?.title = "Olay Konumu"
        map.overlays.add(marker)

        // Burası en kritiği:
        // Haritaya tıklandığında tıkladığımız yeri algılayan yapı
        val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                if (p != null) {
                    konumuGuncelle(p.latitude, p.longitude) // Tek tıkla pini taşı
                }
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                if (p != null) {
                    konumuGuncelle(p.latitude, p.longitude) // Uzun basınca da taşısın
                }
                return true
            }
        })
        map.overlays.add(mapEventsOverlay)
    }

    // İşaretçiyi ve koordinat değişkenlerini senkronize eden ufak bir yardımcı
    private fun konumuGuncelle(lat: Double, lng: Double) {
        currentLat = lat
        currentLng = lng
        val nokta = GeoPoint(lat, lng)
        marker?.position = nokta
        map.invalidate() // Haritayı görsel olarak tazelememiz gerekiyor yoksa pin yerinde kalır
    }

    private fun konumIzniKontrolEt() {
        // İzin varsa direkt GPS'ten konumu al, yoksa kullanıcıdan iste
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
            // Cihazın bilinen son konumunu çekip haritayı oraya kaydırıyoruz
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    konumuGuncelle(location.latitude, location.longitude)
                    map.controller.animateTo(GeoPoint(location.latitude, location.longitude))
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun kategorileriDoldur() {
        // Kategori seçimi için bir liste oluşturup Spinner'a (açılır menü) bağladık
        val list = arrayOf("Güvenlik", "Sağlık", "Teknik", "Temizlik", "Kayıp Eşya", "Diğer")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, list)
        binding.spinnerCategory.adapter = adapter
    }

    private fun resimSecimDialoguGoster() {
        // Kullanıcıya fotoğrafı nereden alacağını soran o meşhur kutucuk
        val options = arrayOf("Fotoğraf Çek", "Galeriden Seç")
        AlertDialog.Builder(requireContext())
            .setTitle("Olay Fotoğrafı")
            .setItems(options) { _, i ->
                if (i == 0) { // Kamera
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        kamerayiAc()
                    } else {
                        requestPermissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                    }
                } else { // Galeri
                    val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    galleryLauncher.launch(intent)
                }
            }.show()
    }

    private fun kamerayiAc() {
        try {
            // Fotoğrafı çekince kaydedeceğimiz boş bir dosya oluşturuyoruz
            val photoFile = createImageFile()
            // Android 7+ sonrası dosya güvenliği için FileProvider kullanmak zorunlu
            photoURI = FileProvider.getUriForFile(
                requireContext(),
                "com.alisemiz.akilli_kampus_uygulamasi.fileprovider",
                photoFile
            )
            cameraLauncher.launch(photoURI)
        } catch (ex: IOException) {
            Toast.makeText(context, "Kamera açılamadı!", Toast.LENGTH_SHORT).show()
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // İsim çakışması olmasın diye dosyayı o anki tarihle adlandırıyoruz
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("OLAY_${timeStamp}_", ".jpg", storageDir)
    }

    private fun olayiKaydet() {
        val baslik = binding.etTitle.text.toString().trim()
        val aciklama = binding.etDescription.text.toString().trim()
        val kategori = binding.spinnerCategory.selectedItem.toString()

        // Boş veriyle veritabanını kirletmemek için kontrol yapıyoruz
        if (baslik.isEmpty() || aciklama.isEmpty()) {
            Toast.makeText(context, "Tüm alanları doldurman gerekiyor!", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSubmit.isEnabled = false // Çift tıklamayı önlemek için butonu kilitledik

        // Resim varsa önce Storage'a yüklüyoruz, oradan URL gelince Firestore'a yazacağız
        if (secilenGorselUri != null) {
            val dosyaAdi = "${UUID.randomUUID()}.jpg" // Karışıklık olmasın diye her resme eşsiz bir ID
            val referans = storage.reference.child("olay_resimleri/$dosyaAdi")

            referans.putFile(secilenGorselUri!!)
                .addOnSuccessListener {
                    referans.downloadUrl.addOnSuccessListener { url ->
                        // Resim başarıyla yüklendi, şimdi Firestore aşamasına geçelim
                        veriyiFirestoreKaydet(baslik, aciklama, kategori, url.toString())
                    }
                }
                .addOnFailureListener {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmit.isEnabled = true
                    Toast.makeText(context, "Görsel yüklenirken bir sorun çıktı.", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Resim yoksa direkt metinleri kaydediyoruz
            veriyiFirestoreKaydet(baslik, aciklama, kategori, null)
        }
    }

    private fun veriyiFirestoreKaydet(baslik: String, aciklama: String, kategori: String, gorselUrl: String?) {
        val uid = auth.currentUser?.uid ?: return

        // Olayı açan kişiyi de takipçi listesine ekliyoruz ki kendi olayından haber alabilsin
        val takipciler = listOf(uid)

        // Veritabanına paket olarak göndereceğimiz nesneyi hazırlıyoruz
        val yeniOlay = hashMapOf(
            "title" to baslik,
            "description" to aciklama,
            "type" to kategori,
            "status" to "Beklemede",
            "timestamp" to Timestamp(Date()),
            "userId" to uid,
            "imageUrl" to gorselUrl,
            "followers" to takipciler,
            "latitude" to currentLat, // Haritadan aldığımız güncel koordinat
            "longitude" to currentLng
        )

        db.collection("incidents").add(yeniOlay)
            .addOnSuccessListener {
                if (_binding != null) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "Olay başarıyla paylaşıldı.", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack() // Listeye geri dön
                }
            }
            .addOnFailureListener {
                if (_binding != null) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmit.isEnabled = true
                    Toast.makeText(context, "Veritabanı hatası: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Uygulama arka plana gidip gelirse haritanın çalışmaya devam etmesi için bu metotlar şart
    override fun onResume() { super.onResume(); map.onResume() }
    override fun onPause() { super.onPause(); map.onPause() }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Hafıza sızıntısı (memory leak) olmasın diye temizlik yapıyoruz
    }
}