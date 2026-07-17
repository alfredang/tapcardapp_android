package com.tertiaryinfotech.tapcard.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactMail
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.tertiaryinfotech.tapcard.ocr.QrAnalyzer
import com.tertiaryinfotech.tapcard.ui.theme.DisplayFontFamily
import com.tertiaryinfotech.tapcard.vm.CardViewModel
import java.io.File

/** The three capture guides. All route through the same auto-detecting capture
 *  (QR first, then on-device OCR); each just frames the subject differently. */
private enum class ScanMode(val label: String, val icon: ImageVector, val frameAspect: Float) {
    EVENT_BADGE("Event Badge", Icons.Filled.Badge, 0.72f),
    PAPER_CARD("Paper Card", Icons.Filled.ContactMail, 1.58f),
    QR_CODE("QR Code", Icons.Filled.QrCode2, 1f),
}

@Composable
fun ScanScreen(vm: CardViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
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

    val imageCapture = remember { ImageCapture.Builder().build() }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var torchOn by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(ScanMode.QR_CODE) }

    // Release the camera (preview, analysis, torch) when we leave the scan screen,
    // so it doesn't keep running in the background and draining the battery.
    DisposableEffect(Unit) {
        onDispose { runCatching { cameraProvider?.unbindAll() } }
    }

    // Pick an existing photo from the gallery and run it through the same reader.
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? -> uri?.let(vm::onImageCaptured) }

    // Keep the torch state in sync with the bound camera.
    LaunchedEffect(camera, torchOn) {
        camera?.let { c ->
            if (c.cameraInfo.hasFlashUnit()) runCatching { c.cameraControl.enableTorch(torchOn) }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val providerFuture = ProcessCameraProvider.getInstance(ctx)
                    providerFuture.addListener({
                        val provider = providerFuture.get()
                        cameraProvider = provider
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        // Live QR/barcode detection — fires the moment a code is framed.
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .apply {
                                setAnalyzer(
                                    ContextCompat.getMainExecutor(ctx),
                                    QrAnalyzer(vm::onScannedCard),
                                )
                            }
                        provider.unbindAll()
                        runCatching {
                            camera = provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture,
                                analysis,
                            )
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
            )

            // Corner-bracket framing guide (shape follows the selected mode).
            CornerFrame(mode)

            // Title + subtitle, near the top.
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 56.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Scan contact information",
                    color = Color.White,
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Scan any QR code, including LinkedIn QR codes",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                )
            }

            // Bottom control panel.
            Box(Modifier.align(Alignment.BottomCenter)) {
                ScanControls(
                    mode = mode,
                    onMode = { mode = it },
                    torchOn = torchOn,
                    enabled = !vm.isScanning,
                    onGallery = { galleryLauncher.launch("image/*") },
                    onShutter = { takePhoto(context, imageCapture, vm::onImageCaptured) },
                    onTorch = { torchOn = !torchOn },
                )
            }
        } else {
            PermissionPrompt(
                onGrant = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onManual = vm::enterScannedManually,
            )
        }

        // Top bar: close (X) + "Enter manually" pill — always on top.
        Row(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(onClick = vm::goHome),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
            }
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(onClick = vm::enterScannedManually)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Enter manually", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }

        if (vm.isScanning) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    Text("Reading…", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun BoxScope.CornerFrame(mode: ScanMode) {
    Box(
        Modifier
            .align(Alignment.Center)
            .fillMaxWidth()
            .padding(horizontal = 40.dp)
            .aspectRatio(mode.frameAspect),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val len = 34.dp.toPx()
            val sw = 5.dp.toPx()
            val col = Color.White
            val w = size.width
            val h = size.height
            fun seg(a: Offset, b: Offset) = drawLine(col, a, b, sw, cap = StrokeCap.Round)
            // Top-left
            seg(Offset(0f, 0f), Offset(len, 0f)); seg(Offset(0f, 0f), Offset(0f, len))
            // Top-right
            seg(Offset(w, 0f), Offset(w - len, 0f)); seg(Offset(w, 0f), Offset(w, len))
            // Bottom-left
            seg(Offset(0f, h), Offset(len, h)); seg(Offset(0f, h), Offset(0f, h - len))
            // Bottom-right
            seg(Offset(w, h), Offset(w - len, h)); seg(Offset(w, h), Offset(w, h - len))
        }
    }
}

@Composable
private fun ScanControls(
    mode: ScanMode,
    onMode: (ScanMode) -> Unit,
    torchOn: Boolean,
    enabled: Boolean,
    onGallery: () -> Unit,
    onShutter: () -> Unit,
    onTorch: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(Color.Black.copy(alpha = 0.92f))
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 18.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ScanMode.values().forEach { m ->
                ModeChip(m, selected = m == mode, modifier = Modifier.weight(1f)) { onMode(m) }
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CircleControl(Icons.Filled.Image, "Choose from gallery", onClick = onGallery)
            Shutter(enabled = enabled, onClick = onShutter)
            CircleControl(
                if (torchOn) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
                "Toggle flashlight",
                active = torchOn,
                onClick = onTorch,
            )
        }
    }
}

@Composable
private fun ModeChip(mode: ScanMode, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f))
            .border(1.5.dp, if (selected) Color.White else Color.Transparent, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(mode.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(6.dp))
        Text(
            mode.label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun CircleControl(icon: ImageVector, desc: String, active: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(if (active) Color.White else Color.White.copy(alpha = 0.14f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = desc, tint = if (active) Color.Black else Color.White, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun Shutter(enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(74.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (enabled) 1f else 0.5f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(62.dp)
                .clip(CircleShape)
                .border(3.dp, Color.Black.copy(alpha = 0.2f), CircleShape),
        )
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
            "Camera access is needed to scan contact information.",
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
