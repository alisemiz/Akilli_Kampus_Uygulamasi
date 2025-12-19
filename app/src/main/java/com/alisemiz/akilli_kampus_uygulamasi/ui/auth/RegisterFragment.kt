package com.alisemiz.akilli_kampus_uygulamasi.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.alisemiz.akilli_kampus_uygulamasi.R
import com.alisemiz.akilli_kampus_uygulamasi.data.model.User
import com.alisemiz.akilli_kampus_uygulamasi.databinding.FragmentRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Giriş ekranına dönme butonu
        binding.tvGoToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        // KAYIT OL BUTONU
        binding.btnRegister.setOnClickListener {
            // 1. Verileri al
            val name = binding.etRegisterName.text.toString().trim()
            val department = binding.etRegisterDepartment.text.toString().trim()
            val email = binding.etRegisterEmail.text.toString().trim()
            val password = binding.etRegisterPassword.text.toString().trim()

            // 2. Kontroller
            if (name.isEmpty() || department.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Lütfen tüm alanları doldurun!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(context, "Şifre en az 6 karakter olmalı!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Butonu kilitle
            binding.btnRegister.isEnabled = false
            binding.btnRegister.text = "Kaydediliyor..."

            // 3. Firebase Auth ile Kullanıcı Oluştur
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { task ->
                    val userId = task.user!!.uid

                    // ROL MANTIĞI
                    // varsayılan rol "user".
                    // Ancak testi kolaylaştırmak için email içinde "admin" geçiyorsa admin yapıyoruz.
                    // Örn: admin@okul.com -> Admin olur. ali@okul.com -> User olur.
                    val userRole = if (email.contains("admin")) {
                        "admin"
                    } else {
                        "user"
                    }

                    // User nesnesini hazırla
                    val newUser = User(
                        uid = userId,
                        name = name,
                        email = email,
                        department = department,
                        role = userRole
                    )

                    // 4. Firestore Veritabanına Kaydet
                    firestore.collection("users").document(userId).set(newUser)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Kayıt Başarılı! Rol: $userRole", Toast.LENGTH_LONG).show()

                            // Başarılı olunca Giriş ekranına at (Auto-login yapmadık, giriş yapsın istiyoruz)
                            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                        }
                        .addOnFailureListener { e ->
                            // Firestore hatası
                            binding.btnRegister.isEnabled = true
                            binding.btnRegister.text = "Kayıt Ol"
                            Toast.makeText(context, "Veritabanı Hatası: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { e ->
                    // Auth hatası (Email zaten var vb.)
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text = "Kayıt Ol"
                    Toast.makeText(context, "Kayıt Başarısız: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}