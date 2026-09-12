package com.example.ml

import com.example.data.entity.BasketItem
import com.example.data.entity.GroceryItem
import java.util.Calendar

enum class UrgencyLevel(val title: String, val badgeColorHex: Long) {
    CRITICAL("🚨 Critical (<24h)", 0xFFEF4444),
    DEPLETING_SOON("⏳ Depleting Soon", 0xFFF59E0B),
    LOW("⚠️ Low Stock", 0xFFEAB308),
    ADEQUATE("✅ In Stock", 0xFF10B981)
}

data class PredictionResult(
    val currentStockPercent: Float,
    val hoursRemaining: Float,
    val runoutEpochMs: Long,
    val urgencyLevel: UrgencyLevel,
    val confidencePercent: Int,
    val recommendedRestockDateStr: String,
    val aiReasoning: String
)

data class PlatformComparison(
    val platformName: String,
    val deliveryTimeFormatted: String,
    val isExpress: Boolean,
    val subtotal: Double,
    val deliveryFee: Double,
    val handlingFee: Double,
    val discount: Double,
    val grandTotal: Double,
    val isBestDeal: Boolean,
    val deepLinkUri: String,
    val webFallbackUrl: String,
    val perkText: String
)

object MlPredictionEngine {

    /**
     * Predicts grocery depletion taking into account elapsed time, household size multiplier,
     * and day-of-week consumption patterns (e.g., weekend breakfast surge).
     */
    fun evaluateItemPrediction(
        item: GroceryItem,
        householdSizeMultiplier: Float = 1.0f,
        currentEpochMs: Long = System.currentTimeMillis()
    ): PredictionResult {
        val calendar = Calendar.getInstance().apply { timeInMillis = currentEpochMs }
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY

        // Weekend surge multiplier for breakfast & snacking items
        val weekendMultiplier = if (isWeekend && (item.category == "Dairy & Eggs" || item.category == "Bakery" || item.category == "Snacks & Drinks")) {
            1.35f
        } else {
            1.0f
        }

        val effectiveDailyRate = item.consumptionRatePerDay * householdSizeMultiplier * weekendMultiplier

        val elapsedMs = (currentEpochMs - item.lastPurchasedEpochMs).coerceAtLeast(0L)
        val elapsedDays = elapsedMs / (24f * 60 * 60 * 1000f)

        // Estimated remaining stock percent based on decay
        val estimatedStock = (1.0f - (elapsedDays * effectiveDailyRate)).coerceIn(0.02f, 1.0f)
        val finalStock = if (item.currentStockPercent <= estimatedStock) {
            item.currentStockPercent
        } else {
            // blend user manual update with model
            (item.currentStockPercent * 0.6f + estimatedStock * 0.4f).coerceIn(0.02f, 1.0f)
        }

        val remainingFraction = finalStock
        val daysRemaining = if (effectiveDailyRate > 0) remainingFraction / effectiveDailyRate else 99f
        val hoursRemaining = daysRemaining * 24f
        val runoutEpochMs = currentEpochMs + (hoursRemaining * 3600 * 1000L).toLong()

        val urgency = when {
            finalStock <= 0.15f || hoursRemaining <= 18f -> UrgencyLevel.CRITICAL
            finalStock <= 0.30f || hoursRemaining <= 48f -> UrgencyLevel.DEPLETING_SOON
            finalStock <= 0.50f || hoursRemaining <= 96f -> UrgencyLevel.LOW
            else -> UrgencyLevel.ADEQUATE
        }

        val confidence = ((item.confidenceScore * 100).toInt()).coerceIn(78, 98)

        val hoursRound = hoursRemaining.toInt()
        val restockStr = when {
            hoursRound <= 6 -> "Within 6 hours (Urgent)"
            hoursRound <= 24 -> "Today before 9:00 PM"
            hoursRound <= 48 -> "Tomorrow"
            daysRemaining <= 7f -> "In ${daysRemaining.toInt()} days"
            else -> "Next week"
        }

        val reasoning = when (urgency) {
            UrgencyLevel.CRITICAL -> "Consumption model detected only ${(finalStock * 100).toInt()}% remaining. At current rate (${String.format("%.2f", effectiveDailyRate)} pack/day), you'll run out by $restockStr."
            UrgencyLevel.DEPLETING_SOON -> "Typical ${item.averageIntervalDays.toInt()}-day refill cycle is nearing end. Reordering now avoids emergency out-of-stock."
            UrgencyLevel.LOW -> "Stock is dropping past 50%. Steady consumption pattern with $confidence% ML confidence."
            UrgencyLevel.ADEQUATE -> "Safe stock level. Estimated runout in ${daysRemaining.toInt()} days."
        }

        return PredictionResult(
            currentStockPercent = finalStock,
            hoursRemaining = hoursRemaining,
            runoutEpochMs = runoutEpochMs,
            urgencyLevel = urgency,
            confidencePercent = confidence,
            recommendedRestockDateStr = restockStr,
            aiReasoning = reasoning
        )
    }

