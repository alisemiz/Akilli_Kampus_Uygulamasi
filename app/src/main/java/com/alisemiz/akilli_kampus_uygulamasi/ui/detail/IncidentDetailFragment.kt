package com.alisemiz.akilli_kampus_uygulamasi.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.alisemiz.akilli_kampus_uygulamasi.databinding.FragmentIncidentDetailBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class IncidentDetailFragment : Fragment() {

    private var _binding: FragmentIncidentDetailBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIncidentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Gönderilen ID'yi yakala (Arguments üzerinden)
        val incidentId = arguments?.getString("incidentId")

        if (incidentId != null) {
            verileriGetir(incidentId)
        } else {
            Toast.makeText(context, "Hata: Olay ID bulunamadı!", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack() // Geri dön
        }

        // Geri butonu
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun verileriGetir(id: String) {
        db.collection("incidents").document(id).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Verileri çek
                    val title = document.getString("title")
                    val description = document.getString("description")
                    val type = document.getString("type")
                    val status = document.getString("status")
                    val timestamp = document.getTimestamp("timestamp")

                    // Ekrana yaz
                    binding.tvDetailTitle.text = title
                    binding.tvDetailDescription.text = description
                    binding.tvDetailType.text = "Tür: $type"
                    binding.tvDetailStatus.text = status ?: "Bilinmiyor"

                    // Tarihi formatla
                    if (timestamp != null) {
                        val date = timestamp.toDate()
                        val format = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("tr", "TR"))
                        binding.tvDetailDate.text = format.format(date)
                    }
                } else {
                    Toast.makeText(context, "Bu olay silinmiş olabilir.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Veri çekilemedi: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}