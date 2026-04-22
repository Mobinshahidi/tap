package com.tap.apk

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
                val appOptions = remember { loadLaunchableApps() }
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

    private fun loadLaunchableApps(): List<AppOption> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(intent, 0)
            .map {
                val packageName = it.activityInfo.packageName
                val label = it.loadLabel(packageManager)?.toString()?.trim().orEmpty()
                val safeLabel = if (label.isBlank()) packageName else label
                AppOption(safeLabel, packageName)
            }
            .distinctBy { it.packageName }
            .filterNot { it.packageName == packageName }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }
}
