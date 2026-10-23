package com.example.fantasmandroid

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.fantasmandroid.ui.FantasmApp
import com.example.fantasmandroid.ui.theme.AccentGreen
import com.example.fantasmandroid.ui.theme.FantasmAndroidTheme
import com.example.fantasmandroid.ui.theme.FantasmDisplay
import com.example.fantasmandroid.ui.theme.Hairline
import com.example.fantasmandroid.ui.theme.IceWhite
import com.example.fantasmandroid.ui.theme.Ink
import com.example.fantasmandroid.ui.theme.SignalGreen
import com.example.fantasmandroid.ui.theme.TextDim
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var controller: FantasmController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            // Keep the Android status bar visible so the HUD sits below it like the reference device.
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            isAppearanceLightStatusBars = false
        }
        controller = FantasmController(applicationContext)

        setContent {
            FantasmAndroidTheme {
                val context = LocalContext.current
                var granted by remember { mutableStateOf(hasRequiredPermissions(context)) }
                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    granted = hasRequiredPermissions(context)
                }

                LaunchedEffect(Unit) {
                    if (!granted) launcher.launch(REQUIRED_PERMISSIONS)
                }
                LaunchedEffect(granted) {
                    if (granted) {
                        controller.stop()
                        controller.start()
                    } else {
                        controller.stop()
                    }
                }

                if (granted) {
                    FantasmApp(controller)
                } else {
                    PermissionGate(
                        onRequest = { launcher.launch(REQUIRED_PERMISSIONS) },
                        onOpenSettings = {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null)
                                    )
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasRequiredPermissions(this)) controller.start()
    }

    override fun onPause() {
        super.onPause()
        // Leaving the device screen or backgrounding the app ends an in-flight session cleanly.
        if (controller.sessionManager.active.value != null) {
            lifecycleScope.launch { controller.sessionManager.stop() }
        }
        controller.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        controller.shutdown()
    }

    private companion object {
        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        fun hasRequiredPermissions(context: android.content.Context): Boolean =
            REQUIRED_PERMISSIONS.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
    }
}

@Composable
private fun PermissionGate(onRequest: () -> Unit, onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding()
            .padding(horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "FANTASM",
            color = IceWhite,
            fontFamily = FantasmDisplay,
            fontSize = 34.sp,
            letterSpacing = 7.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "PARANORMAL RESEARCH DEVICE",
            color = AccentGreen.copy(alpha = 0.8f),
            fontSize = 9.sp,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(40.dp))
        Text(
            text = "CAMERA + MICROPHONE ACCESS",
            color = IceWhite,
            fontSize = 13.sp,
            letterSpacing = 1.5.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Fantasm needs the camera for SLS tracking, depth rendering and Twilight vision, " +
                "and the microphone for the frequency spectrum, EVP capture and ITC responses. " +
                "Everything is processed on this device and never uploaded.",
            color = TextDim,
            fontSize = 12.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(30.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .border(1.dp, SignalGreen.copy(alpha = 0.8f))
                .clickable(onClick = onRequest),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "ALLOW ACCESS",
                color = SignalGreen,
                fontSize = 12.sp,
                letterSpacing = 2.sp
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "OPEN APP SETTINGS",
            color = TextDim,
            fontSize = 10.sp,
            letterSpacing = 1.5.sp,
            modifier = Modifier
                .clickable(onClick = onOpenSettings)
                .padding(8.dp)
        )
        Spacer(Modifier.height(40.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline.copy(alpha = 0.3f)))
        Spacer(Modifier.height(12.dp))
        Text(
            text = "For entertainment purposes only. Fantasm cannot prove or disprove the paranormal.",
            color = TextDim.copy(alpha = 0.7f),
            fontSize = 9.sp,
            textAlign = TextAlign.Center
        )
    }
}
