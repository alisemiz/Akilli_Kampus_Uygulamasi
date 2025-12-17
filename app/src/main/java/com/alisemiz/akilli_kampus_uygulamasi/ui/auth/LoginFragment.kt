package com.alisemiz.akilli_kampus_uygulamasi.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.alisemiz.akilli_kampus_uygulamasi.R
import com.alisemiz.akilli_kampus_uygulamasi.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    // Bu binding sayesinde XML'deki id'lere direkt ulaşacağız.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // "Hesabın yok mu? Kayıt Ol" yazısına tıklanınca kayıt ekranına git
        binding.tvGoToRegister.setOnClickListener {
            // NavGraph içindeki ok işaretinin (action) ID'si
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        // Giriş butonu (Şimdilik sadece tıklama dinleyicisi var)
        binding.btnLogin.setOnClickListener {
            // Buraya Firebase giriş kodları gelecek
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}