package com.alisemiz.akilli_kampus_uygulamasi.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.alisemiz.akilli_kampus_uygulamasi.MainActivity
import com.alisemiz.akilli_kampus_uygulamasi.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = auth.currentUser
        val email = currentUser?.email ?: ""
        binding.tvUserEmail.text = email

        // 1. GÜVENLİ ROL KONTROLÜ
        currentUser?.let { user ->
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val role = document.getString("role") ?: "user"
                        if (role == "admin") {
                            binding.tvUserRole.text = "Rol: YÖNETİCİ (ADMIN)"
                        } else {
                            binding.tvUserRole.text = "Rol: KULLANICI"
                        }
                    } else {
                        binding.tvUserRole.text = "Rol: KULLANICI"
                    }
                }
        }

        // 2. BİLDİRİM AYARLARI (Kaydet/Oku)
        val sharedPref = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        // Mevcut ayarı oku (Varsayılan: true/açık)
        val notificationsEnabled = sharedPref.getBoolean("notifications_enabled", true)
        binding.switchNotifications.isChecked = notificationsEnabled

        // Değişiklik olunca kaydet
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("notifications_enabled", isChecked).apply()

            if (isChecked) {
                Toast.makeText(context, "Bildirimler AÇILDI", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Bildirimler KAPATILDI", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. ÇIKIŞ YAP
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}