package com.example.myticket.preview

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView

class CustomRecyclerView : RecyclerView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    // 公开受保护的方法
    fun enableChildrenDrawingOrder(enabled: Boolean) {
        setChildrenDrawingOrderEnabled(enabled)
    }

    // 获取当前状态
    fun isDrawingOrderEnabled(): Boolean {
        return isChildrenDrawingOrderEnabled()
    }
}
