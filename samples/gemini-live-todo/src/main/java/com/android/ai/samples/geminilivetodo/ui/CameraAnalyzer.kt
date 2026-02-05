package com.android.ai.samples.geminilivetodo.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.core.resolutionselector.ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.xr.projected.ProjectedContext
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.seconds

class CameraAnalyzer(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
) {
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    @OptIn(ExperimentalProjectedApi::class)
    fun startCamera(onFrameCaptured: (Bitmap) -> Unit) {
      val cameraProviderFuture =
          ProcessCameraProvider.getInstance(ProjectedContext.createProjectedDeviceContext(context))
      cameraProviderFuture.addListener(
          {
              val cameraProvider = cameraProviderFuture.get()

              val imageAnalysis =
                  ImageAnalysis.Builder()
                      .setResolutionSelector(
                          ResolutionSelector.Builder()
                              .setResolutionStrategy(
                                  ResolutionStrategy(Size(640, 480), FALLBACK_RULE_CLOSEST_LOWER)
                              )
                              .build()
                          )
                      .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                      .build()
                      .also {
                          it.setAnalyzer(
                              cameraExecutor,
                              SnapshotFrameAnalyzer(onFrameCaptured)
                          )
                      }

              val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
              if (!cameraProvider.hasCamera(cameraSelector)) {
                  Log.w(TAG, "The selected camera is not available.")
                  return@addListener
              }

              try {
                  cameraProvider.unbindAll()
                  cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageAnalysis)
              } catch (exc: Exception) {
                  Log.e(TAG, "Binding camera use cases failed.", exc)
              }
          },
          ContextCompat.getMainExecutor(context)
          )
    }

    fun shutdown() {
        cameraExecutor.shutdown()
    }

    private class SnapshotFrameAnalyzer(private val onFrameCaptured: (Bitmap) -> Unit) :
        ImageAnalysis.Analyzer {
            private var lastFrameTimestamp = 0L
            private val interval = 1.seconds

            @SuppressLint("UnsafeOptInUsageError")
            override fun analyze(image: ImageProxy) {
                val currentTimestamp = System.currentTimeMillis()
                if (currentTimestamp - lastFrameTimestamp >= interval.inWholeMilliseconds) {
                    onFrameCaptured(image.toBitmap())
                    lastFrameTimestamp = currentTimestamp
                }
                image.close()
            }
        }

    companion object {
        private const val TAG = "CameraAnalyzer"
    }
}
