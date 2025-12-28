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

    // ViewBinding için klasik tanımlama, bellekte sızıntı yapmasın diye null yapıyoruz sonda
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

        // Uygulama her açıldığında tekrar şifre sormasın, eğer token varsa direkt içeri alalım.
        if (auth.currentUser != null) {
            anaSayfayaYonlendir()
        }

        // Hesabı olmayanları kayıt ekranına paslıyoruz.
        binding.tvGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        // Şifresini unutanlar için ufak bir pop-up açıp mail istiyoruz, Firebase hallediyor gerisini.
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
                        .addOnSuccessListener {
                            Toast.makeText(context, "Sıfırlama bağlantısı gönderildi!", Toast.LENGTH_LONG).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(context, "Lütfen geçerli bir e-posta yazın.", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            builder.setNegativeButton("İptal") { dialog, _ -> dialog.cancel() }
            builder.show()
        }

        // Login bilgilerini alıp Firebase'e yolluyoruz.
        binding.btnLogin.setOnClickListener {
            val email = binding.etLoginEmail.text.toString().trim()
            val password = binding.etLoginPassword.text.toString().trim()

            // Boş bırakıp girmeye çalışmasınlar diye basit bir mesaj
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "E-posta veya şifre boş olamaz!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // İşlem sürerken butona arka arkaya basıp uygulamayı yormasınlar diye kilitliyoruz.
            binding.btnLogin.isEnabled = false
            binding.btnLogin.text = "Giriş Yapılıyor..."

            // Firebase Auth ile giriş denemesi
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    anaSayfayaYonlendir()
                }
                .addOnFailureListener { e ->
                    // Hata olursa butonu tekrar aktif edip hatayı gösteriyoruz
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = "Giriş Yap"
                    Toast.makeText(context, "Giriş Başarısız: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    // Yetki olayını HomeFragment içerisinde Firestore'dan kontrol ettireceğiz.
    private fun anaSayfayaYonlendir() {
        Toast.makeText(context, "Giriş Başarılı", Toast.LENGTH_SHORT).show()
        findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Binding'i null yaparak Memory Leak'in önüne geçiyoruz
        _binding = null
    }
}