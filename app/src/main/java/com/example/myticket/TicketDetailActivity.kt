package com.example.myticket

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.text.Editable
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.myticket.bean.Ticket
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TicketDetailActivity : AppCompatActivity() {

    private var ticketId: Long = 0
    private var ticketCaseId: Long = 0
    private lateinit var detailView : CardView
    private lateinit var editView   : LinearLayout
    private lateinit var etDateTime: TextInputEditText
    private lateinit var ivTicketImage: ImageView
    private lateinit var btnEdit: ImageButton

    private lateinit var etTitle2       : TextInputEditText
    private lateinit var etLocation2    : TextInputEditText
    private lateinit var ratingBar2 : RatingBar
    private var selectedDate: Calendar = Calendar.getInstance()

    private var isEditMode: Boolean ? = false
    private var selectedImageUri: Uri? = null
    private var isInputValid: Boolean? = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ticket_detail)

        ticketId = intent.getLongExtra("TICKET_ID", 0)
        ticketCaseId = intent.getLongExtra("TICKET_CASE_ID", 0)
        if (ticketId == 0L) {
            Toast.makeText(this, "票据ID无效", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupViews()
        loadTicketData()
    }

    private fun setupViews() {
        detailView =  findViewById(R.id.detail_cardview)
        editView =  findViewById(R.id.editView)
        ivTicketImage = findViewById(R.id.ivTicketImage)
        etDateTime = findViewById(R.id.etDateTime)
        btnEdit = findViewById(R.id.btnEdit)
        etTitle2 = findViewById(R.id.etTitle)
        etLocation2 = findViewById(R.id.etLocation)
        ratingBar2 = findViewById(R.id.ratingBar2)


        detailView.visibility = View.VISIBLE
        editView.visibility = View.GONE
        ivTicketImage.visibility = View.VISIBLE

        // 返回按钮
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // 编辑按钮
        btnEdit.setOnClickListener {
            if (isEditMode == true) {
                saveChangedView()
            } else {
                initDefaultView()
            }
        }

        ivTicketImage.setOnClickListener {
            if (isEditMode == true) {
                checkPermissionAndPickImage()
            }
        }

        // 时间选择
        etDateTime.setOnClickListener {
            showDateTimePicker()
        }

    }

    private fun initDefaultView() {
        btnEdit.setImageResource(
            R.drawable.ic_save
        )
        detailView.visibility = View.GONE
        editView.visibility = View.VISIBLE
        val ticket = getTicketById(ticketId)
        if (ticket == null) {
            // 如果找不到票据，结束活动
            finish()
            return
        }
        ticket.let {
            // 显示票据信息
            etTitle2.text = Editable.Factory.getInstance().newEditable(it.title)
            etDateTime.text = Editable.Factory.getInstance().newEditable(it.getFormattedDateTime())
            etLocation2.text = Editable.Factory.getInstance().newEditable(it.location)
            ratingBar2.rating = it.rating
        }
        isEditMode = true

    }

    private fun saveChangedView() {

        val title = findViewById<TextView>(R.id.etTitle).text.toString().trim()
        val location = findViewById<TextView>(R.id.etLocation).text.toString().trim()
        val rating = ratingBar2.rating

        // 验证输入
        if (title.isEmpty()) {
            etTitle2.error = "请输入剧目名称"
            etTitle2.requestFocus()
            return
        }

        if (location.isEmpty()) {
            etLocation2.error = "请输入地点"
            etLocation2.requestFocus()
            return
        }

        detailView.visibility = View.VISIBLE
        editView.visibility = View.GONE
        btnEdit.setImageResource(
            R.drawable.ic_edit
        )

//        // 保存图片到本地存储
        val imagePath = saveImageToLocalStorage()
        Log.d("TicketEdit", "保存图片路径: $imagePath")
        // 创建新票据对象
        val newTicket = Ticket(
            id = ticketId,
            ticketCaseId = ticketCaseId,
            title = title,
            dateTime = selectedDate.time,
            location = location,
            rating = rating,
            imagePath = imagePath,
            createdAt = Date()
        )

        // 保存票据到数据库
        saveTicketToDatabase(newTicket)
        findViewById<TextView>(R.id.tvTitle).text = title
        findViewById<TextView>(R.id.tvLocation).text = location
        findViewById<RatingBar>(R.id.ratingBar).rating = rating
        updateDateTimeText()
        isEditMode = false

    }

    private fun loadTicketData() {
        // 从数据库加载票据数据
        val ticket = getTicketById(ticketId)
        if (ticket == null) {
            // 如果找不到票据，结束活动
            finish()
            return
        }

        ticket.let {
            // 显示票据图片
            if (!it.imagePath.isNullOrEmpty()) {
                ivTicketImage.setImageDrawable(null)
                Glide.with(this)
                    .load(ticket.imagePath)
                    .centerCrop()
                    .override(1024, 1024)  // Higher resolution
                    .fitCenter()           // Better scaling
                    .diskCacheStrategy(DiskCacheStrategy.ALL)  // Better caching
                    .error(R.drawable.placeholder_ticket)
                    .into(findViewById(R.id.ivTicketImage))
            }

            // 显示票据信息
            findViewById<TextView>(R.id.tvTitle).text = it.title
            findViewById<TextView>(R.id.tvDateTime).text = it.getFormattedDateTime()
            findViewById<TextView>(R.id.tvLocation).text = it.location
            findViewById<RatingBar>(R.id.ratingBar).rating = it.rating
        }
    }

    private fun getTicketById(id: Long): Ticket? {
        // 从 SharedPreferences 获取票据数据
        val sharedPrefs = getSharedPreferences("ticket_prefs", Context.MODE_PRIVATE)
        val gson = Gson()

        // 尝试读取指定ID的票据数据
        val ticketJson = sharedPrefs.getString("ticket_$id", null)

        return if (ticketJson != null) {
            try {
                // 将 JSON 转换为 Ticket 对象
                gson.fromJson(ticketJson, Ticket::class.java)
            } catch (e: Exception) {
                Log.e("TicketDetailActivity", "解析票据数据失败: ID=$id", e)
                Toast.makeText(this, "无法加载票据数据", Toast.LENGTH_SHORT).show()
                null
            }
        } else {
            Log.e("TicketDetailActivity", "未找到票据: ID=$id")
            Toast.makeText(this, "未找到票据", Toast.LENGTH_SHORT).show()
            null
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_EDIT_TICKET && resultCode == Activity.RESULT_OK) {
            // 票据已编辑，刷新数据
            loadTicketData()

            // 设置结果，通知上一级页面更新
            setResult(Activity.RESULT_OK)
        }

        if (requestCode == REQUEST_PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            loadSelectedImage()
        }
    }

    private fun checkPermissionAndPickImage() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_READ_STORAGE
            )
        } else {
            pickImageFromGallery()
        }
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_PICK_IMAGE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_READ_STORAGE && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限被授予，打开图库
                pickImageFromGallery()
            } else {
                // 权限被拒绝
                val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }

                // 检查是否勾选了"不再询问"
                if (!shouldShowRequestPermissionRationale(permission)) {
                    // 用户选择了"不再询问"，引导去设置页面
                    handlePermanentDenial()
                } else {
                    Toast.makeText(this, "需要存储权限来选择图片", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 处理"不再询问"的情况
    private fun handlePermanentDenial() {
        AlertDialog.Builder(this)
            .setTitle("权限被禁用")
            .setMessage("您已禁用存储权限。请前往设置页面手动启用权限。")
            .setPositiveButton("去设置") { _, _ ->
                // 打开应用设置页面
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun loadSelectedImage() {
        selectedImageUri?.let { imagePath ->

            // 显示选择的图片
            ivTicketImage.visibility = View.VISIBLE

            try {
                // Clear existing image first
                ivTicketImage.setImageDrawable(null)

                // Load new image
                ivTicketImage.let {
                    Glide.with(this@TicketDetailActivity)
                        .load(imagePath)
                        .centerCrop()
                        .override(1024, 1024)  // Higher resolution
                        .fitCenter()           // Better scaling
                        .diskCacheStrategy(DiskCacheStrategy.ALL)  // Better caching
                        .error(R.drawable.placeholder_ticket)
                        .into(it)
                }

            } catch (e: Exception) {
                Log.e("OperationActivity", "Error in updateMainPreview: ${e.message}")
                ivTicketImage.visibility = View.GONE
            }

        }

    }

    private fun showDateTimePicker() {
        // 日期选择器
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDate.set(year, month, dayOfMonth)

                // 时间选择器
                TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        selectedDate.set(Calendar.MINUTE, minute)
                        updateDateTimeText()
                    },
                    selectedDate.get(Calendar.HOUR_OF_DAY),
                    selectedDate.get(Calendar.MINUTE),
                    true
                ).show()
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun updateDateTimeText() {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        etDateTime.setText(sdf.format(selectedDate.time))
    }

    private fun saveImageToLocalStorage(): String? {
        if (selectedImageUri == null) {
            val ticket = getTicketById(ticketId)
            return ticket?.imagePath
        }

        try {
            val inputStream = contentResolver.openInputStream(selectedImageUri!!)
            val outputDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val fileName = "ticket_image_${System.currentTimeMillis()}.jpg"
            val outputFile = File(outputDir, fileName)

            val outputStream = FileOutputStream(outputFile)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            return outputFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "保存图片失败", Toast.LENGTH_SHORT).show()
            return null
        }
    }

    // 修改 TicketEditActivity 中的 saveTicketToDatabase 方法
    private fun saveTicketToDatabase(ticket: Ticket) {
        val sharedPrefs = getSharedPreferences("ticket_prefs", Context.MODE_PRIVATE)
        val gson = Gson()

        // 保存单个票据
        sharedPrefs.edit().putString("ticket_${ticket.id}", gson.toJson(ticket)).commit() // 使用commit()确保同步保存

        // 更新与该分类关联的票据ID列表
        val ticketIdsKey = "case_${ticket.ticketCaseId}_ticket_ids"
        val ticketIdsSet = sharedPrefs.getStringSet(ticketIdsKey, mutableSetOf()) ?: mutableSetOf()
        val newTicketIdsSet = HashSet(ticketIdsSet) // 创建新的HashSet确保变更被检测
        newTicketIdsSet.add(ticket.id.toString())
        sharedPrefs.edit().putStringSet(ticketIdsKey, newTicketIdsSet).commit()

        // 保存全局票据ID列表
        val allTicketIdsSet = sharedPrefs.getStringSet("all_ticket_ids", mutableSetOf()) ?: mutableSetOf()
        val newAllTicketIdsSet = HashSet(allTicketIdsSet) // 创建新的HashSet
        newAllTicketIdsSet.add(ticket.id.toString())
        sharedPrefs.edit().putStringSet("all_ticket_ids", newAllTicketIdsSet).commit()

        // 设置结果码确保 TicketPreviewActivity 知道有新数据
        setResult(Activity.RESULT_OK)
        Toast.makeText(this, "票据已保存", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onPause() {
        super.onPause()
        isInputValid = false
    }


    companion object {
        private const val REQUEST_EDIT_TICKET = 1001
        private const val REQUEST_PICK_IMAGE = 1002
        private const val PERMISSION_REQUEST_READ_STORAGE = 2001
    }
}
