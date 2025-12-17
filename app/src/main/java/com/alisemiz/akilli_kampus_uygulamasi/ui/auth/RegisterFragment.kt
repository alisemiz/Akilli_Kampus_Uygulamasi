package com.alisemiz.akilli_kampus_uygulamasi.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.alisemiz.akilli_kampus_uygulamasi.R
import com.alisemiz.akilli_kampus_uygulamasi.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // "Zaten hesabın var mı? Giriş Yap" yazısına tıklanınca giriş ekranına geri dön
        binding.tvGoToLogin.setOnClickListener {
            // NavGraph içindeki geri dönüş oku ID'si
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        // Kayıt butonu (Şimdilik boş)
        binding.btnRegister.setOnClickListener {
            // Buraya Firebase kayıt kodları gelecek
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}