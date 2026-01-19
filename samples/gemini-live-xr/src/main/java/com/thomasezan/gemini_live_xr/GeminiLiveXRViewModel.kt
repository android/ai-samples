package com.thomasezan.gemini_live_xr

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.InlineData
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveGenerationConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@OptIn(PublicPreviewAPI::class)
@HiltViewModel
class GeminiLiveXRViewModel @Inject constructor() : ViewModel() {
    private val TAG = "GeminiLiveXRViewModel"
    private var session: LiveSession? = null

    private val _liveSessionState = MutableStateFlow<LiveSessionState>(LiveSessionState.NotReady)
    val liveSessionState: StateFlow<LiveSessionState> = _liveSessionState.asStateFlow()

    @SuppressLint("MissingPermission")
    fun toggleLiveSession(activity: Activity) {
        viewModelScope.launch {
            if (_liveSessionState.value is LiveSessionState.NotReady) return@launch

            session?.let {
                if (_liveSessionState.value is LiveSessionState.Ready) {
                    if (ContextCompat.checkSelfPermission(
                            activity,
                            Manifest.permission.RECORD_AUDIO,
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        it.startAudioConversation()
                        _liveSessionState.value = LiveSessionState.Running
                    } else {
                        requestAudioPermissionIfNeeded(activity)
                    }
                } else {
                    it.stopAudioConversation()
                    _liveSessionState.value = LiveSessionState.Ready
                }
            }
        }
    }

    fun initializeGeminiLive(activity: Activity) {
        requestAudioPermissionIfNeeded(activity)
        viewModelScope.launch {
            Log.d(TAG, "Start Gemini Live initialization")
            val liveGenerationConfig = liveGenerationConfig {
                speechConfig = SpeechConfig(voice = Voice("Puck"))
                responseModality = ResponseModality.AUDIO
            }

            val systemInstruction = content {
                text("You are a helpful assistant, really good at guiding me to organize and tidy up my desk.")
            }

            val generativeModel = Firebase.ai(backend = GenerativeBackend.vertexAI()).liveModel(
                "gemini-live-2.5-flash-native-audio",
                generationConfig = liveGenerationConfig,
                systemInstruction = systemInstruction,
            )

            try {
                session = generativeModel.connect()
                _liveSessionState.value = LiveSessionState.Ready
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to the model", e)
                _liveSessionState.value = LiveSessionState.Error
            }
        }
    }

    fun sendVideoFrame(frame: Bitmap) {
        viewModelScope.launch {
            val byteArrayOutputStream = ByteArrayOutputStream()
            frame.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
            val jpegBytes = byteArrayOutputStream.toByteArray()

            session?.let {
                if (_liveSessionState.value is LiveSessionState.Running) {
                    it.sendVideoRealtime(InlineData(jpegBytes, "image/jpeg"))
                }
            }
        }
    }

    private fun requestAudioPermissionIfNeeded(activity: Activity) {
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.RECORD_AUDIO,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
    }
}
