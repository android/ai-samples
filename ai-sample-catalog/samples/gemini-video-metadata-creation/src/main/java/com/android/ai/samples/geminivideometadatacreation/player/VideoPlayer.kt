/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.ai.samples.geminivideometadatacreation.player

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.media3.ui.compose.state.rememberPresentationState
import com.android.ai.samples.geminivideometadatacreation.R

/*
 * A Composable function that displays video using ExoPlayer within a PlayerView in Jetpack Compose.
 */
@OptIn(UnstableApi::class) // New Media3 Compose artifact is currently experimental
@Composable
fun VideoPlayer(player: Player?, modifier: Modifier = Modifier) {

    val presentationState = rememberPresentationState(player)
    Box(
        modifier.background(Color.White),
    ) {
        PlayerSurface(
            player,
            modifier = Modifier.resizeWithContentScale(
                contentScale = ContentScale.Fit,
                presentationState.videoSizeDp,
            ),
        )

        PlayPauseButton(
            player,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp),
        )

        if (presentationState.coverSurface) {
            // Cover the surface that is being prepared with a shutter
            Box(Modifier.background(Color.Black))
        }
    }
}

@OptIn(UnstableApi::class) // New Media3 Compose artifact is currently experimental
@Composable
fun PlayPauseButton(player: Player?, modifier: Modifier = Modifier) {
    if (player == null) return

    val state = rememberPlayPauseButtonState(player)

    IconButton(onClick = state::onClick, modifier = modifier, enabled = state.isEnabled) {
        Icon(
            imageVector = if (state.showPlay) Icons.Default.PlayArrow else Icons.Default.Pause,
            contentDescription =
            if (state.showPlay) stringResource(R.string.playpause_button_play)
            else stringResource(R.string.playpause_button_pause),
        )
    }
}
