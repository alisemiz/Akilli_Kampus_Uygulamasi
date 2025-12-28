package com.alisemiz.akilli_kampus_uygulamasi.ui.profile

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alisemiz.akilli_kampus_uygulamasi.data.model.Incident
import com.alisemiz.akilli_kampus_uygulamasi.databinding.ItemNotificationBinding
import java.text.SimpleDateFormat
import java.util.Locale

// Takip edilen olaylardaki güncellemeleri bildirim listesi olarak göstermek için bu adapter'ı tasarladım.
class NotificationAdapter(
    private var notificationList: List<Incident>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    // Tasarım dosyamızdaki (item_notification) bileşenlere erişmek için ViewHolder sınıfımız.
    class NotificationViewHolder(val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        // Her bir bildirim satırı için XML tasarımını koda bağlıyoruz.
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val incident = notificationList[position]

        holder.binding.tvNotifTitle.text = incident.title

        // Duruma göre mesaj ve renk ayarla
        when (incident.status) {
            "Çözüldü" -> {
                holder.binding.tvNotifMessage.text = "Bu olay çözüme kavuşturuldu."
                holder.binding.ivNotifIcon.setColorFilter(Color.parseColor("#4CAF50")) // Yeşil
            }
            "İnceleniyor" -> {
                holder.binding.tvNotifMessage.text = "Yetkililer şu an inceliyor."
                holder.binding.ivNotifIcon.setColorFilter(Color.parseColor("#FF9800")) // Turuncu
            }
            "ACİL" -> {
                holder.binding.tvNotifMessage.text = "⚠️ ACİL DURUM İLANI"
                holder.binding.ivNotifIcon.setColorFilter(Color.parseColor("#F44336")) // Kırmızı
            }
            else -> {
                holder.binding.tvNotifMessage.text = "Durum: ${incident.status}"
                holder.binding.ivNotifIcon.setColorFilter(Color.GRAY)
            }
        }

        // Firebase'den gelen tarih verisini "01 ocak 00:00" gibi bir hale getiriyoruz.
        val sdf = SimpleDateFormat("dd MMM HH:mm", Locale("tr"))
        holder.binding.tvNotifDate.text = incident.timestamp?.toDate()?.let { sdf.format(it) } ?: ""

        // Tıklayınca detaya git
        holder.itemView.setOnClickListener {
            onClick(incident.id)
        }
    }

    override fun getItemCount(): Int = notificationList.size
    // Veritabanından yeni bildirimler geldiğinde listeyi tazelemek için bu fonksiyonu kullanıyoruz.
    fun updateList(newList: List<Incident>) {
        notificationList = newList
        notifyDataSetChanged()
    }
}