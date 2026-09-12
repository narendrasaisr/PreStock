package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.BasketItem
import com.example.data.entity.GroceryItem
import com.example.data.entity.OrderHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface GroceryDao {
    @Query("SELECT * FROM grocery_items ORDER BY currentStockPercent ASC")
    fun getAllGroceries(): Flow<List<GroceryItem>>

    @Query("SELECT * FROM grocery_items WHERE id = :id")
    suspend fun getGroceryById(id: Long): GroceryItem?

    @Query("SELECT * FROM grocery_items WHERE currentStockPercent <= reorderThresholdPercent ORDER BY currentStockPercent ASC")
    fun getCriticalGroceries(): Flow<List<GroceryItem>>

    @Query("SELECT * FROM grocery_items WHERE category = :category ORDER BY currentStockPercent ASC")
    fun getGroceriesByCategory(category: String): Flow<List<GroceryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: GroceryItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<GroceryItem>)

    @Update
    suspend fun update(item: GroceryItem)

    @Query("UPDATE grocery_items SET currentStockPercent = :stockPercent, predictedRunoutEpochMs = :predictedRunout WHERE id = :id")
    suspend fun updateStock(id: Long, stockPercent: Float, predictedRunout: Long)

    @Query("UPDATE grocery_items SET currentStockPercent = 1.0, lastPurchasedEpochMs = :timestamp, predictedRunoutEpochMs = :newRunout WHERE id = :id")
    suspend fun markReplenished(id: Long, timestamp: Long, newRunout: Long)

    @Delete
    suspend fun delete(item: GroceryItem)

    @Query("DELETE FROM grocery_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM grocery_items")
    suspend fun getCount(): Int
}

@Dao
interface OrderHistoryDao {
    @Query("SELECT * FROM order_history ORDER BY timestampEpochMs DESC")
    fun getAllOrders(): Flow<List<OrderHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: OrderHistory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(orders: List<OrderHistory>)

    @Query("SELECT COUNT(*) FROM order_history")
    suspend fun getCount(): Int
}

@Dao
interface BasketDao {
    @Query("SELECT * FROM basket_items")
    fun getAllBasketItems(): Flow<List<BasketItem>>

    @Query("SELECT * FROM basket_items WHERE groceryId = :groceryId LIMIT 1")
    suspend fun getByGroceryId(groceryId: Long): BasketItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: BasketItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<BasketItem>)

    @Update
    suspend fun update(item: BasketItem)

    @Delete
    suspend fun delete(item: BasketItem)

    @Query("DELETE FROM basket_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM basket_items")
    suspend fun clearBasket()
}
