package com.example.notes
import android.Manifest;import android.content.*;import android.content.pm.PackageManager;import android.os.Build;import android.os.Bundle;import android.telephony.SmsManager;import android.view.inputmethod.EditorInfo;import android.widget.*;import androidx.appcompat.app.AppCompatActivity;import androidx.core.app.ActivityCompat;import androidx.core.content.ContextCompat;import androidx.localbroadcastmanager.content.LocalBroadcastManager
class MainActivity:AppCompatActivity(){
    private lateinit var etNumber:EditText;private lateinit var etMessage:EditText;private lateinit var btnSend:Button;private lateinit var tvConversation:TextView;private lateinit var scrollView:ScrollView
    private val prefsName="notes_prefs";private val key="conversation_history"
    private val smsReceiver=object:BroadcastReceiver(){override fun onReceive(c:Context?,i:Intent?){appendMessage("[${i?.getStringExtra("from")?:"?"}]: ${i?.getStringExtra("body")?:return}")}}
    override fun onCreate(s:Bundle?){super.onCreate(s);setContentView(R.layout.activity_main)
        etNumber=findViewById(R.id.etNumber);etMessage=findViewById(R.id.etMessage);btnSend=findViewById(R.id.btnSend);tvConversation=findViewById(R.id.tvConversation);scrollView=findViewById(R.id.scrollView)
        tvConversation.text=getSharedPreferences(prefsName,MODE_PRIVATE).getString(key,"")?:""
        LocalBroadcastManager.getInstance(this).registerReceiver(smsReceiver,IntentFilter("NEW_DATA_SMS"))
        btnSend.setOnClickListener{send()};etMessage.setOnEditorActionListener{_,a,_ -> if(a==EditorInfo.IME_ACTION_SEND){send();true}else false}
        findViewById<Button>(R.id.btnClose).setOnClickListener{finishAndRemoveTask()};findViewById<Button>(R.id.btnClear).setOnClickListener{getSharedPreferences(prefsName,MODE_PRIVATE).edit().putString(key,"").apply();tvConversation.text=""}
        etNumber.setText(getSharedPreferences(prefsName,MODE_PRIVATE).getString("last_number",""))
        val perms=mutableListOf(Manifest.permission.SEND_SMS,Manifest.permission.RECEIVE_SMS,Manifest.permission.READ_SMS);if(Build.VERSION.SDK_INT>=33)perms.add(Manifest.permission.POST_NOTIFICATIONS)
        val need=perms.filter{ContextCompat.checkSelfPermission(this,it)!=PackageManager.PERMISSION_GRANTED};if(need.isNotEmpty())ActivityCompat.requestPermissions(this,need.toTypedArray(),100)
    }
    private fun send(){val n=etNumber.text.toString().trim();val t=etMessage.text.toString().trim();if(n.isEmpty()||t.isEmpty())return;try{val sm=if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S)getSystemService(SmsManager::class.java)else@Suppress("DEPRECATION")SmsManager.getDefault();sm.sendDataMessage(n,null,9999.toShort(),t.toByteArray(Charsets.UTF_8),null,null);appendMessage("Moi: $t");etMessage.text.clear();getSharedPreferences(prefsName,MODE_PRIVATE).edit().putString("last_number",n).apply()}catch(e:Exception){Toast.makeText(this,"Err: ${e.message}",1).show()}}
    private fun appendMessage(m:String){val c=tvConversation.text.toString();val nt=if(c.isEmpty())m else"$c\n\n$m";tvConversation.text=nt;getSharedPreferences(prefsName,MODE_PRIVATE).edit().putString(key,nt).apply();scrollView.post{scrollView.fullScroll(ScrollView.FOCUS_DOWN)}}
    override fun onDestroy(){LocalBroadcastManager.getInstance(this).unregisterReceiver(smsReceiver);super.onDestroy()}
}
