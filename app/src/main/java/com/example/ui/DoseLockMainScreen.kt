package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.alarm.AlarmTone
import com.example.data.model.Medication
import com.example.ui.scanner.CameraPhotoCapture
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGreenSafe
import com.example.ui.theme.CyberRedAlert
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoseLockMainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val medications by viewModel.allMedications.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(CyberSurfaceVariant, RoundedCornerShape(8.dp))
                                .border(1.dp, CyberCyan, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "DOSE//LOCK",
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 18.sp,
                                letterSpacing = 2.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "PHOTO VERIFICATION & GRACE TIMER",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberCyan
                            )
                        }
                    }
                },
                actions = {
                    // Explicit volume/audio test toggle
                    Button(
                        onClick = { viewModel.toggleAudioTest() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.isVolumeTestActive) CyberAmber else CyberSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("audio_test_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isVolumeTestActive) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Test Audio",
                            tint = if (uiState.isVolumeTestActive) CyberBackground else CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.isVolumeTestActive) "SOUND ON" else "TEST SOUND",
                            color = if (uiState.isVolumeTestActive) CyberBackground else CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddMedicationDialog() },
                containerColor = CyberCyan,
                contentColor = CyberBackground,
                modifier = Modifier.testTag("add_medication_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Prescribed Dose")
            }
        },
        containerColor = CyberBackground,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (medications.isEmpty()) {
                EmptyStateView()
            } else {
                MedicationListView(
                    medications = medications,
                    onToggleActive = { viewModel.toggleMedicationActive(it) },
                    onDelete = { viewModel.deleteMedication(it) },
                    onTestAlarm = { viewModel.testInstantAlarm(it) }
                )
            }
        }
    }

    // Add Medication Dialog
    if (uiState.isAddMedicationDialogOpen) {
        AddMedicationDialog(
            initialPhotoUri = uiState.capturedReferencePhoto,
            previewingTone = uiState.previewingTone,
            onDismiss = {
                viewModel.closeAddMedicationDialog()
            },
            onPreviewTone = { tone -> viewModel.previewAlarmTone(tone) },
            onOpenReferenceCamera = { viewModel.setReferenceCameraOpen(true) },
            onSave = { name, dosage, hour, minute, photo, tone, grace ->
                viewModel.saveMedication(
                    name = name,
                    dosage = dosage,
                    hour = hour,
                    minute = minute,
                    photoPath = photo,
                    alarmTone = tone,
                    graceMinutes = grace
                )
            }
        )
    }

    // Camera viewfinder overlay for capturing medicine reference photo
    if (uiState.isReferenceCameraOpen) {
        Dialog(onDismissRequest = { viewModel.setReferenceCameraOpen(false) }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                border = BorderStroke(1.dp, CyberCyan)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CAPTURE MEDICINE REFERENCE PHOTO",
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                    ) {
                        com.example.ui.camera.CameraPreview(
                            modifier = Modifier.fillMaxSize(),
                            guidanceLabel = "[ POSITION MEDICINE BOTTLE OR STRIP ]",
                            captureButtonLabel = "SAVE AS REFERENCE",
                            onImageCaptured = { bitmap, savedUri ->
                                viewModel.onReferencePhotoCaptured(bitmap, savedUri)
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { viewModel.setReferenceCameraOpen(false) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("CANCEL")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Alarm,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "NO MEDICATIONS REGISTERED",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap [+] to add a medicine with a reference photo, alarm tone, and grace countdown timer.",
                color = TextSecondary,
                textAlign = TextAlign.Center,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun MedicationListView(
    medications: List<Medication>,
    onToggleActive: (Medication) -> Unit,
    onDelete: (Medication) -> Unit,
    onTestAlarm: (Medication) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ARMED MEDICATIONS (${medications.size})",
                    color = CyberCyan,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "TAP [▶ TEST] TO TRIGGER FLOW",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        items(medications, key = { it.id }) { med ->
            MedicationCard(
                medication = med,
                onToggleActive = { onToggleActive(med) },
                onDelete = { onDelete(med) },
                onTestAlarm = { onTestAlarm(med) }
            )
        }
    }
}

@Composable
private fun MedicationCard(
    medication: Medication,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit,
    onTestAlarm: () -> Unit
) {
    val hour12 = if (medication.scheduledHour % 12 == 0) 12 else medication.scheduledHour % 12
    val amPm = if (medication.scheduledHour >= 12) "PM" else "AM"
    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d %s", hour12, medication.scheduledMinute, amPm)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("medication_card_${medication.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        border = BorderStroke(1.dp, if (medication.isActive) CyberBorder else CyberBorder.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    // Reference photo thumbnail or camera placeholder
                    if (medication.referencePhotoPath.isNotBlank()) {
                        AsyncImage(
                            model = medication.referencePhotoPath,
                            contentDescription = "Pill Reference",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(CyberSurfaceVariant, RoundedCornerShape(8.dp))
                                .border(1.dp, CyberBorder, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = medication.name,
                            color = if (medication.isActive) TextPrimary else TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (medication.dosage.isNotBlank()) "Dosage: ${medication.dosage}" else "Prescribed Dose",
                            color = if (medication.isActive) CyberCyan else TextMuted,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Switch(
                    checked = medication.isActive,
                    onCheckedChange = { onToggleActive() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberBackground,
                        checkedTrackColor = CyberCyan,
                        uncheckedTrackColor = CyberSurfaceVariant
                    ),
                    modifier = Modifier.testTag("med_toggle_${medication.id}")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Time, Grace Period Badge & Test Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(CyberSurfaceVariant, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = timeFormatted,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        modifier = Modifier
                            .border(1.dp, CyberAmber.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Alarm, contentDescription = null, tint = CyberAmber, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${medication.gracePeriodMinutes}M GRACE",
                            color = CyberAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Test Button - triggers immediate flow
                    Button(
                        onClick = onTestAlarm,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("test_alarm_${medication.id}")
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Test Alarm",
                            tint = CyberBackground,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "TEST",
                            color = CyberBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp).testTag("delete_med_${medication.id}")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Medication",
                            tint = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Tone: ${medication.alarmTone.displayName} • Photo Verification Required",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMedicationDialog(
    initialPhotoUri: String?,
    previewingTone: AlarmTone?,
    onDismiss: () -> Unit,
    onPreviewTone: (AlarmTone) -> Unit,
    onOpenReferenceCamera: () -> Unit,
    onSave: (
        name: String,
        dosage: String,
        hour: Int,
        minute: Int,
        photoUri: String,
        tone: AlarmTone,
        graceMinutes: Int
    ) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var hour by remember { mutableIntStateOf(8) }
    var minute by remember { mutableIntStateOf(30) }
    var selectedTone by remember { mutableStateOf(AlarmTone.SIREN) }
    var graceMinutes by remember { mutableIntStateOf(3) }
    var referencePhoto by remember(initialPhotoUri) { mutableStateOf(initialPhotoUri ?: "") }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onOpenReferenceCamera()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "ARM NEW MEDICATION",
                color = CyberCyan,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Medication Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_med_name"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CyberBorder
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("Dosage (e.g. 500 mg, 1 tablet)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_med_dosage"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CyberBorder
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Schedule Time Picker Card with Radial Clock Dial Dialog
                Text("ALARM SCHEDULE TIME", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(6.dp))

                var showTimePickerModal by remember { mutableStateOf(false) }
                val timePickerState = rememberTimePickerState(
                    initialHour = hour,
                    initialMinute = minute,
                    is24Hour = false
                )

                val displayHour12 = if (hour % 12 == 0) 12 else hour % 12
                val displayAmPm = if (hour >= 12) "PM" else "AM"
                val displayTimeFormatted = String.format(Locale.getDefault(), "%02d:%02d %s", displayHour12, minute, displayAmPm)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberSurfaceVariant, RoundedCornerShape(10.dp))
                        .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                        .clickable { showTimePickerModal = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("open_time_picker_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(CyberBackground, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = displayTimeFormatted,
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Tap to open interactive clock dial",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Button(
                        onClick = { showTimePickerModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "SET TIME",
                            color = CyberBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Interactive Radial Clock Dial TimePickerDialog
                if (showTimePickerModal) {
                    Dialog(onDismissRequest = { showTimePickerModal = false }) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = CyberSurface),
                            border = BorderStroke(1.dp, CyberCyan)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "SELECT ALARM TIME",
                                    color = CyberCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                TimePicker(
                                    state = timePickerState,
                                    colors = TimePickerDefaults.colors(
                                        clockDialColor = CyberBackground,
                                        clockDialSelectedContentColor = CyberBackground,
                                        clockDialUnselectedContentColor = TextPrimary,
                                        selectorColor = CyberCyan,
                                        containerColor = CyberSurface,
                                        periodSelectorBorderColor = CyberBorder,
                                        periodSelectorSelectedContainerColor = CyberCyan,
                                        periodSelectorUnselectedContainerColor = CyberSurfaceVariant,
                                        periodSelectorSelectedContentColor = CyberBackground,
                                        periodSelectorUnselectedContentColor = TextPrimary,
                                        timeSelectorSelectedContainerColor = CyberCyan,
                                        timeSelectorUnselectedContainerColor = CyberSurfaceVariant,
                                        timeSelectorSelectedContentColor = CyberBackground,
                                        timeSelectorUnselectedContentColor = TextPrimary
                                    ),
                                    modifier = Modifier.testTag("material3_time_picker")
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(
                                        onClick = { showTimePickerModal = false },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("CANCEL")
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Button(
                                        onClick = {
                                            hour = timePickerState.hour
                                            minute = timePickerState.minute
                                            showTimePickerModal = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("confirm_time_picker_button")
                                    ) {
                                        Text("OK", color = CyberBackground, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Grace Period Timer Selector (1, 3, 5, 10 min)
                Text("GRACE TIMER DURATION", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1, 3, 5, 10).forEach { mins ->
                        val isSelected = graceMinutes == mins
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) CyberCyan.copy(alpha = 0.2f) else CyberSurfaceVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) CyberCyan else CyberBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { graceMinutes = mins }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${mins}m",
                                color = if (isSelected) CyberCyan else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Capture Reference Photo of Medicine
                Text("PROOF OF MEDICATION: REFERENCE PHOTO", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberSurfaceVariant, RoundedCornerShape(10.dp))
                        .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        if (referencePhoto.isNotBlank()) {
                            AsyncImage(
                                model = referencePhoto,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(CyberBackground, RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = TextMuted)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (referencePhoto.isNotBlank()) "Photo Saved" else "No Photo Saved",
                            color = if (referencePhoto.isNotBlank()) CyberGreenSafe else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Button(
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                onOpenReferenceCamera()
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("capture_medicine_photo_button")
                    ) {
                        Text(
                            text = if (referencePhoto.isNotBlank()) "RETAKE" else "CAPTURE",
                            color = CyberBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Alarm Tone Selection with Preview
                Text("ALARM TONE", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AlarmTone.values().forEach { tone ->
                        val isSelected = selectedTone == tone
                        val isPreviewingThis = previewingTone == tone
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) CyberCyan.copy(alpha = 0.15f) else CyberSurfaceVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) CyberCyan else CyberBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedTone = tone }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tone.displayName,
                                    color = if (isSelected) CyberCyan else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = tone.description,
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                            IconButton(
                                onClick = { onPreviewTone(tone) },
                                modifier = Modifier.size(32.dp).testTag("preview_tone_${tone.name}")
                            ) {
                                Icon(
                                    imageVector = if (isPreviewingThis) Icons.Default.Stop else Icons.Default.VolumeUp,
                                    contentDescription = "Preview Tone",
                                    tint = if (isPreviewingThis) CyberAmber else CyberCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            name,
                            dosage,
                            hour,
                            minute,
                            referencePhoto,
                            selectedTone,
                            graceMinutes
                        )
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("save_medication_button")
            ) {
                Text("ARM MEDICATION", color = CyberBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("CANCEL")
            }
        },
        containerColor = CyberSurface
    )
}
