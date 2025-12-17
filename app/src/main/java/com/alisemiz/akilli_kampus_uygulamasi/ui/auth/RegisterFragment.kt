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

    // Firebase araçlarını tanımlıyoruz
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

        // Firebase'i başlat
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

            // 2. Boş alan kontrolü
            if (name.isEmpty() || department.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Lütfen tüm alanları doldurun!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 3. Şifre uzunluğu kontrolü (Firebase en az 6 ister)
            if (password.length < 6) {
                Toast.makeText(context, "Şifre en az 6 karakter olmalı!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }



            // 4. Butonu Kilitle (Çift tıklamayı ve 'email already in use' hatasını önler)
            binding.btnRegister.isEnabled = false
            binding.btnRegister.text = "Kaydediliyor..." // Kullanıcıya bilgi ver

            // 5. Firebase ile Kayıt İşlemi
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { task ->
                    // Kayıt başarılı oldu, şimdi detayları Firestore'a kaydedelim
                    val userId = task.user!!.uid

                    // User nesnesini oluştur
                    val newUser = User(
                        uid = userId,
                        name = name,
                        email = email,
                        department = department,
                        role = "user" // Varsayılan rol
                    )

                    // Firestore 'users' koleksiyonuna kaydet
                    firestore.collection("users").document(userId).set(newUser)
                        .addOnSuccessListener {
                            // --- BAŞARILI ---
                            Toast.makeText(context, "Kayıt Başarılı!", Toast.LENGTH_LONG).show()

                            // Giriş ekranına yönlendir
                            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)

                            // Not: Sayfa değiştiği için butonu açmaya gerek yok ama temizlik açısından:
                            // binding.btnRegister.isEnabled = true
                        }
                        .addOnFailureListener { e ->
                            // --- FIRESTORE HATASI ---
                            // Eğer veritabanına yazarken hata olursa butonu tekrar açmalıyız
                            binding.btnRegister.isEnabled = true
                            binding.btnRegister.text = "Kayıt Ol" // Eski metne dön

                            Toast.makeText(context, "Veritabanı Hatası: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { e ->
                    // --- AUTH (KAYIT) HATASI ---
                    // E-posta zaten varsa veya internet yoksa burası çalışır.
                    // Butonu tekrar aktif et ki kullanıcı düzeltebilsin.
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text = "Kayıt Ol" // Eski metne dön

                    Toast.makeText(context, "Kayıt Başarısız: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}