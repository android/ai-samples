package com.android.ai.samples.geminilivetodo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.xr.glimmer.GlimmerTheme
import com.android.ai.samples.geminilivetodo.ui.AudioExperience
import com.android.ai.samples.geminilivetodo.ui.GlimmerTodoScreen
import com.android.ai.samples.geminilivetodo.ui.TodoScreenViewModel
import androidx.xr.projected.ProjectedActivityCompat
import androidx.xr.projected.ProjectedDeviceController
import androidx.xr.projected.ProjectedDeviceController.Capability
import androidx.xr.projected.ProjectedDisplayController
import androidx.xr.projected.ProjectedDisplayController.PresentationMode
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
@OptIn(ExperimentalProjectedApi::class)
class GlassesActivity : ComponentActivity() {

    private val viewModel: TodoScreenViewModel by viewModels()
    private var isPermissionsGranted by mutableStateOf(false)
    private var isDisplayCapable by mutableStateOf(false)
    private var areVisualsOn by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.initializeGeminiLive(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            checkAndRequestAudioPermission()
        } else {
            isPermissionsGranted = checkAudioPermissionGranted()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            lifecycleScope.launch {
                try {
                    val deviceController = ProjectedDeviceController.create(this@GlassesActivity)
                    isDisplayCapable = deviceController.capabilities.contains(Capability.CAPABILITY_VISUAL_UI)

                    val displayController = ProjectedDisplayController.create(this@GlassesActivity)
                    displayController.addPresentationModeChangedListener { flags ->
                        areVisualsOn = flags.hasPresentationMode(PresentationMode.VISUALS_ON)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error initializing projected display controllers", e)
                }
            }
        }

        setContent {
            GlimmerTheme {
                RootScreen(
                    isGranted = isPermissionsGranted,
                    isDisplayCapable = isDisplayCapable,
                    areVisualsOn = areVisualsOn,
                    viewModel = viewModel
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
        deviceId: Int,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        if (requestCode != RECORD_AUDIO_PERMISSION_REQUEST_CODE) return

        val isAudioGranted =
            grantResults.getOrNull(permissions.indexOf(Manifest.permission.RECORD_AUDIO)) ==
                PackageManager.PERMISSION_GRANTED

        isPermissionsGranted = isAudioGranted
        Log.d(TAG, "Is Permissions Granted? $isPermissionsGranted")

    }

    private fun checkAudioPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun checkAndRequestAudioPermission() {
        val hasAudioPermission = checkAudioPermissionGranted()
        isPermissionsGranted = hasAudioPermission

        if (!hasAudioPermission) {
            requestAudioPermission()
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun requestAudioPermission() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                ProjectedActivityCompat.requestPermissions(
                    this@GlassesActivity,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    RECORD_AUDIO_PERMISSION_REQUEST_CODE,
                )
            } catch (e: IllegalStateException) {
                // Thrown when the projected system service can't be bound.
                Log.e(TAG, "Audio permission request failed: projected service unavailable.", e)
            }
        }
    }

    private companion object {
        const val RECORD_AUDIO_PERMISSION_REQUEST_CODE = 1002
        const val TAG = "GlassesActivity"
    }
}


@Composable
fun RootScreen(
    isGranted: Boolean,
    isDisplayCapable: Boolean,
    areVisualsOn: Boolean,
    viewModel: TodoScreenViewModel,
    modifier: Modifier = Modifier
) {
    if (!isGranted) {
        Text(
            text = stringResource(R.string.permissions_denied_mic_access),
            modifier = modifier
        )
    } else if (isDisplayCapable && areVisualsOn) {
        // VISUAL MODE: Render UI for display-active glasses
        GlimmerTodoScreen(viewModel = viewModel, modifier = modifier)
    } else {
        // AUDIO MODE: Fall back to Gemini for audio-only or display-off states
        AudioExperience(viewModel = viewModel)
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewRootScreen() {
    GlimmerTheme {
        RootScreen(
            isGranted = false,
            isDisplayCapable = true,
            areVisualsOn = true,
            viewModel = TodoScreenViewModel(com.android.ai.samples.geminilivetodo.data.TodoRepository())
        )
    }
}