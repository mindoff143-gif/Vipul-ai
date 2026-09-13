package com.example.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.util.Log

class CommunicationManager(private val context: Context) {
    private val TAG = "CommunicationManager"

    fun sendSms(phoneNumber: String, message: String): Boolean {
        return try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Direct SMS failed, launching SMS intent: ${e.message}")
            try {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:$phoneNumber")
                    putExtra("sms_body", message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            } catch (ex: Exception) {
                Log.e(TAG, "SMS intent failed: ${ex.message}")
                false
            }
        }
    }

    fun sendWhatsApp(phoneNumber: String?, message: String): Boolean {
        return try {
            val cleanPhone = phoneNumber?.replace(Regex("[^0-9+]"), "") ?: ""
            val uri = if (cleanPhone.isNotEmpty()) {
                val formattedPhone = if (cleanPhone.startsWith("+")) cleanPhone.substring(1) else cleanPhone
                Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(message)}")
            }

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Direct WhatsApp failed, falling back to browser/general send: ${e.message}")
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(message)}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                true
            } catch (ex: Exception) {
                Log.e(TAG, "WhatsApp fallback intent failed: ${ex.message}")
                false
            }
        }
    }

    fun sendTelegram(usernameOrPhone: String?, message: String): Boolean {
        return try {
            val uri = Uri.parse("tg://msg?text=${Uri.encode(message)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("org.telegram.messenger")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Direct Telegram app intent failed, using universal link: ${e.message}")
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://t.me/share/url?url=&text=${Uri.encode(message)}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                true
            } catch (ex: Exception) {
                Log.e(TAG, "Telegram fallback intent failed: ${ex.message}")
                false
            }
        }
    }

    fun makeCall(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Dial intent failed: ${e.message}")
        }
    }
}
