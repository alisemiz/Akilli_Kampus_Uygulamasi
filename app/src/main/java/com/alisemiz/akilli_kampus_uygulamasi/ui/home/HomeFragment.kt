package com.alisemiz.akilli_kampus_uygulamasi.ui.home

import android.app.AlertDialog
import android.os.Bundle
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

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: IncidentAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var isAdmin = false
    private var tumOlaylar = listOf<Incident>() // Veritabanından gelen ham veri
    private var seciliFiltre = "Tümü" // Varsayılan filtre

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. GÜVENLİ ROL KONTROLÜ (Veritabanından)
        checkUserRoleFromFirestore()

        // 2. Arayüz ve Liste Kurulumu
        setupRecyclerView()
        setupSearchView()
        verileriGetir()

        // 3. Navigasyon Butonları
        binding.btnOpenMap.setOnClickListener { findNavController().navigate(R.id.mapFragment) }
        binding.btnAddIncident.setOnClickListener { findNavController().navigate(R.id.addIncidentFragment) }



        // Filtre Butonu
        view.findViewById<View>(R.id.btnFilter)?.setOnClickListener { filtreSecimiGoster() }

        // Acil Durum Butonu (Sadece Admin görebilir - checkUserRole içinde ayarlanır)
        view.findViewById<View>(R.id.btnEmergency)?.setOnClickListener {
            acilDurumYayinla()
        }
    }

    // --- GÜVENLİ ROL KONTROLÜ ---
    private fun checkUserRoleFromFirestore() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val role = document.getString("role")
                        // Sadece veritabanında "admin" yazıyorsa yetki ver
                        if (role == "admin") {
                            isAdmin = true
                            // Gizli butonu görünür yap
                            binding.root.findViewById<View>(R.id.btnEmergency).visibility = View.VISIBLE
                        } else {
                            isAdmin = false
                            binding.root.findViewById<View>(R.id.btnEmergency).visibility = View.GONE
                        }
                    }
                }
                .addOnFailureListener {
                    // Hata durumunda güvenli kal (Admin yetkisi verme)
                    isAdmin = false
                    binding.root.findViewById<View>(R.id.btnEmergency).visibility = View.GONE
                }
        }
    }

    // --- ACİL DURUM YAYINI ---
    private fun acilDurumYayinla() {
        val input = EditText(requireContext())
        input.hint = "Örn: Kampüs tahliye ediliyor!"

        AlertDialog.Builder(requireContext())
            .setTitle("⚠️ ACİL DURUM YAYINI")
            .setMessage("Tüm kullanıcılara acil durum bildirimi gönderilecek. Mesajınız:")
            .setView(input)
            .setPositiveButton("GÖNDER") { _, _ ->
                val mesaj = input.text.toString()
                if (mesaj.isNotEmpty()) {
                    val acilDurum = hashMapOf(
                        "title" to "ACİL DURUM UYARISI",
                        "description" to mesaj,
                        "type" to "Yangın", // Kırmızı gözüksün diye
                        "status" to "ACİL",
                        "timestamp" to Timestamp(Date()),
                        "userId" to auth.currentUser!!.uid,
                        "latitude" to 39.92077, // Kampüs Merkezi (Örnek)
                        "longitude" to 32.85411,
                        "followers" to emptyList<String>()
                    )

                    db.collection("incidents").add(acilDurum)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Acil durum yayınlandı!", Toast.LENGTH_LONG).show()
                        }
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    // --- LİSTE VE FİLTRELEME ---
    private fun filtreSecimiGoster() {
        val secenekler = arrayOf("Tümü", "Takip Ettiklerim", "Yangın", "Sağlık", "Güvenlik", "Teknik", "Genel")

        AlertDialog.Builder(requireContext())
            .setTitle("Filtrele")
            .setItems(secenekler) { _, which ->
                seciliFiltre = secenekler[which]
                listeyiGuncelle()
                Toast.makeText(context, "$seciliFiltre gösteriliyor", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun listeyiGuncelle(arananKelime: String = "") {
        val currentUid = auth.currentUser?.uid

        val filtrelenmisListe = tumOlaylar.filter { olay ->

            // 1. FİLTRE KONTROLÜ
            val turUyumu = when (seciliFiltre) {
                "Tümü" -> true
                "Takip Ettiklerim" -> {
                    // Kullanıcı ID'si, olaydaki followers listesinde var mı?
                    if (currentUid != null) olay.followers.contains(currentUid) else false
                }
                else -> olay.type == seciliFiltre
            }

            // 2. ARAMA KONTROLÜ
            val aramaUyumu = if (arananKelime.isEmpty()) true else {
                olay.title.lowercase().contains(arananKelime.lowercase()) ||
                        olay.description.lowercase().contains(arananKelime.lowercase())
            }

            turUyumu && aramaUyumu
        }

        // Kullanıcıya boş liste uyarısı (Sadece takip edilenlerde)
        if (seciliFiltre == "Takip Ettiklerim" && filtrelenmisListe.isEmpty() && currentUid != null) {
            // Opsiyonel: Toast.makeText(context, "Henüz takip ettiğiniz bir olay yok.", Toast.LENGTH_SHORT).show()
        }

        adapter.updateList(filtrelenmisListe)
    }

    private fun setupRecyclerView() {
        adapter = IncidentAdapter(
            listOf(),
            onClick = { selectedId ->
                // Detaya Git
                val bundle = Bundle().apply { putString("incidentId", selectedId) }
                findNavController().navigate(R.id.incidentDetailFragment, bundle)
            },
            onLongClick = { selectedId ->
                // Silme (Sadece Admin)
                if (isAdmin) silmeOnayiGoster(selectedId)
                else Toast.makeText(context, "Bunu silmek için Admin yetkisi gerekiyor.", Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvIncidents.layoutManager = LinearLayoutManager(context)
        binding.rvIncidents.adapter = adapter
    }

    private fun silmeOnayiGoster(incidentId: String) {
        AlertDialog.Builder(context)
            .setTitle("Olayı Sil")
            .setMessage("Bu bildirimi kalıcı olarak silmek istiyor musunuz?")
            .setPositiveButton("SİL") { _, _ ->
                db.collection("incidents").document(incidentId).delete()
                    .addOnSuccessListener { Toast.makeText(context, "Silindi.", Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                listeyiGuncelle(newText.orEmpty())
                return true
            }
        })
    }

    private fun verileriGetir() {
        db.collection("incidents")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Toast.makeText(context, "Veri hatası: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val list = mutableListOf<Incident>()
                value?.documents?.forEach { doc ->
                    doc.toObject(Incident::class.java)?.let {
                        // ID'yi Firestore belgesinden alıp nesneye ekliyoruz
                        list.add(it.copy(id = doc.id))
                    }
                }
                tumOlaylar = list
                // Veri değişince (veya ilk açılışta) listeyi mevcut filtreye göre güncelle
                listeyiGuncelle()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}