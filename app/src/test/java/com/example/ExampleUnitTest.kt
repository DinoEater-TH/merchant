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
        assertTrue(repo.menuItems.value.isNotEmpty())
        assertTrue(repo.categories.value.isNotEmpty())
        assertEquals("ร้านลูกชิ้นนายเอ็ด", repo.shopInfo.value.name)
    }

    @Test
    fun testStockDeductionOnOrder() {
        val repo = MeatballRepository()
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
        val orders = repo.orders.value.filter { it.customerPhone == customerPhone }

        assertTrue("User should have past orders", orders.size >= 4)

        // Check statuses present
        val hasCompleted = orders.any { it.status == OrderStatus.COMPLETED }
        val hasCookingOrPending = orders.any { it.status == OrderStatus.PENDING || it.status == OrderStatus.COOKING }
        assertTrue("Should have completed orders", hasCompleted)
        assertTrue("Should have active orders", hasCookingOrPending)

        // Check consumption total
        val totalSpent = orders.filter { it.status != OrderStatus.CANCELLED }.sumOf { it.totalAmount }
        assertTrue("Total consumption should be greater than 100", totalSpent > 100.0)

        // Verify all order total costs are positive
        orders.forEach { order ->
            assertTrue("Order ${order.id} total must be > 0", order.totalAmount > 0)
            assertTrue("Order ${order.id} must have items", order.items.isNotEmpty())
        }
    }
}
