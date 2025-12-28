package com.alisemiz.akilli_kampus_uygulamasi.ui.home

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.alisemiz.akilli_kampus_uygulamasi.R
import com.alisemiz.akilli_kampus_uygulamasi.data.model.Incident
import com.alisemiz.akilli_kampus_uygulamasi.databinding.FragmentHomeBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Date

class HomeFragment : Fragment() {

    // ViewBinding kullanarak arayüz elemanlarına daha temiz bir şekilde erişiyoruz.
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Liste yapısı ve veritabanı bağlantıları için gerekli tanımlamalarımız.
    private lateinit var adapter: IncidentAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Uygulama içindeki durumları (rol, liste verisi, filtre) takip etmek için değişkenler.
    private var isAdmin = false
    private var tumOlaylar = listOf<Incident>()
    private var seciliFiltre = "Tümü"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Güvenlik kontrolü: Kullanıcı bir şekilde login olmadan buraya düştüyse işlemi kesiyoruz.
        if (auth.currentUser == null) {
            return
        }

        // RecyclerView ve arama çubuğu ayarlarını yapıyoruz.
        setupRecyclerView()
        setupSearchView()

        // Verileri Firestore'dan anlık olarak çekmeye başlıyoruz.
        verileriGuvenliGetir()
        // Kullanıcının yetkisini kontrol edip admin butonlarını açıyoruz.
        checkUserRoleFromFirestore()

        // Yeni olay bildirme butonuna basınca ekleme formuna gönderiyoruz.
        binding.btnAddIncident.setOnClickListener { findNavController().navigate(R.id.addIncidentFragment) }

        // Filtre ikonuna basınca kategorilerin açılmasını sağladık.
        binding.btnFilter.setOnClickListener { filtreSecimiGoster() }

