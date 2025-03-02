package com.example.myticket

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.myticket.bean.TicketCase

class TicketCaseAdapter(
    private val onDeleteClick: (TicketCase) -> Unit,
    private val onTicketCaseClick: (TicketCase) -> Unit
) : RecyclerView.Adapter<TicketCaseAdapter.TicketCaseViewHolder>() {

    private var ticketCases: List<TicketCase> = listOf()


    class TicketCaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val coverView: View = itemView.findViewById(R.id.viewCoverColor)
        val titleTextView: TextView = itemView.findViewById(R.id.tvTitle)
        val dateTextView: TextView = itemView.findViewById(R.id.tvCreateDate)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketCaseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ticket_case, parent, false)
        return TicketCaseViewHolder(view)
    }

    override fun onBindViewHolder(holder: TicketCaseViewHolder, position: Int) {
        val ticketCase = ticketCases[position]

        holder.coverView.setBackgroundColor(ticketCase.coverColor)
        holder.titleTextView.text = ticketCase.title
        holder.dateTextView.text = ticketCase.getFormattedDate()

        holder.itemView.setOnClickListener {
            onTicketCaseClick(ticketCase)
        }

        // 设置删除按钮的点击事件
        holder.btnDelete.setOnClickListener {
            onDeleteClick(ticketCase)
        }
    }

    override fun getItemCount() = ticketCases.size

    fun updateTicketCases(newTicketCases: List<TicketCase>) {
        val diffCallback = TicketCaseDiffCallback(ticketCases, newTicketCases)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        ticketCases = newTicketCases
        diffResult.dispatchUpdatesTo(this)
    }

    // 更新数据方法
    fun submitList(newList: List<TicketCase>) {
        ticketCases = newList
        notifyDataSetChanged()
    }


    private class TicketCaseDiffCallback(
        private val oldList: List<TicketCase>,
        private val newList: List<TicketCase>
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
