package alp.android.sms2mail

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import kotlin.concurrent.thread

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("Sms2MailPrefs", Context.MODE_PRIVATE)
        val isActive = prefs.Boolean("isActive", false)

        if (!isActive) return

        val gonderenMail = prefs.getString("sender", "") ?: ""
        val uygulamaSifresi = prefs.getString("password", "") ?: ""
        val aliciMail = prefs.getString("receiver", "") ?: ""

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            // Gelen mesaj dizisi boşsa işlem yapma
            if (messages.isNullOrEmpty()) return

            if (gonderenMail.isEmpty() || uygulamaSifresi.isEmpty() || aliciMail.isEmpty()) {
                Log.e("Sms2Mail", "Ayarlar eksik olduğu için mail gönderilemedi.")
                return
            }

            // Gönderen bilgisini ilk parçadan al
            val sender = messages[0].displayOriginatingAddress ?: "Bilinmeyen"
            
            // Tüm SMS parçalarını tek bir metin halinde birleştir
            val fullMessageBody = StringBuilder()
            for (sms in messages) {
                fullMessageBody.append(sms.displayMessageBody ?: "")
            }

            val pendingResult = goAsync()
            thread {
                try {
                    sendEmail(sender, fullMessageBody.toString(), gonderenMail, uygulamaSifresi, aliciMail)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun sendEmail(smsSender: String, smsBody: String, mail: String, sifre: String, alici: String) {
        try {
            val properties = Properties()
            properties["mail.smtp.host"] = "://gmail.com"
            properties["mail.smtp.port"] = "587"
            properties["mail.smtp.starttls.enable"] = "true"
            properties["mail.smtp.auth"] = "true"

            val session = Session.getInstance(properties, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(mail, sifre)
                }
            })

            val message = MimeMessage(session)
            message.setFrom(InternetAddress(mail))
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(alici))
            message.subject = "Yeni SMS Geldi: $smsSender"
            message.setText("Gönderen Numara: $smsSender\n\nMesaj İçeriği:\n$smsBody", "UTF-8")

            Transport.send(message)
            Log.d("Sms2Mail", "E-posta başarıyla gönderildi!")
        } catch (e: Exception) {
            Log.e("Sms2Mail", "E-posta gönderim hatası: ${e.message}")
        }
    }
}
