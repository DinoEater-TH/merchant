package com.example.data.repository

import com.example.data.model.MenuItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** POS drag and drop must only change the order of the cards the shop actually sees. */
class MenuReorderTest {

    private fun item(id: String, category: String, sortOrder: Int) = MenuItem(
        id = id,
        name = id,
        categoryId = category,
        price = 10.0,
        directCost = 5.0,
        sortOrder = sortOrder
    )

    private val items = listOf(
        item("A", "cat_5", 0),
        item("B", "cat_10", 1),
        item("C", "cat_5", 2),
        item("D", "cat_10", 3)
    )

    @Test
    fun draggingInOneCategoryKeepsOtherCategoriesInPlace() {
        // Shop moved C above A inside "cat_5" only.
        val result = applyVisibleOrderSlots(items, listOf("C", "A"))!!
        assertEquals(listOf("C", "B", "A", "D"), result.map { it.id })
        assertEquals(listOf(0, 1, 2, 3), result.map { it.sortOrder })
    }

    @Test
    fun draggingInTheAllTabReordersEverything() {
        val result = applyVisibleOrderSlots(items, listOf("D", "B", "C", "A"))!!
        assertEquals(listOf("D", "B", "C", "A"), result.map { it.id })
        assertEquals(listOf(0, 1, 2, 3), result.map { it.sortOrder })
    }

    @Test
    fun unknownOrDuplicateIdsAreRejected() {
        assertNull(applyVisibleOrderSlots(items, listOf("A", "missing")))
        assertNull(applyVisibleOrderSlots(items, listOf("A", "A")))
        assertNull(applyVisibleOrderSlots(items, emptyList()))
    }

    @Test
    fun tiesInSortOrderStillProduceADeterministicOrder() {
        val tied = listOf(item("A", "cat", 0), item("B", "cat", 0), item("C", "cat", 0))
        val result = applyVisibleOrderSlots(tied, listOf("C", "A", "B"))!!
        assertEquals(listOf("C", "A", "B"), result.map { it.id })
        assertEquals(listOf(0, 1, 2), result.map { it.sortOrder })
    }
}
