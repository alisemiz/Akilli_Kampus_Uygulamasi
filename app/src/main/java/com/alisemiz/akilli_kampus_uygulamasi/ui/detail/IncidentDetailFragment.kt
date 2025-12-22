package com.alisemiz.akilli_kampus_uygulamasi.ui.detail

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.alisemiz.akilli_kampus_uygulamasi.R
import com.alisemiz.akilli_kampus_uygulamasi.data.model.Incident
import com.alisemiz.akilli_kampus_uygulamasi.databinding.FragmentIncidentDetailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.Locale
import com.bumptech.glide.Glide


class IncidentDetailFragment : Fragment() {

    private var _binding: FragmentIncidentDetailBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var incidentId: String? = null
    private var isFollowing = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentIncidentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        incidentId = arguments?.getString("incidentId")

        if (incidentId != null) {
            verileriGetir(incidentId!!)
            rolKontroluYap()
            takipDurumunuKontrolEt()
        }

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnAdminSave.setOnClickListener { adminGuncellemeYap() }
        binding.btnFollow.setOnClickListener { takipIslemiYap() }
    }

    private fun verileriGetir(id: String) {
        db.collection("incidents").document(id).get()
            .addOnSuccessListener { document ->
                if (document.exists() && isAdded) {
                    val incident = document.toObject(Incident::class.java)
                    if (incident != null) {
                        binding.tvDetailTitle.text = incident.title
                        binding.etDetailDescription.setText(incident.description)
                        binding.tvDetailStatus.text = incident.status.uppercase()

                        //Görseli Glide ile Yükle
                        if (incident.imageUrl.isNotEmpty()) {
                            binding.ivDetailImage.visibility = View.VISIBLE
                            Glide.with(this).load(incident.imageUrl).into(binding.ivDetailImage)
                        }

                        //Haritayı Olay Konumuna Odakla
                        val point = GeoPoint(incident.latitude, incident.longitude)
                        binding.mapDetail.controller.setZoom(18.0)
                        binding.mapDetail.controller.setCenter(point)

                        val marker = Marker(binding.mapDetail)
                        marker.position = point
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        binding.mapDetail.overlays.clear()
                        binding.mapDetail.overlays.add(marker)
                        binding.mapDetail.invalidate()

                        incident.timestamp?.let {
                            val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR"))
                            binding.tvDetailDate.text = format.format(it.toDate())
                        }
                        setupSpinner(incident.status)
                    }
                }
            }
    }

    private fun rolKontroluYap() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            if (isAdded && _binding != null) {
                val role = doc.getString("role")
                if (role == "admin") {
                    binding.adminPanel.visibility = View.VISIBLE
                    binding.btnFollow.visibility = View.GONE
                    binding.etDetailDescription.isEnabled = true
                    binding.etDetailDescription.setBackgroundColor(Color.parseColor("#E3F2FD"))
                } else {
                    binding.adminPanel.visibility = View.GONE
                    binding.btnFollow.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupSpinner(currentStatus: String) {
        val statusList = arrayOf("Açık", "İnceleniyor", "Çözüldü")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, statusList)
        binding.spinnerStatusUpdate.adapter = adapter
        val index = statusList.indexOfFirst { it.equals(currentStatus, ignoreCase = true) }
        if (index >= 0) binding.spinnerStatusUpdate.setSelection(index)
    }

    private fun adminGuncellemeYap() {
        val yeniDurum = binding.spinnerStatusUpdate.selectedItem.toString()
        val yeniAciklama = binding.etDetailDescription.text.toString()
        db.collection("incidents").document(incidentId!!).update(mapOf("status" to yeniDurum, "description" to yeniAciklama))
            .addOnSuccessListener { Toast.makeText(context, "Olay güncellendi!", Toast.LENGTH_SHORT).show() }
    }

    private fun takipDurumunuKontrolEt() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("incidents").document(incidentId!!).get().addOnSuccessListener { doc ->
            if (isAdded && _binding != null) {
                val followers = doc.get("followers") as? List<String> ?: emptyList()
                isFollowing = followers.contains(uid)
                guncelleTakipButonu()
            }
        }
    }

    private fun guncelleTakipButonu() {
        if (isFollowing) {
            binding.btnFollow.text = "TAKİBİ BIRAK"
            binding.btnFollow.backgroundTintList = ColorStateList.valueOf(Color.GRAY)
        } else {
            binding.btnFollow.text = "TAKİP ET"
            binding.btnFollow.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
        }
    }

    private fun takipIslemiYap() {
        val uid = auth.currentUser?.uid ?: return
        val docRef = db.collection("incidents").document(incidentId!!)
        val action = if (isFollowing) FieldValue.arrayRemove(uid) else FieldValue.arrayUnion(uid)
        docRef.update("followers", action).addOnSuccessListener {
            isFollowing = !isFollowing
            guncelleTakipButonu()
            Toast.makeText(context, if (isFollowing) "Takip edildi" else "Takip bırakıldı", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}