package com.alisemiz.akilli_kampus_uygulamasi.ui.incident_detail

import android.app.AlertDialog
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
    private var isFollowing = false

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
            olayDetaylariniGetir(incidentId!!)
            // Admin kontrolünü başlat
            adminYetkisiniKontrolEt()
        } else {
            Toast.makeText(context, "Hata: Olay ID bulunamadı!", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        binding.btnFollow.setOnClickListener {
            if (incidentId != null) takipDurumunuDegistir()
        }

        // Admin Butonu Tıklaması
        binding.btnUpdateStatus.setOnClickListener {
            durumDegistirmeDialoguGoster()
        }
    }

    private fun adminYetkisiniKontrolEt() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (_binding != null && document.exists()) {
                    val role = document.getString("role")
                    if (role == "admin") {
                        // Admin ise butonu göster
                        binding.btnUpdateStatus.visibility = View.VISIBLE
                    }
                }
            }
    }

    private fun durumDegistirmeDialoguGoster() {
        val secenekler = arrayOf("İnceleniyor", "Çözüldü", "Beklemede", "ACİL")

        AlertDialog.Builder(requireContext())
            .setTitle("Olay Durumunu Güncelle")
            .setItems(secenekler) { _, which ->
                val yeniDurum = secenekler[which]
                yeniDurumuKaydet(yeniDurum)
            }
            .show()
    }

    private fun yeniDurumuKaydet(yeniDurum: String) {
        if (incidentId == null) return

        db.collection("incidents").document(incidentId!!)
            .update("status", yeniDurum)
            .addOnSuccessListener {
                if (_binding != null) {
                    Toast.makeText(context, "Durum güncellendi: $yeniDurum", Toast.LENGTH_SHORT).show()
                    binding.tvDetailStatus.text = yeniDurum // Ekranda anlık güncelle
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Hata: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun olayDetaylariniGetir(id: String) {
        val uid = auth.currentUser?.uid ?: return

        db.collection("incidents").document(id).get()
            .addOnSuccessListener { document ->
                if (_binding == null) return@addOnSuccessListener

                if (document.exists()) {
                    val title = document.getString("title") ?: ""
                    val description = document.getString("description") ?: ""
                    val type = document.getString("type") ?: ""
                    val status = document.getString("status") ?: ""
                    val timestamp = document.getTimestamp("timestamp")
                    val imageUrl = document.getString("imageUrl")
                    val followers = document.get("followers") as? List<String> ?: emptyList()

                    binding.tvDetailTitle.text = title
                    binding.tvDetailDescription.text = description
                    binding.tvDetailType.text = type
                    binding.tvDetailStatus.text = status

                    if (timestamp != null) {
                        val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("tr"))
                        binding.tvDetailDate.text = sdf.format(timestamp.toDate())
                    }

                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(imageUrl)
                            .centerCrop()
                            .placeholder(android.R.drawable.ic_menu_camera)
                            .into(binding.ivDetailImage)
                    } else {
                        binding.ivDetailImage.setImageResource(android.R.drawable.ic_menu_camera)
                    }

                    isFollowing = followers.contains(uid)
                    butonTasariminiGuncelle()
                }
            }
    }

    private fun takipDurumunuDegistir() {
        val uid = auth.currentUser?.uid ?: return
        val ref = db.collection("incidents").document(incidentId!!)

        binding.btnFollow.isEnabled = false

        if (isFollowing) {
            ref.update("followers", FieldValue.arrayRemove(uid))
                .addOnSuccessListener {
                    isFollowing = false
                    if (_binding != null) {
                        butonTasariminiGuncelle()
                        Toast.makeText(context, "Takip bırakıldı.", Toast.LENGTH_SHORT).show()
                        binding.btnFollow.isEnabled = true
                    }
                }
        } else {
            ref.update("followers", FieldValue.arrayUnion(uid))
                .addOnSuccessListener {
                    isFollowing = true
                    if (_binding != null) {
                        butonTasariminiGuncelle()
                        Toast.makeText(context, "Bildirimler açıldı.", Toast.LENGTH_SHORT).show()
                        binding.btnFollow.isEnabled = true
                    }
                }
        }
    }

    private fun butonTasariminiGuncelle() {
        if (isFollowing) {
            binding.btnFollow.text = "Takibi Bırak"
            binding.btnFollow.setBackgroundColor(Color.parseColor("#FFEBEE"))
            binding.btnFollow.setTextColor(Color.parseColor("#D32F2F"))
            binding.btnFollow.setIconResource(android.R.drawable.ic_menu_close_clear_cancel)
            binding.btnFollow.iconTint = ColorStateList.valueOf(Color.parseColor("#D32F2F"))
        } else {
            binding.btnFollow.text = "Takip Et"
            binding.btnFollow.setBackgroundColor(Color.parseColor("#E8EAF6"))
            binding.btnFollow.setTextColor(Color.parseColor("#1A237E"))
            binding.btnFollow.setIconResource(android.R.drawable.ic_input_add)
            binding.btnFollow.iconTint = ColorStateList.valueOf(Color.parseColor("#1A237E"))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}