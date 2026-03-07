package com.phoneintel.app.ui.focus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.phoneintel.app.data.repository.FocusRepository
import com.phoneintel.app.ui.theme.CoralAccent
import com.phoneintel.app.ui.theme.IndigoBase
import com.phoneintel.app.ui.theme.PhoneIntelTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.phoneintel.app.service.FocusEnforcementService


/**
 * Fullscreen overlay shown when a blocked app is opened during a Focus session.
 * Launched by FocusEnforcementService with FLAG_ACTIVITY_NEW_TASK.
 *
 * The user can either go back (previous app / home) or end their focus session.
 * excludeFromRecents = true keeps this out of the recents switcher.
 */
@AndroidEntryPoint
class FocusBlockedActivity : ComponentActivity() {

    @Inject lateinit var focusRepository: FocusRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val blockedPkg = intent.getStringExtra(EXTRA_BLOCKED_PKG) ?: "this app"

        setContent {
            PhoneIntelTheme {
                FocusBlockedScreen(
                    blockedPackage = blockedPkg,
                    focusIntent = focusRepository.focusState.value.intent.label,
                    focusEmoji = focusRepository.focusState.value.intent.emoji,
                    onGoBack = { finish() },
                    onEndFocus = {
                        focusRepository.stopFocus()
                        this@FocusBlockedActivity.stopService(
                            android.content.Intent(this@FocusBlockedActivity, FocusEnforcementService::class.java)
                        )
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_BLOCKED_PKG = "blocked_pkg"
    }
}

@Composable
private fun FocusBlockedScreen(
    blockedPackage: String,
    focusIntent: String,
    focusEmoji: String,
    onGoBack: () -> Unit,
    onEndFocus: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1B2A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Big block icon
            Box(
                Modifier.size(100.dp).clip(CircleShape).background(CoralAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(focusEmoji, style = MaterialTheme.typography.displaySmall)
            }
            Spacer(Modifier.height(28.dp))

            Text("This app is blocked", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))

            val appName = blockedPackage.substringAfterLast('.').replaceFirstChar { it.uppercase() }
            Text(
                "$appName is blocked during your $focusIntent focus session.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Stay the course — you'll thank yourself later.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            Button(
                onClick = onGoBack,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoBase)
            ) {
                Text("Stay Focused", fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.height(12.dp))
            TextButton(
                onClick = onEndFocus,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("End Focus Session", color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
