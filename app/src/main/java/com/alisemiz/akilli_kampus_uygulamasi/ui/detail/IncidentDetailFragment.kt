package com.alisemiz.akilli_kampus_uygulamasi.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.alisemiz.akilli_kampus_uygulamasi.databinding.FragmentIncidentDetailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class IncidentDetailFragment : Fragment() {

    private var _binding: FragmentIncidentDetailBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var incidentId: String? = null
    private var isFollowing = false // Kullanıcı takip ediyor mu?

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIncidentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        incidentId = arguments?.getString("incidentId")

        if (incidentId != null) {
            verileriGetir(incidentId!!)
            rolKontroluYap()
            takipDurumunuKontrolEt()
        } else {
            Toast.makeText(context, "Hata: ID bulunamadı", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        // Admin Kaydet Butonu
        binding.btnAdminSave.setOnClickListener { adminGuncellemeYap() }

        // User Takip Butonu
        binding.btnFollow.setOnClickListener { takipIslemiYap() }
    }

    // --- 1. VERİLERİ GETİR ---
    private fun verileriGetir(id: String) {
        db.collection("incidents").document(id).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    binding.tvDetailTitle.text = document.getString("title")
                    binding.etDetailDescription.setText(document.getString("description")) // EditText içine koyduk

                    val status = document.getString("status") ?: "Açık"
                    binding.tvDetailStatus.text = status.uppercase()

                    val timestamp = document.getTimestamp("timestamp")
                    if (timestamp != null) {
                        val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR"))
                        binding.tvDetailDate.text = format.format(timestamp.toDate())
                    }

                    // Spinner'ı mevcut duruma göre ayarla
                    setupSpinner(status)
                }
            }
    }

    // --- 2. ROL KONTROLÜ (Admin vs User) ---
    private fun rolKontroluYap() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            val role = doc.getString("role")

            if (role == "admin") {
                // ADMIN GÖRÜNÜMÜ
                binding.adminPanel.visibility = View.VISIBLE
                binding.btnFollow.visibility = View.GONE

                // Açıklamayı düzenleyebilsin diye kilidi aç
                binding.etDetailDescription.isEnabled = true
                binding.etDetailDescription.background = android.graphics.drawable.ColorDrawable(android.graphics.Color.LTGRAY) // Belli olsun diye renk ver
            } else {
                // USER GÖRÜNÜMÜ
                binding.adminPanel.visibility = View.GONE
                binding.btnFollow.visibility = View.VISIBLE

                // Açıklama kilitli kalsın
                binding.etDetailDescription.isEnabled = false
            }
        }
    }

    // --- 3. ADMIN İŞLEMLERİ ---
    private fun setupSpinner(currentStatus: String) {
        val statusList = arrayOf("Açık", "İnceleniyor", "Çözüldü")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, statusList)
        binding.spinnerStatusUpdate.adapter = adapter

        // Mevcut durumu seçili getir
        val index = statusList.indexOfFirst { it.equals(currentStatus, ignoreCase = true) }
        if (index >= 0) binding.spinnerStatusUpdate.setSelection(index)
    }

    private fun adminGuncellemeYap() {
        val yeniDurum = binding.spinnerStatusUpdate.selectedItem.toString()
        val yeniAciklama = binding.etDetailDescription.text.toString()

        db.collection("incidents").document(incidentId!!)
            .update(
                mapOf(
                    "status" to yeniDurum,
                    "description" to yeniAciklama
                )
            )
            .addOnSuccessListener {
                Toast.makeText(context, "Olay güncellendi!", Toast.LENGTH_SHORT).show()
                binding.tvDetailStatus.text = yeniDurum.uppercase()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Hata: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // --- 4. USER TAKİP İŞLEMLERİ ---
    private fun takipDurumunuKontrolEt() {
        val uid = auth.currentUser?.uid ?: return
        // Takipçiler dizisinde bu kullanıcı var mı?
        db.collection("incidents").document(incidentId!!).get()
            .addOnSuccessListener { doc ->
                val followers = doc.get("followers") as? List<String> ?: emptyList()
                if (followers.contains(uid)) {
                    isFollowing = true
                    binding.btnFollow.text = "TAKİBİ BIRAK"
                    binding.btnFollow.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.GRAY)
                } else {
                    isFollowing = false
                    binding.btnFollow.text = "TAKİP ET"
                    binding.btnFollow.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4CAF50")) // Yeşil
                }
            }
    }

    private fun takipIslemiYap() {
        val uid = auth.currentUser?.uid ?: return
        val docRef = db.collection("incidents").document(incidentId!!)

        if (isFollowing) {
            // Takipten Çık (Array'den sil)
            docRef.update("followers", FieldValue.arrayRemove(uid))
                .addOnSuccessListener {
                    isFollowing = false
                    binding.btnFollow.text = "TAKİP ET"
                    binding.btnFollow.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4CAF50"))
                    Toast.makeText(context, "Takip bırakıldı.", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Takip Et (Array'e ekle)
            docRef.update("followers", FieldValue.arrayUnion(uid))
                .addOnSuccessListener {
                    isFollowing = true
                    binding.btnFollow.text = "TAKİBİ BIRAK"
                    binding.btnFollow.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.GRAY)
                    Toast.makeText(context, "Bildirim takip listesine eklendi.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}