package com.example.notes
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MainActivity : AppCompatActivity() {
    private lateinit var etNumber: EditText
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var tvConversation: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var layoutNumber: LinearLayout
    private lateinit var tvNumberBadge: TextView
    private val prefsName = "notes_prefs"
    private val key = "conversation_history"
    private val numberKey = "last_number"

    private val smsReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            val from = i?.getStringExtra("from") ?: "?"
            val body = i?.getStringExtra("body") ?: return
            appendNote(from, body, false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        etNumber = findViewById(R.id.etNumber)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        tvConversation = findViewById(R.id.tvConversation)
        scrollView = findViewById(R.id.scrollView)
        layoutNumber = findViewById(R.id.layoutNumber)
        tvNumberBadge = findViewById(R.id.tvNumberBadge)

        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        val savedNumber = prefs.getString(numberKey, "") ?: ""

        if (savedNumber.isNotEmpty()) {
            layoutNumber.visibility = View.GONE
            tvNumberBadge.visibility = View.VISIBLE
            tvNumberBadge.text = "Sync: $savedNumber"
            etNumber.setText(savedNumber)
        }

        loadConversation()
        LocalBroadcastManager.getInstance(this).registerReceiver(smsReceiver, IntentFilter("NEW_DATA_SMS"))

        findViewById<Button>(R.id.btnSaveNumber).setOnClickListener { saveNumber() }
        btnSend.setOnClickListener { sendSms() }
        etMessage.setOnEditorActionListener { _, a, _ -> if (a == EditorInfo.IME_ACTION_SEND) { sendSms(); true } else false }

        tvNumberBadge.setOnClickListener {
            layoutNumber.visibility = View.VISIBLE
            tvNumberBadge.visibility = View.GONE
        }

        findViewById<Button>(R.id.btnClear).setOnClickListener {
            prefs.edit().putString(key, "").apply()
            tvConversation.text = ""
            Toast.makeText(this, "Notes effacees", Toast.LENGTH_SHORT).show()
        }

        val perms = mutableListOf(Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
        if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        val need = perms.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (need.isNotEmpty()) ActivityCompat.requestPermissions(this, need.toTypedArray(), 100)
    }

    private fun saveNumber() {
        val n = etNumber.text.toString().trim()
        if (n.isEmpty()) { Toast.makeText(this, "Entre un numero", Toast.LENGTH_SHORT).show(); return }
        getSharedPreferences(prefsName, MODE_PRIVATE).edit().putString(numberKey, n).apply()
        layoutNumber.visibility = View.GONE
        tvNumberBadge.visibility = View.VISIBLE
        tvNumberBadge.text = "Sync: $n"
        Toast.makeText(this, "Numero enregistre", Toast.LENGTH_SHORT).show()
    }

    private fun sendSms() {
        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        var number = prefs.getString(numberKey, "") ?: ""
        if (number.isEmpty()) number = etNumber.text.toString().trim()
        val text = etMessage.text.toString().trim()
        if (number.isEmpty()) { Toast.makeText(this, "Renseigne le numero d'abord", Toast.LENGTH_SHORT).show(); layoutNumber.visibility = View.VISIBLE; return }
        if (text.isEmpty()) return
        try {
            val sm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) getSystemService(SmsManager::class.java) else SmsManager.getDefault()
            sm.sendDataMessage(number, null, 9999.toShort(), text.toByteArray(Charsets.UTF_8), null, null)
            appendNote("Moi", text, true)
            etMessage.text.clear()
            if (prefs.getString(numberKey, "")!!.isEmpty()) {
                prefs.edit().putString(numberKey, number).apply()
                layoutNumber.visibility = View.GONE
                tvNumberBadge.visibility = View.VISIBLE
                tvNumberBadge.text = "Sync: $number"
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Err: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun appendNote(author: String, body: String, isMe: Boolean) {
        val current = tvConversation.text.toString()
        val newBlock = if (isMe) "Moi: $body" else "$author: $body"
        val newText = if (current.isEmpty() || current == "Aucune note pour l'instant...") newBlock else "$current\n\n---\n$newBlock"
        tvConversation.text = newText
        getSharedPreferences(prefsName, MODE_PRIVATE).edit().putString(key, newText).apply()
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun loadConversation() {
        val txt = getSharedPreferences(prefsName, MODE_PRIVATE).getString(key, "") ?: ""
        tvConversation.text = if (txt.isEmpty()) "Aucune note pour l'instant..." else txt
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(smsReceiver)
        super.onDestroy()
    }
}
