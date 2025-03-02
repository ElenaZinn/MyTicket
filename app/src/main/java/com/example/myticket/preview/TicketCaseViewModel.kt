package com.example.myticket.preview

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myticket.bean.Ticket
import com.example.myticket.bean.TicketCase
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TicketCaseViewModel : ViewModel() {
    private val _ticketCases = MutableLiveData<List<TicketCase>>()
    val ticketCases: LiveData<List<TicketCase>> = _ticketCases

    // 加载所有票夹
    fun loadTicketCases(context: Context) {
        viewModelScope.launch {
            val cases = withContext(Dispatchers.IO) {
                loadTicketCasesFromStorage(context)
            }
            _ticketCases.value = cases
        }
    }

    // 从SharedPreferences加载数据
    private fun loadTicketCasesFromStorage(context: Context): List<TicketCase> {
        val sharedPrefs = context.getSharedPreferences("ticket_prefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val caseIdsSet = sharedPrefs.getStringSet("all_case_ids", mutableSetOf()) ?: mutableSetOf()
        val ticketCaseList = mutableListOf<TicketCase>()

        for (caseIdStr in caseIdsSet) {
            val caseId = caseIdStr.toLongOrNull() ?: continue
            val ticketCaseJson = sharedPrefs.getString("ticket_case_$caseId", null) ?: continue

            try {
                val ticketCase = gson.fromJson(ticketCaseJson, TicketCase::class.java)
                ticketCaseList.add(ticketCase)
            } catch (e: Exception) {
                Log.e("TicketCaseViewModel", "解析票据分类失败: ID=$caseId", e)
            }
        }

        return ticketCaseList
    }

    // 保存票夹
    fun saveTicketCase(context: Context, ticketCase: TicketCase) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val sharedPrefs = context.getSharedPreferences("ticket_prefs", Context.MODE_PRIVATE)
                val gson = Gson()

                // 生成唯一ID
                val caseId = ticketCase.id ?: generateUniqueId(context)
                val ticketCaseWithId = ticketCase.copy(id = caseId)

                // 保存票据分类
                sharedPrefs.edit().putString("ticket_case_$caseId", gson.toJson(ticketCaseWithId)).apply()

                // 更新分类ID列表
                val caseIdsSet = sharedPrefs.getStringSet("all_case_ids", mutableSetOf()) ?: mutableSetOf()
                val newCaseIdsSet = caseIdsSet.toMutableSet()
                newCaseIdsSet.add(caseId.toString())
                sharedPrefs.edit().putStringSet("all_case_ids", newCaseIdsSet).apply()
            }

            // 重新加载数据以更新UI
            loadTicketCases(context)
        }
    }

    // 删除票夹
    fun deleteTicketCase(context: Context, ticketCase: TicketCase) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val sharedPrefs = context.getSharedPreferences("ticket_prefs", Context.MODE_PRIVATE)
                val editor = sharedPrefs.edit()

                // 1. 获取该分类下所有票据ID
                val ticketIdsKey = "case_${ticketCase.id}_ticket_ids"
                val ticketIdsSet = sharedPrefs.getStringSet(ticketIdsKey, mutableSetOf()) ?: mutableSetOf()

                // 2. 删除每个票据
                for (ticketIdStr in ticketIdsSet) {
                    editor.remove("ticket_$ticketIdStr")
                }

                // 3. 删除票据ID集合
                editor.remove(ticketIdsKey)

                // 4. 从全局分类ID列表中移除
                val caseIdsSet = sharedPrefs.getStringSet("all_case_ids", mutableSetOf()) ?: mutableSetOf()
                val newCaseIdsSet = caseIdsSet.toMutableSet()
                newCaseIdsSet.remove(ticketCase.id.toString())
                editor.putStringSet("all_case_ids", newCaseIdsSet)

                // 5. 删除分类本身
                editor.remove("ticket_case_${ticketCase.id}")

                // 应用更改
                editor.apply()
            }

            // 重新加载数据以更新UI
            loadTicketCases(context)
        }
    }

    // 保存空票据
    private fun saveEmptyTicket(context: Context, ticket: Ticket) {
        val sharedPrefs = context.getSharedPreferences("ticket_prefs", Context.MODE_PRIVATE)
        val gson = Gson()

        // 保存票据
        sharedPrefs.edit().putString("ticket_${ticket.id}", gson.toJson(ticket)).apply()

        // 更新该分类下的票据ID列表
        val ticketIdsKey = "case_${ticket.ticketCaseId}_ticket_ids"
        val ticketIdsSet = sharedPrefs.getStringSet(ticketIdsKey, mutableSetOf()) ?: mutableSetOf()
        val newTicketIdsSet = ticketIdsSet.toMutableSet()
        newTicketIdsSet.add(ticket.id.toString())
        sharedPrefs.edit().putStringSet(ticketIdsKey, newTicketIdsSet).apply()
    }

    // 生成唯一ID
    private fun generateUniqueId(context: Context): Long {
        val sharedPrefs = context.getSharedPreferences("ticket_prefs", Context.MODE_PRIVATE)
        val currentId = sharedPrefs.getInt("last_id", 0)
        val newId = currentId + 1
        sharedPrefs.edit().putInt("last_id", newId).apply()
        return newId.toLong()
    }
}
