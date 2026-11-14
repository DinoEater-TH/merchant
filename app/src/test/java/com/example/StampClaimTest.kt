package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.repository.MeatballRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StampClaimTest {

    private lateinit var context: Context

    @Before
    fun clearSavedState() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("meatball_shop_state", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private fun repository(): MeatballRepository {
        val repo = MeatballRepository(context)
        repo.updateShopInfo(repo.shopInfo.value.copy(spendPerStamp = 50.0))
        return repo
    }

    private fun guestOrder(id: String = "ORD-CLAIM", total: Double = 120.0) = Order(
        id = id,
        customerPhone = "",
        isGuest = true,
        items = listOf(
            OrderItem(
                itemId = "item_01",
                name = "ลูกชิ้นเอ็นเนื้อ",
                price = total,
                directCost = 5.0,
                quantity = 1
            )
        ),
        subtotal = total,
        totalAmount = total,
        timestamp = "2026-11-14 20:00"
    )

    @Test
    fun guestOrder_claimedByNewPhone_createsMemberAndAwardsStamps() {
        val repo = repository()
        repo.addOrder(guestOrder())
        assertNull(repo.users.value["0891111111"])

        val result = repo.claimOrderStamps("ORD-CLAIM", "0891111111", "ลูกค้าทดสอบ")

        assertTrue(result.ok)
        assertTrue(result.isNewMember)
        assertEquals(2, result.stampsAwarded)
        assertEquals(2, result.totalStamps)
        val member = repo.users.value["0891111111"]
        assertNotNull(member)
        assertEquals("ลูกค้าทดสอบ", member!!.name)
        assertEquals(2, member.stamps)
        assertEquals("ORD-CLAIM", member.history.first().orderId)
        val order = repo.orders.value.first()
        assertEquals("0891111111", order.customerPhone)
        assertFalse(order.isGuest)

        // Reloading from disk keeps the claim.
        val reloaded = MeatballRepository(context)
        assertEquals(2, reloaded.users.value["0891111111"]?.stamps)
        assertEquals("0891111111", reloaded.orders.value.first().customerPhone)
    }

    @Test
    fun claimingSameOrderTwice_neverDoubleCounts() {
        val repo = repository()
        repo.addOrder(guestOrder())
        repo.claimOrderStamps("ORD-CLAIM", "0891111111")

        val second = repo.claimOrderStamps("ORD-CLAIM", "0891111111")

        assertTrue(second.ok)
        assertEquals(0, second.stampsAwarded)
        assertEquals(2, repo.users.value["0891111111"]?.stamps)
        assertEquals(1, repo.users.value["0891111111"]?.history?.size)
        assertEquals(1, repo.users.value["0891111111"]?.totalOrders)
    }

    @Test
    fun movingClaimToAnotherPhone_takesTheStampsBack() {
        val repo = repository()
        repo.addOrder(guestOrder())
        repo.claimOrderStamps("ORD-CLAIM", "0891111111")

        val moved = repo.claimOrderStamps("ORD-CLAIM", "0892222222")

        assertTrue(moved.ok)
        assertEquals(2, moved.stampsAwarded)
        assertEquals(0, repo.users.value["0891111111"]?.stamps)
        assertEquals(0, repo.users.value["0891111111"]?.totalOrders)
        assertTrue(repo.users.value["0891111111"]?.history?.isEmpty() == true)
        assertEquals(2, repo.users.value["0892222222"]?.stamps)
        assertEquals("ORD-CLAIM", repo.users.value["0892222222"]?.history?.first()?.orderId)
    }

    @Test
    fun invalidPhone_isRejectedWithoutTouchingData() {
        val repo = repository()
        repo.addOrder(guestOrder())

        val result = repo.claimOrderStamps("ORD-CLAIM", "08123")

        assertFalse(result.ok)
        assertTrue(repo.users.value.isEmpty())
        assertTrue(repo.orders.value.first().isGuest)
    }

    @Test
    fun orderAlreadyAwardedOnPlacement_isNotAwardedAgain() {
        val repo = repository()
        repo.addOrder(guestOrder().copy(customerPhone = "0833333333", isGuest = false))

        val result = repo.claimOrderStamps("ORD-CLAIM", "0833333333")

        assertTrue(result.ok)
        assertEquals(0, result.stampsAwarded)
        assertEquals(2, repo.users.value["0833333333"]?.stamps)
        assertEquals(1, repo.users.value["0833333333"]?.history?.size)
    }
}
