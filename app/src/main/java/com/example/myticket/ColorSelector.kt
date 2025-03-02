package com.example.myticket

import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.LinearLayout
import android.widget.Space

class ColorSelector @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val colors = listOf(
        Color.parseColor("#E8B4B8"), // 莫兰迪红
        Color.parseColor("#A3C9A8"), // 莫兰迪绿
        Color.parseColor("#A5B8D1")  // 莫兰迪蓝
    )

    private var selectedColorIndex = 0
    private var colorViews = ArrayList<View>()

    var onColorSelectedListener: ((Int) -> Unit)? = null

    init {
        orientation = HORIZONTAL

        // 创建三个颜色选项
        for (i in colors.indices) {
            val colorView = createColorView(colors[i], i)
            colorViews.add(colorView)
            addView(colorView)

            // 如果不是最后一个，添加间隔
            if (i < colors.size - 1) {
                addView(Space(context).apply {
                    layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
                })
            }
        }

        // 默认选中第一个
        updateSelection(0)
    }

    private fun createColorView(color: Int, index: Int): View {
        val view = View(context)
        view.setBackgroundColor(color)
        view.layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 3f)

        // 设置圆角
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height,
                    resources.getDimensionPixelSize(R.dimen.color_view_corner_radius).toFloat())
            }
        }
        view.clipToOutline = true

        // 设置点击事件
        view.setOnClickListener {
            updateSelection(index)
            onColorSelectedListener?.invoke(colors[index])
        }

        return view
    }

    private fun updateSelection(index: Int) {
        selectedColorIndex = index

        // 更新所有视图的状态
        for (i in colorViews.indices) {
            val view = colorViews[i]
            if (i == selectedColorIndex) {
                view.elevation = resources.getDimension(R.dimen.selected_color_elevation)

                // 不使用XML资源，改为动态创建带边框的GradientDrawable
                val shape = GradientDrawable()
                shape.cornerRadius = resources.getDimensionPixelSize(R.dimen.color_view_corner_radius).toFloat()
                shape.setColor(colors[i]) // 设置原始莫兰迪颜色
                shape.setStroke(dpToPx(2), Color.parseColor("#2196F3")) // 添加蓝色边框

                view.background = shape
            } else {
                view.elevation = 0f
                view.setBackgroundColor(colors[i])
            }
        }
    }

    // 辅助方法：dp转px
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }


    fun getSelectedColor(): Int = colors[selectedColorIndex]
}
