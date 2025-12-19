package com.alisemiz.akilli_kampus_uygulamasi.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.alisemiz.akilli_kampus_uygulamasi.R
import com.alisemiz.akilli_kampus_uygulamasi.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginFragment : Fragment() {

    // ViewBinding tanımlamaları. Nullable yapıyoruz ki memory leak olmasın.
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    // Firebase yetki objemiz
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Firebase'i başlatıyoruz.
        auth = FirebaseAuth.getInstance()

        // Eğer kullanıcı zaten giriş yapmışsa, tekrar sormaya gerek yok direkt içeri alalım.
        if (auth.currentUser != null) {
            // Zaten girişli ise direkt Ana Sayfaya yönlendir
            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
        }

        // "Kayıt Ol" yazısına tıklarsa kayıt ekranına geri yolluyoruz
        binding.tvGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        // GİRİŞ YAP BUTONU
        binding.btnLogin.setOnClickListener {

            // 1. Verileri al ve temizle (trim)
            val email = binding.etLoginEmail.text.toString().trim()
            val password = binding.etLoginPassword.text.toString().trim()

            // 2. Boş alan kontrolü.
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "E-posta veya şifre boş olamaz.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 3. Butonu kilitliyoruz.(Çift tıklama bug'ını önlemek için)
            binding.btnLogin.isEnabled = false
            binding.btnLogin.text = "Giriliyor..."

            // 4. Firebase ile giriş denemesi
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    //BAŞARILI
                    Toast.makeText(context, "Giriş Başarılı!", Toast.LENGTH_SHORT).show()

                    // NavGraph'ta tanımladığımız ok (action) üzerinden Home'a gidiyoruz
                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)

                    // finish() yapmamıza gerek yok, nav_graph'taki popUpTo bunu hallediyor.
                }
                .addOnFailureListener { e ->
                    //HATA
                    // Butonu tekrar açalım ki tekrar deneyebilsin
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = "Giriş Yap"

                    // Hatayı göster
                    Toast.makeText(context, "Giriş Başarısız: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}