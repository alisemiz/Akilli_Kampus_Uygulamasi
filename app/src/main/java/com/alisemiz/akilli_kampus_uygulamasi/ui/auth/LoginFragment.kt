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

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

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

        auth = FirebaseAuth.getInstance()

        // 1. OTOMATİK GİRİŞ KONTROLÜ
        // Kullanıcı zaten giriş yapmışsa direkt ana sayfaya al
        if (auth.currentUser != null) {
            anaSayfayaYonlendir()
        }

        // Kayıt Ol Ekranına Geçiş
        binding.tvGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        // 2. ŞİFREMİ UNUTTUM DİALOGU
        binding.tvForgotPassword.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Şifre Sıfırlama")
            builder.setMessage("Sıfırlama bağlantısı göndermek için e-posta adresinizi girin:")

            val inputEmail = EditText(requireContext())
            inputEmail.hint = "E-posta adresiniz"
            inputEmail.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            builder.setView(inputEmail)

            builder.setPositiveButton("Gönder") { dialog, _ ->
                val emailToSend = inputEmail.text.toString().trim()
                if (emailToSend.isNotEmpty()) {
                    auth.sendPasswordResetEmail(emailToSend)
                        .addOnSuccessListener { Toast.makeText(context, "Sıfırlama bağlantısı gönderildi!", Toast.LENGTH_LONG).show() }
                        .addOnFailureListener { e -> Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_LONG).show() }
                } else {
                    Toast.makeText(context, "Lütfen geçerli bir e-posta yazın.", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            builder.setNegativeButton("İptal") { dialog, _ -> dialog.cancel() }
            builder.show()
        }

        // 3. GİRİŞ YAP BUTONU
        binding.btnLogin.setOnClickListener {
            val email = binding.etLoginEmail.text.toString().trim()
            val password = binding.etLoginPassword.text.toString().trim()

            // Boş alan kontrolü
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "E-posta veya şifre boş olamaz!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Butonu kilitle
            binding.btnLogin.isEnabled = false
            binding.btnLogin.text = "Giriş Yapılıyor..."

            // Firebase Auth Girişi
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    // Başarılı olursa direkt yönlendir
                    anaSayfayaYonlendir()
                }
                .addOnFailureListener { e ->
                    // Hata durumunda butonu aç
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = "Giriş Yap"
                    Toast.makeText(context, "Giriş Başarısız: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    // TEK YÖNLENDİRME FONKSİYONU
    // Admin de olsa User da olsa hepsi 'homeFragment'a gider.
    // Yetki kontrolü HomeFragment içinde yapılır.
    private fun anaSayfayaYonlendir() {
        Toast.makeText(context, "Giriş Başarılı", Toast.LENGTH_SHORT).show()
        // AdminHomeFragment yönlendirmesini kaldırdık, herkes HomeFragment'a gidiyor.
        findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}