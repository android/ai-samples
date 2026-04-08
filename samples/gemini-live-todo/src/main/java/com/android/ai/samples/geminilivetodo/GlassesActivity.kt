package com.android.ai.samples.geminilivetodo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.xr.glimmer.GlimmerTheme
import com.android.ai.samples.geminilivetodo.ui.GlimmerTodoScreen
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.projected.permissions.ProjectedPermissionsRequestParams
import androidx.xr.projected.permissions.ProjectedPermissionsResultContract
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "GlassesActivity"

@AndroidEntryPoint
class GlassesActivity : ComponentActivity() {

    private var isPermissionsGranted by mutableStateOf(false)

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val allGranted = checkAllPermissionsGranted()
        isPermissionsGranted = allGranted

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
                RootScreen(isGranted = isPermissionsGranted)
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
fun RootScreen(isGranted: Boolean, modifier: Modifier = Modifier) {
    if (isGranted) {
        GlimmerTodoScreen(modifier = modifier)
    } else {
        Text(
            text = stringResource(R.string.permissions_denied_mic_access),
            modifier = modifier
        )
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewRootScreen() {
    GlimmerTheme {
        RootScreen(isGranted = false)
    }
}