package com.tap.apk.actions

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.SystemClock
import com.tap.apk.models.FlashMode
import com.tap.apk.models.TapAction
import com.tap.apk.models.TapEvent
import com.tap.apk.models.TapPatternConfig

class ActionRouter(private val context: Context) {
    private val cameraManager = context.getSystemService(CameraManager::class.java)
    private var torchEnabled = false
    private val lastRun = mutableMapOf<TapEvent, Long>()

    fun execute(event: TapEvent, config: TapPatternConfig) {
        if (!config.enabled) return
        val now = SystemClock.elapsedRealtime()
        val elapsed = now - (lastRun[event] ?: 0L)
        if (elapsed < config.cooldownMs) return

        when (val action = config.action) {
            TapAction.None -> return
            is TapAction.Flashlight -> setFlash(action.mode)
            is TapAction.LaunchApp -> launchPackage(action.packageName)
            is TapAction.Termux -> runTermux(action.command)
        }
        lastRun[event] = now
    }

    private fun setFlash(mode: FlashMode) {
        val manager = cameraManager ?: return
        val cameraId = runCatching {
            manager.cameraIdList.firstOrNull()
        }.getOrNull() ?: return

        try {
            val next = when (mode) {
                FlashMode.Toggle -> !torchEnabled
                FlashMode.On -> true
                FlashMode.Off -> false
            }
            manager.setTorchMode(cameraId, next)
            torchEnabled = next
        } catch (_: CameraAccessException) {
        } catch (_: SecurityException) {
        }
    }

    private fun launchPackage(packageName: String) {
        if (packageName.isBlank()) return
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
    }

    private fun runTermux(command: String) {
        if (command.isBlank()) return
        val intent = Intent("com.termux.app.RUN_COMMAND").apply {
            setClassName("com.termux", "com.termux.app.RunCommandService")
            putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/sh")
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", command))
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
        }
        runCatching {
            context.startService(intent)
        }.onFailure {
            val fallback = Intent("com.termux.app.RUN_COMMAND").apply {
                putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/sh")
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", command))
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
                `package` = "com.termux"
            }
            runCatching { context.sendBroadcast(fallback) }
        }
    }
}
