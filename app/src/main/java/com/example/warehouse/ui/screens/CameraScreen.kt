package com.example.warehouse.ui.screens

import android.Manifest
import android.content.Context
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.warehouse.ui.theme.SafetyOrange
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.util.concurrent.Executor

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onPhotoTaken: (File) -> Unit,
    onError: (String) -> Unit
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    if (cameraPermissionState.status.isGranted) {
        CameraPreview(onPhotoTaken, onError)
    } else {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("Wymagane uprawnienia kamery", color = Color.White)
        }
    }
}

@Composable
fun CameraPreview(
    onPhotoTaken: (File) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    val previewView = remember { PreviewView(context) }
    var isCapturing by remember { mutableStateOf(false) }

    LaunchedEffect(lifecycleOwner) {
        android.util.Log.d("WAREHOUSE_CAMERA", "Starting camera binding...")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
                android.util.Log.d("WAREHOUSE_CAMERA", "Camera bound successfully")
            } catch (exc: Exception) {
                android.util.Log.e("WAREHOUSE_CAMERA", "Camera binding failed", exc)
                onError("Nie udało się uruchomić kamery: ${exc.message}. Sprawdź uprawnienia.")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        
        // Overlay Celownika - Pełny ekran / Pionowy
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize(0.85f) // Wykorzystaj 85% ekranu
                .border(2.dp, SafetyOrange.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
        )

        // Rogi celownika dla lepszej widoczności (opcjonalne, ale pomocne)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize(0.85f)
                .border(4.dp, SafetyOrange, RoundedCornerShape(16.dp))
        )

        // Shutter Button
        Button(
            onClick = {
                if (!isCapturing) {
                    val capture = imageCapture
                    if (capture != null) {
                        isCapturing = true
                        capturePhoto(context, capture, onPhotoTaken, onError) {
                            isCapturing = false
                        }
                    } else {
                        onError("Kamera nie jest gotowa")
                    }
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp).size(80.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange),
            shape = MaterialTheme.shapes.extraLarge,
            enabled = !isCapturing
        ) {
             if (isCapturing) {
                 CircularProgressIndicator(color = Color.White)
             } else {
                 Text("SKANUJ")
             }
        }
    }
}

private fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onPhotoTaken: (File) -> Unit,
    onError: (String) -> Unit,
    onFinished: () -> Unit
) {
    val photoFile = File(
        context.externalCacheDir,
        "ocr_${System.currentTimeMillis()}.jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onFinished()
                onPhotoTaken(photoFile)
            }

            override fun onError(exc: ImageCaptureException) {
                onFinished()
                onError("Nie udało się zapisać zdjęcia: ${exc.message}")
            }
        }
    )
}
