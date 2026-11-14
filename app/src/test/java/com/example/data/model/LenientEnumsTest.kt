package com.example.data.model

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The customer web used to store Thai labels in `options.skewer`, which made the default
 * Moshi enum adapter throw and reject the whole Firebase state ("Read data error").
 */
class LenientEnumsTest {

    private val moshi = Moshi.Builder()
        .add(LenientEnums.FACTORY)
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun thaiSkewerLabelParsesInsteadOfThrowing() {
        val adapter = moshi.adapter(AddonOptions::class.java)
        val parsed = adapter.fromJson(
            """{"skewer":"เสียบไม้","skewerLabels":["เสียบไม้"],"comment":"","selectedAddons":[]}"""
        )!!
        assertEquals(SkewerOption.SKEWERED, parsed.skewer)
        assertEquals(listOf("เสียบไม้"), parsed.skewerLabels)
    }

    @Test
    fun multiSelectLabelKeepsFirstKnownOption() {
        val adapter = moshi.adapter(AddonOptions::class.java)
        val parsed = adapter.fromJson(
            """{"skewer":"เสียบไม้, รับไม้เสียบแยก","skewerLabels":["เสียบไม้","รับไม้เสียบแยก"]}"""
        )!!
        assertEquals(SkewerOption.SKEWERED, parsed.skewer)
        assertEquals("เสียบไม้, รับไม้เสียบแยก", parsed.skewerDisplayText())
    }

    @Test
    fun unknownEnumValuesFallBackInsteadOfFailing() {
        val orderAdapter = moshi.adapter(Order::class.java)
        val order = orderAdapter.fromJson(
            """{"id":"ORD-1","fulfillment":"ส่งเอง","status":"อะไรก็ไม่รู้","options":{"skewer":"ใส่ถุง (ไม่เสียบไม้)"}}"""
        )!!
        assertEquals(OrderStatus.PENDING, order.status)
        assertEquals(FulfillmentType.DELIVERY, order.fulfillment)
        assertEquals(SkewerOption.IN_BAG, order.options.skewer)
    }

    @Test
    fun enumNamesStillSerializeTheSameWay() {
        val adapter = moshi.adapter(OrderStatus::class.java)
        assertEquals("\"COOKING\"", adapter.toJson(OrderStatus.COOKING))
    }
}
