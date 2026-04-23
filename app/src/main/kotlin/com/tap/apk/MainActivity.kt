package com.tap.apk

import android.Manifest
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.tap.apk.actions.ActionRouter
import com.tap.apk.data.TapSettingsDataStore
import com.tap.apk.models.TapEvent
import com.tap.apk.models.TapPatternConfig
import com.tap.apk.ui.AppOption
import com.tap.apk.ui.TapSettingsScreen
import com.tap.apk.ui.TapTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var dataStore: TapSettingsDataStore
    private lateinit var actionRouter: ActionRouter

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataStore = TapSettingsDataStore(this)
        actionRouter = ActionRouter(this)

        requestCameraPermissionIfMissing()

        setContent {
            TapTheme {
                val appOptions = remember { loadInstalledApps() }
                val settings by dataStore.settingsFlow.collectAsState(
                    initial = mapOf(
                        TapEvent.Single to TapPatternConfig(),
                        TapEvent.Double to TapPatternConfig(),
                        TapEvent.Triple to TapPatternConfig(),
                    )
                )

                TapSettingsScreen(
                    settings = settings,
                    appOptions = appOptions,
                    onSave = { event, config ->
                        lifecycleScope.launch {
                            dataStore.updateConfig(event, config)
                        }
                    },
                    onTest = { event ->
                        lifecycleScope.launch {
                            val config = dataStore.getConfig(event)
                            actionRouter.execute(event, config)
                        }
                    },
                )
            }
        }

        if (!Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                .orEmpty()
                .contains(packageName)
        ) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private fun requestCameraPermissionIfMissing() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun loadInstalledApps(): List<AppOption> {
        val installedApps: List<ApplicationInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledApplications(0)
        }
        return installedApps
            .map {
                val packageName = it.packageName
                val label = packageManager.getApplicationLabel(it).toString().trim()
                val safeLabel = if (label.isBlank()) packageName else label
                AppOption(safeLabel, packageName)
            }
            .filterNot { it.packageName == packageName }
            .distinctBy { it.packageName }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }
}
