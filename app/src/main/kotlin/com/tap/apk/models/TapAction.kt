package com.tap.apk.models

sealed class TapAction {
    data object None : TapAction()
    data class Flashlight(val mode: FlashMode) : TapAction()
    data class LaunchApp(val packageName: String) : TapAction()
    data class Termux(val command: String) : TapAction()
}

enum class FlashMode {
    Toggle,
    On,
    Off,
}

data class TapPatternConfig(
    val enabled: Boolean = true,
    val cooldownMs: Long = 1_000,
    val action: TapAction = TapAction.None,
)
