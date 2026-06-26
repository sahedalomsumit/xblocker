package com.sahed.xblocker.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahed.xblocker.ui.theme.XBlockerTheme

/**
 * Full-screen "Site Blocked" overlay shown when the Accessibility Service
 * detects a blocked domain. Pressing "Go Back" finishes this activity
 * (the browser is already behind it, already navigated back).
 */
class BlockedActivity : ComponentActivity() {

    companion object {
        const val EXTRA_DOMAIN = "blocked_domain"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val domain = intent?.getStringExtra(EXTRA_DOMAIN) ?: "this site"

        setContent {
            XBlockerTheme {
                val bgColor = Color(0xFF0A0A14)
                val accentRed = Color(0xFFE05252)
                val accentRedDim = Color(0x22E05252)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0x33E05252), bgColor),
                                radius = 1000f
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Icon
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(accentRedDim),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Block,
                                contentDescription = null,
                                tint = accentRed,
                                modifier = Modifier.size(60.dp)
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Site Blocked",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = domain,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = accentRed,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "This website is on your block list.\nXBlocker has prevented you from accessing it.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFFAAAAAA),
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = { finish() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentRed
                            )
                        ) {
                            Text(
                                text = "Go Back",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
