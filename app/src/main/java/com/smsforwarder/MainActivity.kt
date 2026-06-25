package com.smsforwarder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: PrefsManager
    private lateinit var switchEnabled: Switch
    private lateinit var switchCallForward: Switch
    private lateinit var rgMethod: RadioGroup
    private lateinit var etWebhook: EditText
    private lateinit var etPushplusToken: EditText
    private lateinit var spFilterMode: Spinner
    private lateinit var etFilterList: EditText
    private lateinit var tvStatus: TextView
    private lateinit var tvHistory: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = PrefsManager(this)
        initViews()
        loadConfig()
        checkPermission()
    }

    private fun initViews() {
        switchEnabled = findViewById(R.id.switch_enabled)
        switchCallForward = findViewById(R.id.switch_call_forward)
        rgMethod = findViewById(R.id.rg_method)
        etWebhook = findViewById(R.id.et_webhook_url)
        etPushplusToken = findViewById(R.id.et_pushplus_token)
        spFilterMode = findViewById(R.id.sp_filter_mode)
        etFilterList = findViewById(R.id.et_filter_list)
        tvStatus = findViewById(R.id.tv_status)
        tvHistory = findViewById(R.id.tv_history)

        switchEnabled.setOnCheckedChangeListener { _, checked ->
            if (checked && !hasPermission()) {
                requestPermission()
                switchEnabled.isChecked = false
                return@setOnCheckedChangeListener
            }
            prefs.isEnabled = checked
            tvStatus.text = if (checked) "● 监听中" else "○ 已停止"
        }

        switchCallForward.setOnCheckedChangeListener { _, checked ->
            prefs.callForwardEnabled = checked
        }

        rgMethod.setOnCheckedChangeListener { _, id ->
            prefs.method = when (id) {
                R.id.rb_pushplus -> "pushplus"
                R.id.rb_webhook -> "webhook"
                R.id.rb_both -> "both"
                else -> "pushplus"
            }
            updateFormVisibility()
        }

        findViewById<Button>(R.id.btn_save).setOnClickListener { saveConfig() }

        spFilterMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, pos: Int, id: Long) {
                prefs.filterMode = when (pos) { 1 -> "whitelist"; 2 -> "blacklist"; else -> "none" }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadConfig() {
        switchEnabled.isChecked = prefs.isEnabled
        switchCallForward.isChecked = prefs.callForwardEnabled
        etWebhook.setText(prefs.webhookUrl)
        etPushplusToken.setText(prefs.pushplusToken)
        etFilterList.setText(prefs.filterList.joinToString("\n"))
        tvStatus.text = if (prefs.isEnabled) "● 监听中" else "○ 已停止"

        when (prefs.method) {
            "pushplus" -> rgMethod.check(R.id.rb_pushplus)
            "webhook" -> rgMethod.check(R.id.rb_webhook)
            "both" -> rgMethod.check(R.id.rb_both)
        }
        updateFormVisibility()

        spFilterMode.setSelection(
            when (prefs.filterMode) { "whitelist" -> 1; "blacklist" -> 2; else -> 0 }
        )

        refreshHistory()
    }

    private fun saveConfig() {
        prefs.webhookUrl = etWebhook.text.toString().trim()
        prefs.pushplusToken = etPushplusToken.text.toString().trim()
        prefs.filterList = etFilterList.text.toString()
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
        Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show()
    }

    private fun updateFormVisibility() {
        val method = prefs.method
        etWebhook.isEnabled = method == "webhook" || method == "both"
        etPushplusToken.isEnabled = method == "pushplus" || method == "both"
    }

    private fun refreshHistory() {
        val list = prefs.getHistory()
        if (list.isEmpty()) {
            tvHistory.text = "暂无转发记录"
            return
        }
        val fmt = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
        tvHistory.text = list.take(20).joinToString("\n\n") { entry ->
            "[${fmt.format(Date(entry.timestamp))}]\n来自: ${entry.sender}\n${entry.body}"
        }
    }

    private fun checkPermission() {
        if (!hasPermission()) requestPermission()
    }

    private fun hasPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.POST_NOTIFICATIONS),
            100
        )
    }
}