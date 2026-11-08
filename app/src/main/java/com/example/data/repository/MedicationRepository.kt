package com.example.data.repository

import com.example.data.db.MedicationDao
import com.example.data.model.DoseLog
import com.example.data.model.DoseStatus
import com.example.data.model.Medication
import kotlinx.coroutines.flow.Flow

class MedicationRepository(private val dao: MedicationDao) {
    val allMedications: Flow<List<Medication>> = dao.getAllMedications()
    val activeMedications: Flow<List<Medication>> = dao.getActiveMedications()
    val allDoseLogs: Flow<List<DoseLog>> = dao.getAllDoseLogs()
    val verifiedCount: Flow<Int> = dao.getVerifiedDosesCount()
    val escalatedCount: Flow<Int> = dao.getEscalatedDosesCount()

    suspend fun getMedicationById(id: Long): Medication? {
        return dao.getMedicationById(id)
    }

    suspend fun getActiveMedicationsSync(): List<Medication> {
        return dao.getActiveMedicationsSync()
    }

    suspend fun insertMedication(medication: Medication): Long {
        return dao.insertMedication(medication)
    }

    suspend fun updateMedication(medication: Medication) {
        dao.updateMedication(medication)
    }

    suspend fun deleteMedication(medication: Medication) {
        dao.deleteMedication(medication)
    }

    suspend fun logDose(
        medicationId: Long,
        scheduledTimestamp: Long,
        status: DoseStatus,
        verificationMetadata: String
    ): Long {
        val log = DoseLog(
            medicationId = medicationId,
            scheduledTimestamp = scheduledTimestamp,
            takenTimestamp = System.currentTimeMillis(),
            status = status,
            verificationMetadata = verificationMetadata
        )
        return dao.insertDoseLog(log)
    }
}
