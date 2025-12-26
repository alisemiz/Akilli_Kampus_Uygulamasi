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
        // Burada da aynı Adapter'ı kullanıyoruz
        adapter = IncidentAdapter(
            listOf(),
            onClick = { id ->
                val bundle = Bundle().apply { putString("incidentId", id) }
                findNavController().navigate(R.id.action_notificationsFragment_to_incidentDetailFragment, bundle)
            },
            onLongClick = {
                // Takip listesinde uzun basınca bir şey yapmasın veya silme sorusu sorsun
                // Şimdilik boş bırakıyoruz
            }
        )
        binding.rvNotifications.layoutManager = LinearLayoutManager(context)
        binding.rvNotifications.adapter = adapter
    }

    private fun takipEdilenleriGetir() {
        val uid = auth.currentUser?.uid ?: return

        // SORGUNUN MANTIĞI: 'followers' dizisi içinde 'uid' olanları getir
        db.collection("incidents")
            .whereArrayContains("followers", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (_binding == null) return@addSnapshotListener

                if (error != null) {
                    Toast.makeText(context, "Hata: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val liste = mutableListOf<Incident>()
                value?.documents?.forEach { doc ->
                    val incident = doc.toObject(Incident::class.java)?.copy(id = doc.id)
                    if (incident != null) liste.add(incident)
                }

                adapter.updateList(liste)

                // Boşluk Kontrolü
                if (liste.isEmpty()) {
                    binding.rvNotifications.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.VISIBLE
                } else {
                    binding.rvNotifications.visibility = View.VISIBLE
                    binding.layoutEmptyState.visibility = View.GONE
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}