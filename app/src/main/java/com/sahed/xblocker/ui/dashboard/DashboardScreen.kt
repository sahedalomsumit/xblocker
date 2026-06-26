package com.sahed.xblocker.ui.dashboard

import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShieldMoon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sahed.xblocker.ui.theme.Accent
import com.sahed.xblocker.ui.theme.AccentDim
import com.sahed.xblocker.ui.theme.Amber
import com.sahed.xblocker.ui.theme.AmberDim
import com.sahed.xblocker.ui.theme.BgDark
import com.sahed.xblocker.ui.theme.Border
import com.sahed.xblocker.ui.theme.CardBg
import com.sahed.xblocker.ui.theme.CardBg2
import com.sahed.xblocker.ui.theme.ChipActive
import com.sahed.xblocker.ui.theme.ChipInactive
import com.sahed.xblocker.ui.theme.Emerald
import com.sahed.xblocker.ui.theme.EmeraldDim
import com.sahed.xblocker.ui.theme.GradientCardEnd
import com.sahed.xblocker.ui.theme.GradientCardStart
import com.sahed.xblocker.ui.theme.Rose
import com.sahed.xblocker.ui.theme.TextDisabled
import com.sahed.xblocker.ui.theme.TextMain
import com.sahed.xblocker.ui.theme.TextMuted

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val vpnLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.startVpn()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            color = TextMain
        )

        // VPN Status Hero Card
        VpnStatusCard(
            isActive = state.isVpnActive,
            onToggle = {
                if (state.isVpnActive) {
                    viewModel.requestDisable()
                } else {
                    val vpnIntent = VpnService.prepare(context)
                    if (vpnIntent == null) {
                        viewModel.startVpn()
                    } else {
                        vpnLauncher.launch(vpnIntent)
                    }
                }
            }
        )

        // Active Timer Card
        AnimatedVisibility(
            visible = state.activeDisableTimer != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            DisableTimerCard(
                remainingMs = state.timerRemainingMs,
                onCancel = viewModel::cancelDisableTimer
            )
        }

        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Blocked Today",
                value = state.blockedToday.toString(),
                icon = Icons.Outlined.Block,
                tint = Rose
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Custom Rules",
                value = state.customDomainCount.toString(),
                icon = Icons.Outlined.List,
                tint = Accent
            )
        }

        // Default list size card
        InfoCard(
            title = "Default Blocklist",
            body = "500+ adult domains bundled and active",
            icon = Icons.Outlined.Shield,
            tint = Emerald
        )

        Spacer(Modifier.height(80.dp)) // bottom nav clearance
    }
}

@Composable
private fun VpnStatusCard(isActive: Boolean, onToggle: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(GradientCardStart, GradientCardEnd)
                    )
                )
                .border(1.dp, Border, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Pulsing shield icon
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(if (isActive) EmeraldDim else ChipInactive),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Outlined.Shield else Icons.Outlined.ShieldMoon,
                        contentDescription = null,
                        tint = if (isActive) Emerald else TextDisabled,
                        modifier = Modifier.size(48.dp)
                    )
                }

                // Status label
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isActive) Emerald else TextDisabled)
                    )
                    Text(
                        text = if (isActive) "BLOCKER ACTIVE" else "BLOCKER INACTIVE",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isActive) Emerald else TextDisabled,
                        letterSpacing = 1.5.sp
                    )
                }

                Text(
                    text = if (isActive)
                        "All adult content is being blocked system-wide"
                    else
                        "Adult content is not being blocked",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )

                // Toggle button
                if (isActive) {
                    OutlinedButton(
                        onClick = onToggle,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Rose.copy(alpha = 0.4f))
                    ) {
                        Icon(Icons.Outlined.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Disable (3hr delay)", style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    Button(
                        onClick = onToggle,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent)
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Enable Blocker", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun DisableTimerCard(remainingMs: Long, onCancel: () -> Unit) {
    val hours = remainingMs / 3_600_000
    val minutes = (remainingMs % 3_600_000) / 60_000
    val seconds = (remainingMs % 60_000) / 1_000
    val formatted = "%02d:%02d:%02d".format(hours, minutes, seconds)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AmberDim),
        border = androidx.compose.foundation.BorderStroke(1.dp, Amber.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Outlined.AccessTime, contentDescription = null, tint = Amber, modifier = Modifier.size(28.dp))
                Column {
                    Text("Blocker disables in", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    Text(
                        text = formatted,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Amber
                    )
                }
            }
            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Amber),
                border = androidx.compose.foundation.BorderStroke(1.dp, Amber.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Outlined.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = TextMain
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    body: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg2),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, color = TextMain)
                Text(body, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }
    }
}
