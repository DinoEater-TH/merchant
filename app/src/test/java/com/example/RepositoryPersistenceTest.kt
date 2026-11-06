package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.MeatballRepository
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RepositoryPersistenceTest {

    private lateinit var context: Context

    @Before
    fun clearSavedState() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("meatball_shop_state", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun shopState_survivesRepositoryRecreation() {
        val first = MeatballRepository(context)
        first.addOrUpdateMenuItem(
            com.example.data.model.MenuItem(
                id = "item_01",
                name = "ลูกชิ้นเอ็นเนื้อ",
                categoryId = "cat_1",
                price = 10.0,
                directCost = 5.5,
                stockQty = 10,
                isUnlimitedStock = false
            )
        )
        first.updateItemStock("item_01", 7)
        first.updateShopInfo(first.shopInfo.value.copy(name = "ร้านทดสอบ"))
        first.addMemberStamp("0899999999", 3)
        first.setAdminPin("5678")

        val second = MeatballRepository(context)

        assertEquals("ร้านทดสอบ", second.shopInfo.value.name)
        assertEquals(7, second.menuItems.value.first { it.id == "item_01" }.stockQty)
        assertEquals(3, second.users.value["0899999999"]?.stamps)
        assertEquals("5678", second.adminPin.value)
    }

    @Test
    fun placedOrder_isPersistedAndStockIsDeducted() {
        val first = MeatballRepository(context)
        first.addOrUpdateMenuItem(
            com.example.data.model.MenuItem(
                id = "item_01",
                name = "ลูกชิ้นเอ็นเนื้อ",
                categoryId = "cat_1",
                price = 10.0,
                directCost = 5.5,
                stockQty = 10,
                isUnlimitedStock = false
            )
        )
        val before = first.menuItems.value.first { it.id == "item_01" }.stockQty

        first.addOrder(
            com.example.data.model.Order(
                id = "ORD-PERSIST",
                customerPhone = "0812345678",
                items = listOf(
                    com.example.data.model.OrderItem(
                        itemId = "item_01",
                        name = "ลูกชิ้นเอ็นเนื้อ",
                        price = 10.0,
                        directCost = 5.5,
                        quantity = 2
                    )
                ),
                totalAmount = 20.0
            )
        )

        val second = MeatballRepository(context)
        assertEquals(before - 2, second.menuItems.value.first { it.id == "item_01" }.stockQty)
        assertEquals("ORD-PERSIST", second.orders.value.first().id)
    }
}
