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

    // ViewBinding kullanarak arayüz elemanlarına null-safe bir şekilde erişiyoruz.
    private var _binding: FragmentIncidentDetailBinding? = null
    private val binding get() = _binding!!

    // Veritabanı ve kullanıcı işlemleri için Firebase araçlarını tanımlıyoruz.
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

        // Bir önceki ekrandan (Home veya Map) gönderilen benzersiz olay ID'sini alıyoruz.
        incidentId = arguments?.getString("incidentId")

        if (incidentId != null) {
            // ID varsa verileri çek ve admin mi kontrol et.
            olayDetaylariniGetir(incidentId!!)
            adminYetkisiniKontrolEt()
        } else {
            // Beklenmedik bir hata olursa kullanıcıyı geri gönderiyoruz.
            Toast.makeText(context, "Hata: Olay ID bulunamadı!", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        // Takip etme/bırakma butonunun tıklama olayı.
        binding.btnFollow.setOnClickListener {
            if (incidentId != null) takipDurumunuDegistir()
        }

        // Sadece adminlerin görebildiği durum güncelleme butonu.
        binding.btnUpdateStatus.setOnClickListener {
            durumDegistirmeDialoguGoster()
        }

        // Olayı paylaşan kişinin görebildiği silme butonu.
        binding.btnDelete.setOnClickListener {
            silmeOnayiGoster()
        }
    }

    // Kullanıcının admin olup olmadığını Firestore'dan kontrol eden fonksiyon.
    private fun adminYetkisiniKontrolEt() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (_binding != null && document.exists()) {
                    val role = document.getString("role")
                    // Eğer kullanıcı admin ise "Durum Güncelle" butonu görünür hale geliyor.
                    if (role == "admin") {
                        binding.btnUpdateStatus.visibility = View.VISIBLE
                    }
                }
            }
    }

    // Olayın tüm detaylarını Firestore'dan çekip ekrana basan fonksiyon.
    private fun olayDetaylariniGetir(id: String) {
        val uid = auth.currentUser?.uid ?: return

        db.collection("incidents").document(id).get()
            .addOnSuccessListener { document ->
                if (_binding == null) return@addOnSuccessListener

                if (document.exists()) {
                    // Veritabanındaki alanları değişkenlere atıyoruz.
                    val title = document.getString("title") ?: ""
                    val description = document.getString("description") ?: ""
                    val type = document.getString("type") ?: ""
                    val status = document.getString("status") ?: ""
                    val timestamp = document.getTimestamp("timestamp")
                    val imageUrl = document.getString("imageUrl")
                    val ownerId = document.getString("userId") ?: ""
                    val followers = document.get("followers") as? List<String> ?: emptyList()

                    // UI elemanlarını dolduruyoruz.
                    binding.tvDetailTitle.text = title
                    binding.tvDetailDescription.text = description
                    binding.tvDetailType.text = type
                    binding.tvDetailStatus.text = status

                    // Tarihi Türkiye lokasyonuna göre (Gün Ay Yıl) formatlıyoruz.
                    if (timestamp != null) {
                        val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("tr"))
                        binding.tvDetailDate.text = sdf.format(timestamp.toDate())
                    }

                    // Glide kütüphanesi ile resim yükleme; resim yoksa sistem ikonu gösteriliyor.
                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(imageUrl)
                            .centerCrop()
                            .placeholder(android.R.drawable.ic_menu_camera)
                            .into(binding.ivDetailImage)
                    } else {
                        binding.ivDetailImage.setImageResource(android.R.drawable.ic_menu_camera)
                    }

                    // Olay incelenmeye başladıysa veya çözüldüyse silme butonu gizlenir.
                    if (ownerId == uid && status == "Beklemede") {
                        binding.btnDelete.visibility = View.VISIBLE
                    } else {
                        binding.btnDelete.visibility = View.GONE
                    }

                    // Kullanıcının bu olayı takip edip etmediğini kontrol ediyoruz.
                    isFollowing = followers.contains(uid)
                    butonTasariminiGuncelle()
                }
            }
    }

    // Silme işlemi öncesi kullanıcıdan onay alan AlertDialog.
    private fun silmeOnayiGoster() {
        AlertDialog.Builder(requireContext())
            .setTitle("Olayı Sil")
            .setMessage("Bu bildirimi silmek istediğinize emin misiniz? Bu işlem geri alınamaz.")
            .setPositiveButton("EVET, SİL") { _, _ ->
                incidentId?.let { id ->
                    db.collection("incidents").document(id).delete()
                        .addOnSuccessListener {
                            Toast.makeText(context, "Olay silindi.", Toast.LENGTH_SHORT).show()
                            findNavController().popBackStack()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Silinemedi: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    // Adminin durumu güncelleyebilmesi için seçenekler sunan menü.
    private fun durumDegistirmeDialoguGoster() {
        val secenekler = arrayOf("Beklemede", "İnceleniyor", "Çözüldü", "ACİL")

        AlertDialog.Builder(requireContext())
            .setTitle("Olay Durumunu Güncelle")
            .setItems(secenekler) { _, which ->
                val yeniDurum = secenekler[which]
                yeniDurumuKaydet(yeniDurum)
            }
            .show()
    }

    // Yeni durumu Firestore'a yazan ve anlık olarak UI'ı güncelleyen fonksiyon.
    private fun yeniDurumuKaydet(yeniDurum: String) {
        if (incidentId == null) return

        db.collection("incidents").document(incidentId!!)
            .update("status", yeniDurum)
            .addOnSuccessListener {
                if (_binding != null) {
                    Toast.makeText(context, "Durum güncellendi: $yeniDurum", Toast.LENGTH_SHORT).show()
                    binding.tvDetailStatus.text = yeniDurum

                    // Eğer admin durumu "Beklemede"den farklı bir şeye çevirdiyse
                    // silme butonunu anında gizliyoruz (Kendi olayı olsa bile silemez).
                    if (yeniDurum != "Beklemede") {
                        binding.btnDelete.visibility = View.GONE
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Hata: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Firestore'da 'followers' dizisine kullanıcı ID'sini ekleyip çıkaran mantık.
    private fun takipDurumunuDegistir() {
        val uid = auth.currentUser?.uid ?: return
        val ref = db.collection("incidents").document(incidentId!!)
        binding.btnFollow.isEnabled = false
        if (isFollowing) {
            ref.update("followers", FieldValue.arrayRemove(uid)).addOnSuccessListener {
                isFollowing = false
                if(_binding!=null) { butonTasariminiGuncelle(); binding.btnFollow.isEnabled = true }
            }
        } else {
            ref.update("followers", FieldValue.arrayUnion(uid)).addOnSuccessListener {
                isFollowing = true
                if(_binding!=null) { butonTasariminiGuncelle(); binding.btnFollow.isEnabled = true }
            }
        }
    }

    // Takip edilip edilmediğine göre butonun rengini ve ikonunu değiştiren görsel fonksiyon.
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
        // Memory leak (bellek sızıntısı) oluşmaması için binding referansını temizliyoruz.
        _binding = null
    }
}