package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.entity.BasketItem
import com.example.data.entity.GroceryItem
import com.example.ml.MlPredictionEngine
import com.example.ml.UrgencyLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Grocery Predictor", appName)
  }

  @Test
  fun `test ML prediction urgency for low stock item`() {
    val now = System.currentTimeMillis()
    val milkItem = GroceryItem(
      id = 1L,
      name = "Amul Taaza Toned Milk",
      brand = "Amul",
      category = "Dairy & Eggs",
      unit = "1 L",
      currentStockPercent = 0.12f,
      consumptionRatePerDay = 0.5f,
      lastPurchasedEpochMs = now - (2 * 24 * 3600 * 1000L),
      predictedRunoutEpochMs = now + (6 * 3600 * 1000L),
      preferredPlatform = "Zepto",
      estimatedPrice = 54.0,
      averageIntervalDays = 2.0f
    )

    val prediction = MlPredictionEngine.evaluateItemPrediction(milkItem, 1.0f, now)
    assertEquals(UrgencyLevel.CRITICAL, prediction.urgencyLevel)
    assertTrue(prediction.confidencePercent >= 78)
    assertNotNull(prediction.recommendedRestockDateStr)
  }

  @Test
  fun `test multi platform price comparison`() {
    val basket = listOf(
      BasketItem(
        id = 1L,
        groceryId = 1L,
        name = "Amul Taaza Toned Milk",
        brand = "Amul",
        quantity = 2,
        unit = "1 L",
        unitPrice = 54.0,
        platform = "Zepto",
        urgencyBadge = "Critical"
      )
    )

    val comparisons = MlPredictionEngine.comparePlatforms(basket)
    assertEquals(4, comparisons.size)
    assertTrue(comparisons.any { it.platformName == "Zepto" })
    assertTrue(comparisons.any { it.platformName == "Blinkit" })
    assertTrue(comparisons.any { it.platformName == "Swiggy Instamart" })
    assertTrue(comparisons.any { it.platformName == "Amazon Fresh" })
    assertTrue(comparisons.any { it.isBestDeal })
  }
}

