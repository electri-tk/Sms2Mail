package alp.android.sms2mail

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.widget.SwitchCompat
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val editSender = findViewById<EditText>(R.id.editSenderMail)
        val editPassword = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editPasswordYeni)
        val editReceiver = findViewById<EditText>(R.id.editReceiverMail)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val switchService = findViewById<SwitchCompat>(R.id.switchService)

        // Hafızadaki kayıtlı bilgileri çek
        val prefs = getSharedPreferences("Sms2MailPrefs", Context.MODE_PRIVATE)
        editSender.setText(prefs.getString("sender", ""))
        editReceiver.setText(prefs.getString("receiver", ""))
        switchService.isChecked = prefs.getBoolean("isActive", false)
        editPassword.setText(prefs.getString("password", ""))

        // Kaydet butonuna basıldığında bilgileri hafızaya yaz
        btnSave.setOnClickListener {
            prefs.edit().apply {
                putString("sender", editSender.text.toString().trim())
                putString("password", editPassword.text.toString().trim())
                putString("receiver", editReceiver.text.toString().trim())
                apply()
            }
            Toast.makeText(this, "Ayarlar Kaydedildi!", Toast.LENGTH_SHORT).show()
        }


        // Switch açılıp kapandığında servisi yönet
        switchService.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("isActive", isChecked).apply()
            if (isChecked) {
                checkPermissionsAndStart()
            } else {
                stopService(Intent(this, SmsService::class.java))
            }
        }
    }

    private fun checkPermissionsAndStart() {
        val permissions = mutableListOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), PERMISSION_CODE)
        } else {
            startSmsService()
            ignoreBatteryOptimizations()
        }
    }

    private fun startSmsService() {
        val serviceIntent = Intent(this, SmsService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun ignoreBatteryOptimizations() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startSmsService()
                ignoreBatteryOptimizations()
            } else {
                Toast.makeText(this, "SMS ve Bildirim izinleri gerekli!", Toast.LENGTH_SHORT).show()
                findViewById<SwitchCompat>(R.id.switchService).isChecked = false
            }
        }
    }

}