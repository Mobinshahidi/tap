package com.tap.apk.detection

import com.tap.apk.models.TapEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MultiTapDetectorTest {
    @Test
    fun detectsSingleTap() {
        val detector = MultiTapDetector()
        assertNull(detector.onTapPeak(0L))
        assertEquals(TapEvent.Single, detector.flush(350L))
    }

    @Test
    fun detectsDoubleTapWithinWindow() {
        val detector = MultiTapDetector()
        detector.onTapPeak(0L)
        detector.onTapPeak(220L)
        assertEquals(TapEvent.Double, detector.flush(350L))
    }

    @Test
    fun detectsTripleTapWithinWindow() {
        val detector = MultiTapDetector()
        detector.onTapPeak(0L)
        detector.onTapPeak(200L)
        detector.onTapPeak(360L)
        assertEquals(TapEvent.Triple, detector.flush(420L))
    }
}
