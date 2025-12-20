package com.alisemiz.akilli_kampus_uygulamasi.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alisemiz.akilli_kampus_uygulamasi.data.model.Incident
import com.alisemiz.akilli_kampus_uygulamasi.databinding.ItemIncidentBinding
import java.text.SimpleDateFormat
import java.util.Locale

// GÜNCELLEME: onLongClick (Uzun basma) parametresi eklendi
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

        holder.binding.tvTitle.text = incident.title
        holder.binding.tvDescription.text = incident.description
        holder.binding.tvStatus.text = incident.status

        if (incident.timestamp != null) {
            val date = incident.timestamp!!.toDate()
            val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR"))
            holder.binding.tvDate.text = format.format(date)
        }

        val iconRes = when (incident.type) {
            "Yangın" -> android.R.drawable.ic_dialog_alert
            "Sağlık" -> android.R.drawable.ic_menu_add
            "Güvenlik" -> android.R.drawable.ic_lock_lock
            "Teknik" -> android.R.drawable.ic_menu_manage
            else -> android.R.drawable.ic_dialog_info
        }

        holder.binding.imgIcon.setImageResource(iconRes)

        // Normal Tıklama -> Detaya Git
        holder.itemView.setOnClickListener {
            onClick(incident.id)
        }

        // YENİ: Uzun Basma -> Silme İşlemini Tetikle
        holder.itemView.setOnLongClickListener {
            onLongClick(incident.id)
            true // true dönersek "tıklama" olayını iptal eder, sadece uzun basmayı çalıştırır
        }
    }

    override fun getItemCount(): Int = incidentList.size

    fun updateList(newList: List<Incident>) {
        incidentList = newList
        notifyDataSetChanged()
    }
}