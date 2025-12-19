package com.alisemiz.akilli_kampus_uygulamasi.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alisemiz.akilli_kampus_uygulamasi.data.model.Incident
import com.alisemiz.akilli_kampus_uygulamasi.databinding.ItemIncidentBinding
import java.text.SimpleDateFormat
import java.util.Locale

// Rubrik: Her bildirimin satırında tür ikonu, başlık, açıklama, zaman ve durum bulunur
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
            // Rubrik: Başlık ve Açıklama alanları
            tvTitle.text = item.title
            tvDescription.text = item.description

            // Rubrik: Durum alanı (Açık/İnceleniyor/Çözüldü)
            tvStatus.text = item.status

            // Rubrik: Oluşturulma zamanı formatlama
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            tvDate.text = item.timestamp?.toDate()?.let { sdf.format(it) } ?: ""

            // Not: Tür ikonunu (ivTypeIcon) sonraki aşamada dinamik yapacağız
        }
    }

    override fun getItemCount(): Int = list.size

    // Arama ve filtreleme için listeyi güncelleme fonksiyonu
    fun updateList(newList: List<Incident>) {
        list = newList
        notifyDataSetChanged()
    }
}