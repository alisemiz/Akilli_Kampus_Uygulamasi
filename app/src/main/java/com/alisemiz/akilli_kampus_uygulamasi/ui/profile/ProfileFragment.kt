package com.alisemiz.akilli_kampus_uygulamasi.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.alisemiz.akilli_kampus_uygulamasi.MainActivity
import com.alisemiz.akilli_kampus_uygulamasi.R
import com.alisemiz.akilli_kampus_uygulamasi.data.model.Incident
import com.alisemiz.akilli_kampus_uygulamasi.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // DİKKAT: Artık NotificationAdapter kullanıyoruz
    private lateinit var adapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Başlangıçta gizle
        binding.tvFollowHeader.visibility = View.GONE
        binding.rvFollowedIncidents.visibility = View.GONE
        binding.tvEmptyState.visibility = View.GONE

        setupProfileAndRole()
        setupNotificationSettings()

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }
    }

    private fun setupProfileAndRole() {
        val currentUser = auth.currentUser
        val email = currentUser?.email ?: "Misafir"
        binding.tvUserEmail.text = email

        if (currentUser != null) {
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val role = document.getString("role") ?: "user"

                        if (role == "admin") {
                            // Admin Görünümü
                            binding.tvUserRole.text = "Rol: YÖNETİCİ (ADMIN)"
                            binding.tvUnitInfo.text = "Birim: Rektörlük / Güvenlik Merkezi"

                            // Admin için liste GİZLİ
                            binding.tvFollowHeader.visibility = View.GONE
                            binding.rvFollowedIncidents.visibility = View.GONE
                        } else {
                            // Kullanıcı Görünümü
                            binding.tvUserRole.text = "Rol: ÖĞRENCİ / PERSONEL"
                            binding.tvUnitInfo.text = "Birim: Mühendislik Fakültesi"

                            // Başlığı değiştiriyoruz: "Son Bildirimler"
                            binding.tvFollowHeader.text = "Son Bildirimler"
                            binding.tvFollowHeader.visibility = View.VISIBLE
                            binding.rvFollowedIncidents.visibility = View.VISIBLE

                            setupNotificationList()
                        }
                    }
                }
        }
    }

    private fun setupNotificationSettings() {
        val sharedPref = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()

        binding.switchStatusUpdates.isChecked = sharedPref.getBoolean("notify_status", true)
        binding.switchEmergency.isChecked = sharedPref.getBoolean("notify_emergency", true)

        binding.switchStatusUpdates.setOnCheckedChangeListener { _, isChecked ->
            editor.putBoolean("notify_status", isChecked).apply()
            editor.putBoolean("notifications_enabled", isChecked).apply()
            Toast.makeText(context, "Durum bildirimleri ${if(isChecked) "Açıldı" else "Kapatıldı"}", Toast.LENGTH_SHORT).show()
        }

        binding.switchEmergency.setOnCheckedChangeListener { _, isChecked ->
            editor.putBoolean("notify_emergency", isChecked).apply()
            Toast.makeText(context, "Acil durum bildirimleri ${if(isChecked) "Açıldı" else "Kapatıldı"}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupNotificationList() {
        val uid = auth.currentUser?.uid ?: return

        // Yeni Adapter'ı bağlıyoruz
        adapter = NotificationAdapter(
            listOf(),
            onClick = { selectedId ->
                // Tıklayınca yine detaya gitsin
                val bundle = Bundle().apply { putString("incidentId", selectedId) }
                findNavController().navigate(R.id.incidentDetailFragment, bundle)
            }
        )
        binding.rvFollowedIncidents.layoutManager = LinearLayoutManager(context)
        binding.rvFollowedIncidents.adapter = adapter

        // Takip ettiğim olayları çek
        db.collection("incidents")
            .whereArrayContains("followers", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener

                val list = mutableListOf<Incident>()
                value?.documents?.forEach { doc ->
                    doc.toObject(Incident::class.java)?.let {
                        list.add(it.copy(id = doc.id))
                    }
                }

                adapter.updateList(list)

                if (list.isEmpty()) {
                    binding.tvEmptyState.text = "Henüz bir bildirim yok."
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.rvFollowedIncidents.visibility = View.GONE
                } else {
                    binding.tvEmptyState.visibility = View.GONE
                    binding.rvFollowedIncidents.visibility = View.VISIBLE
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}