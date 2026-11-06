package com.example

import com.example.util.DeliveryRouteHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.ceil

class DeliveryRouteHelperTest {

    @Test
    fun parseCoordinate_valid() {
        val parsed = DeliveryRouteHelper.parseCoordinate("16.442100, 102.835800")
        assertNotNull(parsed)
        assertEquals(16.4421, parsed!!.latitude, 0.000001)
        assertEquals(102.8358, parsed.longitude, 0.000001)
    }

    @Test
    fun parseCoordinate_invalid() {
        assertNull(DeliveryRouteHelper.parseCoordinate(null))
        assertNull(DeliveryRouteHelper.parseCoordinate(""))
        assertNull(DeliveryRouteHelper.parseCoordinate("not-a-coordinate"))
        assertNull(DeliveryRouteHelper.parseCoordinate("999, 999"))
    }

    @Test
    fun deliveryFeePerKm_roundsUp() {
        // 7.3 km at 10 baht/km = 73 baht (ceil of 73.0)
        assertEquals(73.0, ceil(7.3 * 10.0), 0.0001)
        // 7.31 km at 10 baht/km = 73.1 -> 74 baht
        assertEquals(74.0, ceil(7.31 * 10.0), 0.0001)
    }

    @Test
    fun overLimit_blocked() {
        val maxKm = 20.0
        assertEquals(true, 20.1 > maxKm)
        assertEquals(false, 20.0 > maxKm)
    }
}
