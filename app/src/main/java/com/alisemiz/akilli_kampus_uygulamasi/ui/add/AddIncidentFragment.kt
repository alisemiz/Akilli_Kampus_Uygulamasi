package com.alisemiz.akilli_kampus_uygulamasi.ui.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.alisemiz.akilli_kampus_uygulamasi.databinding.FragmentAddIncidentBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class AddIncidentFragment : Fragment() {

    private var _binding: FragmentAddIncidentBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddIncidentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinner()

        binding.btnSubmit.setOnClickListener {
            gonder()
        }
    }

    private fun setupSpinner() {
        // Spinner için seçenekler
        val types = arrayOf("Genel", "Yangın", "Sağlık", "Güvenlik", "Teknik")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, types)
        binding.spinnerType.adapter = adapter
    }

    private fun gonder() {
        val title = binding.etIncidentTitle.text.toString().trim()
        val desc = binding.etIncidentDesc.text.toString().trim()
        val type = binding.spinnerType.selectedItem.toString()

        if (title.isEmpty() || desc.isEmpty()) {
            Toast.makeText(context, "Lütfen başlık ve açıklama giriniz.", Toast.LENGTH_SHORT).show()
            return
        }

        // Firestore'a gidecek veri paketi
        val incidentData = hashMapOf(
            "title" to title,
            "description" to desc,
            "type" to type,
            "status" to "Açık", // Yeni açılan olay her zaman "Açık"tır
            "timestamp" to Timestamp(Date()), // Şu anki zaman
            "userId" to (auth.currentUser?.uid ?: "Anonymous"), // Gönderen kişi
            // Harita bozulmasın diye varsayılan koordinat (0,0 veya okul merkezi)
            // İstersen buraya okulunun koordinatlarını elle yazabilirsin.
            "latitude" to 39.92077,
            "longitude" to 32.85411
        )

        binding.btnSubmit.isEnabled = false // Çift tıklamayı önle
        binding.btnSubmit.text = "Gönderiliyor..."

        db.collection("incidents")
            .add(incidentData)
            .addOnSuccessListener {
                Toast.makeText(context, "Bildirim başarıyla gönderildi!", Toast.LENGTH_LONG).show()
                findNavController().popBackStack() // Ana sayfaya dön
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnSubmit.isEnabled = true
                binding.btnSubmit.text = "BİLDİRİMİ GÖNDER"
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}