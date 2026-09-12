package com.example.data.repository

import com.example.data.dao.BasketDao
import com.example.data.dao.GroceryDao
import com.example.data.dao.OrderHistoryDao
import com.example.data.entity.BasketItem
import com.example.data.entity.GroceryItem
import com.example.data.entity.OrderHistory
import kotlinx.coroutines.flow.Flow

class GroceryRepository(
    private val groceryDao: GroceryDao,
    private val orderHistoryDao: OrderHistoryDao,
    private val basketDao: BasketDao
) {
    val allGroceries: Flow<List<GroceryItem>> = groceryDao.getAllGroceries()
    val criticalGroceries: Flow<List<GroceryItem>> = groceryDao.getCriticalGroceries()
    val allOrders: Flow<List<OrderHistory>> = orderHistoryDao.getAllOrders()
    val basketItems: Flow<List<BasketItem>> = basketDao.getAllBasketItems()

    suspend fun insertGrocery(item: GroceryItem) = groceryDao.insert(item)

    suspend fun updateGrocery(item: GroceryItem) = groceryDao.update(item)

    suspend fun deleteGrocery(id: Long) = groceryDao.deleteById(id)

    suspend fun updateStock(id: Long, newStockPercent: Float, estimatedDaysLeft: Float) {
        val now = System.currentTimeMillis()
        val runoutMs = now + (estimatedDaysLeft * 24 * 60 * 60 * 1000L).toLong()
        groceryDao.updateStock(id, newStockPercent.coerceIn(0f, 1f), runoutMs)
    }

    suspend fun markReplenished(id: Long, intervalDays: Float) {
        val now = System.currentTimeMillis()
        val runoutMs = now + (intervalDays * 24 * 60 * 60 * 1000L).toLong()
        groceryDao.markReplenished(id, now, runoutMs)
    }

    suspend fun addToBasket(item: GroceryItem, urgencyReason: String = "Predicted Need") {
        val existing = basketDao.getByGroceryId(item.id)
        if (existing != null) {
            basketDao.update(existing.copy(quantity = existing.quantity + 1))
        } else {
            basketDao.insert(
                BasketItem(
                    groceryId = item.id,
                    name = item.name,
                    brand = item.brand,
                    quantity = 1,
                    unit = item.unit,
                    unitPrice = item.estimatedPrice,
                    platform = item.preferredPlatform,
                    urgencyBadge = urgencyReason
                )
            )
        }
    }

    suspend fun updateBasketItemQuantity(id: Long, newQty: Int) {
        if (newQty <= 0) {
            basketDao.deleteById(id)
        } else {
            // we can retrieve and update or handle directly
        }
    }

    suspend fun updateBasketItem(basketItem: BasketItem) {
        basketDao.update(basketItem)
    }

    suspend fun removeBasketItem(id: Long) {
        basketDao.deleteById(id)
    }

    suspend fun clearBasket() {
        basketDao.clearBasket()
    }

    suspend fun recordOrder(order: OrderHistory) {
        orderHistoryDao.insert(order)
    }

    suspend fun checkoutBasket(
        platform: String,
        deliveryMins: Int,
        items: List<BasketItem>,
        totalAmount: Double
    ) {
        val now = System.currentTimeMillis()
        val orderNumber = (10000..99999).random()
        val prefix = when (platform.lowercase()) {
            "zepto" -> "ZPT"
            "blinkit" -> "BLK"
            "swiggy instamart" -> "SWG"
            else -> "AMZ"
        }
        val order = OrderHistory(
            orderId = "$prefix-$orderNumber",
            platform = platform,
            timestampEpochMs = now,
            totalAmount = totalAmount,
            itemsCount = items.sumOf { it.quantity },
            itemsSummary = items.joinToString(", ") { "${it.name} (${it.quantity}x)" },
            deliveryTimeMins = deliveryMins
        )
        orderHistoryDao.insert(order)

        // Mark ordered groceries as replenished to 100% and calculate next predicted runout
        for (item in items) {
            val grocery = groceryDao.getGroceryById(item.groceryId)
            if (grocery != null) {
                // Slightly adjust interval using learning factor
                val nextRunout = now + (grocery.averageIntervalDays * 24 * 60 * 60 * 1000L).toLong()
                groceryDao.markReplenished(grocery.id, now, nextRunout)
            }
        }

        // Clear checked out basket
        basketDao.clearBasket()
    }
}
