package com.coolappstore.everdialer.by.svhp.controller

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import com.coolappstore.everdialer.by.svhp.controller.util.WhatsAppCallLauncher
import com.coolappstore.everdialer.by.svhp.modal.data.AppDatabase
import com.coolappstore.everdialer.by.svhp.modal.data.CallSource
import com.coolappstore.everdialer.by.svhp.modal.data.CallType
import com.coolappstore.everdialer.by.svhp.modal.data.MediaType
import com.coolappstore.everdialer.by.svhp.modal.data.UnifiedCallLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WhatsAppCallNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName ?: return
        if (pkg != WhatsAppCallLauncher.PACKAGE_WHATSAPP && pkg != WhatsAppCallLauncher.PACKAGE_WHATSAPP_BUSINESS) {
            return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim() ?: ""
        val channelId = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notification.channelId ?: ""
        } else ""

        val combinedContent = "$text $subText $channelId".lowercase()

        val isMissed = combinedContent.contains("missed") ||
                notification.category == Notification.CATEGORY_MISSED_CALL

        val isIncoming = combinedContent.contains("incoming") ||
                combinedContent.contains("calling") ||
                notification.category == Notification.CATEGORY_CALL ||
                channelId.contains("call", ignoreCase = true)

        if (!isMissed && !isIncoming) {
            return
        }

        val isVideo = combinedContent.contains("video") || title.lowercase().contains("video")
        val mediaType = if (isVideo) MediaType.VIDEO else MediaType.VOICE
        val callType = if (isMissed) CallType.MISSED else CallType.INCOMING
        val source = if (pkg == WhatsAppCallLauncher.PACKAGE_WHATSAPP_BUSINESS) {
            CallSource.WHATSAPP_BUSINESS
        } else {
            CallSource.WHATSAPP
        }

        val hasDigits = title.any { it.isDigit() }
        val phoneNumber = if (hasDigits) {
            WhatsAppCallLauncher.normalizeForWhatsApp(applicationContext, title)
        } else {
            title
        }
        val displayName = if (hasDigits) null else title

        if (phoneNumber.isBlank() && displayName.isNullOrBlank()) return

        serviceScope.launch {
            try {
                val dao = AppDatabase.getInstance(applicationContext).unifiedCallLogDao()
                // Prevent duplicate entries within 15 seconds (e.g. repeated ringing updates)
                val recent = dao.getRecentByNumberSince(phoneNumber, System.currentTimeMillis() - 15_000)
                if (recent != null && recent.callType == callType) {
                    return@launch
                }

                dao.insert(
                    UnifiedCallLog(
                        phoneNumber = if (hasDigits) "+$phoneNumber" else phoneNumber,
                        displayName = displayName,
                        callType = callType,
                        callSource = source,
                        mediaType = mediaType,
                        timestamp = sbn.postTime.takeIf { it > 0 } ?: System.currentTimeMillis()
                    )
                )
            } catch (_: Exception) {}
        }
    }

    companion object {
        /**
         * Checks if the user has granted Notification Listener access to this app.
         */
        fun isNotificationAccessGranted(context: Context): Boolean {
            val packageName = context.packageName
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            if (!flat.isNullOrBlank()) {
                val names = flat.split(":")
                for (name in names) {
                    val cn = ComponentName.unflattenFromString(name)
                    if (cn != null && TextUtils.equals(packageName, cn.packageName)) {
                        return true
                    }
                }
            }
            return false
        }

        /**
         * Opens the Android system settings screen where the user can grant Notification Access.
         */
        fun openNotificationAccessSettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }
}
