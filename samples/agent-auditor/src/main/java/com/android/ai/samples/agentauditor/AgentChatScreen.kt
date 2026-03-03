package com.android.ai.samples.agentauditor

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.ai.uicomponent.GenerateButton
import com.android.ai.uicomponent.MessageList
import com.android.ai.uicomponent.SampleDetailTopAppBar
import com.android.ai.uicomponent.TextInput
import com.android.ai.uicomponent.ChatMessage as UiChatMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentChatScreen(viewModel: AgentViewModel = hiltViewModel()) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    // Map internal messages to UiChatMessage for the UI Component MessageList
    val uiMessages = messages.mapIndexed { index, msg ->
        if (msg.content != null) {
            val isUser = msg.role == "user"
            UiChatMessage(
                text = msg.content,
                timestamp = index.toLong(),
                isIncoming = !isUser
            )
        } else if (msg.toolCalls != null && msg.role == "model") {
            val isApproval = msg.toolCalls.any { it.name == "request_user_approval" }
            val text = if (isApproval) "Agent is requesting your approval..." else "_Agent is querying tools:_ " + msg.toolCalls.joinToString { it.name }
            UiChatMessage(
                text = text,
                timestamp = index.toLong(),
                isIncoming = true
            )
        } else if (msg.toolResponses != null && msg.role == "user") {
            UiChatMessage(
                text = "_Returned tool results to Agent:_ " + msg.toolResponses.joinToString { it.name },
                timestamp = index.toLong(),
                isIncoming = false
            )
        } else {
            UiChatMessage("", index.toLong(), false)
        }
    }.filter { it.text.isNotEmpty() }.reversed()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .imePadding(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            SampleDetailTopAppBar(
                sampleName = stringResource(R.string.agent_auditor_sample_title),
                sampleDescription = stringResource(R.string.agent_auditor_sample_description),
                sourceCodeUrl = "https://github.com/android/ai-samples/tree/main/samples/agent-auditor",
                modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                onBackClick = { backDispatcher?.onBackPressed() },
                topAppBarState = topAppBarState,
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            MessageList(
                modifier = Modifier
                    .widthIn(max = 646.dp)
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp),
                messages = uiMessages,
            )

            // Dynamic Agent UI Overlays based on State
            when (val state = uiState) {
                is ChatUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(60.dp)
                            .align(Alignment.Center),
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                is ChatUiState.Error -> {
                    Text(
                        text = "Error: ${state.message}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {}
            }

            // Input UI
            if (uiState is ChatUiState.AwaitingUserApproval) {
                val approvalState = uiState as ChatUiState.AwaitingUserApproval
                ApprovalCard(
                    message = approvalState.message,
                    onApprove = { viewModel.onApprovalResult(true, approvalState.pendingToolCalls) },
                    onDeny = { viewModel.onApprovalResult(false, approvalState.pendingToolCalls) },
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                        .align(Alignment.BottomCenter)
                        .widthIn(max = 646.dp)
                )
            } else {
                val textFieldState = rememberTextFieldState()
                TextInput(
                    state = textFieldState,
                    placeholder = "Can I spend \$70 on dinner tonight?",
                    primaryButton = {
                        GenerateButton(
                            icon = painterResource(id = com.android.ai.uicomponent.R.drawable.ic_ai_send),
                            modifier = Modifier
                                .width(72.dp)
                                .height(55.dp)
                                .padding(4.dp),
                            enabled = uiState !is ChatUiState.Loading,
                            onClick = {
                                val txt = textFieldState.text.toString()
                                if (txt.isNotBlank()) {
                                    viewModel.sendMessage(txt)
                                    textFieldState.setTextAndPlaceCursorAtEnd("")
                                }
                            },
                        )
                    },
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.BottomCenter)
                        .widthIn(max = 646.dp)
                        .fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun ApprovalCard(message: String, onApprove: () -> Unit, onDeny: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(24.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Approval Required",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = onDeny,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Deny")
            }
            Button(
                onClick = onApprove,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)) // Dark Green
            ) {
                Text("Approve")
            }
        }
    }
}
