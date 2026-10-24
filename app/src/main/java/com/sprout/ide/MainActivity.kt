package com.sprout.ide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sprout.ide.ui.screens.CollabsScreen
import com.sprout.ide.ui.screens.CommunityScreen
import com.sprout.ide.ui.screens.CreateAccountScreen
import com.sprout.ide.ui.screens.EditProfileScreen
import com.sprout.ide.ui.screens.HomeScreen
import com.sprout.ide.ui.screens.OnboardingScreen
import com.sprout.ide.ui.screens.ProfileScreen
import com.sprout.ide.ui.screens.PublicProjectScreen
import com.sprout.ide.ui.screens.PreviewScreen
import com.sprout.ide.ui.screens.ProjectScreen
import com.sprout.ide.ui.screens.SettingsScreen
import com.sprout.ide.ui.theme.SproutTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val settings by vm.settingsRepo.state.collectAsStateWithLifecycle()
            SproutTheme(settings) {
                val phase = when {
                    !vm.onboarded -> 0
                    vm.needsAccount -> 1
                    else -> 2
                }
                AnimatedContent(phase, transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(250)) }, label = "phase") { ph ->
                    when (ph) {
                        0 -> OnboardingScreen(vm)
                        1 -> CreateAccountScreen(vm)
                        else -> MainContent()
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun MainContent() {
        BackHandler(enabled = vm.backStack.size > 1) { vm.back() }
        val depth = vm.backStack.size
        AnimatedContent(
            targetState = vm.screen to depth,
            transitionSpec = {
                val forward = targetState.second >= initialState.second
                val dir = if (forward) 1 else -1
                (slideInHorizontally(tween(380)) { it / 5 * dir } + fadeIn(tween(260)) + scaleIn(tween(380), 0.96f))
                    .togetherWith(slideOutHorizontally(tween(380)) { -it / 5 * dir } + fadeOut(tween(200)) + scaleOut(tween(380), 0.96f))
            },
            contentKey = { it.first },
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
            label = "screens",
        ) { (screen, _) ->
            when (screen) {
                Screen.Home -> HomeScreen(vm)
                is Screen.Project -> ProjectScreen(vm, screen.dir)
                Screen.Settings -> SettingsScreen(vm)
                is Screen.Preview -> PreviewScreen(vm, screen.file)
                is Screen.Profile -> ProfileScreen(vm, screen.uid)
                Screen.EditProfile -> EditProfileScreen(vm)
                Screen.Community -> CommunityScreen(vm)
                is Screen.PublicProject -> PublicProjectScreen(vm, screen.id)
                Screen.Collabs -> CollabsScreen(vm)
            }
        }
    }

    override fun onStop() {
        vm.saveAll()
        super.onStop()
    }
}
