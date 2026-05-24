package br.com.seunome.mobulite.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.seunome.mobulite.data.remote.ChatMessage
import br.com.seunome.mobulite.data.remote.RetrofitClient
import br.com.seunome.mobulite.data.remote.SendMessageRequest
import br.com.seunome.mobulite.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

private fun formatMsgTime(iso: String?): String = if (iso.isNullOrBlank()) "" else
    try { timeFormatter.format(Instant.parse(iso)) } catch (_: Exception) { "" }

// ─── Full-screen chat overlay ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    rideId: String,
    myRole: String,       // "PASSENGER" | "DRIVER"
    partnerName: String,
    isDriverSide: Boolean,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var sending   by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf(false) }

    fun fetchMessages() = scope.launch {
        runCatching {
            val fetched = if (isDriverSide)
                RetrofitClient.driverApi.getChatMessages(rideId)
            else
                RetrofitClient.api.getChatMessages(rideId)
            messages = fetched
            if (fetched.isNotEmpty()) listState.animateScrollToItem(fetched.lastIndex)
        }.onFailure { loadError = true }
    }

    // Poll every 4 seconds while visible
    LaunchedEffect(rideId) {
        while (true) {
            fetchMessages()
            delay(4_000)
        }
    }

    fun sendMessage() {
        val text = inputText.trim()
        if (text.isBlank() || sending) return
        scope.launch {
            sending = true
            inputText = ""
            runCatching {
                val msg = if (isDriverSide)
                    RetrofitClient.driverApi.sendChatMessage(rideId, SendMessageRequest(text))
                else
                    RetrofitClient.api.sendChatMessage(rideId, SendMessageRequest(text))
                messages = messages + msg
                listState.animateScrollToItem(messages.lastIndex)
            }
            sending = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(AppVioletDarker, AppPurple)))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                    Surface(shape = CircleShape, color = Color.White.copy(0.2f), modifier = Modifier.size(38.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Chat, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                    Column {
                        Text(partnerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Chat da corrida", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.75f))
                    }
                }
            }

            // Message list
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillParentMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Chat, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f), modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Nenhuma mensagem ainda",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                            )
                            Text(
                                "Inicie a conversa com ${partnerName.split(" ").first()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f)
                            )
                        }
                    }
                }
                items(messages, key = { it.id }) { msg ->
                    val isMine = msg.senderRole == myRole
                    ChatBubble(msg = msg, isMine = isMine)
                }
            }

            // Input area
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { if (it.length <= 500) inputText = it },
                        placeholder = { Text("Mensagem…", style = MaterialTheme.typography.bodyMedium) },
                        modifier = Modifier.weight(1f),
                        minLines = 1,
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppPurple,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    FloatingActionButton(
                        onClick = { sendMessage() },
                        modifier = Modifier.size(50.dp),
                        shape = CircleShape,
                        containerColor = if (inputText.isBlank()) MaterialTheme.colorScheme.surfaceVariant else AppPurple,
                        contentColor = if (inputText.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp)
                    ) {
                        if (sending) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─── Chat bubble ─────────────────────────────────────────────────────────────

@Composable
private fun ChatBubble(msg: ChatMessage, isMine: Boolean) {
    val bubbleColor = if (isMine) AppPurple else MaterialTheme.colorScheme.surfaceVariant
    val textColor   = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
    val timeColor   = if (isMine) Color.White.copy(0.65f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
    val alignment   = if (isMine) Alignment.End else Alignment.Start
    val shape = if (isMine)
        RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
    else
        RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Column {
                Text(msg.content, style = MaterialTheme.typography.bodyMedium, color = textColor, lineHeight = 20.sp)
                Spacer(Modifier.height(3.dp))
                Text(
                    formatMsgTime(msg.sentAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = timeColor,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

// ─── Floating chat button ─────────────────────────────────────────────────────

@Composable
fun ChatFab(
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        FloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = AppPurple,
            contentColor = Color.White,
            modifier = Modifier.size(52.dp)
        ) {
            Icon(Icons.Default.Chat, null, modifier = Modifier.size(22.dp))
        }
        AnimatedVisibility(
            visible = unreadCount > 0,
            modifier = Modifier.align(Alignment.TopEnd),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                shape = CircleShape,
                color = AppRed,
                modifier = Modifier.size(18.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        if (unreadCount > 9) "9+" else "$unreadCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}
