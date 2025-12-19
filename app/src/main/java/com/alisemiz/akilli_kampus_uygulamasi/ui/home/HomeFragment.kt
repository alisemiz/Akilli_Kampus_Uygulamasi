package com.alisemiz.akilli_kampus_uygulamasi.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.alisemiz.akilli_kampus_uygulamasi.R
import com.alisemiz.akilli_kampus_uygulamasi.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    // Binding null olursa uygulama patlar, o yüzden bu yapı şart diyebiliriz.
    private val binding get() = _binding!!

    // Hem Auth hem Firestore lazım
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Araçları başlatalım
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Şu anki kullanıcı kim?
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // Email yerine veritabanından İSİM çekeceğiz.
            // Kullanıcının UID'sini alıp veritabanına soruyoruz.
            val userId = currentUser.uid

            // Kullanıcı verisini çekerken ekranda geçici bir şey yazsın
            binding.tvUserEmail.text = "Yükleniyor..."

            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Veritabanındaki 'name' alanını alıyoruz (Register'da böyle kaydetmiştik)
                        val userName = document.getString("name")

                        // İsmi bulduysak yazalım, bulamazsak maili yazalım (Yedek plan)
                        if (!userName.isNullOrEmpty()) {

                            binding.tvUserEmail.text = userName.uppercase() // İsim büyük harfle yazalım.
                        } else {
                            binding.tvUserEmail.text = currentUser.email
                        }
                    }
                }
                .addOnFailureListener {
                    // Veritabanına ulaşamazsak mecbur maili gösterelim.
                    binding.tvUserEmail.text = currentUser.email
                }
        }

        // ÇIKIŞ YAP BUTONU
        binding.btnLogout.setOnClickListener {
            // Firebase'den çıkış yap (Oturumu kapat)
            auth.signOut()

            // Login ekranına geri dön diyelim.
            findNavController().navigate(R.id.loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Belleği temizle
        _binding = null
    }
}