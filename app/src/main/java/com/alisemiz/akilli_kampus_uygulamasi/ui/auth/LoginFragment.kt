package com.alisemiz.akilli_kampus_uygulamasi.ui.auth

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.alisemiz.akilli_kampus_uygulamasi.R
import com.alisemiz.akilli_kampus_uygulamasi.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginFragment : Fragment() {

    // ViewBinding tanımlamaları. Nullable yapıyoruz ki bellek sızıntısı (memory leak) olmasın.

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

        // 1. OTOMATİK GİRİŞ KONTROLÜ
        // Eğer kullanıcı daha önce giriş yapmış ve çıkmamışsa, tekrar login sormayalım.
        // Direkt ana sayfaya alalım, kullanıcı deneyimi (UX) artsın.
        if (auth.currentUser != null) {
            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
        }

        // "Kayıt Ol" yazısına tıklarsa kayıt ekranına geri yolluyoruz
        binding.tvGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        // ŞİFREMI UNUTTUM kısmı

        binding.tvForgotPassword.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Şifre Sıfırlama")
            builder.setMessage("Sıfırlama bağlantısı göndermek için e-posta adresinizi girin:")

            // Dialog içine bir EditText (yazı alanı) ekleyelim
            val inputEmail = EditText(requireContext())
            inputEmail.hint = "E-posta adresiniz"
            inputEmail.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            builder.setView(inputEmail)

            // "Gönder" butonu
            builder.setPositiveButton("Gönder") { dialog, _ ->
                val emailToSend = inputEmail.text.toString().trim()
                if (emailToSend.isNotEmpty()) {
                    // Firebase tek satırda sıfırlama maili atıyor, mükemmel kolaylık.
                    auth.sendPasswordResetEmail(emailToSend)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Sıfırlama bağlantısı e-postana gönderildi!", Toast.LENGTH_LONG).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(context, "Lütfen geçerli bir e-posta yazın.", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            // "İptal" butonu
            builder.setNegativeButton("İptal") { dialog, _ ->
                dialog.cancel()
            }
            builder.show()
        }

        // 3. GİRİŞ YAP BUTONU
        binding.btnLogin.setOnClickListener {

            // Verileri al ve temizle (boşlukları sil)
            val email = binding.etLoginEmail.text.toString().trim()
            val password = binding.etLoginPassword.text.toString().trim()

            // Boş alan kontrolü. Kullanıcı boş basıp sistemi yormasın.
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "E-posta veya şifre boş olamaz hocam!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Butonu kilitliyoruz! (Çift tıklama bug'ını önlemek için)
            binding.btnLogin.isEnabled = false
            binding.btnLogin.text = "Giriliyor..."

            // Firebase ile giriş denemesi
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    //BAŞARILI
                    Toast.makeText(context, "Giriş Başarılı!", Toast.LENGTH_SHORT).show()

                    // NavGraph'ta tanımladığımız ok (action) üzerinden Home'a gidiyoruz
                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                }
                .addOnFailureListener { e ->
                    // HATA
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