package com.alisemiz.akilli_kampus_uygulamasi.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alisemiz.akilli_kampus_uygulamasi.data.model.Incident
import com.alisemiz.akilli_kampus_uygulamasi.databinding.ItemIncidentBinding
import java.text.SimpleDateFormat
import java.util.Locale

class IncidentAdapter(private var list: List<Incident>) :
    RecyclerView.Adapter<IncidentAdapter.IncidentViewHolder>() {

    class IncidentViewHolder(val binding: ItemIncidentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncidentViewHolder {
        val binding = ItemIncidentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return IncidentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IncidentViewHolder, position: Int) {
        val item = list[position]
        holder.binding.apply {
            // Verileri metin kutularına bağla [cite: 36, 37]
            tvTitle.text = item.title
            tvDescription.text = item.description
            tvStatus.text = item.status

            // Tarih formatlama [cite: 36]
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            tvDate.text = item.timestamp?.toDate()?.let { sdf.format(it) } ?: ""

            // Rubrik: Tür bazlı farklı ikon kullanımı
            when (item.type) {
                "Sağlık" -> ivTypeIcon.setImageResource(android.R.drawable.ic_menu_call)
                "Güvenlik" -> ivTypeIcon.setImageResource(android.R.drawable.ic_lock_lock)
                "Teknik" -> ivTypeIcon.setImageResource(android.R.drawable.ic_menu_preferences)
                "Çevre" -> ivTypeIcon.setImageResource(android.R.drawable.ic_menu_delete)
                else -> ivTypeIcon.setImageResource(android.R.drawable.ic_dialog_alert)
            }
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateList(newList: List<Incident>) {
        list = newList
        notifyDataSetChanged()
    }
}