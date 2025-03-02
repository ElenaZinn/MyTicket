package com.example.myticket

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupWindow
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myticket.bean.Ticket
import com.example.myticket.bean.TicketCase
import com.example.myticket.preview.TicketCaseViewModel
import com.example.myticket.preview.TicketPreviewActivity
import com.google.gson.Gson
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: TicketCaseAdapter
    private val ticketCaseList = mutableListOf<TicketCase>()
    private lateinit var viewModel: TicketCaseViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化ViewModel
        viewModel = ViewModelProvider(this).get(TicketCaseViewModel::class.java)

        // 设置RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewTicketCases)
        recyclerView.layoutManager = GridLayoutManager(this, 2) // 两列网格布局

        adapter = TicketCaseAdapter(
            onTicketCaseClick = { ticketCase ->
                // 处理点击事件，打开票夹预览页面
                val intent = Intent(this, TicketPreviewActivity::class.java)
                intent.putExtra("TICKET_CASE_ID", ticketCase.id)
                startActivity(intent)
            },
            onDeleteClick = { ticketCase ->
                // 处理删除点击
                deleteTicketCase(ticketCase)
            }
        )

        recyclerView.adapter = adapter

        // 观察数据变化
        viewModel.ticketCases.observe(this) { cases ->
            adapter.submitList(cases)
        }

        // 添加按钮点击事件
        findViewById<ImageButton>(R.id.fab_add).setOnClickListener {
            showCreateTicketCaseDialog2()
        }

        // 加载票夹列表
        viewModel.loadTicketCases(this)
    }

    private fun createNewTicketCase(title: String, isLandscape: Boolean, ticketType: Int, coverColor: Int) {
        // 创建新票夹并保存
        val newId = System.currentTimeMillis()
        // 创建新票夹并通过ViewModel保存
        val newTicketCase = TicketCase(
            id = newId, // ViewModel会生成ID
            title = title,
            createDate = Date(),
            coverColor = coverColor,
            isLandscape = isLandscape,
            ticketType = ticketType
        )

        // 通过ViewModel保存
        viewModel.saveTicketCase(this, newTicketCase)

        // 保存到数据库或本地存储
        saveTickerCase(newTicketCase)

        // 更新列表
        ticketCaseList.add(newTicketCase)
        adapter.updateTicketCases(ticketCaseList)

    }

    fun saveTickerCase(ticketCase: TicketCase) {
        val sharedPrefs = getSharedPreferences("ticket_prefs", Context.MODE_PRIVATE)
        val gson = Gson()

        // 生成唯一ID（如果需要）
        val caseId = ticketCase.id ?: generateUniqueId()
        val ticketCaseWithId = ticketCase.copy(id = caseId)

        // 保存票据分类
        sharedPrefs.edit().putString("ticket_case_$caseId", gson.toJson(ticketCaseWithId)).apply()

        // 更新分类ID列表
        val caseIdsSet = sharedPrefs.getStringSet("all_case_ids", mutableSetOf()) ?: mutableSetOf()
        val newCaseIdsSet = caseIdsSet.toMutableSet()
        newCaseIdsSet.add(caseId.toString())
        sharedPrefs.edit().putStringSet("all_case_ids", newCaseIdsSet).apply()

        // 从LocalDate获取Date对象
        val today = LocalDate.now()
        val date = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant())


    }

    private fun saveEmptyTicket(ticket: Ticket) {
        val sharedPrefs = getSharedPreferences("ticket_prefs", Context.MODE_PRIVATE)
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

    // 生成唯一ID的辅助方法
    private fun generateUniqueId(): Long {
        val sharedPrefs = getSharedPreferences("ticket_prefs", Context.MODE_PRIVATE)
        val currentId = sharedPrefs.getInt("last_id", 0)
        val newId = currentId + 1
        sharedPrefs.edit().putInt("last_id", newId).apply()
        return newId.toLong()
    }

    private fun showCreateTicketCaseDialog2() {
        // 创建自定义对话框布局
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_ticket_case, null)

        val etTitle = dialogView.findViewById<EditText>(R.id.etTicketCaseTitle)
        val rgOrientation = dialogView.findViewById<RadioGroup>(R.id.rgScreenOrientation)
//        val rgType = dialogView.findViewById<RadioGroup>(R.id.rgTicketType)
        val colorSelector = dialogView.findViewById<ColorSelector>(R.id.colorSelector)

        // 获取屏幕宽度
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        // 计算16dp对应的像素值
        val marginInPixels = (16 * displayMetrics.density).toInt()

        // 计算对话框宽度(屏幕宽度减去两侧边距)
        val dialogWidth = screenWidth - (2 * marginInPixels)

        // 测量视图获取实际尺寸
        dialogView.measure(
            View.MeasureSpec.makeMeasureSpec(dialogWidth, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val dialogHeight = dialogView.measuredHeight

        // 创建 PopupWindow，设置固定宽度以确保边距
        val popupWindow = PopupWindow(
            dialogView,
            dialogWidth,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        // 设置背景和动画
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.animationStyle = R.style.PopupAnimation

        // 确保FAB已完成布局后再显示PopupWindow
        findViewById<ImageButton>(R.id.fab_add).post {
            val location = IntArray(2)
            findViewById<ImageButton>(R.id.fab_add).getLocationOnScreen(location)

            // 计算位置：水平居中(考虑边距)，垂直位置确保不遮挡FAB
            val x = marginInPixels // 左边距为16dp

            // 在FAB上方保留足够间距，确保不遮挡
            val safetyMargin = resources.getDimensionPixelSize(R.dimen.popup_margin) // 32dp
            val y = location[1] - dialogHeight - safetyMargin

            // 显示PopupWindow
            popupWindow.showAtLocation(findViewById<ImageButton>(R.id.fab_add), Gravity.NO_GRAVITY, x, y)
        }

        // 设置按钮点击事件
        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            popupWindow.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnConfirm).setOnClickListener {
            val title = etTitle.text.toString()
            if (title.isEmpty()) {
                Toast.makeText(this, "请输入票夹名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val isLandscape = rgOrientation.checkedRadioButtonId == R.id.rbLandscape
//            val ticketType = if (rgType.checkedRadioButtonId == R.id.rbType1) 1 else 2
            val coverColor = colorSelector.getSelectedColor()

            createNewTicketCase(title, isLandscape, 1, coverColor)
            popupWindow.dismiss()
        }
    }


    private fun deleteTicketCase(ticketCase: TicketCase) {
        // 确认对话框
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除\"${ticketCase.title}\"吗？这将删除该分类下的所有票据。")
            .setPositiveButton("删除") { _, _ ->
                // 通过ViewModel执行删除操作
                viewModel.deleteTicketCase(this, ticketCase)

                Toast.makeText(this, "已删除票据分类", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteTicketCaseFromStorage(ticketCase: TicketCase) {
        // 使用SharedPreferences删除数据
        val sharedPrefs = getSharedPreferences("ticket_prefs", Context.MODE_PRIVATE)
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


}
