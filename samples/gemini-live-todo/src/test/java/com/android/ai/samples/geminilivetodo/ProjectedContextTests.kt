package com.android.ai.samples.geminilivetodo

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.projected.ProjectedContext
import androidx.xr.projected.ProjectedDeviceController
import androidx.xr.projected.ProjectedDeviceController.Capability.Companion.CAPABILITY_VISUAL_UI
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.projected.testing.ProjectedTestRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.intArrayOf

@OptIn(ExperimentalProjectedApi::class)
@Config(sdk = [Build.VERSION_CODES.BAKLAVA])
@RunWith(AndroidJUnit4::class)
class ProjectedContextTests {
    @get:Rule
    val projectedTestRule = ProjectedTestRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private lateinit var projectedDeviceController: ProjectedDeviceController


    @Test
    fun app_initializesProjectedContext_whenDeviceIsConnected() {
        val projectedContext = ProjectedContext.createProjectedDeviceContext(context)
        assertThat(projectedContext).isNotNull()
    }

    @Test
    fun app_throwsException_whenDeviceDisconnected() {
        projectedTestRule.isDeviceConnected = false

        assertThrows(IllegalStateException::class.java) {
            ProjectedContext.createProjectedDeviceContext(context)
        }
    }

    @Test
    fun capabilities_includesVisualUiByDefault_returnsCapabilityVisualUi() {
        projectedTestRule.launchTestProjectedDeviceActivity { activity ->
            runBlocking {
                projectedDeviceController = ProjectedDeviceController.create(activity)
            }

            assertThat(projectedDeviceController.capabilities).contains(CAPABILITY_VISUAL_UI)
        }
    }

    @Test
    fun capabilities_emptyCapabilities_doesNotReturnCapabilityVisualUi() {
        projectedTestRule.launchTestProjectedDeviceActivity { activity ->
            projectedTestRule.capabilities = setOf()
            runBlocking {
                projectedDeviceController = ProjectedDeviceController.create(activity)
            }

            assertThat(projectedDeviceController.capabilities).doesNotContain(CAPABILITY_VISUAL_UI)
        }
    }
}
