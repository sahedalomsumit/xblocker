package com.sahed.xblocker.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.sahed.xblocker.ui.navigation.MainNavGraph
import com.sahed.xblocker.ui.onboarding.OnboardingScreen
import com.sahed.xblocker.ui.onboarding.OnboardingViewModel
import com.sahed.xblocker.ui.theme.XBlockerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            XBlockerTheme {
                // Read onboarding state asynchronously — null means still loading
                val mainViewModel: MainViewModel = hiltViewModel()
                val onboardingDone by mainViewModel.isOnboardingDone.collectAsState()

                when (onboardingDone) {
                    null -> {
                        // Still loading from DataStore — render nothing to avoid flicker
                    }
                    false -> {
                        val viewModel: OnboardingViewModel = hiltViewModel()
                        OnboardingScreen(
                            viewModel = viewModel,
                            onFinished = {
                                // isOnboardingDone flow will emit true automatically;
                                // no recreate() needed — the when block recomposes itself.
                            }
                        )
                    }
                    true -> {
                        MainNavGraph()
                    }
                }
            }
        }
    }
}
