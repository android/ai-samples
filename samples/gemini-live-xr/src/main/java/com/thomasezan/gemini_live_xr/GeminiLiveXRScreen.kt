package com.thomasezan.gemini_live_xr

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.ai.uicomponent.GenerateButton
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.seconds

@Composable
fun GeminiLiveXRScreen(viewModel: GeminiLiveXRViewModel = hiltViewModel()) {
    val liveSessionState by viewModel.liveSessionState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current as Activity
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
        viewModel.initializeGeminiLive(activity)
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (hasCameraPermission) {
            CameraPreview(
                onFrameCaptured = { bitmap ->
                    viewModel.sendVideoFrame(bitmap)
                }
            )
        }

        when (liveSessionState) {
            is LiveSessionState.NotReady -> {
                CircularProgressIndicator()
            }
            is LiveSessionState.Ready, is LiveSessionState.Running -> {
                val isRunning = liveSessionState is LiveSessionState.Running
                GenerateButton(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
                    text = if (isRunning) "Stop Live Session" else "Start Live Session",
                    icon = painterResource(id = com.android.ai.uicomponent.R.drawable.ic_ai_mic),
                    onClick = {
                        viewModel.toggleLiveSession(activity)
                    }
                )
            }
            is LiveSessionState.Error -> {
                GenerateButton(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
                    text = "Retry Initialization",
                    onClick = {
                        viewModel.initializeGeminiLive(activity)
                    }
                )
            }
        }
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onFrameCaptured: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val isXRDevice = remember {
        context.packageManager.hasSystemFeature("android.hardware.type.pc") ||
                context.packageManager.hasSystemFeature("com.google.android.feature.XR")
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                if (isXRDevice) {
                    visibility = android.view.View.INVISIBLE
                }
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = buildImageAnalysis(cameraExecutor, onFrameCaptured)

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Binding failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}

private fun buildImageAnalysis(
    executor: Executor,
    onFrameCaptured: (Bitmap) -> Unit
): ImageAnalysis {
    var lastFrameTimestamp = 0L
    val interval = 1.seconds // Only capture one frame per second

    return ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
        .build()
        .also {
            it.setAnalyzer(executor) { imageProxy ->
                val currentTimestamp = System.currentTimeMillis()

                if (currentTimestamp - lastFrameTimestamp >= interval.inWholeMilliseconds) {
                    val bitmap = imageProxy.toBitmap()
                    onFrameCaptured(bitmap)
                    lastFrameTimestamp = currentTimestamp
                }
                imageProxy.close()
            }
        }
}
