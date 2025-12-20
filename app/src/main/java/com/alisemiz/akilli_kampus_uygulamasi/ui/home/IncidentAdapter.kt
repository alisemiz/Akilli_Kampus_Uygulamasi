package com.alisemiz.akilli_kampus_uygulamasi.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alisemiz.akilli_kampus_uygulamasi.data.model.Incident
import com.alisemiz.akilli_kampus_uygulamasi.databinding.ItemIncidentBinding
import java.text.SimpleDateFormat
import java.util.Locale

class IncidentAdapter(
    private var incidentList: List<Incident>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<IncidentAdapter.IncidentViewHolder>() {

    class IncidentViewHolder(val binding: ItemIncidentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncidentViewHolder {
        val binding = ItemIncidentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return IncidentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IncidentViewHolder, position: Int) {
        val incident = incidentList[position]

        holder.binding.tvTitle.text = incident.title
        holder.binding.tvDescription.text = incident.description
        holder.binding.tvStatus.text = incident.status

        if (incident.timestamp != null) {
            val date = incident.timestamp!!.toDate()
            val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR"))
            holder.binding.tvDate.text = format.format(date)
        }

        // --- İKON DÜZELTMESİ YAPILDI ---
        // 'ic_fire' yerine 'ic_dialog_alert' (Ünlem) kullandık.
        // 'ic_secure' yerine 'ic_lock_lock' (Kilit) kullandık.
        // Bunlar her Android cihazda %100 vardır.
        val iconRes = when (incident.type) {
            "Yangın" -> android.R.drawable.ic_dialog_alert // Kırmızı Ünlem
            "Sağlık" -> android.R.drawable.ic_menu_add    // Artı İşareti (Haç gibi)
            "Güvenlik" -> android.R.drawable.ic_lock_lock // Kilit
            "Teknik" -> android.R.drawable.ic_menu_manage // İngiliz Anahtarı
            else -> android.R.drawable.ic_dialog_info     // Bilgi (i)
        }

        holder.binding.imgIcon.setImageResource(iconRes)

        holder.itemView.setOnClickListener {
            onClick(incident.id)
        }
    }

    override fun getItemCount(): Int = incidentList.size

    fun updateList(newList: List<Incident>) {
        incidentList = newList
        notifyDataSetChanged()
    }
}