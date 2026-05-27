package com.dmc.mongoclient

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmc.mongoclient.data.auth.BiometricGate
import com.dmc.mongoclient.data.settings.AppSettings
import com.dmc.mongoclient.data.settings.AppSettingsSnapshot
import com.dmc.mongoclient.ui.auth.LockScreen
import com.dmc.mongoclient.ui.nav.AppNavHost
import com.dmc.mongoclient.ui.theme.MongoClientTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var appSettings: AppSettings
    @Inject lateinit var biometricGate: BiometricGate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by appSettings.flow.collectAsStateWithLifecycle(initialValue = AppSettingsSnapshot())
            // Only gate when (a) the user asks for it AND (b) the device
            // can actually authenticate. Locking with no way to unlock would
            // strand the user.
            val canAuthenticate = remember(settings.requireAuthOnOpen) {
                biometricGate.availability(this@MainActivity) == BiometricGate.Availability.AVAILABLE
            }
            val gateActive = settings.requireAuthOnOpen && canAuthenticate

            // `unlocked` is per-Activity-instance — survives recomposition but
            // not process death. On cold start we always re-prompt.
            var unlocked by remember { mutableStateOf(false) }
            var lastError by remember { mutableStateOf<String?>(null) }

            MongoClientTheme(themeMode = settings.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (!gateActive || unlocked) {
                        AppNavHost()
                    } else {
                        LockScreen(
                            statusMessage = lastError,
                            onUnlock = {
                                biometricGate.authenticate(
                                    activity = this@MainActivity,
                                    title = "Unlock Sextant",
                                    subtitle = "Authenticate to access your saved connections",
                                    onSuccess = {
                                        lastError = null
                                        unlocked = true
                                    },
                                    onError = { msg -> lastError = msg },
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
