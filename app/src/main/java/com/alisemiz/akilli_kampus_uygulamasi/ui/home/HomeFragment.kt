package com.alisemiz.akilli_kampus_uygulamasi.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
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
    private val db = FirebaseFirestore.getInstance()

    // Arama işlemi için tüm verileri tutan yedek liste
    private var tumOlaylar = listOf<Incident>()

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
        setupSearchView()
        verileriGetir()
    }

    private fun setupRecyclerView() {
        adapter = IncidentAdapter(listOf())
        binding.rvIncidents.layoutManager = LinearLayoutManager(context)
        binding.rvIncidents.adapter = adapter
    }

    private fun setupSearchView() {
        // Rubrik: Başlık ve açıklama içinde anahtar kelime araması yapılabilir
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                val arananKelime = newText.orEmpty().lowercase()
                val filtrelenmisListe = if (arananKelime.isEmpty()) {
                    tumOlaylar
                } else {
                    tumOlaylar.filter {
                        it.title.lowercase().contains(arananKelime) ||
                                it.description.lowercase().contains(arananKelime)
                    }
                }
                adapter.updateList(filtrelenmisListe)
                return true
            }
        })
    }

    private fun verileriGetir() {
        // Rubrik: Liste kronolojik olarak (en yeni üstte) sıralanır
        db.collection("incidents")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Toast.makeText(context, "Hata: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val list = mutableListOf<Incident>()
                value?.documents?.forEach { doc ->
                    doc.toObject(Incident::class.java)?.let {
                        list.add(it.copy(id = doc.id))
                    }
                }
                tumOlaylar = list
                adapter.updateList(tumOlaylar)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}