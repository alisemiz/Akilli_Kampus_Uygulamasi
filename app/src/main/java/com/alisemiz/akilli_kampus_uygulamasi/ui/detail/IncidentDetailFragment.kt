package com.alisemiz.akilli_kampus_uygulamasi.ui.incident_detail

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.alisemiz.akilli_kampus_uygulamasi.databinding.FragmentIncidentDetailBinding
import com.bumptech.glide.Glide
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

        // Argümanlardan ID'yi al
        incidentId = arguments?.getString("incidentId")

        if (incidentId != null) {
            olayDetaylariniGetir(incidentId!!)
        } else {
            Toast.makeText(context, "Hata: Olay ID bulunamadı!", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }

        // Geri Butonu
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        // Takip Et / Bırak Butonu
        binding.btnFollow.setOnClickListener {
            if (incidentId != null) {
                takipDurumunuDegistir()
            }
        }
    }

    private fun olayDetaylariniGetir(id: String) {
        val uid = auth.currentUser?.uid ?: return

        db.collection("incidents").document(id).get()
            .addOnSuccessListener { document ->
                // Fragment kapanmışsa işlemi durdur (NullPointer önleyici)
                if (_binding == null) return@addOnSuccessListener

                if (document.exists()) {
                    // 1. Verileri Ekrana Yerleştir
                    val title = document.getString("title") ?: ""
                    val description = document.getString("description") ?: ""
                    val type = document.getString("type") ?: ""
                    val status = document.getString("status") ?: ""
                    val timestamp = document.getTimestamp("timestamp")
                    val imageUrl = document.getString("imageUrl")

                    // Takipçi Listesini Al
                    val followers = document.get("followers") as? List<String> ?: emptyList()

                    // Yazıları Ata
                    binding.tvDetailTitle.text = title
                    binding.tvDetailDescription.text = description
                    binding.tvDetailType.text = type
                    binding.tvDetailStatus.text = status

                    // Tarih Formatla
                    if (timestamp != null) {
                        val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("tr"))
                        binding.tvDetailDate.text = sdf.format(timestamp.toDate())
                    }

                    // Resim Yükle (Glide)
                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(imageUrl)
                            .centerCrop()
                            .placeholder(android.R.drawable.ic_menu_camera)
                            .into(binding.ivDetailImage)
                    } else {
                        // Resim yoksa varsayılanı göster veya gizle
                        binding.ivDetailImage.setImageResource(android.R.drawable.ic_menu_camera)
                    }

                    // 2. Takip Durumunu Kontrol Et
                    isFollowing = followers.contains(uid)
                    butonTasariminiGuncelle()
                }
            }
            .addOnFailureListener {
                if (_binding != null) {
                    Toast.makeText(context, "Veri yüklenemedi: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun takipDurumunuDegistir() {
        val uid = auth.currentUser?.uid ?: return
        val ref = db.collection("incidents").document(incidentId!!)

        // Çift tıklamayı önlemek için butonu kilitle
        binding.btnFollow.isEnabled = false

        if (isFollowing) {
            // TAKİBİ BIRAK (ArrayRemove)
            ref.update("followers", FieldValue.arrayRemove(uid))
                .addOnSuccessListener {
                    isFollowing = false
                    if (_binding != null) {
                        butonTasariminiGuncelle()
                        Toast.makeText(context, "Takip bırakıldı.", Toast.LENGTH_SHORT).show()
                        binding.btnFollow.isEnabled = true
                    }
                }
                .addOnFailureListener {
                    if (_binding != null) binding.btnFollow.isEnabled = true
                }
        } else {
            // TAKİP ET (ArrayUnion)
            ref.update("followers", FieldValue.arrayUnion(uid))
                .addOnSuccessListener {
                    isFollowing = true
                    if (_binding != null) {
                        butonTasariminiGuncelle()
                        Toast.makeText(context, "Bildirimler açıldı.", Toast.LENGTH_SHORT).show()
                        binding.btnFollow.isEnabled = true
                    }
                }
                .addOnFailureListener {
                    if (_binding != null) binding.btnFollow.isEnabled = true
                }
        }
    }

    private fun butonTasariminiGuncelle() {
        // Renk ve İkon Ayarları
        if (isFollowing) {
            // Takip Ediliyor Modu (Kırmızı tonları - Bırakmak için)
            binding.btnFollow.text = "Takibi Bırak"
            binding.btnFollow.setBackgroundColor(Color.parseColor("#FFEBEE")) // Açık Kırmızı Arka Plan
            binding.btnFollow.setTextColor(Color.parseColor("#D32F2F"))       // Koyu Kırmızı Yazı

            // Sistem İkonu: Çarpı/İptal
            binding.btnFollow.setIconResource(android.R.drawable.ic_menu_close_clear_cancel)
            binding.btnFollow.iconTint = ColorStateList.valueOf(Color.parseColor("#D32F2F"))
        } else {
            // Takip Et Modu (Mavi tonları - Eklemek için)
            binding.btnFollow.text = "Takip Et"
            binding.btnFollow.setBackgroundColor(Color.parseColor("#E8EAF6")) // Açık Mavi Arka Plan
            binding.btnFollow.setTextColor(Color.parseColor("#1A237E"))       // Koyu Mavi Yazı

            // Sistem İkonu: Artı/Ekle
            binding.btnFollow.setIconResource(android.R.drawable.ic_input_add)
            binding.btnFollow.iconTint = ColorStateList.valueOf(Color.parseColor("#1A237E"))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}