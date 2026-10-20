package com.coolappstore.everdialer.by.svhp.controller.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Telephony
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.coolappstore.everdialer.by.svhp.modal.`interface`.ICallLogRepository
import com.coolappstore.everdialer.by.svhp.modal.`interface`.IContactsRepository
import com.coolappstore.everdialer.by.svhp.modal.`interface`.ISmsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID

enum class PcSyncStatus {
    DISABLED,
    CONNECTING,
    CONNECTED,
    SYNCING,
    DISCONNECTED,
    ERROR
}

class PcBluetoothSyncManager(
    private val context: Context,
    private val prefs: PreferenceManager,
    private val contactsRepository: IContactsRepository,
    private val callLogRepository: ICallLogRepository,
    private val smsRepository: ISmsRepository
) {
    companion object {
        private const val TAG = "PcBluetoothSyncManager"
        val EVER_DIALER_SYNC_UUID: UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        const val RFCOMM_CHANNEL = 4
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var syncJob: Job? = null

    private val _status = MutableStateFlow(PcSyncStatus.DISABLED)
    val status: StateFlow<PcSyncStatus> = _status.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow<Long?>(null)
    val lastSyncTimestamp: StateFlow<Long?> = _lastSyncTimestamp.asStateFlow()

    @Volatile
    private var activeSocket: BluetoothSocket? = null
    @Volatile
    private var activeWriter: BufferedWriter? = null

    init {
        val isEnabled = prefs.getBoolean(PreferenceManager.KEY_PC_SYNC_BLUETOOTH_ENABLED, false)
        if (isEnabled) {
            start()
        }
    }

    fun hasBluetoothPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    fun onBluetoothSyncToggled(enabled: Boolean) {
        prefs.setBoolean(PreferenceManager.KEY_PC_SYNC_BLUETOOTH_ENABLED, enabled)
        if (enabled) {
            start()
        } else {
            stop()
        }
    }

    @Synchronized
    fun start() {
        stop()
        _status.value = PcSyncStatus.CONNECTING
        syncJob = scope.launch {
            runSyncLoop()
        }
    }

    @Synchronized
    fun stop() {
        syncJob?.cancel()
        syncJob = null
        closeActiveConnection()
        _status.value = PcSyncStatus.DISABLED
        _connectedDeviceName.value = null
    }

    private fun closeActiveConnection() {
        try {
            activeWriter?.close()
        } catch (_: Exception) {}
        activeWriter = null

        try {
            activeSocket?.close()
        } catch (_: Exception) {}
        activeSocket = null
    }

    fun triggerSyncNow() {
        scope.launch {
            val writer = activeWriter
            val socket = activeSocket
            if (writer != null && socket != null && socket.isConnected) {
                Log.d(TAG, "triggerSyncNow: socket already connected, sending data directly")
                sendSyncData(writer)
            } else {
                Log.d(TAG, "triggerSyncNow: socket not connected, triggering immediate connect")
                start()
            }
        }
    }

    fun triggerImmediateSync() {
        val writer = activeWriter
        val socket = activeSocket
        if (writer != null && socket != null && socket.isConnected) {
            scope.launch(Dispatchers.IO) {
                delay(350)
                try {
                    Log.i(TAG, "triggerImmediateSync: pushing fresh data to PC...")
                    sendSyncData(writer)
                } catch (e: Exception) {
                    Log.w(TAG, "triggerImmediateSync error: ${e.message}")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun runSyncLoop() {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter

        if (adapter == null || !adapter.isEnabled) {
            Log.w(TAG, "Bluetooth adapter unavailable or disabled")
            _status.value = PcSyncStatus.ERROR
            return
        }

        while (scope.isActive) {
            try {
                if (!hasBluetoothPermission()) {
                    Log.w(TAG, "BLUETOOTH_CONNECT permission not yet granted. Waiting...")
                    _status.value = PcSyncStatus.ERROR
                    delay(3000)
                    continue
                }

                _status.value = PcSyncStatus.CONNECTING

                // 1. Search bonded devices for a paired PC or computer
                val bonded = try {
                    adapter.bondedDevices?.toList() ?: emptyList()
                } catch (e: SecurityException) {
                    Log.e(TAG, "Missing Bluetooth permissions: ${e.message}")
                    _status.value = PcSyncStatus.ERROR
                    delay(3000)
                    continue
                }

                // Filter out audio headsets and watches
                val pcCandidates = bonded.filter { dev ->
                    val major = dev.bluetoothClass?.majorDeviceClass
                    val name = dev.name?.lowercase() ?: ""
                    val isAudioOrWatch = major == BluetoothClass.Device.Major.AUDIO_VIDEO ||
                            major == BluetoothClass.Device.Major.WEARABLE ||
                            name.contains("fireboltt") || name.contains("buds") ||
                            name.contains("headset") || name.contains("watch") ||
                            name.contains("airpods")
                    !isAudioOrWatch
                }.sortedByDescending { dev ->
                    val name = dev.name?.lowercase() ?: ""
                    val isConnected = try {
                        val m = dev.javaClass.getMethod("isConnected")
                        (m.invoke(dev) as? Boolean) == true
                    } catch (_: Exception) {
                        false
                    }

                    var score = 0
                    if (name.contains("fedora")) score += 50
                    if (isConnected) score += 30
                    if (dev.bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.COMPUTER) score += 20
                    if (name.contains("pc") || name.contains("laptop") || name.contains("desktop") || name.contains("linux") || name.contains("ubuntu")) score += 15
                    score
                }

                var connected = false
                for (device in pcCandidates) {
                    if (!scope.isActive) break
                    connected = attemptClientConnect(device)
                    if (connected) break
                }

                if (!connected && scope.isActive) {
                    // 2. Also listen as RFCOMM server in case PC connects to mobile
                    listenAsServer(adapter)
                }

                if (scope.isActive) {
                    delay(3000)
                }
            } catch (e: CancellationException) {
                break
            } catch (e: Exception) {
                Log.e(TAG, "Exception in sync loop: ${e.message}", e)
                _status.value = PcSyncStatus.DISCONNECTED
                delay(3000)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun attemptClientConnect(device: BluetoothDevice): Boolean {
        val devName = device.name ?: device.address
        Log.d(TAG, "Attempting client connect to $devName (${device.address})")

        // Strategy A: Direct RFCOMM channel 4 (connects immediately without SDP lookup)
        try {
            val method = device.javaClass.getMethod("createInsecureRfcommSocket", Int::class.javaPrimitiveType)
            val socket = method.invoke(device, RFCOMM_CHANNEL) as BluetoothSocket
            socket.connect()
            if (socket.isConnected) {
                Log.i(TAG, "Successfully connected to $devName on direct RFCOMM channel $RFCOMM_CHANNEL")
                handleConnection(socket, devName)
                return true
            }
        } catch (e: Exception) {
            Log.d(TAG, "Direct channel $RFCOMM_CHANNEL connect to $devName failed: ${e.message}")
        }

        // Strategy B: SDP Ever Dialer UUID
        try {
            val socket = device.createInsecureRfcommSocketToServiceRecord(EVER_DIALER_SYNC_UUID)
            socket.connect()
            if (socket.isConnected) {
                Log.i(TAG, "Successfully connected to $devName with UUID $EVER_DIALER_SYNC_UUID")
                handleConnection(socket, devName)
                return true
            }
        } catch (e: Exception) {
            Log.d(TAG, "SDP connect to $devName via $EVER_DIALER_SYNC_UUID failed: ${e.message}")
        }

        // Strategy C: SDP SPP UUID
        try {
            val socket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            if (socket.isConnected) {
                Log.i(TAG, "Successfully connected to $devName with UUID $SPP_UUID")
                handleConnection(socket, devName)
                return true
            }
        } catch (e: Exception) {
            Log.d(TAG, "SDP connect to $devName via $SPP_UUID failed: ${e.message}")
        }

        return false
    }

    @SuppressLint("MissingPermission")
    private suspend fun listenAsServer(adapter: BluetoothAdapter) {
        var serverSocket: BluetoothServerSocket? = null
        try {
            Log.d(TAG, "Opening RFCOMM server socket with UUID $EVER_DIALER_SYNC_UUID")
            serverSocket = adapter.listenUsingInsecureRfcommWithServiceRecord(
                "EverDialerSync",
                EVER_DIALER_SYNC_UUID
            )

            val timeoutJob = scope.launch {
                delay(3000)
                try { serverSocket?.close() } catch (_: Exception) {}
            }

            val clientSocket = try {
                withContext(Dispatchers.IO) {
                    serverSocket.accept()
                }
            } catch (e: Exception) {
                null
            } finally {
                timeoutJob.cancel()
            }

            if (clientSocket != null) {
                val clientDevName = clientSocket.remoteDevice?.name ?: "Ever Dialer PC"
                Log.i(TAG, "Accepted incoming connection from $clientDevName")
                handleConnection(clientSocket, clientDevName)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Server socket listen note: ${e.message}")
        } finally {
            try {
                serverSocket?.close()
            } catch (_: Exception) {}
        }
    }

    private fun computeStateFingerprint(): String {
        val sb = StringBuilder()
        // 1. Call log
        try {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls._ID, CallLog.Calls.DATE),
                null, null,
                "${CallLog.Calls.DATE} DESC LIMIT 1"
            )?.use { c ->
                if (c.moveToFirst()) {
                    sb.append("calls:").append(c.getLong(0)).append("-").append(c.getLong(1)).append(";")
                }
            }
        } catch (_: Exception) {}

        // 2. Contacts
        try {
            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.STARRED,
                    ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP
                ),
                null, null,
                "${ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP} DESC LIMIT 1"
            )?.use { c ->
                sb.append("cnt:").append(c.count).append(",")
                if (c.moveToFirst()) {
                    val sIdx = c.getColumnIndex(ContactsContract.Contacts.STARRED)
                    val tIdx = c.getColumnIndex(ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP)
                    if (sIdx >= 0) sb.append(c.getInt(sIdx)).append(",")
                    if (tIdx >= 0) sb.append(c.getLong(tIdx)).append(";")
                }
            }
        } catch (_: Exception) {}

        // Starred contacts count
        try {
            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts._ID),
                "${ContactsContract.Contacts.STARRED} = 1",
                null, null
            )?.use { c ->
                sb.append("fav:").append(c.count).append(";")
            }
        } catch (_: Exception) {}

        // 3. SMS (query by highest _ID and DATE, and conversation snippet/date)
        try {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms._ID, Telephony.Sms.DATE, Telephony.Sms.READ),
                null, null,
                "${Telephony.Sms._ID} DESC LIMIT 1"
            )?.use { c ->
                if (c.moveToFirst()) {
                    sb.append("sms:").append(c.getLong(0)).append("-").append(c.getLong(1)).append("-").append(c.getInt(2)).append(";")
                }
            }
        } catch (_: Exception) {}

        try {
            context.contentResolver.query(
                Uri.parse("content://mms-sms/conversations"),
                arrayOf("date", "snippet"),
                null, null,
                "date DESC LIMIT 1"
            )?.use { c ->
                if (c.moveToFirst()) {
                    val d = c.getLong(0)
                    val s = c.getString(1)?.hashCode() ?: 0
                    sb.append("mms_conv:").append(d).append("-").append(s).append(";")
                }
            }
        } catch (_: Exception) {}

        // 4. Notes
        try {
            val notes = NoteManager.getNotesDir(context).listFiles() ?: emptyArray()
            val maxMod = notes.maxOfOrNull { it.lastModified() } ?: 0L
            sb.append("notes:").append(notes.size).append("-").append(maxMod)
        } catch (_: Exception) {}

        return sb.toString()
    }

    private suspend fun handleConnection(socket: BluetoothSocket, deviceName: String) {
        activeSocket = socket
        _connectedDeviceName.value = deviceName
        _status.value = PcSyncStatus.CONNECTED

        val writer = BufferedWriter(OutputStreamWriter(socket.outputStream, Charsets.UTF_8))
        activeWriter = writer

        val reader = BufferedReader(InputStreamReader(socket.inputStream, Charsets.UTF_8))

        var lastFingerprint = computeStateFingerprint()

        // 1. Send full sync data immediately upon connection
        sendSyncData(writer)

        // 2. Set up ContentObserver for real-time changes (calls, contacts, SMS)
        val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            private var lastTrigger = 0L
            override fun onChange(selfChange: Boolean) {
                val now = System.currentTimeMillis()
                if (now - lastTrigger > 500) {
                    lastTrigger = now
                    scope.launch(Dispatchers.IO) {
                        delay(400)
                        val newFp = computeStateFingerprint()
                        if (newFp != lastFingerprint) {
                            Log.i(TAG, "ContentObserver: data change detected on phone, syncing to PC...")
                            lastFingerprint = newFp
                            sendSyncData(writer)
                        }
                    }
                }
            }
        }

        try {
            context.contentResolver.registerContentObserver(CallLog.Calls.CONTENT_URI, true, contentObserver)
            context.contentResolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contentObserver)
            context.contentResolver.registerContentObserver(Telephony.Sms.CONTENT_URI, true, contentObserver)
            context.contentResolver.registerContentObserver(Telephony.Sms.Inbox.CONTENT_URI, true, contentObserver)
            context.contentResolver.registerContentObserver(Uri.parse("content://mms-sms/conversations"), true, contentObserver)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register content observers: ${e.message}")
        }

        val smsReceiver = object : BroadcastReceiver() {
            override fun onReceive(pContext: Context?, intent: Intent?) {
                Log.i(TAG, "Dynamic SMS broadcast received (${intent?.action}), triggering sync...")
                triggerImmediateSync()
            }
        }
        try {
            val filter = IntentFilter().apply {
                addAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
                addAction(Telephony.Sms.Intents.SMS_DELIVER_ACTION)
            }
            ContextCompat.registerReceiver(context, smsReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        } catch (e: Exception) {
            Log.w(TAG, "Could not register dynamic SMS receiver: ${e.message}")
        }

        // 3. Periodic change-detector coroutine (runs every 2.5s for notes or missed provider events)
        val periodicSyncJob = scope.launch(Dispatchers.IO) {
            while (isActive && socket.isConnected) {
                delay(2500)
                if (!socket.isConnected) break
                val newFp = computeStateFingerprint()
                if (newFp != lastFingerprint) {
                    Log.i(TAG, "Periodic check detected data change on phone: syncing to PC...")
                    lastFingerprint = newFp
                    sendSyncData(writer)
                }
            }
        }

        // 4. Read incoming commands from Ever Dialer PC
        try {
            while (scope.isActive && socket.isConnected) {
                val line = withContext(Dispatchers.IO) {
                    reader.readLine()
                } ?: break

                Log.d(TAG, "Received command from PC: $line")
                try {
                    val cmdObj = JSONObject(line)
                    val cmd = cmdObj.optString("cmd")
                    when (cmd) {
                        "request_sync" -> {
                            lastFingerprint = computeStateFingerprint()
                            sendSyncData(writer)
                        }
                        "make_call" -> {
                            val number = cmdObj.optString("number")
                            Log.i(TAG, "Executing make_call for $number via Ever Dialer")
                            if (number.isNotBlank()) {
                                withContext(Dispatchers.Main) {
                                    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                                    val accounts = try { telecomManager?.callCapablePhoneAccounts ?: emptyList() } catch (_: Throwable) { emptyList() }
                                    val defaultSim = prefs.getInt(PreferenceManager.KEY_DEFAULT_SIM, 1)
                                    val handle = if (accounts.isNotEmpty()) {
                                        getPhoneAccountForSimSlot(context, accounts, if (defaultSim == 2) 1 else 0)
                                    } else null
                                    makeCall(context, number, accountHandle = handle, skipConfirm = true)
                                }
                            }
                        }
                        "send_sms" -> {
                            val phone = cmdObj.optString("phone").ifBlank { cmdObj.optString("number") }
                            val text = cmdObj.optString("text").ifBlank { cmdObj.optString("body") }
                            Log.i(TAG, "Executing send_sms via Ever Dialer to $phone: $text")
                            if (phone.isNotBlank() && text.isNotBlank()) {
                                val success = smsRepository.sendSms(phone, text)
                                Log.i(TAG, "send_sms result: $success")
                                delay(400)
                                lastFingerprint = computeStateFingerprint()
                                sendSyncData(writer)
                            }
                        }
                        "toggle_favorite" -> {
                            val contactId = cmdObj.optString("contactId")
                            val isFav = cmdObj.optBoolean("isFavorite", false)
                            Log.i(TAG, "Executing toggle_favorite for contactId $contactId -> $isFav")
                            if (contactId.isNotBlank()) {
                                try {
                                    val values = ContentValues().apply {
                                        put(ContactsContract.Contacts.STARRED, if (isFav) 1 else 0)
                                    }
                                    context.contentResolver.update(
                                        ContactsContract.Contacts.CONTENT_URI,
                                        values,
                                        "${ContactsContract.Contacts._ID} = ?",
                                        arrayOf(contactId)
                                    )
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error toggling favorite for $contactId: ${e.message}")
                                }
                                delay(300)
                                lastFingerprint = computeStateFingerprint()
                                sendSyncData(writer)
                            }
                        }
                        "add_note" -> {
                            val title = cmdObj.optString("title", "Untitled Note")
                            val content = cmdObj.optString("content", "")
                            NoteManager.writeNote(context, title, "", content)
                            lastFingerprint = computeStateFingerprint()
                            sendSyncData(writer)
                        }
                        "delete_note" -> {
                            val id = cmdObj.optString("id", "")
                            if (id.isNotBlank()) {
                                val file = NoteManager.getNotesDir(context).listFiles()
                                    ?.firstOrNull { it.nameWithoutExtension == id }
                                if (file != null) {
                                    NoteManager.deleteNoteFile(file)
                                }
                            }
                            lastFingerprint = computeStateFingerprint()
                            sendSyncData(writer)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error handling command JSON: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Connection lost: ${e.message}")
        } finally {
            try {
                context.unregisterReceiver(smsReceiver)
            } catch (_: Exception) {}
            periodicSyncJob.cancel()
            try {
                context.contentResolver.unregisterContentObserver(contentObserver)
            } catch (_: Exception) {}
            closeActiveConnection()
            _status.value = PcSyncStatus.DISCONNECTED
            _connectedDeviceName.value = null
        }
    }

    private suspend fun sendSyncData(writer: BufferedWriter) = withContext(Dispatchers.IO) {
        try {
            _status.value = PcSyncStatus.SYNCING

            val syncFavorites = prefs.getBoolean(PreferenceManager.KEY_PC_SYNC_FAVOURITES, true)
            val syncCallLogs = prefs.getBoolean(PreferenceManager.KEY_PC_SYNC_CALL_LOGS, true)
            val syncContacts = prefs.getBoolean(PreferenceManager.KEY_PC_SYNC_CONTACTS, true)
            val syncSms = prefs.getBoolean(PreferenceManager.KEY_PC_SYNC_SMS, true)
            val syncNotes = prefs.getBoolean(PreferenceManager.KEY_PC_SYNC_NOTES, true)

            // 1. Real Contacts (Only saved contacts from phone)
            val contactsArray = JSONArray()
            if (syncContacts || syncFavorites) {
                val realContacts = try {
                    contactsRepository.getContacts()
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching contacts: ${e.message}")
                    emptyList()
                }

                val filtered = if (syncContacts) {
                    realContacts
                } else {
                    realContacts.filter { it.isFavorite }
                }

                for (c in filtered) {
                    val phone = c.phones.firstOrNull()?.number ?: c.phoneNumbers.firstOrNull() ?: ""
                    val cObj = JSONObject().apply {
                        put("id", c.id)
                        put("name", c.name.ifBlank { phone })
                        put("phone", phone)
                        put("isFavorite", c.isFavorite)
                        put("account", c.sourceAccounts.firstOrNull() ?: "Phone")
                    }
                    contactsArray.put(cObj)
                }
            }

            // 2. Real Call Logs
            val callLogsArray = JSONArray()
            if (syncCallLogs) {
                val realLogs = try {
                    callLogRepository.getCallLogs(limit = 100)
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching call logs: ${e.message}")
                    emptyList()
                }

                for (log in realLogs) {
                    val lObj = JSONObject().apply {
                        put("id", "${log.number}_${log.date}")
                        put("name", log.name ?: log.number)
                        put("number", log.number)
                        put("type", when (log.type) {
                            android.provider.CallLog.Calls.OUTGOING_TYPE -> "outgoing"
                            android.provider.CallLog.Calls.MISSED_TYPE -> "missed"
                            else -> "incoming"
                        })
                        put("timestamp", log.date)
                        put("duration", log.duration)
                        put("simSlot", log.simSlot)
                    }
                    callLogsArray.put(lObj)
                }
            }

            // 3. Real SMS Threads
            val smsArray = JSONArray()
            if (syncSms) {
                val convs = try {
                    smsRepository.getConversations().take(50)
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching SMS conversations: ${e.message}")
                    emptyList()
                }

                for (conv in convs) {
                    val msgs = try {
                        smsRepository.getMessagesForThread(conv.threadId).takeLast(50)
                    } catch (_: Exception) {
                        emptyList()
                    }

                    val msgsArray = JSONArray()
                    for (m in msgs) {
                        val mObj = JSONObject().apply {
                            put("id", m.id.toString())
                            put("body", m.body)
                            put("timestamp", m.date)
                            put("isOutgoing", m.isOutgoing)
                        }
                        msgsArray.put(mObj)
                    }

                    val tObj = JSONObject().apply {
                        put("id", conv.threadId.toString())
                        put("contactName", conv.contactName ?: conv.address)
                        put("phone", conv.address)
                        put("snippet", conv.snippet)
                        put("timestamp", conv.date)
                        put("unreadCount", conv.unreadCount)
                        put("messages", msgsArray)
                    }
                    smsArray.put(tObj)
                }
            }

            // 4. Real Notes
            val notesArray = JSONArray()
            if (syncNotes) {
                val realNotes = try {
                    NoteManager.getAllNotes(context)
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching notes: ${e.message}")
                    emptyList()
                }

                for (n in realNotes) {
                    val nObj = JSONObject().apply {
                        put("id", n.file.nameWithoutExtension)
                        put("title", if (n.contactName.isNotBlank() && n.contactName != "Unknown") n.contactName else "Note")
                        put("content", n.content)
                        put("timestamp", n.lastModified)
                    }
                    notesArray.put(nObj)
                }
            }

            // Build wrapper payload
            val payload = JSONObject().apply {
                put("contacts", contactsArray)
                put("callLogs", callLogsArray)
                put("smsThreads", smsArray)
                put("notes", notesArray)
                put("deviceName", Build.MODEL)
                put("timestamp", System.currentTimeMillis())
            }

            val root = JSONObject().apply {
                put("type", "sync_data")
                put("payload", payload)
            }

            writer.write(root.toString())
            writer.write("\n")
            writer.flush()

            _lastSyncTimestamp.value = System.currentTimeMillis()
            _status.value = PcSyncStatus.CONNECTED
            Log.i(TAG, "Sync payload sent: ${contactsArray.length()} contacts, ${callLogsArray.length()} calls, ${smsArray.length()} threads, ${notesArray.length()} notes")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending sync data: ${e.message}", e)
            _status.value = PcSyncStatus.ERROR
        }
    }
}
