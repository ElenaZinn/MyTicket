package com.example.myticket.preview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.myticket.R
import com.example.myticket.bean.Ticket

class ThumbnailAdapter(
    private var tickets: List<Ticket>,
    private val onThumbnailClick: (Int) -> Unit
) : RecyclerView.Adapter<ThumbnailAdapter.ThumbnailViewHolder>() {

    var selectedPosition = 0
        set(value) {
            val oldPosition = field
            field = value
            notifyItemChanged(oldPosition)
            notifyItemChanged(value)
        }

    class ThumbnailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.ivThumbnail)
        val selectedBorder: View = itemView.findViewById(R.id.selectedBorder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThumbnailViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_thumbnail, parent, false)
        return ThumbnailViewHolder(view)
    }

    override fun onBindViewHolder(holder: ThumbnailViewHolder, position: Int) {
        val ticket = tickets[position]

        // 设置缩略图
        if (!ticket.imagePath.isNullOrEmpty()) {
            Glide.with(holder.imageView.context)
                .load(ticket.imagePath)
                .centerCrop()
                .override(1024, 1024)  // Higher resolution
                .fitCenter()           // Better scaling
                .diskCacheStrategy(DiskCacheStrategy.ALL)  // Better caching
                .error(R.drawable.placeholder_ticket)
                .into(holder.imageView)

        } else {
            holder.imageView.setImageResource(R.drawable.placeholder_thumbnail)
        }

        // 设置选中状态
        holder.selectedBorder.visibility = if (position == selectedPosition) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            if (position != selectedPosition) {
                onThumbnailClick(position)
            }
        }
    }

    override fun getItemCount() = tickets.size

    fun updateTickets(newTickets: List<Ticket>) {
        tickets = newTickets
        notifyDataSetChanged()
    }
}
