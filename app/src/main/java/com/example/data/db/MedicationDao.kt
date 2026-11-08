package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DoseLog
import com.example.data.model.Medication
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications ORDER BY scheduledHour ASC, scheduledMinute ASC")
    fun getAllMedications(): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE isActive = 1")
    fun getActiveMedications(): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE id = :id LIMIT 1")
    suspend fun getMedicationById(id: Long): Medication?

    @Query("SELECT * FROM medications WHERE isActive = 1")
    suspend fun getActiveMedicationsSync(): List<Medication>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: Medication): Long

    @Update
    suspend fun updateMedication(medication: Medication)

    @Delete
    suspend fun deleteMedication(medication: Medication)

    // Dose Logs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoseLog(log: DoseLog): Long

    @Query("SELECT * FROM dose_logs ORDER BY takenTimestamp DESC")
    fun getAllDoseLogs(): Flow<List<DoseLog>>

    @Query("SELECT * FROM dose_logs WHERE medicationId = :medicationId ORDER BY takenTimestamp DESC")
    fun getDoseLogsForMedication(medicationId: Long): Flow<List<DoseLog>>

    @Query("SELECT COUNT(*) FROM dose_logs WHERE status = 'TAKEN_VERIFIED'")
    fun getVerifiedDosesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM dose_logs WHERE status = 'ESCALATED'")
    fun getEscalatedDosesCount(): Flow<Int>
}
