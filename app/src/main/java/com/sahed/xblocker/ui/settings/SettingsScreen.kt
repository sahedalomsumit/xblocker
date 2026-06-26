package com.sahed.xblocker.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sahed.xblocker.BuildConfig
import com.sahed.xblocker.ui.theme.Accent
import com.sahed.xblocker.ui.theme.AccentDim
import com.sahed.xblocker.ui.theme.BgDark
import com.sahed.xblocker.ui.theme.Border
import com.sahed.xblocker.ui.theme.CardBg
import com.sahed.xblocker.ui.theme.CardBg2
import com.sahed.xblocker.ui.theme.TextDisabled
import com.sahed.xblocker.ui.theme.TextMain
import com.sahed.xblocker.ui.theme.TextMuted

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = TextMain
        )

        // Section: Theme
        SectionHeader("Theme")
        SettingsToggleCard(
            icon = Icons.Outlined.DarkMode,
            title = "Dark Mode",
            subtitle = "Enable dark theme for the application",
            checked = state.isDarkMode,
            onCheckedChange = viewModel::setDarkMode
        )


        // Section: About
        SectionHeader("About")
        InfoCard(
            icon = Icons.Outlined.Shield,
            title = "XBlocker",
            subtitle = "Version ${BuildConfig.VERSION_NAME}"
        )
        InfoCard(
            icon = Icons.Outlined.PhoneAndroid,
            title = "No Data Collected",
            subtitle = "All filtering is done locally on your device"
        )
        InfoCard(
            icon = Icons.Outlined.Info,
            title = "© Copyright by",
            subtitle = "Sahed Alom Sumit"
        )

        Spacer(Modifier.height(24.dp))
        SettingsFooter()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsFooter() {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "heartPulse")
    val heartScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartScale"
    )

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sahedalomsumit.com"))
                    context.startActivity(intent)
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Built with",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
            Icon(
                imageVector = Icons.Outlined.Favorite,
                contentDescription = null,
                tint = Color(0xFFEF4444),
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer {
                        scaleX = heartScale
                        scaleY = heartScale
                    }
            )
            Text(
                text = "by",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
            Text(
                text = "Sahed",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                ),
                color = Accent
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = TextMuted,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}

@Composable
private fun SettingsToggleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AccentDim),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = TextMain)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TextMain,
                    checkedTrackColor = Accent,
                    uncheckedThumbColor = TextDisabled,
                    uncheckedTrackColor = CardBg2
                )
            )
        }
    }
}


@Composable
private fun InfoCard(icon: ImageVector, title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg2),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, color = TextMain)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }
    }
}
