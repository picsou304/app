package com.dailyvlog.app.ui.group

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailyvlog.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    state: GroupUiState,
    onCreateGroup: (name: String, emoji: String, colorHex: String, switchTime: String, maxDuration: Int) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("🎬") }
    var switchTime by remember { mutableStateOf("09:00") }
    var maxDuration by remember { mutableIntStateOf(60) }

    val emojis = listOf("🎬", "🍿", "🔥", "🚀", "📸", "🎉", "🍕", "⚡")
    val durationOptions = listOf(30, 60, 90, 120)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("create_group_screen"),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_group_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("create_group_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val error = state.errorMessage
            if (error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .testTag("create_group_error_banner")
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Selected Emoji Preview
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .testTag("create_group_emoji_preview"),
                contentAlignment = Alignment.Center
            ) {
                Text(text = selectedEmoji, fontSize = 40.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Emoji Selection Row
            Text(
                text = stringResource(R.string.choose_emoji),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                emojis.forEach { emoji ->
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (selectedEmoji == emoji) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = if (selectedEmoji == emoji) 2.dp else 0.dp,
                                color = if (selectedEmoji == emoji) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectedEmoji = emoji }
                            .testTag("emoji_option_$emoji"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 20.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Name Input
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.group_name_label)) },
                placeholder = { Text("Ex. Les Potes, Famille...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_group_name_input")
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Switch Time Input
            OutlinedTextField(
                value = switchTime,
                onValueChange = { switchTime = it },
                label = { Text(stringResource(R.string.switch_time_label)) },
                supportingText = { Text(stringResource(R.string.switch_time_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_group_time_input")
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Max Duration
            Text(
                text = stringResource(R.string.max_duration_label),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                durationOptions.forEach { sec ->
                    FilterChip(
                        selected = maxDuration == sec,
                        onClick = { maxDuration = sec },
                        label = { Text(stringResource(R.string.max_duration_seconds, sec)) },
                        modifier = Modifier.testTag("duration_chip_$sec")
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = { onCreateGroup(name, selectedEmoji, "#FF5252", switchTime, maxDuration) },
                enabled = !state.isLoading && name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("create_group_submit_btn")
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = stringResource(R.string.btn_create_group),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
