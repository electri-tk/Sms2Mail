package alp.android.sms2mail

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("Sms2MailPrefs", Context.MODE_PRIVATE)
        val isActive = prefs.getBoolean("isActive", false)

        if (!isActive) return

        val gonderenMail = prefs.getString("sender", "") ?: ""
        val uygulamaSifresi = prefs.getString("password", "") ?: ""
        val aliciMail = prefs.getString("receiver", "") ?: ""

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) return

            if (gonderenMail.isEmpty() || uygulamaSifresi.isEmpty() || aliciMail.isEmpty()) {
                Log.e("Sms2Mail", "Ayarlar eksik olduğu için mail gönderilemedi.")
                return
            }

            // Gelen SMS parçalarını gönderen numaraya göre grupla ve birleştir
            val groupedMessages = messages.groupBy { it.displayOriginatingAddress ?: "Bilinmeyen" }
                .mapValues { entry ->
                    entry.value.joinToString(separator = "") { it.displayMessageBody ?: "" }
                }

            // Yalnızca internet bağlantısı varken çalışması için kısıtlama tanımla
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            for ((sender, fullBody) in groupedMessages) {
                val inputData = workDataOf(
                    "smsSender" to sender,
                    "smsBody" to fullBody,
                    "mail" to gonderenMail,
                    "sifre" to uygulamaSifresi,
                    "alici" to aliciMail
                )

                val emailWorkRequest = OneTimeWorkRequestBuilder<EmailWorker>()
                    .setConstraints(constraints)
                    .setInputData(inputData)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        30,
                        TimeUnit.SECONDS
                    )
                    .build()

                WorkManager.getInstance(context).enqueue(emailWorkRequest)
            }
        }
    }
}
