package com.tertiaryinfotech.tapcard.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.tertiaryinfotech.tapcard.vm.CardViewModel
import java.io.File

@Composable
fun ScanScreen(vm: CardViewModel) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    DisposableEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
        onDispose { }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            CameraCapture(
                onCaptured = vm::onImageCaptured,
                enabled = !vm.isScanning,
            )
        } else {
            PermissionPrompt(
                onGrant = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onManual = vm::startManualEntry,
            )
        }

        // Top bar: back + title.
        Row(
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = vm::goHome) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Scan business card", color = Color.White, fontWeight = FontWeight.SemiBold)
        }

        if (vm.isScanning) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    Text("Reading card…", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun CameraCapture(onCaptured: (Uri) -> Unit, enabled: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                providerFuture.addListener({
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    provider.unbindAll()
                    runCatching {
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture,
                        )
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
        )

        // Framing guide for the card.
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .align(Alignment.Center)
                .aspectRatio(1.6f)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(16.dp)),
        )

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Align the card inside the frame",
                color = Color.White,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(16.dp))
            // Shutter button.
            Box(
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(if (enabled) Color.White else Color.Gray)
                    .clickable(enabled = enabled) { takePhoto(context, imageCapture, onCaptured) },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .border(3.dp, Color.Black.copy(alpha = 0.25f), CircleShape),
                )
            }
        }
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onCaptured: (Uri) -> Unit,
) {
    val dir = File(context.cacheDir, "share").apply { mkdirs() }
    val file = File(dir, "scan-${System.nanoTime()}.jpg")
    val options = ImageCapture.OutputFileOptions.Builder(file).build()
    imageCapture.takePicture(
        options,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onCaptured(Uri.fromFile(file))
            }

            override fun onError(exception: ImageCaptureException) {
                // Surface nothing fancy — fall through; user can retry.
            }
        },
    )
}

@Composable
private fun PermissionPrompt(onGrant: () -> Unit, onManual: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "Camera access is needed to scan a business card.",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onGrant) { Text("Allow camera") }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.clickable(onClick = onManual).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text("Enter details manually", color = Color.White)
        }
    }
}
