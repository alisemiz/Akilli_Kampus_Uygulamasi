package com.alisemiz.akilli_kampus_uygulamasi.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.alisemiz.akilli_kampus_uygulamasi.data.model.Incident
import com.alisemiz.akilli_kampus_uygulamasi.databinding.FragmentHomeBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: IncidentAdapter
    private val db = FirebaseFirestore.getInstance() // Firestore bağlantısı [cite: 121]

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        verileriGetir()
    }

    private fun setupRecyclerView() {
        adapter = IncidentAdapter(listOf()) // Başlangıçta boş liste
        binding.rvIncidents.layoutManager = LinearLayoutManager(context)
        binding.rvIncidents.adapter = adapter
    }

    private fun verileriGetir() {
        // Rubrik: Liste yukarıdan aşağıya kronolojik olarak sıralanabilir
        db.collection("incidents")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Toast.makeText(context, "Veri çekilemedi: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val incidentList = mutableListOf<Incident>()
                value?.documents?.forEach { document ->
                    val incident = document.toObject(Incident::class.java)
                    if (incident != null) {
                        // Firebase'den gelen döküman ID'sini modele set ediyoruz
                        incidentList.add(incident.copy(id = document.id))
                    }
                }
                adapter.updateList(incidentList) // Listeyi güncelle [cite: 35]
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}