    /**
     * Compares the entire basket across 4 quick commerce platforms:
     * Zepto, Blinkit, Swiggy Instamart, Amazon Fresh.
     */
    fun comparePlatforms(basketItems: List<BasketItem>): List<PlatformComparison> {
        val baseSubtotal = basketItems.sumOf { it.unitPrice * it.quantity }
        if (baseSubtotal <= 0) return emptyList()

        val platforms = listOf(
            Triple("Zepto", "9 mins ⚡", "zepto://"),
            Triple("Blinkit", "11 mins ⚡", "blinkit://"),
            Triple("Swiggy Instamart", "14 mins ⚡", "swiggy://instamart"),
            Triple("Amazon Fresh", "Same Day (2h) 📦", "amazon://fresh")
        )

        val results = platforms.map { (name, deliveryTime, uri) ->
            val priceMultiplier = when (name) {
                "Zepto" -> 0.98 // ₹2-3 cheaper on dairy & fresh
                "Blinkit" -> 0.97 // ₹3-5 promotions
                "Swiggy Instamart" -> 1.00 // Standard catalogue
                "Amazon Fresh" -> 0.93 // Bulk staple advantage
                else -> 1.00
            }

            val subtotal = baseSubtotal * priceMultiplier
            val deliveryFee = when {
                name == "Amazon Fresh" && subtotal >= 499 -> 0.0
                name == "Amazon Fresh" -> 40.0
                subtotal >= 199 -> 0.0
                else -> 25.0
            }
            val handlingFee = when (name) {
                "Zepto" -> 12.0
                "Blinkit" -> 10.0
                "Swiggy Instamart" -> 15.0
                "Amazon Fresh" -> 5.0
                else -> 10.0
            }
            val discount = when (name) {
                "Zepto" -> if (subtotal > 200) 25.0 else 0.0
                "Blinkit" -> if (subtotal > 250) 30.0 else 0.0
                "Swiggy Instamart" -> if (subtotal > 300) 40.0 else 0.0
                "Amazon Fresh" -> if (subtotal > 500) 60.0 else 0.0
                else -> 0.0
            }

            val grandTotal = (subtotal + deliveryFee + handlingFee - discount).coerceAtLeast(10.0)

            val perk = when (name) {
                "Zepto" -> "⚡ Fastest Dark Store dispatch in your pincode"
                "Blinkit" -> "🥦 Farm pick guarantee & fresh quality check"
                "Swiggy Instamart" -> "🎁 Instant cashback with Swiggy One"
                "Amazon Fresh" -> "💰 Maximum bulk savings on staples"
                else -> ""
            }

            val webFallback = when (name) {
                "Zepto" -> "https://www.zeptonow.com"
                "Blinkit" -> "https://blinkit.com"
                "Swiggy Instamart" -> "https://www.swiggy.com/instamart"
                else -> "https://www.amazon.in/fresh"
            }

            PlatformComparison(
                platformName = name,
                deliveryTimeFormatted = deliveryTime,
                isExpress = name != "Amazon Fresh",
                subtotal = subtotal,
                deliveryFee = deliveryFee,
                handlingFee = handlingFee,
                discount = discount,
                grandTotal = grandTotal,
                isBestDeal = false, // will update below
                deepLinkUri = uri,
                webFallbackUrl = webFallback,
                perkText = perk
            )
        }

        val minPrice = results.minOfOrNull { it.grandTotal } ?: 0.0
        return results.map { it.copy(isBestDeal = it.grandTotal == minPrice) }
    }
}
