package com.example.myticket.preview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.myticket.R
import com.example.myticket.bean.Ticket

class TicketPagerAdapter(
    private var tickets: List<Ticket>,
    private val onTicketClick: (Ticket) -> Unit,
    private val onDeleteClick: (Ticket, Int) -> Unit // 添加删除回调
) : RecyclerView.Adapter<TicketPagerAdapter.TicketViewHolder>() {


    // 添加变量控制蒙版可见性
    private var isOverlayVisible = true

    // 设置蒙版可见性的方法
    fun setOverlayVisibility(visible: Boolean) {
        this.isOverlayVisible = visible
    }


    class TicketViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.ivTicketImage)
        val titleTextView: TextView = itemView.findViewById(R.id.tvTitle)
        val dateTimeTextView: TextView = itemView.findViewById(R.id.tvDateTime)
        val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBar)
        val deleteButton: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ticket_case2, parent, false)

        // 应用边距，使每个项目有足够的空间显示效果
        val layoutParams = view.layoutParams
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        view.layoutParams = layoutParams

        return TicketViewHolder(view)
    }

    // 在 TicketPagerAdapter 中添加
    override fun onBindViewHolder(holder: TicketViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.isNotEmpty() && payloads[0] == "UPDATE_OVERLAY") {
            // 只更新蒙版可见性，不重新绑定整个视图
            val overlayView = holder.itemView.findViewById<View>(R.id.overlayView)
            overlayView.visibility = if (isOverlayVisible) View.VISIBLE else View.INVISIBLE
        } else {
            // 完整绑定
            super.onBindViewHolder(holder, position, payloads)
        }
    }


    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        val ticket = tickets[position]

        // 动态设置蒙版高度为屏幕高度的 48.2%
        val overlayView = holder.itemView.findViewById<LinearLayout>(R.id.overlayView)
        overlayView.visibility = if (isOverlayVisible) View.VISIBLE else View.INVISIBLE
        val params = overlayView.layoutParams
        val screenHeight = holder.itemView.resources.displayMetrics.heightPixels
        params.height = (screenHeight * 0.15).toInt()  // 改回原来的48.2%
        overlayView.layoutParams = params


        // 设置票据图片
        if (!ticket.imagePath.isNullOrEmpty()) {
            // Clear existing image first
            holder.imageView.setImageDrawable(null)

            Glide.with(holder.imageView.context)
                .load(ticket.imagePath)
                .centerCrop()
                .override(1024, 1024)  // Higher resolution
                .fitCenter()           // Better scaling
                .diskCacheStrategy(DiskCacheStrategy.ALL)  // Better caching
                .error(R.drawable.placeholder_ticket)
                .into(holder.imageView)

        } else {
            holder.imageView.setImageResource(R.drawable.placeholder_ticket)
        }

        // 设置票据信息
        holder.titleTextView.text = ticket.title
        holder.dateTimeTextView.text = ticket.getFormattedDateTime()
        holder.ratingBar.rating = ticket.rating

        // 设置删除按钮点击事件
        holder.deleteButton.setOnClickListener {
            onDeleteClick(ticket, position)
        }

        // 设置整个项目的点击事件
        holder.itemView.setOnClickListener {
            onTicketClick(ticket)
        }
    }

    override fun getItemCount() = tickets.size

    fun updateTickets(newTickets: List<Ticket>) {
        // 创建全新的列表实例以确保引用变更
        val oldList = ArrayList(tickets)
        val newList = ArrayList(newTickets)

        // 使用 DiffUtil 计算差异
        val diffResult = DiffUtil.calculateDiff(TicketDiffCallback(oldList, newList))

        // 更新数据源
        tickets = newList

        // 应用更新
        diffResult.dispatchUpdatesTo(this)

        // 如果 DiffUtil 无效，可以使用这个方法强制刷新
        // notifyDataSetChanged()
    }



    private class TicketDiffCallback(
        private val oldList: List<Ticket>,
        private val newList: List<Ticket>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
