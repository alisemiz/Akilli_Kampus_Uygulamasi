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
    private var tumOlaylar = listOf<Incident>() // Veritabanındaki HAM veriler
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

        // 1. Yetki ve Arayüz Kurulumları
        checkUserRoleFromFirestore()
        setupRecyclerView()
        setupSearchView()

        // 2. Verileri Getir
        verileriGetirVeAyikla()

        // 3. Tıklama Olayları
        binding.btnOpenMap.setOnClickListener { findNavController().navigate(R.id.mapFragment) }
        binding.btnAddIncident.setOnClickListener { findNavController().navigate(R.id.addIncidentFragment) }
        binding.btnFilter.setOnClickListener { filtreSecimiGoster() }

        // Admin butonu (Başlangıçta gizli)
        binding.btnEmergency.visibility = View.GONE
        binding.btnEmergency.setOnClickListener { acilDurumYayinla() }
    }

    private fun checkUserRoleFromFirestore() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val role = document.getString("role")
                        if (role == "admin") {
                            isAdmin = true
                            binding.btnEmergency.visibility = View.VISIBLE
                        }
                    }
                }
        }
    }

    private fun setupRecyclerView() {
        adapter = IncidentAdapter(
            listOf(),
            onClick = { selectedId ->
                val bundle = Bundle().apply { putString("incidentId", selectedId) }
                findNavController().navigate(R.id.incidentDetailFragment, bundle)
            },
            onLongClick = { selectedId ->
                if (isAdmin) silmeOnayiGoster(selectedId)
                else Toast.makeText(context, "Bunu sadece Yöneticiler silebilir.", Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvIncidents.layoutManager = LinearLayoutManager(context)
        binding.rvIncidents.adapter = adapter
    }

    private fun verileriGetirVeAyikla() {
        // Tüm olayları tarihe göre (Yeniden eskiye) çekiyoruz
        db.collection("incidents").orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->

                // Hata varsa veya veri yoksa BOŞ ekran göster
                if (error != null) {
                    binding.rvIncidents.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.VISIBLE
                    return@addSnapshotListener
                }

                val geciciListe = mutableListOf<Incident>()
                var latestEmergency: Incident? = null

                value?.documents?.forEach { doc ->
                    val incident = doc.toObject(Incident::class.java)?.copy(id = doc.id)

                    if (incident != null) {
                        // Hepsini ana listeye ekle (Filtrede ayıklayacağız)
                        geciciListe.add(incident)

                        // En güncel ACİL durumu yakala (Banner için)
                        if (incident.status == "ACİL" && latestEmergency == null) {
                            latestEmergency = incident
                        }
                    }
                }

                // --- 1. BANNER YÖNETİMİ ---
                if (latestEmergency != null) {
                    binding.cardEmergencyBanner.visibility = View.VISIBLE
                    binding.tvEmergencyText.text = latestEmergency!!.description
                    binding.cardEmergencyBanner.setOnClickListener {
                        val bundle = Bundle().apply { putString("incidentId", latestEmergency!!.id) }
                        findNavController().navigate(R.id.incidentDetailFragment, bundle)
                    }
                } else {
                    binding.cardEmergencyBanner.visibility = View.GONE
                }

                // --- 2. LİSTE GÜNCELLEME ---
                tumOlaylar = geciciListe
                listeyiGuncelle()
            }
    }

    private fun listeyiGuncelle(aranan: String = "") {
        val uid = auth.currentUser?.uid

        // Filtreleme Mantığı
        val filtrelenmis = tumOlaylar.filter { olay ->

            // A. Kategoriye Göre
            val turUyumu = when (seciliFiltre) {
                // "Tümü" ise: ACİL olanları listede gösterme (Tepede banner var zaten)
                "Tümü" -> olay.status != "ACİL"

                // "Acil Durumlar" ise: SADECE ACİL olanları göster
                "Acil Durumlar" -> olay.status == "ACİL"

                // "Takip Ettiklerim"
                "Takip Ettiklerim" -> if (uid != null) olay.followers.contains(uid) else false

                // Diğer Tipler (Yangın, Sağlık vb.)
                else -> olay.type == seciliFiltre
            }

            // B. Arama Çubuğuna Göre
            val aramaUyumu = if (aranan.isEmpty()) true else olay.title.lowercase().contains(aranan.lowercase())

            turUyumu && aramaUyumu
        }

        adapter.updateList(filtrelenmis)

        // --- 3. BOŞ LİSTE KONTROLÜ (GÜNCELLENDİ) ---
        if (filtrelenmis.isEmpty()) {
            // Liste boşsa: RecyclerView gizle, Resimli uyarıyı göster
            binding.rvIncidents.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
        } else {
            // Liste doluysa: Uyarıyı gizle, Listeyi göster
            binding.rvIncidents.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
        }
    }

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

    private fun acilDurumYayinla() {
        val input = EditText(requireContext())
        input.hint = "Örn: Kampüs tahliye ediliyor!"
        AlertDialog.Builder(requireContext())
            .setTitle("⚠️ ACİL DURUM YAYINI")
            .setView(input)
            .setPositiveButton("YAYINLA") { _, _ ->
                val mesaj = input.text.toString()
                if (mesaj.isNotEmpty()) {
                    val acilDurum = hashMapOf(
                        "title" to "ACİL DURUM",
                        "description" to mesaj,
                        "type" to "Güvenlik",
                        "status" to "ACİL",
                        "timestamp" to Timestamp(Date()),
                        "userId" to auth.currentUser!!.uid,
                        "latitude" to 39.92077,
                        "longitude" to 32.85411,
                        "followers" to emptyList<String>()
                    )
                    db.collection("incidents").add(acilDurum)
                        .addOnSuccessListener { Toast.makeText(context, "Yayınlandı!", Toast.LENGTH_SHORT).show() }
                }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}