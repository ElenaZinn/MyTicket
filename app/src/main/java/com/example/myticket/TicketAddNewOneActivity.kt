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
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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


class TicketAddNewOneActivity : AppCompatActivity() {

    private lateinit var ivTicketImage: ImageView
    private lateinit var layoutAddImage: LinearLayout
    private lateinit var etTitle: TextInputEditText
    private lateinit var etDateTime: TextInputEditText
    private lateinit var etLocation: TextInputEditText
    private lateinit var ratingBar: RatingBar

    private var ticketCaseId: Long = 0
    private var selectedImageUri: Uri? = null
    private var selectedDate: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ticket_edit)

        ticketCaseId = intent.getLongExtra("TICKET_CASE_ID", 0)
        if (ticketCaseId == 0L) {
            Toast.makeText(this, "票夹ID无效", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupListeners()
    }

    private fun initViews() {
        ivTicketImage = findViewById(R.id.ivTicketImage)
        layoutAddImage = findViewById(R.id.layoutAddImage)
        etTitle = findViewById(R.id.etTitle)
        etDateTime = findViewById(R.id.etDateTime)
        etLocation = findViewById(R.id.etLocation)
        ratingBar = findViewById(R.id.ratingBar)

        // 设置当前日期和时间
        updateDateTimeText()
    }

    private fun setupListeners() {
        // 取消按钮
        findViewById<ImageButton>(R.id.btnCancel).setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        // 完成按钮
        findViewById<Button>(R.id.btnComplete).setOnClickListener {
            saveTicket()
        }

        // 添加图片区域点击
        layoutAddImage.setOnClickListener {
            checkPermissionAndPickImage()
        }

        // 已添加的图片点击
        ivTicketImage.setOnClickListener {
            checkPermissionAndPickImage()
        }

        // 时间选择
        etDateTime.setOnClickListener {
            showDateTimePicker()
        }
    }

    private fun updateDateTimeText() {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        etDateTime.setText(sdf.format(selectedDate.time))
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            loadSelectedImage()
        }
    }

    private fun loadSelectedImage() {
        selectedImageUri?.let { imagePath ->

            // 显示选择的图片
            ivTicketImage.visibility = View.VISIBLE
            layoutAddImage.visibility = View.GONE

            try {
                // Clear existing image first
                ivTicketImage.setImageDrawable(null)

                // Load new image
                Glide.with(this@TicketAddNewOneActivity)
                    .load(imagePath)
                    .centerCrop()
                    .override(1024, 1024)  // Higher resolution
                    .fitCenter()           // Better scaling
                    .diskCacheStrategy(DiskCacheStrategy.ALL)  // Better caching
                    .error(R.drawable.placeholder_ticket)
                    .into(ivTicketImage)

            } catch (e: Exception) {
                Log.e("OperationActivity", "Error in updateMainPreview: ${e.message}")
                ivTicketImage.visibility = View.GONE
            }

        }

    }


    private fun saveTicket() {
        val title = etTitle.text.toString().trim()
        val location = etLocation.text.toString().trim()
        val rating = ratingBar.rating

        // 验证输入
        if (title.isEmpty()) {
            etTitle.error = "请输入剧目名称"
            etTitle.requestFocus()
            return
        }

        if (location.isEmpty()) {
            etLocation.error = "请输入地点"
            etLocation.requestFocus()
            return
        }

//        // 保存图片到本地存储
        val imagePath = saveImageToLocalStorage()
        Log.d("TicketEdit", "保存图片路径: $imagePath")

        // 创建新票据对象
        val newTicket = Ticket(
            id = System.currentTimeMillis(),
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

        // 返回结果
        val resultIntent = Intent()
        resultIntent.putExtra("TICKET_ID", newTicket.id)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun saveImageToLocalStorage(): String? {
        if (selectedImageUri == null) {
            return null
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


    // 在适当的位置（如按钮点击事件）添加此方法
    private fun checkStoragePermissionAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13及以上使用READ_MEDIA_IMAGES权限
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // 已有权限，直接打开图库
                    pickImageFromGallery()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_IMAGES) -> {
                    // 用户之前拒绝过，显示说明
                    showPermissionExplanationDialog(Manifest.permission.READ_MEDIA_IMAGES)
                }
                else -> {
                    // 首次请求权限
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                        PERMISSION_REQUEST_READ_STORAGE
                    )
                }
            }
        } else {
            // Android 12及以下使用READ_EXTERNAL_STORAGE权限
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // 已有权限，直接打开图库
                    pickImageFromGallery()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                    // 用户之前拒绝过，显示说明
                    showPermissionExplanationDialog(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                else -> {
                    // 首次请求权限
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        PERMISSION_REQUEST_READ_STORAGE
                    )
                }
            }
        }
    }

    // 显示权限说明对话框
    private fun showPermissionExplanationDialog(permission: String) {
        AlertDialog.Builder(this)
            .setTitle("需要存储权限")
            .setMessage("此功能需要访问您的图库才能选择图片。请授予存储权限。")
            .setPositiveButton("确定") { _, _ ->
                // 再次请求权限
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission),
                    PERMISSION_REQUEST_READ_STORAGE
                )
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "无法选择图片，因为缺少必要权限", Toast.LENGTH_SHORT).show()
            }
            .show()
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


    companion object {
        private const val REQUEST_PICK_IMAGE = 1001
        private const val PERMISSION_REQUEST_READ_STORAGE = 2001
    }
}
