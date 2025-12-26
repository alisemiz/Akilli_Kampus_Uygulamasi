package com.alisemiz.akilli_kampus_uygulamasi.ui.home

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alisemiz.akilli_kampus_uygulamasi.R
import com.alisemiz.akilli_kampus_uygulamasi.data.model.Incident
import com.alisemiz.akilli_kampus_uygulamasi.databinding.ItemIncidentBinding
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Locale

class IncidentAdapter(
    private var incidentList: List<Incident>,
    private val onClick: (String) -> Unit,       // Normal Tıklama (Detay)
    private val onLongClick: (String) -> Unit    // Uzun Tıklama (Silme)
) : RecyclerView.Adapter<IncidentAdapter.IncidentViewHolder>() {

    class IncidentViewHolder(val binding: ItemIncidentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncidentViewHolder {
        val binding = ItemIncidentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return IncidentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IncidentViewHolder, position: Int) {
        val incident = incidentList[position]
        val context = holder.itemView.context

        // 1. Temel Metin Bilgileri
        holder.binding.tvTitle.text = incident.title
        holder.binding.tvType.text = incident.type
        holder.binding.tvStatus.text = incident.status

        // 2. Tarih Formatlama
        val sdf = SimpleDateFormat("dd MMM HH:mm", Locale("tr"))
        val dateString = incident.timestamp?.toDate()?.let { sdf.format(it) } ?: ""
        holder.binding.tvDate.text = " • $dateString"

        // 3. Durum Rozeti (Badge) Renklendirme
        val backgroundDrawable = holder.binding.tvStatus.background as? GradientDrawable

        val colorCode = when (incident.status) {
            "Açık" -> "#D32F2F"        // Kırmızı
            "Çözüldü" -> "#388E3C"     // Yeşil
            "İnceleniyor" -> "#F57C00" // Turuncu
            "ACİL" -> "#B71C1C"        // Koyu Kırmızı
            else -> "#757575"          // Gri
        }

        backgroundDrawable?.setColor(Color.parseColor(colorCode))

        // 4. Görsel Yükleme (Glide ile)
        if (!incident.imageUrl.isNullOrEmpty()) {
            Glide.with(context)
                .load(incident.imageUrl)
                .centerCrop()
                // DÜZELTME BURADA: android.R.drawable kullandık
                .placeholder(android.R.drawable.ic_menu_camera)
                .error(android.R.drawable.ic_menu_camera)
                .into(holder.binding.ivIncidentIcon)
        } else {
            // DÜZELTME BURADA: Resim yoksa yine sistem ikonunu kullan
            holder.binding.ivIncidentIcon.setImageResource(android.R.drawable.ic_menu_camera)
        }

        // 5. Tıklama Olayları
        holder.itemView.setOnClickListener {
            if (incident.id.isNotEmpty()) {
                onClick(incident.id)
            }
        }

        holder.itemView.setOnLongClickListener {
            if (incident.id.isNotEmpty()) {
                onLongClick(incident.id)
                true
            } else {
                false
            }
        }
    }

    override fun getItemCount(): Int = incidentList.size

    fun updateList(newList: List<Incident>) {
        incidentList = newList
        notifyDataSetChanged()
    }
}