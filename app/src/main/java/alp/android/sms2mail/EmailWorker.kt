package alp.android.sms2mail

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class EmailWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val smsSender = inputData.getString("smsSender") ?: return Result.failure()
        val smsBody = inputData.getString("smsBody") ?: return Result.failure()
        val mail = inputData.getString("mail") ?: return Result.failure()
        val sifre = inputData.getString("sifre") ?: return Result.failure()
        val alici = inputData.getString("alici") ?: return Result.failure()

        return try {
            val properties = Properties()
            properties["mail.smtp.host"] = "smtp.gmail.com"
            properties["mail.smtp.port"] = "587"
            properties["mail.smtp.starttls.enable"] = "true"
            properties["mail.smtp.auth"] = "true"
            properties["mail.smtp.connectiontimeout"] = "15000"
            properties["mail.smtp.timeout"] = "15000"

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
            Log.d("Sms2Mail", "E-posta başarıyla gönderildi! Gönderen: $smsSender")
            Result.success()
        } catch (e: Exception) {
            Log.e("Sms2Mail", "Gönderim başarısız, tekrar denenecek ($smsSender): ${e.message}")
            // Geçici ağ hatalarında WorkManager görevi kuyrukta tutup tekrar dener
            Result.retry()
        }
    }
}
