package com.example.myticket.bean

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TicketCase(
    val id: Long = 0,
    val title: String,
    val createDate: Date = Date(),
    val coverColor: Int,
    val isLandscape: Boolean,
    val ticketType: Int
) {
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(createDate)
    }
}
