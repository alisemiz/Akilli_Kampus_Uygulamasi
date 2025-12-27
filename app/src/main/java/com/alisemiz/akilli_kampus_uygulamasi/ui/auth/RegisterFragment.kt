package com.alisemiz.akilli_kampus_uygulamasi.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.alisemiz.akilli_kampus_uygulamasi.R
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

        binding.tvGoToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

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

            binding.btnRegister.isEnabled = false
            binding.btnRegister.text = "Kaydediliyor..."

            // 3. Firebase Auth ile Kullanıcı Oluştur
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { task ->
                    val userId = task.user!!.uid
                    val userRole = if (email.contains("admin")) "admin" else "user"

                    // --- DÜZELTİLEN KISIM BURASI ---
                    // User sınıfı yerine HashMap kullanarak etiketleri biz belirliyoruz.
                    // Profil sayfasının okuyabilmesi için 'department' verisini 'unit' anahtarına atıyoruz.

                    val userMap = hashMapOf(
                        "uid" to userId,
                        "name" to name,
                        "email" to email,
                        "unit" to department, // <-- İŞTE ÇÖZÜM: 'department' değil 'unit' olarak kaydediyoruz.
                        "role" to userRole
                    )

                    // 4. Firestore Veritabanına Kaydet
                    firestore.collection("users").document(userId).set(userMap)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Kayıt Başarılı!", Toast.LENGTH_LONG).show()
                            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                        }
                        .addOnFailureListener { e ->
                            binding.btnRegister.isEnabled = true
                            binding.btnRegister.text = "Kayıt Ol"
                            Toast.makeText(context, "Veritabanı Hatası: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { e ->
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