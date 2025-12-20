package com.alisemiz.akilli_kampus_uygulamasi.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.alisemiz.akilli_kampus_uygulamasi.R
import com.alisemiz.akilli_kampus_uygulamasi.data.model.Incident
import com.alisemiz.akilli_kampus_uygulamasi.databinding.FragmentNotificationsBinding
import com.alisemiz.akilli_kampus_uygulamasi.ui.home.IncidentAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: IncidentAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        takipEdilenleriGetir()
    }

    private fun setupRecyclerView() {
        // Mevcut IncidentAdapter'ı yeniden kullanıyoruz (Kod tekrarı yok!)
        adapter = IncidentAdapter(
            listOf(),
            onClick = { selectedId ->
                // Detaya git
                val bundle = Bundle().apply { putString("incidentId", selectedId) }
                findNavController().navigate(R.id.incidentDetailFragment, bundle)
            },
            onLongClick = {
                // Bildirim ekranında silme işlemi olmasın, sadece görüntüleme
            }
        )
        binding.rvNotifications.layoutManager = LinearLayoutManager(context)
        binding.rvNotifications.adapter = adapter
    }

    private fun takipEdilenleriGetir() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            binding.tvEmptyState.visibility = View.VISIBLE
            return
        }

        // SORGULAMA: 'followers' listesinde benim ID'm var mı?
        db.collection("incidents")
            .whereArrayContains("followers", uid)
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

                adapter.updateList(list)

                // Liste boşsa uyarı göster
                if (list.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.rvNotifications.visibility = View.GONE
                } else {
                    binding.tvEmptyState.visibility = View.GONE
                    binding.rvNotifications.visibility = View.VISIBLE
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}