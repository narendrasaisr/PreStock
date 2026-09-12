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

                val sampleGroceries = QuickCommerceCatalog.getComprehensiveCatalog(now)

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