        // Acil durum butonu normalde gizli, sadece admin kontrolünden sonra açılacak.
        binding.btnEmergency.visibility = View.GONE
        binding.btnEmergency.setOnClickListener { acilDurumYayinla() }
    }

    // Firestore'u "Real-time" dinleyerek yeni bir olay eklendiğinde listenin otomatik yenilenmesini sağlıyoruz.
    private fun verileriGuvenliGetir() {
        try {
            // Olayları en yeni tarihten başlayacak şekilde sıralayarak çekiyoruz.
            db.collection("incidents").orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { value, error ->
                    if (_binding == null) return@addSnapshotListener

                    if (error != null) {
                        Log.e("HomeFragment", "Veri çekme hatası", error)
                        binding.rvIncidents.visibility = View.GONE
                        binding.layoutEmptyState.visibility = View.VISIBLE
                        return@addSnapshotListener
                    }

                    val geciciListe = mutableListOf<Incident>()
                    var latestEmergency: Incident? = null

                    value?.documents?.forEach { doc ->
                        try {
                            val incident = doc.toObject(Incident::class.java)?.copy(id = doc.id)
                            if (incident != null) {
                                geciciListe.add(incident)
                                // Eğer aktif bir "ACİL" durum varsa banner'da göstermek için ayırıyoruz.
                                if (incident.status == "ACİL" && latestEmergency == null) {
                                    latestEmergency = incident
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("HomeFragment", "Bozuk veri atlandı: ${doc.id}", e)
                        }
                    }

                    tumOlaylar = geciciListe

                    //Eğer bir acil durum varsa ekranın üstünde kırmızı bir uyarı kartı çıkıyor.
                    if (latestEmergency != null) {
                        binding.cardEmergencyBanner.visibility = View.VISIBLE
                        binding.tvEmergencyText.text = latestEmergency!!.description
                        binding.cardEmergencyBanner.setOnClickListener {
                            val bundle = Bundle().apply { putString("incidentId", latestEmergency!!.id) }
                            try {
                                findNavController().navigate(R.id.incidentDetailFragment, bundle)
                            } catch (e: Exception) { }
                        }
                    } else {
                        binding.cardEmergencyBanner.visibility = View.GONE
                    }

                    // Hem filtreleri hem de listeyi güncelliyoruz.
                    listeyiGuncelle()
                }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Genel hata", e)
        }
    }

    // Kullanıcının rolünü Firestore'dan çekerek yetkilerini belirliyoruz.
    private fun checkUserRoleFromFirestore() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (_binding != null && document.exists()) {
                    val role = document.getString("role")
                    if (role == "admin") {
                        isAdmin = true
                        // Sadece admin olanlar acil durum yayınlama butonunu görebilir.
                        binding.btnEmergency.visibility = View.VISIBLE
                    }
                }
            }
    }

    private fun setupRecyclerView() {
        adapter = IncidentAdapter(
            listOf(),
            onClick = { selectedId ->
                // Bir olaya tıklandığında detay sayfasına ID ile birlikte geçiyoruz.
                if (selectedId.isNotEmpty()) {
                    try {
                        val bundle = Bundle().apply { putString("incidentId", selectedId) }
                        findNavController().navigate(R.id.incidentDetailFragment, bundle)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            },
            onLongClick = { selectedId ->
                // Uzun basınca; sadece adminse silme seçeneği sunuyoruz.
                if (isAdmin) silmeOnayiGoster(selectedId)
                else Toast.makeText(context, "Bunu sadece Yöneticiler silebilir.", Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvIncidents.layoutManager = LinearLayoutManager(context)
        binding.rvIncidents.adapter = adapter
    }

    // Arama çubuğu ve filtre butonunun ortak çalıştığı ana liste güncelleme fonksiyonumuz.
    private fun listeyiGuncelle(aranan: String = "") {
        val uid = auth.currentUser?.uid
        val filtrelenmis = tumOlaylar.filter { olay ->
            // Duruma ve kategoriye göre filtreleme mantığı.
            val turUyumu = when (seciliFiltre) {
                "Tümü" -> olay.status != "ACİL"
                "Acil Durumlar" -> olay.status == "ACİL"
                "Takip Ettiklerim" -> if (uid != null) olay.followers.contains(uid) else false
                else -> olay.type == seciliFiltre
            }
            // Başlık içinde aranan kelimenin geçip geçmediğini kontrol ediyoruz.
            val aramaUyumu = if (aranan.isEmpty()) true else olay.title.lowercase().contains(aranan.lowercase())
            turUyumu && aramaUyumu
        }

        adapter.updateList(filtrelenmis)

        // Eğer liste boşsa kullanıcıya "Sonuç bulunamadı" görselini gösteriyoruz.
        if (filtrelenmis.isEmpty()) {
            binding.rvIncidents.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
        } else {
            binding.rvIncidents.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                // Her harf yazıldığında listeyi anında daraltıyoruz.
                listeyiGuncelle(newText.orEmpty())
                return true
            }
        })
    }

    // Filtre ikonuna basılınca ekranda çıkan seçim penceresi.
    private fun filtreSecimiGoster() {
        val secenekler = arrayOf("Tümü", "Acil Durumlar", "Takip Ettiklerim", "Yangın", "Sağlık", "Güvenlik", "Teknik")
        AlertDialog.Builder(requireContext())
            .setTitle("Filtrele")
            .setItems(secenekler) { _, which ->
                seciliFiltre = secenekler[which]
                listeyiGuncelle()
            }
            .show()
    }

    // Adminlerin bir bildirimi kalıcı olarak sildiği kısımdaki onay mesajı.
    private fun silmeOnayiGoster(incidentId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Olayı Sil")
            .setMessage("Bu bildirimi kalıcı olarak silmek istiyor musunuz?")
            .setPositiveButton("SİL") { _, _ ->
                db.collection("incidents").document(incidentId).delete()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    // Adminlerin tüm kampüse duyurduğu acil durum ilanını oluşturma fonksiyonu.
    private fun acilDurumYayinla() {
        val input = EditText(requireContext())
        input.hint = "Örn: Kampüs tahliye ediliyor!"
        AlertDialog.Builder(requireContext())
            .setTitle("⚠️ ACİL DURUM YAYINI")
            .setMessage("Bu mesaj, uygulamayı kullanan HERKESE acil durum bildirimi olarak gönderilecektir.")
            .setView(input)
            .setPositiveButton("YAYINLA") { _, _ ->
                val mesaj = input.text.toString()
                if (mesaj.isNotEmpty()) {
                    // Veritabanına özel bir 'ACİL' statusuyla yeni bir olay ekliyoruz.
                    val acilDurum = hashMapOf(
                        "title" to "ACİL DURUM",
                        "description" to mesaj,
                        "type" to "Güvenlik",
                        "status" to "ACİL",
                        "timestamp" to Timestamp(Date()),
                        "userId" to auth.currentUser!!.uid,
                        "latitude" to 39.92077, // Kampüs merkezi koordinatları
                        "longitude" to 32.85411,
                        "followers" to emptyList<String>()
                    )
                    db.collection("incidents").add(acilDurum)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Acil durum herkese bildirildi!", Toast.LENGTH_LONG).show()
                        }
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Bellek sızıntısı olmaması için binding referansını null yapıyoruz.
        _binding = null
    }
}