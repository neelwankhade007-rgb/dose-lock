package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.alarm.AlarmTone

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dosage: String,
    val scheduledHour: Int, // 0-23
    val scheduledMinute: Int, // 0-59
    val referencePhotoPath: String = "", // Saved reference snapshot URI/Path of pill bottle/strip
    val alarmTone: AlarmTone = AlarmTone.SIREN,
    val gracePeriodMinutes: Int = 3, // Customizable 1, 3, 5, 10 min
    val isActive: Boolean = true,
    val instructions: String = ""
)
