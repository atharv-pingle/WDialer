package com.coolappstore.everdialer.by.svhp.modal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UnifiedCallLogDao {

    @Query("SELECT * FROM unified_call_logs ORDER BY timestamp DESC")
    fun getAllCallLogsFlow(): Flow<List<UnifiedCallLog>>

    @Query("SELECT * FROM unified_call_logs ORDER BY timestamp DESC")
    fun getAllCallLogsSync(): List<UnifiedCallLog>

    @Query("SELECT * FROM unified_call_logs WHERE callType = :type ORDER BY timestamp DESC")
    fun getCallsByTypeFlow(type: CallType): Flow<List<UnifiedCallLog>>

    @Query("SELECT * FROM unified_call_logs WHERE callSource = :source ORDER BY timestamp DESC")
    fun getCallsBySourceFlow(source: CallSource): Flow<List<UnifiedCallLog>>

    @Query("SELECT * FROM unified_call_logs WHERE callType = 'MISSED' ORDER BY timestamp DESC")
    fun getMissedCallsFlow(): Flow<List<UnifiedCallLog>>

    @Query("SELECT * FROM unified_call_logs WHERE phoneNumber = :phoneNumber ORDER BY timestamp DESC LIMIT 1")
    suspend fun getMostRecentByNumber(phoneNumber: String): UnifiedCallLog?

    @Query("SELECT * FROM unified_call_logs WHERE phoneNumber = :phoneNumber AND timestamp > :sinceTimestamp ORDER BY timestamp DESC LIMIT 1")
    suspend fun getRecentByNumberSince(phoneNumber: String, sinceTimestamp: Long): UnifiedCallLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(callLog: UnifiedCallLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(callLogs: List<UnifiedCallLog>): List<Long>

    @Update
    suspend fun update(callLog: UnifiedCallLog): Int

    @Query("DELETE FROM unified_call_logs WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM unified_call_logs WHERE phoneNumber = :phoneNumber")
    suspend fun deleteByNumber(phoneNumber: String): Int

    @Query("DELETE FROM unified_call_logs")
    suspend fun clearAll(): Int
}
