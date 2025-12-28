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

    // ViewBinding kullanarak arayüz elemanlarına daha güvenli bir şekilde erişiyoruz
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    // Firebase Auth ve Firestore nesnelerimizi tanımlıyoruz
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

        // Firebase servislerini burada ayağa kaldırıyoruz
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Zaten hesabı olanların giriş ekranına geri dönebilmesi için
        binding.tvGoToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        // Kayıt Ol butonuna basıldığında yapılacak işlemler
        binding.btnRegister.setOnClickListener {
            val name = binding.etRegisterName.text.toString().trim()
            val department = binding.etRegisterDepartment.text.toString().trim()
            val email = binding.etRegisterEmail.text.toString().trim()
            val password = binding.etRegisterPassword.text.toString().trim()

            // Kullanıcının hiçbir alanı boş bırakmamasını sağlıyoruz
            if (name.isEmpty() || department.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Lütfen tüm alanları doldurun!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Firebase şifre için en az 6 karakter şartı koşuyoruz
            if (password.length < 6) {
                Toast.makeText(context, "Şifre en az 6 karakter olmalı!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // İşlem başlarken butonu pasif yapıp kullanıcıya bilgi veriyoruz
            binding.btnRegister.isEnabled = false
            binding.btnRegister.text = "Kaydediliyor..."

            // Firebase Auth ile yeni kullanıcı hesabını oluşturuyoruz
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { task ->
                    val userId = task.user!!.uid
                    // Eğer e-posta içinde "admin" geçiyorsa otomatik yetki veriyoruz
                    val userRole = if (email.contains("admin")) "admin" else "user"

                    // Profil sayfasında verileri okurken sorun yaşamamak için 'department' verisini
                    // veritabanına 'unit' anahtarıyla kaydediyoruz.
                    val userMap = hashMapOf(
                        "uid" to userId,
                        "name" to name,
                        "email" to email,
                        "unit" to department,
                        "role" to userRole
                    )

                    // Auth başarılı olduktan sonra ek bilgileri Firestore'a yazıyoruz
                    firestore.collection("users").document(userId).set(userMap)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Kayıt Başarılı!", Toast.LENGTH_LONG).show()
                            // Kayıt bittikten sonra kullanıcıyı giriş yapması için login ekranına atıyoruz
                            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                        }
                        .addOnFailureListener { e ->
                            // Veritabanı hatası alırsak butonu tekrar açıyoruz
                            binding.btnRegister.isEnabled = true
                            binding.btnRegister.text = "Kayıt Ol"
                            Toast.makeText(context, "Veritabanı Hatası: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { e ->
                    // Auth kaydı başarısız olursa (örneğin mail zaten varsa) kullanıcıyı bilgilendiriyoruz
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text = "Kayıt Ol"
                    Toast.makeText(context, "Kayıt Başarısız: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Uygulamanın performansını korumak ve memory leak oluşmaması için binding'i temizliyoruz
        _binding = null
    }
}