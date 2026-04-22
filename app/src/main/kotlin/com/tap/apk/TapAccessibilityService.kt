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

    private var gravityX = 0f
    private var gravityY = 0f
    private var gravityZ = 0f
    private var linearX = 0f
    private var linearY = 0f
    private var linearZ = 0f
    private var gyroX = 0f
    private var gyroY = 0f
    private var gyroZ = 0f
    private var lastSampleMs = 0L
    private var lastPeakMs = 0L
    private var lowMotionSamples = 0
    private val samplePeriodMs = 20L
    private val gravityAlpha = 0.85f
    private val motionNoiseThreshold = 0.6f
    private val minSpikeThreshold = 3.2f
    private val maxRotationRate = 1.7f
    private val minQuietSamples = 4

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
                gravityX = (gravityAlpha * gravityX) + ((1f - gravityAlpha) * event.values[0])
                gravityY = (gravityAlpha * gravityY) + ((1f - gravityAlpha) * event.values[1])
                gravityZ = (gravityAlpha * gravityZ) + ((1f - gravityAlpha) * event.values[2])

                linearX = event.values[0] - gravityX
                linearY = event.values[1] - gravityY
                linearZ = event.values[2] - gravityZ
            }

            Sensor.TYPE_GYROSCOPE -> {
                gyroX = event.values[0]
                gyroY = event.values[1]
                gyroZ = event.values[2]
            }
        }

        val linearMagnitude = magnitude(linearX, linearY, linearZ)
        val rotationMagnitude = magnitude(gyroX, gyroY, gyroZ)
        val zImpact = abs(linearZ)
        val xyImpact = abs(linearX) + abs(linearY)

        if (linearMagnitude < motionNoiseThreshold) {
            lowMotionSamples = (lowMotionSamples + 1).coerceAtMost(20)
        } else {
            lowMotionSamples = 0
        }

        if (lowMotionSamples < minQuietSamples) return
        if (rotationMagnitude > maxRotationRate) return

        val spike = (zImpact * 1.25f) + (xyImpact * 0.35f)
        if (spike >= minSpikeThreshold && nowMs - lastPeakMs > 120L) {
            lastPeakMs = nowMs
            detector.onTapPeak(nowMs)
            lowMotionSamples = 0
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
