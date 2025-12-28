package com.alisemiz.akilli_kampus_uygulamasi.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.alisemiz.akilli_kampus_uygulamasi.R
import com.alisemiz.akilli_kampus_uygulamasi.databinding.FragmentAdminHomeBinding
import com.google.firebase.auth.FirebaseAuth

class AdminHomeFragment : Fragment() {

    // ViewBinding yapısını fragment içerisinde güvenli kullanmak için bu şekilde tanımladım.
    private var _binding: FragmentAdminHomeBinding? = null
    private val binding get() = _binding!!

    // Admin çıkış işlemleri için Firebase Auth nesnesine ihtiyacım var.
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // XML tasarımını (layout) koda bağlıyoruz.
        _binding = FragmentAdminHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Firebase örneğini burada başlattım.
        auth = FirebaseAuth.getInstance()

        // Bu buton ile yönetici panelinden güvenli bir şekilde çıkış yapılmasını sağladım.
        binding.btnAdminLogout.setOnClickListener {
            auth.signOut()

            // Kullanıcıyı tekrar login ekranına yönlendirerek yetkisiz erişimi engelliyoruz.
            findNavController().navigate(R.id.loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Fragment'ın görünümü yok edildiğinde binding nesnesini temizliyorum.
        _binding = null
    }
}