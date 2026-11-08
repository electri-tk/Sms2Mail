package alp.android.sms2mail

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Telefon tamamen açıldığında tetiklenir
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("Sms2MailPrefs", Context.MODE_PRIVATE)
            val isActive = prefs.getBoolean("isActive", false)

            // Eğer uygulama kapatılmadan önce Switch "Açık" durumdaysa servisi tekrar başlat
            if (isActive) {
                val serviceIntent = Intent(context, SmsService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}