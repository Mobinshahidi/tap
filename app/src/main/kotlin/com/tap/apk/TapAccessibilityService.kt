package com.tap.apk

import android.accessibilityservice.AccessibilityService
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.tap.apk.actions.ActionRouter
import com.tap.apk.data.TapSettingsDataStore
import com.tap.apk.detection.MultiTapDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

class TapAccessibilityService : AccessibilityService(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private val detector = MultiTapDetector()
    private lateinit var router: ActionRouter
    private lateinit var store: TapSettingsDataStore
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var accelDelta = 0f
    private var accelBaseline = 0f
    private var gyroMag = 0f
    private var lastSampleMs = 0L
    private var lastPeakMs = 0L
    private var consecutiveMotionSamples = 0
    private val samplePeriodMs = 20L
    private val peakThreshold = 2.5f
    private val gyroscopeWeight = 0.85f
    private val motionGateThreshold = 0.45f
    private val steadyAlpha = 0.92f

    private val handler = Handler(Looper.getMainLooper())
    private val flushTask = object : Runnable {
        override fun run() {
            flushAndDispatch()
            handler.postDelayed(this, 80L)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        sensorManager = getSystemService(SensorManager::class.java)
        router = ActionRouter(this)
        store = TapSettingsDataStore(this)
        registerSensors()
        handler.post(flushTask)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(flushTask)
        sensorManager.unregisterListener(this)
    }

    override fun onInterrupt() = Unit

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onSensorChanged(event: SensorEvent) {
        val nowMs = SystemClock.elapsedRealtime()
        if (nowMs - lastSampleMs < samplePeriodMs) return
        lastSampleMs = nowMs

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val rawAccel = magnitude(event.values[0], event.values[1], event.values[2]) - SensorManager.GRAVITY_EARTH
                accelBaseline = (accelBaseline * steadyAlpha) + (rawAccel * (1f - steadyAlpha))
                accelDelta = rawAccel - accelBaseline
            }

            Sensor.TYPE_GYROSCOPE -> {
                gyroMag = magnitude(event.values[0], event.values[1], event.values[2])
            }
        }

        val motionSignal = abs(accelDelta) + (gyroMag * 0.35f)
        if (motionSignal >= motionGateThreshold) {
            consecutiveMotionSamples = (consecutiveMotionSamples + 1).coerceAtMost(10)
        } else {
            consecutiveMotionSamples = 0
        }

        if (consecutiveMotionSamples < 2) return

        val composite = (abs(accelDelta) * 0.95f) + (gyroMag * gyroscopeWeight)
        if (composite >= peakThreshold && nowMs - lastPeakMs > 100L) {
            lastPeakMs = nowMs
            detector.onTapPeak(nowMs)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun flushAndDispatch() {
        val tapEvent = detector.flush(SystemClock.elapsedRealtime()) ?: return
        serviceScope.launch {
            val config = store.getConfig(tapEvent)
            router.execute(tapEvent, config)
        }
    }

    private fun registerSensors() {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    private fun magnitude(x: Float, y: Float, z: Float): Float = sqrt((x * x) + (y * y) + (z * z))
}
