package com.example

import com.example.data.model.AddonOptions
import com.example.data.model.FulfillmentType
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.repository.MeatballRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testInitialRepositoryState() {
        val repo = MeatballRepository()
        // No demo/sample data: everything real comes from the shop's Firebase state.
        assertTrue(repo.menuItems.value.isEmpty())
        assertTrue(repo.orders.value.isEmpty())
        assertTrue(repo.users.value.isEmpty())
        assertTrue(repo.hiddenCosts.value.isEmpty())
        assertTrue(repo.categories.value.isNotEmpty())
        assertEquals("DinoEater ลูกชิ้นทอด", repo.shopInfo.value.name)
    }

    @Test
    fun testStockDeductionOnOrder() {
        val repo = MeatballRepository()
        repo.addOrUpdateMenuItem(
            com.example.data.model.MenuItem(
                id = "item_01",
                name = "ลูกชิ้นเอ็นเนื้อ",
                categoryId = "cat_1",
                price = 10.0,
                directCost = 5.5,
                stockQty = 50,
                isUnlimitedStock = false
            )
        )
        val initialBeefStock = repo.menuItems.value.find { it.id == "item_01" }?.stockQty ?: 50

        val order = Order(
            id = "ORD-TEST-1",
            customerPhone = "0812345678",
            isGuest = false,
            fulfillment = FulfillmentType.DELIVERY,
            items = listOf(
                OrderItem(
                    itemId = "item_01",
                    name = "ลูกชิ้นเอ็นเนื้อ",
                    price = 10.0,
                    directCost = 5.5,
                    quantity = 5
                )
            ),
            options = AddonOptions(),
            totalAmount = 45.0,
            status = OrderStatus.PENDING
        )

        repo.addOrder(order)
        val updatedStock = repo.menuItems.value.find { it.id == "item_01" }?.stockQty ?: 0
        assertEquals(initialBeefStock - 5, updatedStock)
    }

    @Test
    fun testBreakEvenCalculation() {
        val repo = MeatballRepository()
        repo.addHiddenCost("เติมแก๊สหุงต้ม", "utilities", 390.0)
        val summary = repo.calculateBreakEvenSummary()
        assertTrue(summary.breakEvenUnitsNeeded > 0)
        assertTrue(summary.gasTankRemainingPercent in 1..100)
        assertTrue(summary.smartAdvices.isNotEmpty())
    }

    @Test
    fun testMemberStampRewardRedeem() {
        val repo = MeatballRepository()
        repo.setCustomerPhone("0899999999")
        repo.addMemberStamp("0899999999", 10)
        val user = repo.users.value["0899999999"]
        assertNotNull(user)
        assertTrue((user?.stamps ?: 0) >= 10)

        val redeemed = repo.redeemReward("0899999999")
        assertTrue(redeemed)
    }

    @Test
    fun testCustomerOrderHistoryAndConsumption() {
        val repo = MeatballRepository()
        val customerPhone = "0812345678"
        assertTrue("No sample orders should exist", repo.orders.value.isEmpty())

        repo.addOrder(
            Order(
                id = "ORD-TEST-COMPLETED",
                customerPhone = customerPhone,
                items = listOf(OrderItem("item_01", "ลูกชิ้นเอ็นเนื้อ", 10.0, 5.5, 5)),
                totalAmount = 45.0,
                status = OrderStatus.COMPLETED,
                timestamp = "2026-11-05 12:00"
            )
        )
        repo.addOrder(
            Order(
                id = "ORD-TEST-PENDING",
                customerPhone = customerPhone,
                items = listOf(OrderItem("item_02", "ลูกชิ้นหมูล้วนแท้", 10.0, 5.0, 2)),
                totalAmount = 20.0,
                status = OrderStatus.PENDING,
                timestamp = "2026-11-06 09:00"
            )
        )

        val orders = repo.orders.value.filter { it.customerPhone == customerPhone }
        assertEquals(2, orders.size)

        val hasCompleted = orders.any { it.status == OrderStatus.COMPLETED }
        val hasCookingOrPending = orders.any { it.status == OrderStatus.PENDING || it.status == OrderStatus.COOKING }
        assertTrue("Should have completed orders", hasCompleted)
        assertTrue("Should have active orders", hasCookingOrPending)

        val totalSpent = orders.filter { it.status != OrderStatus.CANCELLED }.sumOf { it.totalAmount }
        assertEquals(65.0, totalSpent, 0.001)
        assertTrue(orders.all { it.totalAmount > 0 && it.items.isNotEmpty() })
    }
}
