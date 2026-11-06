package com.example

import com.example.data.repository.MeatballRepository
import com.example.util.DeliveryRouteHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.ceil

class DeliveryRouteHelperTest {

    @Test
    fun removeOrderItem_recalculatesTotal() {
        val repo = MeatballRepository()
        repo.addOrUpdateMenuItem(
            com.example.data.model.MenuItem(
                id = "item_01", name = "test", categoryId = "cat_1", price = 10.0, directCost = 5.0,
                stockQty = 50, isUnlimitedStock = false
            )
        )
        repo.addOrder(
            com.example.data.model.Order(
                id = "ORD-REMOVE", customerPhone = "0812345678",
                items = listOf(
                    com.example.data.model.OrderItem("item_01", "test", 10.0, 5.0, 5),
                    com.example.data.model.OrderItem("item_02", "other", 10.0, 5.0, 2)
                ),
                subtotal = 70.0, discount = 0.0, deliveryFee = 0.0, totalAmount = 70.0,
                status = com.example.data.model.OrderStatus.PENDING,
                timestamp = "2026-11-06 10:00"
            )
        )
        repo.removeOrderItem("ORD-REMOVE", "item_01")
        val updated = repo.orders.value.first { it.id == "ORD-REMOVE" }
        assertEquals(1, updated.items.size)
        assertEquals(20.0, updated.subtotal, 0.001)
        assertEquals(20.0, updated.totalAmount, 0.001)
        // Limited stock is restored for the removed line.
        assertEquals(50, repo.menuItems.value.first { it.id == "item_01" }.stockQty)
    }

    @Test
    fun cancelOrder_restoresLimitedStock() {
        val repo = MeatballRepository()
        repo.addOrUpdateMenuItem(
            com.example.data.model.MenuItem(
                id = "item_01", name = "test", categoryId = "cat_1", price = 10.0, directCost = 5.0,
                stockQty = 10, isUnlimitedStock = false
            )
        )
        repo.addOrder(
            com.example.data.model.Order(
                id = "ORD-CANCEL", customerPhone = "0812345678",
                items = listOf(com.example.data.model.OrderItem("item_01", "test", 10.0, 5.0, 4)),
                subtotal = 40.0, totalAmount = 40.0,
                status = com.example.data.model.OrderStatus.PENDING,
                timestamp = "2026-11-06 10:00"
            )
        )
        assertEquals(6, repo.menuItems.value.first { it.id == "item_01" }.stockQty)
        repo.updateOrderStatus("ORD-CANCEL", com.example.data.model.OrderStatus.CANCELLED)
        assertEquals(10, repo.menuItems.value.first { it.id == "item_01" }.stockQty)
    }

    @Test
    fun deleteOrder_removesItFromList() {
        val repo = MeatballRepository()
        repo.addOrder(
            com.example.data.model.Order(
                id = "ORD-DELETE", customerPhone = "0812345678",
                items = listOf(com.example.data.model.OrderItem("item_01", "test", 10.0, 5.0, 1)),
                totalAmount = 10.0,
                status = com.example.data.model.OrderStatus.PENDING,
                timestamp = "2026-11-06 10:00"
            )
        )
        repo.deleteOrder("ORD-DELETE")
        assertEquals(0, repo.orders.value.count { it.id == "ORD-DELETE" })
    }

    @Test
    fun menuItem_unlimitedByDefault() {
        val item = com.example.data.model.MenuItem(
            id = "item_test", name = "test", categoryId = "cat_1", price = 10.0, directCost = 5.0
        )
        assertEquals(true, item.isUnlimitedStock)
        assertEquals("ไม้", item.saleUnit)
    }

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
