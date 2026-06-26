package com.sahed.xblocker.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.sahed.xblocker.data.datastore.AppPreferences
import com.sahed.xblocker.ui.navigation.MainNavGraph
import com.sahed.xblocker.ui.onboarding.OnboardingScreen
import com.sahed.xblocker.ui.onboarding.OnboardingViewModel
import com.sahed.xblocker.ui.theme.XBlockerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Synchronously read onboarding state once (avoids flicker)
        val onboardingDone = runBlocking { preferences.isOnboardingDone.first() }

        setContent {
            XBlockerTheme {
                if (!onboardingDone) {
                    val viewModel: OnboardingViewModel = hiltViewModel()
                    OnboardingScreen(
                        viewModel = viewModel,
                        onFinished = {
                            // Recreate to show main nav
                            recreate()
                        }
                    )
                } else {
                    MainNavGraph()
                }
            }
        }
    }
}
