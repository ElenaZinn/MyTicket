package com.example.myticket.preview

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.myticket.R
import com.example.myticket.bean.Ticket
import com.example.myticket.TicketAddNewOneActivity
import com.example.myticket.bean.TicketCase
import com.example.myticket.TicketDetailActivity
import com.google.gson.Gson
import java.util.Date
import kotlin.math.abs

class TicketPreviewActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var thumbnailRecyclerView: RecyclerView
    private lateinit var pagerAdapter: TicketPagerAdapter
    private lateinit var thumbnailAdapter: ThumbnailAdapter
    private lateinit var emptyTip: TextView

    private var ticketCaseId: Long = 0
    private val tickets = mutableListOf<Ticket>()

    // 添加变量控制蒙版显示状态
    private var isOverlayVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ticket_preview)


        ticketCaseId = intent.getLongExtra("TICKET_CASE_ID", 0)
        if (ticketCaseId == 0L) {
            Toast.makeText(this, "票夹ID无效", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        viewPager = findViewById(R.id.viewPagerTickets)
        emptyTip = findViewById(R.id.emptyTip)

        // 在 onCreate() 或者初始化 adapter 的地方修改
        pagerAdapter = TicketPagerAdapter(
            emptyList(),
            { ticket -> openTicketDetailScreen(ticket) },
            { ticket, position -> showDeleteConfirmation(ticket, position) }
        )
        viewPager.adapter = pagerAdapter

        viewPager.visibility = View.INVISIBLE
        emptyTip.visibility = View.VISIBLE

        // 初始化视图
        setupViews()

        // 加载票夹和票据数据
        loadTicketCase()
        loadTickets()
    }

    private fun setupViews() {
        // 设置标题栏
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<ImageButton>(R.id.btnAddTicket).setOnClickListener {
            openTicketEditScreen()
        }

        setupViewPager(tickets)
        fixViewPagerChildrenDrawingOrder()

        // 设置缩略图RecyclerView
        thumbnailRecyclerView = findViewById(R.id.recyclerViewThumbnails)
        thumbnailAdapter = ThumbnailAdapter(tickets) { position ->
            // 点击缩略图切换到对应票据
            viewPager.setCurrentItem(position, true)
        }
        thumbnailRecyclerView.adapter = thumbnailAdapter

        // 设置ViewPager页面改变监听
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // 更新缩略图选中状态
                thumbnailAdapter.selectedPosition = position

                // 确保选中的缩略图可见
                thumbnailRecyclerView.smoothScrollToPosition(position)

                // 更新箭头按钮状态
                updateArrowButtons(position)

                // 强制刷新当前页面及相邻页面的层级
                viewPager.post {
                    for (i in 0 until viewPager.childCount) {
                        val child = viewPager.getChildAt(i)
                        child?.invalidate()
                    }
                }
            }
        })

        val btnToggleOverlay = findViewById<ImageButton>(R.id.btnToggleOverlay)
        btnToggleOverlay.setOnClickListener {
            // 保存当前位置
            val currentPosition = viewPager.currentItem

            // 切换状态
            isOverlayVisible = !isOverlayVisible

            // 更新按钮图标
            updateOverlayButtonIcon()

            // 通知适配器更新
            pagerAdapter.setOverlayVisibility(isOverlayVisible)

            // 使用部分更新而不是完全刷新
            // 避免使用 notifyDataSetChanged() 以防止位置重置
            for (i in 0 until pagerAdapter.itemCount) {
                pagerAdapter.notifyItemChanged(i, "UPDATE_OVERLAY")
            }

            // 确保保持在当前页面位置
            viewPager.post {
                viewPager.setCurrentItem(currentPosition, false)
            }
        }


        // 设置左右箭头按钮
        findViewById<ImageButton>(R.id.btnPrevious).setOnClickListener {
            val currentPosition = viewPager.currentItem
            if (currentPosition > 0) {
                viewPager.setCurrentItem(currentPosition - 1, true)
            }
        }

        findViewById<ImageButton>(R.id.btnNext).setOnClickListener {
            val currentPosition = viewPager.currentItem
            if (currentPosition < tickets.size - 1) {
                viewPager.setCurrentItem(currentPosition + 1, true)
            }
        }

        // 设置ViewPager2的页面间距
        val pageMarginPx = resources.getDimensionPixelOffset(R.dimen.viewpager_page_margin)
        val offsetPx = resources.getDimensionPixelOffset(R.dimen.viewpager_page_offset)
        viewPager.setPageTransformer { page, position ->
            val viewPager = page.parent.parent as ViewPager2
            val offset = position * -(2 * offsetPx + pageMarginPx)

            if (viewPager.orientation == ViewPager2.ORIENTATION_HORIZONTAL) {
                if (ViewCompat.getLayoutDirection(viewPager) == ViewCompat.LAYOUT_DIRECTION_RTL) {
                    page.translationX = -offset
                } else {
                    page.translationX = offset
                }
            } else {
                page.translationY = offset
            }

            // 缩放效果
            val absPosition = abs(position)
            page.scaleY = 1 - (0.25f * absPosition)
            page.alpha = 0.25f + (1 - absPosition) * 0.75f
        }


    }

    private fun loadTicketCase() {
        // 从数据库加载票夹信息
        val sharedPrefs = getSharedPreferences("ticket_prefs", Context.MODE_PRIVATE)
        val gson = Gson()

        val ticketCaseJson = sharedPrefs.getString("ticket_case_$ticketCaseId", null)
        if (ticketCaseJson != null) {
            try {
                val ticketCase = gson.fromJson(ticketCaseJson, TicketCase::class.java)
                // 设置标题
                findViewById<TextView>(R.id.tvTitle).text = ticketCase?.title ?: "我的票夹"

            } catch (e: Exception) {
                Log.e("TicketPreview", "解析票据分类失败", e)
                Toast.makeText(this, "无法加载票据分类信息", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "未找到票据分类信息", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadTickets() {
        // 从数据库加载票据列表
        val sharedPrefs = getSharedPreferences("ticket_prefs", Context.MODE_PRIVATE)
        val gson = Gson()

        val ticketIdsKey = "case_${ticketCaseId}_ticket_ids"
        val ticketIdsSet = sharedPrefs.getStringSet(ticketIdsKey, mutableSetOf()) ?: mutableSetOf()

        tickets.clear() // 清空现有数据

        // 将 Set 转换为 List 并按 ID 排序（确保最新添加的票据在最后）
        val ticketIdsList = ticketIdsSet.toList().sortedBy { it.toLongOrNull() ?: 0L }

        for (ticketIdStr in ticketIdsList) {
            val ticketId = ticketIdStr.toLongOrNull() ?: continue
            val ticketJson = sharedPrefs.getString("ticket_$ticketId", null) ?: continue

            try {
                val ticket = gson.fromJson(ticketJson, Ticket::class.java)
                tickets.add(ticket)
            } catch (e: Exception) {
                Log.e("TicketPreview", "解析票据失败: ID=$ticketId", e)
            }
        }

        // 所有数据加载完成后，更新适配器
        pagerAdapter.updateTickets(tickets)
        thumbnailAdapter.updateTickets(tickets)

        // 初始化箭头按钮状态
        if (tickets.isNotEmpty()) {
            updateArrowButtons(0)
            viewPager.visibility = View.VISIBLE
            emptyTip.visibility = View.INVISIBLE
        } else {
            viewPager.visibility = View.INVISIBLE
            emptyTip.visibility = View.VISIBLE
            findViewById<ImageButton>(R.id.btnPrevious).isEnabled = false
            findViewById<ImageButton>(R.id.btnNext).isEnabled = false
        }
    }



    private fun updateArrowButtons(position: Int) {
        findViewById<ImageButton>(R.id.btnPrevious).isEnabled = position > 0
        findViewById<ImageButton>(R.id.btnNext).isEnabled = position < tickets.size - 1
    }

    private fun openTicketEditScreen() {
        val intent = Intent(this, TicketAddNewOneActivity::class.java)
        intent.putExtra("TICKET_CASE_ID", ticketCaseId)
        startActivityForResult(intent, REQUEST_ADD_TICKET)
    }

    private fun openTicketDetailScreen(ticket: Ticket) {
        val intent = Intent(this, TicketDetailActivity::class.java)
        intent.putExtra("TICKET_ID", ticket.id)
        intent.putExtra("TICKET_CASE_ID", ticketCaseId)
        startActivityForResult(intent, REQUEST_EDIT_TICKET)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_ADD_TICKET, REQUEST_EDIT_TICKET -> {
                    // 重新加载票据列表
                    loadTickets()

                    // 如果是新添加的票据，滚动到最后一个位置（最新添加的票据）
                    if (requestCode == REQUEST_ADD_TICKET && tickets.isNotEmpty()) {
                        val lastPosition = tickets.size - 1
                        viewPager.setCurrentItem(lastPosition, false)
                        thumbnailAdapter.selectedPosition = lastPosition
                        thumbnailRecyclerView.scrollToPosition(lastPosition)

                        // 更新箭头按钮状态
                        updateArrowButtons(lastPosition)
                    }
                }
            }
        }
    }


    private fun getTicketCaseById(id: Long): TicketCase? {
        // 从数据库获取票夹信息
        // 这里返回模拟数据
        return TicketCase(
            id = id,
            title = "我的电影票",
            createDate = Date(),
            coverColor = Color.parseColor("#E8B4B8"),
            isLandscape = false,
            ticketType = 1
        )
    }

    private fun setupViewPager(tickets: MutableList<Ticket>) {
        // 设置离屏页面限制为3（增加缓存页面数量）
        viewPager.offscreenPageLimit = 3

        // 获取资源中定义的尺寸
        val pageMargin = resources.getDimensionPixelOffset(R.dimen.viewpager_page_margin)
        val pageOffset = resources.getDimensionPixelOffset(R.dimen.viewpager_page_offset)

        viewPager.setPageTransformer { page, position ->
            val myOffset = position * -(2 * pageOffset + pageMargin)
            val absPosition = Math.abs(position)

            // 动态设置层级，确保当前页面(position=0)始终在最上层
            // 层级值随着与当前页的距离递减
            val zIndex = 100 - (absPosition * 50)
            page.translationZ = zIndex
            page.elevation = zIndex

            // 透明度调整
            page.alpha = 0.5f + (1 - absPosition) * 0.5f

            // 位置调整
            if (position < -1) { // 左侧超出一屏的页面
                page.translationX = -myOffset
                page.translationY = (-pageOffset / 2).toFloat()
            } else if (position <= 1) { // 当前页及左右两侧的页面
                page.translationX = myOffset
                page.translationY = position * pageOffset
            } else { // 右侧超出一屏的页面
                page.translationX = -myOffset
                page.translationY = (pageOffset / 2).toFloat()
            }

            // 缩放效果
            val scaleFactor = 0.85f + (1 - absPosition) * 0.15f
            page.scaleX = scaleFactor
            page.scaleY = scaleFactor
        }
    }

    private fun fixViewPagerChildrenDrawingOrder() {
        try {
            // 获取 ViewPager2 内部的 RecyclerView
            val recyclerViewField = ViewPager2::class.java.getDeclaredField("mRecyclerView")
            recyclerViewField.isAccessible = true
            val recyclerView = recyclerViewField.get(viewPager) as RecyclerView

            // 更强大的绘制顺序回调，确保当前页面始终最后绘制
            recyclerView.setChildDrawingOrderCallback { childCount, i ->
                val currentItem = viewPager.currentItem

                // 尝试获取当前视图的位置
                val currentView = findViewForPosition(recyclerView, currentItem)
                val currentPosition = if (currentView != null) recyclerView.indexOfChild(currentView) else -1

                // 如果找不到当前视图或索引无效，使用默认顺序
                if (currentPosition == -1) return@setChildDrawingOrderCallback i

                // 调整绘制顺序，确保当前页面最后绘制
                return@setChildDrawingOrderCallback when {
                    i == childCount - 1 -> currentPosition
                    i >= currentPosition -> i + 1
                    else -> i
                }
            }

            // 启用自定义绘制顺序
            val setChildrenDrawingOrderEnabledMethod = RecyclerView::class.java.getDeclaredMethod(
                "setChildrenDrawingOrderEnabled",
                Boolean::class.java
            )
            setChildrenDrawingOrderEnabledMethod.isAccessible = true
            setChildrenDrawingOrderEnabledMethod.invoke(recyclerView, true)

        } catch (e: Exception) {
            Log.e("ViewPager2Fix", "无法修改 ViewPager2 绘制顺序", e)
        }
    }

    // 辅助方法：根据位置查找对应的视图
    private fun findViewForPosition(recyclerView: RecyclerView, position: Int): View? {
        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i)
            val holder = recyclerView.getChildViewHolder(child)
            if (holder.absoluteAdapterPosition == position) {
                return child
            }
        }
        return null
    }

    // 添加删除确认对话框方法
    private fun showDeleteConfirmation(ticket: Ticket, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("删除确认")
            .setMessage("确定要删除这张票吗？")
            .setPositiveButton("删除") { _, _ ->
                deleteTicket(ticket, position)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteTicket(ticket: Ticket, position: Int) {
        // 1. 从数据库中删除
        deleteTicketFromDatabase(ticket.id)

        // 2. 记录当前票据数量和位置，用于后续决定定位到哪个位置
        val originalSize = tickets.size
        val originalPosition = position

        // 3. 创建新的票据列表
        val updatedTickets = ArrayList(tickets)
        updatedTickets.removeAt(position)

        // 4. 更新内存中的列表
        tickets.clear()
        tickets.addAll(updatedTickets)

        // 5. 更新适配器 - 使用新的列表实例确保刷新
        pagerAdapter.updateTickets(ArrayList(updatedTickets))
        thumbnailAdapter.updateTickets(ArrayList(updatedTickets))

        // 6. 根据不同情况处理页面位置
        if (tickets.isEmpty()) {
            // 如果没有票据了，返回上一页
            Toast.makeText(this, "没有更多票据", Toast.LENGTH_SHORT).show()
            finish()
        } else if (tickets.size == 1) {
            // 如果只剩一张票据，定位到这张票据
            viewPager.setCurrentItem(0,     true)
            thumbnailAdapter.selectedPosition = 0
            thumbnailRecyclerView.scrollToPosition(0)
        } else if (originalSize > 2) {
            // 如果原来超过两张票据
            if (originalPosition > 0) {
                // 如果不是第一张，定位到前一张
                val newPosition = originalPosition - 1
                viewPager.setCurrentItem(newPosition, true)
                thumbnailAdapter.selectedPosition = newPosition
                thumbnailRecyclerView.scrollToPosition(newPosition)
            } else {
                // 如果是第一张，定位到新的第一张
                viewPager.setCurrentItem(0, true)
                thumbnailAdapter.selectedPosition = 0
                thumbnailRecyclerView.scrollToPosition(0)
            }
        } else {
            // 其他情况，保持在当前位置或调整到有效位置
            val newPosition = if (originalPosition >= tickets.size) tickets.size - 1 else originalPosition
            viewPager.setCurrentItem(newPosition, true)
            thumbnailAdapter.selectedPosition = newPosition
            thumbnailRecyclerView.scrollToPosition(newPosition)
        }

        // 7. 更新箭头按钮状态
        updateArrowButtons(viewPager.currentItem)

        Toast.makeText(this, "票据已删除", Toast.LENGTH_SHORT).show()
    }


    // 从数据库删除票据
    private fun deleteTicketFromDatabase(ticketId: Long) {
        val sharedPrefs = getSharedPreferences("ticket_prefs", Context.MODE_PRIVATE)

        // 删除票据数据
        sharedPrefs.edit().remove("ticket_$ticketId").commit()

        // 从票夹关联列表中删除
        val ticketIdsKey = "case_${ticketCaseId}_ticket_ids"
        val ticketIdsSet = sharedPrefs.getStringSet(ticketIdsKey, mutableSetOf()) ?: mutableSetOf()
        val newTicketIdsSet = HashSet(ticketIdsSet)
        newTicketIdsSet.remove(ticketId.toString())
        sharedPrefs.edit().putStringSet(ticketIdsKey, newTicketIdsSet).commit()

        // 从全局列表中删除
        val allTicketIdsSet = sharedPrefs.getStringSet("all_ticket_ids", mutableSetOf()) ?: mutableSetOf()
        val newAllTicketIdsSet = HashSet(allTicketIdsSet)
        newAllTicketIdsSet.remove(ticketId.toString())
        sharedPrefs.edit().putStringSet("all_ticket_ids", newAllTicketIdsSet).commit()
    }

    private fun updateOverlayButtonIcon() {
        val btnToggleOverlay = findViewById<ImageButton>(R.id.btnToggleOverlay)
        if (isOverlayVisible) {
            btnToggleOverlay.setImageResource(R.drawable.ic_eye_open) // 显示睁眼图标
        } else {
            btnToggleOverlay.setImageResource(R.drawable.ic_eye_closed) // 显示闭眼图标
        }
    }


    companion object {
        private const val REQUEST_ADD_TICKET = 1001
        private const val REQUEST_EDIT_TICKET = 1002
    }
}
