package com.explainmymoney.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.explainmymoney.data.database.AppDatabase
import com.explainmymoney.data.parser.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    
    private val smsParser = SmsParser()
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        
        messages.forEach { smsMessage ->
            val sender = smsMessage.displayOriginatingAddress ?: return@forEach
            val body = smsMessage.messageBody ?: return@forEach
            val timestamp = smsMessage.timestampMillis

            val transaction = smsParser.parseTransactionSms(sender, body, timestamp)
            
            transaction?.let { tx ->
                scope.launch {
                    try {
                        val database = AppDatabase.getDatabase(context)
                        database.transactionDao().insertTransaction(tx)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
