package com.alisemiz.akilli_kampus_uygulamasi.ui.profile

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.alisemiz.akilli_kampus_uygulamasi.R
import com.alisemiz.akilli_kampus_uygulamasi.data.model.Incident
import com.alisemiz.akilli_kampus_uygulamasi.databinding.FragmentProfileBinding
import com.alisemiz.akilli_kampus_uygulamasi.ui.home.IncidentAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: IncidentAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Sayfa açıldığında verileri otomatik çekip ekrana basıyoruz.
        kullaniciBilgileriniGetir()
        setupRecyclerView()
        takipEdilenleriGetir()

        // Switch butonlarının mantığını çalıştırıyoruz.
        bildirimAyarlariniYonet()

        // ÇIKIŞ YAPMA İŞLEMİ
        binding.btnLogout.setOnClickListener {
            // 1. Firebase'den çıkış yap
            auth.signOut()

            // 2. Uygulamayı yeniden başlatarak Login ekranına at
            val intent = requireActivity().intent
            requireActivity().finish()
            startActivity(intent)
        }
    }

    private fun bildirimAyarlariniYonet() {
        // SharedPreferences (Hafıza) Erişim
        val sharedPref = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        // Uygulama açıldığında switch'lerin doğru konumda durmasını sağlar.
        binding.switchStatus.isChecked = sharedPref.getBoolean("pref_status_updates", true)
        binding.switchEmergency.isChecked = sharedPref.getBoolean("pref_emergency", true)

        // Kullanıcı butona bastığı an yeni durumu hafızaya yazar.
        binding.switchStatus.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("pref_status_updates", isChecked).apply()
        }

        binding.switchEmergency.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("pref_emergency", isChecked).apply()
        }
    }
    // ------------------------------------------------

    private fun kullaniciBilgileriniGetir() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (_binding != null && document.exists()) {
                    val email = document.getString("email")
                    val role = document.getString("role") ?: "Kullanıcı"
                    val unit = document.getString("unit") ?: "Birim Yok"

                    binding.tvUserEmail.text = email
                    binding.tvUserRole.text = role.uppercase()
                    binding.tvUnitInfo.text = unit
                }
            }
    }

    private fun setupRecyclerView() {
        adapter = IncidentAdapter(
            listOf(),
            onClick = { id ->
                val bundle = Bundle().apply { putString("incidentId", id) }
                try {
                    findNavController().navigate(R.id.incidentDetailFragment, bundle)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            onLongClick = {}
        )
        binding.rvFollowedIncidents.layoutManager = LinearLayoutManager(context)
        binding.rvFollowedIncidents.adapter = adapter
    }

    // Kullanıcının takip ettiği son 5 bildirimi Firestore'dan çekiyoruz.
    private fun takipEdilenleriGetir() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("incidents")
            .whereArrayContains("followers", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(5)
            .addSnapshotListener { value, error ->
                if (_binding == null) return@addSnapshotListener

                if (value != null && !value.isEmpty) {
                    val liste = mutableListOf<Incident>()
                    value.documents.forEach { doc ->
                        val incident = doc.toObject(Incident::class.java)?.copy(id = doc.id)
                        if (incident != null) liste.add(incident)
                    }
                    adapter.updateList(liste)
                    // Liste boş değilse UI'da ilgili kısımları göster.
                    binding.rvFollowedIncidents.visibility = View.VISIBLE
                    binding.tvEmptyState.visibility = View.GONE
                } else {
                    binding.rvFollowedIncidents.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.VISIBLE
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Bellek sızıntısı olmaması için binding'i null'a çekiyoruz.
        _binding = null
    }
}