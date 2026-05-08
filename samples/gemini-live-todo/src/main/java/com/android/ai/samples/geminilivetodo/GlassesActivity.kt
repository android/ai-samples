package com.android.ai.samples.geminilivetodo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
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
import androidx.xr.projected.ProjectedDeviceController
import androidx.xr.projected.ProjectedDeviceController.Capability
import androidx.xr.projected.ProjectedDisplayController
import androidx.xr.projected.ProjectedDisplayController.PresentationMode
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.projected.permissions.ProjectedPermissionsRequestParams
import androidx.xr.projected.permissions.ProjectedPermissionsResultContract
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

private const val TAG = "GlassesActivity"

@AndroidEntryPoint
class GlassesActivity : ComponentActivity() {

    private val viewModel: TodoScreenViewModel by viewModels()
    private var isPermissionsGranted by mutableStateOf(false)
    private var isDisplayCapable by mutableStateOf(false)
    private var areVisualsOn by mutableStateOf(false)

    private val requiredPermissions = listOf(
        Manifest.permission.RECORD_AUDIO
    )

    @OptIn(ExperimentalProjectedApi::class)
    private val requestPermissionLauncher: ActivityResultLauncher<List<ProjectedPermissionsRequestParams>> =
        registerForActivityResult(ProjectedPermissionsResultContract()) { results ->
            val granted = requiredPermissions.all { permission ->
                results[permission] == true
            }
            isPermissionsGranted = granted
            setupContent()
        }

    @OptIn(ExperimentalProjectedApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        viewModel.initializeGeminiLive(this)

        val allGranted = checkAllPermissionsGranted()
        isPermissionsGranted = allGranted

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

        setupContent()


        if (!allGranted) {
            requestPermissions()
        }
    }


    private fun checkAllPermissionsGranted(): Boolean {
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun setupContent() {
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

    @OptIn(ExperimentalProjectedApi::class)
    private fun requestPermissions() {
        requestPermissionLauncher.launch(
            listOf(
                ProjectedPermissionsRequestParams(
                    permissions = requiredPermissions,
                    rationale = getString(R.string.permission_rationale_mic_access)
                )
            )
        )
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