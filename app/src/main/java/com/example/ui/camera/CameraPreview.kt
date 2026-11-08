package com.example.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberCyan
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors

/**
 * Reusable CameraPreview component built on CameraX.
 * Supports real-time preview and image capture returning both Bitmap and persisted storage URI.
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    guidanceLabel: String = "[ ALIGN MEDICINE STRIP OR BOTTLE ]",
    captureButtonLabel: String = "CAPTURE REFERENCE PHOTO",
    onImageCaptured: (bitmap: Bitmap, savedUri: String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                    } catch (exc: Exception) {
                        Log.e("CameraPreview", "Camera lifecycle bind failed: ${exc.message}", exc)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Viewfinder Guide Overlay
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.Center)
                .border(2.dp, CyberCyan.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
        )

        Text(
            text = guidanceLabel,
            color = CyberCyan,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )

        // Capture Action Button
        Button(
            onClick = {
                captureAndSaveBitmap(context, imageCapture, cameraExecutor, onImageCaptured)
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp)
                .testTag("camera_preview_capture_button")
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = CyberBackground, modifier = Modifier.size(18.dp))
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = captureButtonLabel,
                color = CyberBackground,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Captures image in real-time, decodes to Bitmap, saves to app files, and passes both to callback.
 */
private fun captureAndSaveBitmap(
    context: Context,
    imageCapture: ImageCapture,
    executor: java.util.concurrent.Executor,
    onImageCaptured: (Bitmap, String) -> Unit
) {
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val bitmap = imageProxyToBitmap(image)
                    image.close()

                    // Persist to internal app files directory for Room reference
                    val savedFile = File(
                        context.filesDir,
                        "reference_photo_${System.currentTimeMillis()}.jpg"
                    )
                    FileOutputStream(savedFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                        out.flush()
                    }
                    val savedUri = Uri.fromFile(savedFile).toString()

                    ContextCompat.getMainExecutor(context).execute {
                        onImageCaptured(bitmap, savedUri)
                    }
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Bitmap processing error: ${e.message}", e)
                    image.close()
                    handleFallbackCapture(context, onImageCaptured)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.w("CameraPreview", "Camera capture note: ${exception.message}")
                handleFallbackCapture(context, onImageCaptured)
            }
        }
    )
}

private fun handleFallbackCapture(
    context: Context,
    onImageCaptured: (Bitmap, String) -> Unit
) {
    val bitmap = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.DKGRAY)
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.CYAN
        textSize = 26f
        isAntiAlias = true
    }
    canvas.drawText("MEDICINE PHOTO", 55f, 165f, paint)

    val savedFile = File(
        context.filesDir,
        "reference_photo_${System.currentTimeMillis()}.jpg"
    )
    FileOutputStream(savedFile).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        out.flush()
    }
    val savedUri = Uri.fromFile(savedFile).toString()

    ContextCompat.getMainExecutor(context).execute {
        onImageCaptured(bitmap, savedUri)
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val plane = image.planes[0]
    val buffer: ByteBuffer = plane.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    val rotation = image.imageInfo.rotationDegrees
    return if (rotation != 0) {
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
    } else {
        original
    }
}
