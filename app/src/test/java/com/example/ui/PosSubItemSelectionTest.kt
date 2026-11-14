package com.example.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The app POS must let the shop pick sub-items (เมนูรอง) per unit, exactly like the
 * customer web card: เกี๊ยวห่อลูกชิ้น = หมู x2 + คอกเทล x1.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PosSubItemSelectionTest {

    private val subs = """[
        {"id":"s1","name":"ลูกชิ้นหมู","enabled":true},
        {"id":"s2","name":"คอกเทล","enabled":true},
        {"id":"s3","name":"ลูกชิ้นเอ็นหมู","enabled":true}
    ]"""

    @Test
    fun countsBecomePerUnitSummary() {
        val result = buildSubItemSelection(subs, mapOf("ลูกชิ้นหมู" to 2, "คอกเทล" to 1))
        assertEquals("ลูกชิ้นหมู x2, คอกเทล x1", result.summary)
        // One name per unit so the stored order matches the cart quantity.
        assertEquals("ลูกชิ้นหมู,ลูกชิ้นหมู,คอกเทล", result.selected)
    }

    @Test
    fun zeroAndNegativeCountsAreIgnored() {
        val result = buildSubItemSelection(subs, mapOf("ลูกชิ้นหมู" to 0, "คอกเทล" to -3))
        assertEquals("", result.summary)
        assertEquals("", result.selected)
    }

    @Test
    fun disabledSubItemsAreNeverCounted() {
        val json = """[{"id":"s1","name":"ลูกชิ้นหมู","enabled":false},{"id":"s2","name":"คอกเทล","enabled":true}]"""
        val result = buildSubItemSelection(json, mapOf("ลูกชิ้นหมู" to 3, "คอกเทล" to 1))
        assertEquals("คอกเทล x1", result.summary)
    }

    @Test
    fun brokenOrEmptyConfigDoesNotCrash() {
        assertEquals("", buildSubItemSelection("", mapOf("ลูกชิ้นหมู" to 1)).summary)
        assertEquals("", buildSubItemSelection("not-json", mapOf("ลูกชิ้นหมู" to 1)).summary)
    }
}
