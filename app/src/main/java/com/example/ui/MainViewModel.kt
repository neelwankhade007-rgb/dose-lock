package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.alarm.AlarmTone
import com.example.alarm.DoseScheduler
import com.example.alarm.SynthesizedSoundEngine
import com.example.data.model.Medication
import com.example.data.repository.MedicationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DoseLockUiState(
    val isAddMedicationDialogOpen: Boolean = false,
    val isReferenceCameraOpen: Boolean = false,
    val capturedReferencePhoto: String? = null,
    val previewingTone: AlarmTone? = null,
    val isVolumeTestActive: Boolean = false
)

class MainViewModel(
    private val repository: MedicationRepository,
    private val scheduler: DoseScheduler
) : ViewModel() {

    private val previewSoundEngine = SynthesizedSoundEngine()

    val allMedications: StateFlow<List<Medication>> = repository.allMedications
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(DoseLockUiState())
    val uiState: StateFlow<DoseLockUiState> = _uiState.asStateFlow()

    init {
        // Seed default medication if none exist
        viewModelScope.launch {
            val existing = repository.getActiveMedicationsSync()
            if (existing.isEmpty()) {
                val defaultMed = Medication(
                    name = "Metformin HCl",
                    dosage = "500 mg",
                    scheduledHour = 8,
                    scheduledMinute = 0,
                    referencePhotoPath = "",
                    alarmTone = AlarmTone.SIREN,
                    gracePeriodMinutes = 3,
                    instructions = "Take with water after breakfast."
                )
                val id = repository.insertMedication(defaultMed)
                scheduler.scheduleMedicationAlarm(defaultMed.copy(id = id))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        previewSoundEngine.stop()
    }

    fun openAddMedicationDialog() {
        _uiState.value = _uiState.value.copy(
            isAddMedicationDialogOpen = true,
            capturedReferencePhoto = null
        )
    }

    fun closeAddMedicationDialog() {
        previewSoundEngine.stop()
        _uiState.value = _uiState.value.copy(
            isAddMedicationDialogOpen = false,
            isReferenceCameraOpen = false,
            previewingTone = null
        )
    }

    fun setReferenceCameraOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(isReferenceCameraOpen = open)
    }

    fun onReferencePhotoCaptured(bitmap: android.graphics.Bitmap, photoUri: String) {
        _uiState.value = _uiState.value.copy(
            capturedReferencePhoto = photoUri,
            isReferenceCameraOpen = false
        )
    }

    fun toggleAudioTest() {
        if (_uiState.value.isVolumeTestActive) {
            previewSoundEngine.stop()
            _uiState.value = _uiState.value.copy(isVolumeTestActive = false, previewingTone = null)
        } else {
            _uiState.value = _uiState.value.copy(isVolumeTestActive = true, previewingTone = AlarmTone.SIREN)
            previewSoundEngine.startLoop(AlarmTone.SIREN)
        }
    }

    fun previewAlarmTone(tone: AlarmTone) {
        if (_uiState.value.previewingTone == tone) {
            previewSoundEngine.stop()
            _uiState.value = _uiState.value.copy(previewingTone = null, isVolumeTestActive = false)
        } else {
            _uiState.value = _uiState.value.copy(previewingTone = tone, isVolumeTestActive = true)
            previewSoundEngine.playPreview(tone, durationMs = 3000L) {
                if (_uiState.value.previewingTone == tone) {
                    _uiState.value = _uiState.value.copy(previewingTone = null, isVolumeTestActive = false)
                }
            }
        }
    }

    fun saveMedication(
        name: String,
        dosage: String,
        hour: Int,
        minute: Int,
        photoPath: String,
        alarmTone: AlarmTone,
        graceMinutes: Int
    ) {
        previewSoundEngine.stop()
        viewModelScope.launch {
            val med = Medication(
                name = name,
                dosage = dosage,
                scheduledHour = hour,
                scheduledMinute = minute,
                referencePhotoPath = photoPath,
                alarmTone = alarmTone,
                gracePeriodMinutes = graceMinutes,
                isActive = true
            )
            val id = repository.insertMedication(med)
            val insertedMed = med.copy(id = id)
            scheduler.scheduleMedicationAlarm(insertedMed)
            closeAddMedicationDialog()
        }
    }

    fun toggleMedicationActive(medication: Medication) {
        viewModelScope.launch {
            val updated = medication.copy(isActive = !medication.isActive)
            repository.updateMedication(updated)
            if (updated.isActive) {
                scheduler.scheduleMedicationAlarm(updated)
            } else {
                scheduler.cancelMedicationAlarm(updated.id)
            }
        }
    }

    fun deleteMedication(medication: Medication) {
        viewModelScope.launch {
            scheduler.cancelMedicationAlarm(medication.id)
            repository.deleteMedication(medication)
        }
    }

    fun testInstantAlarm(medication: Medication) {
        previewSoundEngine.stop()
        _uiState.value = _uiState.value.copy(isVolumeTestActive = false)
        scheduler.triggerInstantTestAlarm(medication)
    }
}

class MainViewModelFactory(
    private val repository: MedicationRepository,
    private val scheduler: DoseScheduler
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(repository, scheduler) as T
    }
}
