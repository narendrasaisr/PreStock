package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "grocery_items")
data class GroceryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val brand: String = "",
    val category: String, // "Dairy & Eggs", "Fresh Produce", "Bakery", "Pantry Staples", "Snacks & Drinks", "Household"
    val unit: String, // "1 L", "500 g", "6 pcs", "1 kg", "250 g"
    val currentStockPercent: Float, // 0.0f to 1.0f
    val consumptionRatePerDay: Float, // fraction depleted per day (e.g., 0.33 for 3-day cycle)
    val lastPurchasedEpochMs: Long,
    val predictedRunoutEpochMs: Long,
    val preferredPlatform: String, // "Zepto", "Blinkit", "Swiggy Instamart", "Amazon Fresh"
    val estimatedPrice: Double,
    val reorderThresholdPercent: Float = 0.25f,
    val averageIntervalDays: Float = 4.0f,
    val confidenceScore: Float = 0.92f,
    val autoAddRecommended: Boolean = true
)

@Entity(tableName = "order_history")
data class OrderHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderId: String,
    val platform: String, // "Zepto", "Blinkit", "Swiggy Instamart", "Amazon Fresh"
    val timestampEpochMs: Long,
    val totalAmount: Double,
    val itemsCount: Int,
    val itemsSummary: String,
    val deliveryTimeMins: Int
)

@Entity(tableName = "basket_items")
data class BasketItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val groceryId: Long,
    val name: String,
    val brand: String = "",
    val quantity: Int = 1,
    val unit: String,
    val unitPrice: Double,
    val platform: String,
    val urgencyBadge: String // "Critical (<24h)", "Predicted Depletion", "Routine Refill"
)
