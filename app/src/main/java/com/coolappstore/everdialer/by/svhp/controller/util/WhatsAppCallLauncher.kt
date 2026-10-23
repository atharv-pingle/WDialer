package com.coolappstore.everdialer.by.svhp.controller.util

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import android.widget.Toast
import com.coolappstore.everdialer.by.svhp.modal.data.AppDatabase
import com.coolappstore.everdialer.by.svhp.modal.data.CallSource
import com.coolappstore.everdialer.by.svhp.modal.data.CallType
import com.coolappstore.everdialer.by.svhp.modal.data.MediaType
import com.coolappstore.everdialer.by.svhp.modal.data.UnifiedCallLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

object WhatsAppCallLauncher {

    const val PACKAGE_WHATSAPP = "com.whatsapp"
    const val PACKAGE_WHATSAPP_BUSINESS = "com.whatsapp.w4b"
    private const val MIME_VOIP_CALL = "vnd.android.cursor.item/vnd.com.whatsapp.voip.call"
    private const val MIME_VIDEO_CALL = "vnd.android.cursor.item/vnd.com.whatsapp.video.call"

    /**
     * Checks if WhatsApp or WhatsApp Business is installed.
     */
    fun isWhatsAppInstalled(context: Context): Boolean {
        val pm = context.packageManager
        return try {
            pm.getPackageInfo(PACKAGE_WHATSAPP, 0)
            true
        } catch (_: Exception) {
            try {
                pm.getPackageInfo(PACKAGE_WHATSAPP_BUSINESS, 0)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    /**
     * Formats a raw phone number into a clean digits string with country code.
     * If no country code is present, it uses the device's SIM/network country prefix.
     */
    fun normalizeForWhatsApp(context: Context, rawNumber: String): String {
        var trimmed = rawNumber.trim()
        if (trimmed.startsWith("00")) {
            trimmed = "+" + trimmed.substring(2)
        }

        val hasPlus = trimmed.startsWith("+")
        val digitsOnly = trimmed.filter { it.isDigit() }

        if (hasPlus) {
            return digitsOnly
        }

        // Check if digits already seem to include a valid country code (length >= 11)
        if (digitsOnly.length >= 11 && !digitsOnly.startsWith("0")) {
            return digitsOnly
        }

        // Clean local trunk prefix (e.g. leading 0)
        val cleanLocal = if (digitsOnly.startsWith("0")) digitsOnly.substring(1) else digitsOnly

        // Get default country dialing code from SIM or Network
        val countryPrefix = getDefaultCountryDialingCode(context)
        return countryPrefix + cleanLocal
    }

    /**
     * Maps ISO country code to calling prefix.
     */
    private fun getDefaultCountryDialingCode(context: Context): String {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val simCountry = tm?.simCountryIso?.uppercase(Locale.US)
            val netCountry = tm?.networkCountryIso?.uppercase(Locale.US)
            val iso = if (!simCountry.isNullOrBlank()) simCountry else if (!netCountry.isNullOrBlank()) netCountry else Locale.getDefault().country
            countryCodeMap[iso] ?: "91" // Default fallback: +91
        } catch (_: Exception) {
            "91"
        }
    }

    /**
     * Places a WhatsApp Voice Call to [rawNumber].
     * 1. Logs to Room DB as OUTGOING.
     * 2. Attempts direct WhatsApp VoIP intent via contact ID.
     * 3. Falls back to deep link intent.
     */
    fun launchVoiceCall(context: Context, rawNumber: String, displayName: String? = null) {
        launchCallInternal(context, rawNumber, displayName, isVideo = false)
    }

    /**
     * Places a WhatsApp Video Call to [rawNumber].
     */
    fun launchVideoCall(context: Context, rawNumber: String, displayName: String? = null) {
        launchCallInternal(context, rawNumber, displayName, isVideo = true)
    }

    /**
     * Opens a direct WhatsApp text chat window with [rawNumber].
     */
    fun launchChat(context: Context, rawNumber: String) {
        val cleanNumber = normalizeForWhatsApp(context, rawNumber)
        try {
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                if (isPackageInstalled(context, PACKAGE_WHATSAPP)) {
                    setPackage(PACKAGE_WHATSAPP)
                } else if (isPackageInstalled(context, PACKAGE_WHATSAPP_BUSINESS)) {
                    setPackage(PACKAGE_WHATSAPP_BUSINESS)
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchCallInternal(
        context: Context,
        rawNumber: String,
        displayName: String?,
        isVideo: Boolean
    ) {
        val cleanNumber = normalizeForWhatsApp(context, rawNumber)
        val formattedE164 = "+$cleanNumber"

        // 1. Log outgoing call to local Room DB immediately
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(context).unifiedCallLogDao()
                dao.insert(
                    UnifiedCallLog(
                        phoneNumber = formattedE164,
                        displayName = displayName,
                        callType = CallType.OUTGOING,
                        callSource = CallSource.WHATSAPP,
                        mediaType = if (isVideo) MediaType.VIDEO else MediaType.VOICE,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (_: Exception) {}
        }

        // 2. Query contact for direct WhatsApp Data ID
        val mimeType = if (isVideo) MIME_VIDEO_CALL else MIME_VOIP_CALL
        val dataId = findWhatsAppContactDataId(context, cleanNumber, mimeType)

        if (dataId != null) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(
                        Uri.parse("content://com.android.contacts/data/$dataId"),
                        mimeType
                    )
                    setPackage(PACKAGE_WHATSAPP)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return
            } catch (_: Exception) {}
        }

        // 3. Fallback: Launch WhatsApp chat / call gateway
        try {
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                if (isPackageInstalled(context, PACKAGE_WHATSAPP)) {
                    setPackage(PACKAGE_WHATSAPP)
                } else if (isPackageInstalled(context, PACKAGE_WHATSAPP_BUSINESS)) {
                    setPackage(PACKAGE_WHATSAPP_BUSINESS)
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "WhatsApp is not installed on this device", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Looks up ContactsContract.Data for a WhatsApp VoIP record matching [targetNumber].
     */
    private fun findWhatsAppContactDataId(
        context: Context,
        targetNumber: String,
        mimeType: String
    ): Long? {
        var cursor: Cursor? = null
        return try {
            val projection = arrayOf(
                ContactsContract.Data._ID,
                ContactsContract.Data.DATA1
            )
            val selection = "${ContactsContract.Data.MIMETYPE} = ?"
            val selectionArgs = arrayOf(mimeType)

            cursor = context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )

            if (cursor != null && cursor.moveToFirst()) {
                val idIndex = cursor.getColumnIndex(ContactsContract.Data._ID)
                val data1Index = cursor.getColumnIndex(ContactsContract.Data.DATA1)

                val last7Target = if (targetNumber.length >= 7) targetNumber.takeLast(7) else targetNumber

                do {
                    val id = cursor.getLong(idIndex)
                    val data1 = cursor.getString(data1Index) ?: ""
                    val digits = data1.filter { it.isDigit() }

                    if (digits.endsWith(last7Target) || digits == targetNumber) {
                        return id
                    }
                } while (cursor.moveToNext())
            }
            null
        } catch (_: Exception) {
            null
        } finally {
            cursor?.close()
        }
    }

    private fun isPackageInstalled(context: Context, pkg: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(pkg, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    private val countryCodeMap = mapOf(
        "US" to "1",
        "CA" to "1",
        "GB" to "44",
        "IN" to "91",
        "AE" to "971",
        "SA" to "966",
        "AU" to "61",
        "DE" to "49",
        "FR" to "33",
        "IT" to "39",
        "ES" to "34",
        "NL" to "31",
        "SG" to "65",
        "MY" to "60",
        "ID" to "62",
        "TH" to "66",
        "PH" to "63",
        "BR" to "55",
        "MX" to "52",
        "ZA" to "27",
        "RU" to "7",
        "JP" to "81",
        "KR" to "82",
        "CN" to "86",
        "HK" to "852",
        "NZ" to "64"
    )
}
