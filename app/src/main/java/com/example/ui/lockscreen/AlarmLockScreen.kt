package com.example.ui.lockscreen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
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
import kotlinx.coroutines.delay

enum class LockoutStage {
    ALARM_RINGING,
    GRACE_COUNTDOWN,
    CAMERA_VERIFYING,
    ESCALATED_REENGAGED
}

@Composable
fun AlarmLockScreen(
    medName: String,
    medDosage: String,
    referencePhotoUri: String,
    alarmToneName: String,
    graceMinutes: Int,
    onSilenceForGrace: () -> Unit,
    onEscalateBackOn: () -> Unit,
    onVerifiedDismissal: (photoUri: String) -> Unit
) {
    // Intercept back gesture - trapped until photo verification
    BackHandler(enabled = true) {}

    val context = LocalContext.current
    var stage by remember { mutableStateOf(LockoutStage.ALARM_RINGING) }
    val totalGraceSeconds = remember(graceMinutes) { (graceMinutes.coerceAtLeast(1)) * 60 }
    var remainingSeconds by remember(totalGraceSeconds) { mutableIntStateOf(totalGraceSeconds) }

    // Camera Permission Launcher
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            stage = LockoutStage.CAMERA_VERIFYING
        }
    }

    // Grace Countdown Timer Effect
    LaunchedEffect(stage, remainingSeconds) {
        if (stage == LockoutStage.GRACE_COUNTDOWN || stage == LockoutStage.CAMERA_VERIFYING) {
            if (remainingSeconds > 0) {
                delay(1000L)
                remainingSeconds -= 1
            } else {
                // Timer reached 00:00 -> ESCALATION
                stage = LockoutStage.ESCALATED_REENGAGED
                onEscalateBackOn()
            }
        }
    }

    val alertPulseColor by animateColorAsState(
        targetValue = if (stage == LockoutStage.ESCALATED_REENGAGED || stage == LockoutStage.ALARM_RINGING) CyberRedAlert else CyberAmber,
        animationSpec = infiniteRepeatable(
            animation = tween(450),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alertPulse"
    )

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("alarm_lock_screen"),
        color = if (stage == LockoutStage.ESCALATED_REENGAGED) Color(0xFF140507) else CyberBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            // Stage Status Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = when (stage) {
                            LockoutStage.ESCALATED_REENGAGED -> CyberRedAlert.copy(alpha = 0.25f)
                            LockoutStage.ALARM_RINGING -> CyberRedAlert.copy(alpha = 0.15f)
                            else -> CyberSurfaceVariant
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = when (stage) {
                            LockoutStage.ESCALATED_REENGAGED -> CyberRedAlert
                            LockoutStage.ALARM_RINGING -> CyberRedAlert
                            else -> CyberCyan
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(vertical = 12.dp, horizontal = 16.dp)
            ) {
                Icon(
                    imageVector = when (stage) {
                        LockoutStage.ESCALATED_REENGAGED -> Icons.Default.Warning
                        LockoutStage.ALARM_RINGING -> Icons.Default.VolumeUp
                        LockoutStage.GRACE_COUNTDOWN -> Icons.Default.VolumeOff
                        LockoutStage.CAMERA_VERIFYING -> Icons.Default.CameraAlt
                    },
                    contentDescription = null,
                    tint = if (stage == LockoutStage.ESCALATED_REENGAGED || stage == LockoutStage.ALARM_RINGING) CyberRedAlert else CyberCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = when (stage) {
                            LockoutStage.ALARM_RINGING -> "MEDICATION ALARM RINGING"
                            LockoutStage.GRACE_COUNTDOWN -> "GRACE TIMER ACTIVE (MUTED)"
                            LockoutStage.CAMERA_VERIFYING -> "CAMERA PROOF IN PROGRESS"
                            LockoutStage.ESCALATED_REENGAGED -> "ESCALATION: TIMER EXPIRED"
                        },
                        color = if (stage == LockoutStage.ESCALATED_REENGAGED || stage == LockoutStage.ALARM_RINGING) CyberRedAlert else CyberCyan,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (stage == LockoutStage.ESCALATED_REENGAGED) "MAXIMUM SIREN RESUMED • SNAP PHOTO TO STOP"
                        else if (stage == LockoutStage.GRACE_COUNTDOWN) "Walk to medicine & take dose before timer ends"
                        else "Tone: $alarmToneName",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Medicine Name Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CyberBorder)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = medName.uppercase(),
                        color = TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                    if (medDosage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "PRESCRIBED DOSAGE: $medDosage",
                            color = CyberCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stage A: ALARM RINGING
            if (stage == LockoutStage.ALARM_RINGING) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, alertPulseColor)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = alertPulseColor,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "ALARM LOOPING AT FULL GAIN",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Silence the siren and receive a $graceMinutes-minute countdown window to reach your medicine and take your dose.",
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                stage = LockoutStage.GRACE_COUNTDOWN
                                onSilenceForGrace()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("silence_and_start_grace_button")
                        ) {
                            Icon(Icons.Default.VolumeOff, contentDescription = null, tint = CyberBackground)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SILENCE & START ${graceMinutes}M GRACE TIMER",
                                color = CyberBackground,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Stage B & C & D: GRACE TIMER COUNTDOWN, CAMERA VERIFYING, OR ESCALATED
            if (stage != LockoutStage.ALARM_RINGING) {
                // Live Countdown Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (stage == LockoutStage.ESCALATED_REENGAGED) CyberRedAlert.copy(alpha = 0.15f) else CyberSurface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        if (stage == LockoutStage.ESCALATED_REENGAGED) CyberRedAlert else CyberCyan
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (stage == LockoutStage.ESCALATED_REENGAGED) "ESCALATION BREACH: 00:00" else "GRACE COUNTDOWN REMAINING",
                            color = if (stage == LockoutStage.ESCALATED_REENGAGED) CyberRedAlert else CyberAmber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val formattedTime = String.format(
                            "%02d:%02d",
                            remainingSeconds / 60,
                            remainingSeconds % 60
                        )
                        Text(
                            text = formattedTime,
                            color = if (stage == LockoutStage.ESCALATED_REENGAGED) CyberRedAlert else TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 42.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        val progress = if (totalGraceSeconds > 0) (remainingSeconds.toFloat() / totalGraceSeconds.toFloat()) else 0f
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = if (stage == LockoutStage.ESCALATED_REENGAGED) CyberRedAlert else CyberCyan,
                            trackColor = CyberBorder
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Walk to your medicine, take your dose, and capture photo proof before timer expires.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Reference Snapshot vs Active Camera Preview
                if (stage == LockoutStage.CAMERA_VERIFYING) {
                    // Active Camera Viewfinder for Verification
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CyberSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, CyberCyan)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "CAPTURE VERIFICATION SNAPSHOT",
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                            ) {
                                com.example.ui.camera.CameraPreview(
                                    modifier = Modifier.fillMaxSize(),
                                    guidanceLabel = "[ SNAP ACTIVE DOSE PROOF ]",
                                    captureButtonLabel = "CONFIRM & VERIFY DOSE",
                                    onImageCaptured = { _, savedUri ->
                                        onVerifiedDismissal(savedUri)
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Show reference photo and trigger button
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CyberSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, CyberBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "REGISTERED REFERENCE PHOTO",
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            if (referencePhotoUri.isNotBlank()) {
                                AsyncImage(
                                    model = referencePhotoUri,
                                    contentDescription = "Reference Medicine Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(160.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .background(CyberSurfaceVariant, RoundedCornerShape(12.dp))
                                        .border(1.dp, CyberBorder, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = {
                                    if (hasCameraPermission) {
                                        stage = LockoutStage.CAMERA_VERIFYING
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (stage == LockoutStage.ESCALATED_REENGAGED) CyberRedAlert else CyberCyan
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("take_verification_photo_button")
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = CyberBackground)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "TAKE VERIFICATION PHOTO",
                                    color = CyberBackground,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
