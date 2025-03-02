package com.example.myticket.bean

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Ticket(
    val id              : Long = 0,
    val ticketCaseId    : Long,
    var title           : String,
    var dateTime        : Date,
    var location        : String,
    var rating          : Float = 0f,
    var imagePath       : String? = null,
    val createdAt       : Date = Date()
) : Comparable<Ticket> {

    override fun compareTo(other: Ticket): Int {
        // 按创建时间降序排序（新的在前）
        return other.createdAt.compareTo(this.createdAt)
    }

    fun getFormattedDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(dateTime)
    }
}
