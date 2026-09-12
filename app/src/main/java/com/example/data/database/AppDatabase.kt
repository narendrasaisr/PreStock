package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.BasketDao
import com.example.data.dao.GroceryDao
import com.example.data.dao.OrderHistoryDao
import com.example.data.entity.BasketItem
import com.example.data.entity.GroceryItem
import com.example.data.entity.OrderHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [GroceryItem::class, OrderHistory::class, BasketItem::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun groceryDao(): GroceryDao
    abstract fun orderHistoryDao(): OrderHistoryDao
    abstract fun basketDao(): BasketDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "grocery_predictor_database"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.groceryDao(), database.orderHistoryDao(), database.basketDao())
                    }
                }
            }

            private suspend fun populateInitialData(
                groceryDao: GroceryDao,
                orderHistoryDao: OrderHistoryDao,
                basketDao: BasketDao
            ) {
                val now = System.currentTimeMillis()
                val oneDayMs = 24 * 60 * 60 * 1000L

                val sampleGroceries = listOf(
                    GroceryItem(
                        name = "Fresh Homogenised Toned Milk",
                        brand = "Amul Taaza",
                        category = "Dairy & Eggs",
                        unit = "1 L",
                        currentStockPercent = 0.12f, // 12% left -> Critical!
                        consumptionRatePerDay = 0.35f,
                        lastPurchasedEpochMs = now - (2L * oneDayMs + 10 * 3600 * 1000L),
                        predictedRunoutEpochMs = now + (9 * 3600 * 1000L), // Runs out in 9 hrs
                        preferredPlatform = "Zepto",
                        estimatedPrice = 56.0,
                        averageIntervalDays = 3.0f,
                        confidenceScore = 0.96f
                    ),
                    GroceryItem(
                        name = "Brown High-Protein Farm Eggs",
                        brand = "Eggoz",
                        category = "Dairy & Eggs",
                        unit = "6 pcs",
                        currentStockPercent = 0.18f, // 1 egg left
                        consumptionRatePerDay = 0.28f,
                        lastPurchasedEpochMs = now - (3L * oneDayMs),
                        predictedRunoutEpochMs = now + (18 * 3600 * 1000L), // Runs out tomorrow morning
                        preferredPlatform = "Blinkit",
                        estimatedPrice = 64.0,
                        averageIntervalDays = 3.5f,
                        confidenceScore = 0.94f
                    ),
                    GroceryItem(
                        name = "Whole Wheat 100% Brown Bread",
                        brand = "English Oven",
                        category = "Bakery",
                        unit = "400 g",
                        currentStockPercent = 0.22f, // End slices left
                        consumptionRatePerDay = 0.30f,
                        lastPurchasedEpochMs = now - (2L * oneDayMs + 18 * 3600 * 1000L),
                        predictedRunoutEpochMs = now + (22 * 3600 * 1000L),
                        preferredPlatform = "Swiggy Instamart",
                        estimatedPrice = 45.0,
                        averageIntervalDays = 3.2f,
                        confidenceScore = 0.91f
                    ),
                    GroceryItem(
                        name = "Fresh Red Hybrid Tomatoes",
                        brand = "Farm Fresh",
                        category = "Fresh Produce",
                        unit = "1 kg",
                        currentStockPercent = 0.25f,
                        consumptionRatePerDay = 0.20f,
                        lastPurchasedEpochMs = now - (4L * oneDayMs),
                        predictedRunoutEpochMs = now + (1L * oneDayMs + 6 * 3600 * 1000L),
                        preferredPlatform = "Zepto",
                        estimatedPrice = 42.0,
                        averageIntervalDays = 5.0f,
                        confidenceScore = 0.88f
                    ),
                    GroceryItem(
                        name = "Fresh Robusta Bananas",
                        brand = "Direct Farm",
                        category = "Fresh Produce",
                        unit = "500 g",
                        currentStockPercent = 0.35f,
                        consumptionRatePerDay = 0.33f,
                        lastPurchasedEpochMs = now - (2L * oneDayMs),
                        predictedRunoutEpochMs = now + (2L * oneDayMs),
                        preferredPlatform = "Blinkit",
                        estimatedPrice = 36.0,
                        averageIntervalDays = 3.0f,
                        confidenceScore = 0.85f
                    ),
                    GroceryItem(
                        name = "Classic Instant Coffee Granules",
                        brand = "Nescafé",
                        category = "Beverages",
                        unit = "50 g",
                        currentStockPercent = 0.45f,
                        consumptionRatePerDay = 0.08f,
                        lastPurchasedEpochMs = now - (8L * oneDayMs),
                        predictedRunoutEpochMs = now + (5L * oneDayMs),
                        preferredPlatform = "Amazon Fresh",
                        estimatedPrice = 168.0,
                        averageIntervalDays = 14.0f,
                        confidenceScore = 0.92f
                    ),
                    GroceryItem(
                        name = "Pasteurised Salted Table Butter",
                        brand = "Amul",
                        category = "Dairy & Eggs",
                        unit = "100 g",
                        currentStockPercent = 0.40f,
                        consumptionRatePerDay = 0.14f,
                        lastPurchasedEpochMs = now - (5L * oneDayMs),
                        predictedRunoutEpochMs = now + (3L * oneDayMs + 12 * 3600 * 1000L),
                        preferredPlatform = "Zepto",
                        estimatedPrice = 60.0,
                        averageIntervalDays = 7.0f,
                        confidenceScore = 0.89f
                    ),
                    GroceryItem(
                        name = "Shudh Whole Wheat Chakki Atta",
                        brand = "Aashirvaad",
                        category = "Pantry Staples",
                        unit = "5 kg",
                        currentStockPercent = 0.55f,
                        consumptionRatePerDay = 0.045f,
                        lastPurchasedEpochMs = now - (10L * oneDayMs),
                        predictedRunoutEpochMs = now + (11L * oneDayMs),
                        preferredPlatform = "Amazon Fresh",
                        estimatedPrice = 249.0,
                        averageIntervalDays = 22.0f,
                        confidenceScore = 0.95f
                    ),
                    GroceryItem(
                        name = "Sunlite Refined Sunflower Oil",
                        brand = "Fortune",
                        category = "Pantry Staples",
                        unit = "1 L",
                        currentStockPercent = 0.65f,
                        consumptionRatePerDay = 0.05f,
                        lastPurchasedEpochMs = now - (7L * oneDayMs),
                        predictedRunoutEpochMs = now + (13L * oneDayMs),
                        preferredPlatform = "Blinkit",
                        estimatedPrice = 139.0,
                        averageIntervalDays = 20.0f,
                        confidenceScore = 0.90f
                    ),
                    GroceryItem(
                        name = "India's Magic Masala Potato Chips",
                        brand = "Lay's",
                        category = "Snacks & Drinks",
                        unit = "50 g",
                        currentStockPercent = 0.20f,
                        consumptionRatePerDay = 0.40f,
                        lastPurchasedEpochMs = now - (2L * oneDayMs),
                        predictedRunoutEpochMs = now + (14 * 3600 * 1000L),
                        preferredPlatform = "Swiggy Instamart",
                        estimatedPrice = 20.0,
                        averageIntervalDays = 3.0f,
                        confidenceScore = 0.84f
                    ),
                    GroceryItem(
                        name = "Greek Natural Yogurt - Blueberry",
                        brand = "Epigamia",
                        category = "Dairy & Eggs",
                        unit = "90 g",
                        currentStockPercent = 0.10f,
                        consumptionRatePerDay = 0.50f,
                        lastPurchasedEpochMs = now - (1L * oneDayMs + 16 * 3600 * 1000L),
                        predictedRunoutEpochMs = now + (8 * 3600 * 1000L),
                        preferredPlatform = "Zepto",
                        estimatedPrice = 50.0,
                        averageIntervalDays = 2.0f,
                        confidenceScore = 0.93f
                    ),
                    GroceryItem(
                        name = "Matic Top Load Liquid Detergent",
                        brand = "Surf Excel",
                        category = "Household",
                        unit = "1 L",
                        currentStockPercent = 0.70f,
                        consumptionRatePerDay = 0.03f,
                        lastPurchasedEpochMs = now - (9L * oneDayMs),
                        predictedRunoutEpochMs = now + (23L * oneDayMs),
                        preferredPlatform = "Amazon Fresh",
                        estimatedPrice = 225.0,
                        averageIntervalDays = 30.0f,
                        confidenceScore = 0.97f
                    )
                )

                groceryDao.insertAll(sampleGroceries)

                // Seed realistic order histories from Zepto, Blinkit, Instamart, Amazon Fresh
                val sampleOrders = listOf(
                    OrderHistory(
                        orderId = "ZPT-92810",
                        platform = "Zepto",
                        timestampEpochMs = now - (2L * oneDayMs + 10 * 3600 * 1000L),
                        totalAmount = 265.0,
                        itemsCount = 4,
                        itemsSummary = "Amul Milk 1L, Epigamia Yogurt, Lay's Chips, Bread",
                        deliveryTimeMins = 9
                    ),
                    OrderHistory(
                        orderId = "BLK-48192",
                        platform = "Blinkit",
                        timestampEpochMs = now - (3L * oneDayMs),
                        totalAmount = 380.0,
                        itemsCount = 5,
                        itemsSummary = "Eggoz Brown Eggs (6), Bananas 500g, Tomatoes 1kg, Coriander",
                        deliveryTimeMins = 12
                    ),
                    OrderHistory(
                        orderId = "SWG-81720",
                        platform = "Swiggy Instamart",
                        timestampEpochMs = now - (5L * oneDayMs),
                        totalAmount = 320.0,
                        itemsCount = 3,
                        itemsSummary = "English Oven Bread, Amul Butter 100g, Magic Masala Chips",
                        deliveryTimeMins = 15
                    ),
                    OrderHistory(
                        orderId = "AMZ-10492",
                        platform = "Amazon Fresh",
                        timestampEpochMs = now - (10L * oneDayMs),
                        totalAmount = 890.0,
                        itemsCount = 6,
                        itemsSummary = "Aashirvaad Atta 5kg, Fortune Oil 1L, Nescafe 50g, Surf Excel 1L",
                        deliveryTimeMins = 110
                    )
                )
                orderHistoryDao.insertAll(sampleOrders)

                // Pre-populate critical items into pre-emptive basket
                val sampleBasket = listOf(
                    BasketItem(
                        groceryId = 1L,
                        name = "Fresh Homogenised Toned Milk",
                        brand = "Amul Taaza",
                        quantity = 2,
                        unit = "1 L",
                        unitPrice = 56.0,
                        platform = "Zepto",
                        urgencyBadge = "Critical (<24h)"
                    ),
                    BasketItem(
                        groceryId = 2L,
                        name = "Brown High-Protein Farm Eggs",
                        brand = "Eggoz",
                        quantity = 1,
                        unit = "6 pcs",
                        unitPrice = 64.0,
                        platform = "Blinkit",
                        urgencyBadge = "Critical (<24h)"
                    ),
                    BasketItem(
                        groceryId = 3L,
                        name = "Whole Wheat 100% Brown Bread",
                        brand = "English Oven",
                        quantity = 1,
                        unit = "400 g",
                        unitPrice = 45.0,
                        platform = "Swiggy Instamart",
                        urgencyBadge = "Critical (<24h)"
                    )
                )
                basketDao.insertAll(sampleBasket)
            }
        }
    }
}
