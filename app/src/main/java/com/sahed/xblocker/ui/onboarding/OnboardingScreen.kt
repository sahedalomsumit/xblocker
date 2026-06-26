package com.sahed.xblocker.ui.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sahed.xblocker.service.XBlockerAccessibilityService
import com.sahed.xblocker.ui.theme.Accent
import com.sahed.xblocker.ui.theme.AccentDim
import com.sahed.xblocker.ui.theme.BgDark
import com.sahed.xblocker.ui.theme.Border
import com.sahed.xblocker.ui.theme.TextMuted

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val body: String,
    val actionLabel: String,
    val isOptional: Boolean = false
)

private val pages = listOf(
    OnboardingPage(
        icon = Icons.Outlined.Shield,
        title = "Welcome to XBlocker",
        body = "Block adult content across every browser in real-time — using Accessibility Service monitoring. No VPN, no cloud, no tracking, no account needed.",
        actionLabel = "Get Started"
    ),
    OnboardingPage(
        icon = Icons.Outlined.Accessibility,
        title = "Accessibility Permission",
        body = "XBlocker needs Accessibility access to watch your browser's URL bar. It only reads the current URL — no keystrokes, no personal data, no tracking.",
        actionLabel = "Open Accessibility Settings"
    ),
    OnboardingPage(
        icon = Icons.Outlined.Notifications,
        title = "Notifications",
        body = "Allow notifications to receive alerts when your 3-hour disable timer completes.",
        actionLabel = "Allow Notifications",
        isOptional = true
    ),
    OnboardingPage(
        icon = Icons.Outlined.Security,
        title = "All Set",
        body = "XBlocker is ready. The blocker is now active. To disable it, you'll need to wait 3 hours — giving you time to reconsider.",
        actionLabel = "Start Blocking"
    )
)

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onFinished: () -> Unit
) {
    var currentPage by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // When returning to page 1 (Accessibility) from system settings, auto-advance if enabled
    DisposableEffect(lifecycleOwner, currentPage) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && currentPage == 1) {
                if (XBlockerAccessibilityService.isRunning(context)) {
                    currentPage = 2
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) {
        currentPage = 3
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Background gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(AccentDim, BgDark),
                        radius = 900f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Progress dots
            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(if (index == currentPage) 24.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(if (index == currentPage) Accent else Border)
                    )
                }
            }

            // Page content
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    (slideInHorizontally(tween(400)) { it } + fadeIn(tween(400))) togetherWith
                            (slideOutHorizontally(tween(400)) { -it } + fadeOut(tween(400)))
                },
                label = "onboarding_page"
            ) { page ->
                OnboardingPageContent(pages[page])
            }

            // Action button
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val page = pages[currentPage]

                Button(
                    onClick = {
                        when (currentPage) {
                            0 -> currentPage = 1
                            1 -> {
                                // Already enabled? skip straight to next page
                                if (XBlockerAccessibilityService.isRunning(context)) {
                                    currentPage = 2
                                } else {
                                    // Open system Accessibility Settings
                                    context.startActivity(
                                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                    )
                                    // The DisposableEffect above will auto-advance when we come back
                                }
                            }
                            2 -> {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    currentPage = 3
                                }
                            }
                            3 -> {
                                viewModel.completeOnboarding()
                                onFinished()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent
                    )
                ) {
                    Text(
                        text = page.actionLabel,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                if (page.isOptional) {
                    OutlinedButton(
                        onClick = { currentPage++ },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted)
                    ) {
                        Text("Skip for now", style = MaterialTheme.typography.labelLarge)
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(AccentDim),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(56.dp)
            )
        }

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = page.body,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
