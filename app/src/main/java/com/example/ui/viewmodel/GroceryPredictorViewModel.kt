package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiClient
import com.example.data.database.AppDatabase
import com.example.data.entity.BasketItem
import com.example.data.entity.GroceryItem
import com.example.data.entity.OrderHistory
import com.example.data.repository.GroceryRepository
import com.example.ml.MlPredictionEngine
import com.example.ml.PlatformComparison
import com.example.ml.PredictionResult
import com.example.ml.UrgencyLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab(val title: String) {
    PREDICTIONS("Pantry & AI"),
    SMART_BASKET("Smart Basket"),
    HISTORY("Sync & History"),
    AI_ADVISOR("Gemini Insights")
}

data class GroceryWithPrediction(
    val item: GroceryItem,
    val prediction: PredictionResult
)

data class AiMessage(
    val sender: String, // "User" or "Gemini"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

class GroceryPredictorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GroceryRepository

    init {
        val db = AppDatabase.getDatabase(application, viewModelScope)
        repository = GroceryRepository(db.groceryDao(), db.orderHistoryDao(), db.basketDao())
    }

    private val _currentTab = MutableStateFlow(AppTab.PREDICTIONS)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedPlatformFilter = MutableStateFlow("All")
    val selectedPlatformFilter: StateFlow<String> = _selectedPlatformFilter.asStateFlow()

    private val _householdSize = MutableStateFlow("2 Persons")
    val householdSize: StateFlow<String> = _householdSize.asStateFlow()

    private val _householdMultiplier = MutableStateFlow(1.0f)

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // AI Advisor State
    private val _aiMessages = MutableStateFlow<List<AiMessage>>(
        listOf(
            AiMessage(
                sender = "Gemini",
                message = "👋 Hello! I'm your Predictive Grocery AI. I continuously analyze your household consumption across **Zepto**, **Blinkit**, **Swiggy Instamart**, and **Amazon Fresh**.\n\nAsk me about upcoming stockouts, weekend meal replenishment, or store price matches!"
            )
        )
    )
    val aiMessages: StateFlow<List<AiMessage>> = _aiMessages.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    val rawGroceries: StateFlow<List<GroceryItem>> = repository.allGroceries.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allOrders: StateFlow<List<OrderHistory>> = repository.allOrders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val basketItems: StateFlow<List<BasketItem>> = repository.basketItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Combined filtered items with real-time ML evaluation
    val groceriesWithPredictions: StateFlow<List<GroceryWithPrediction>> = combine(
        rawGroceries,
        _selectedCategory,
        _selectedPlatformFilter,
        _householdMultiplier
    ) { groceries, category, platform, multiplier ->
        val now = System.currentTimeMillis()
        groceries
            .map { item ->
                val prediction = MlPredictionEngine.evaluateItemPrediction(item, multiplier, now)
                GroceryWithPrediction(item, prediction)
            }
            .filter { itemWithPred ->
                val categoryMatch = when (category) {
                    "All" -> true
                    "🚨 Critical" -> itemWithPred.prediction.urgencyLevel == UrgencyLevel.CRITICAL
                    else -> itemWithPred.item.category.equals(category, ignoreCase = true)
                }
                val platformMatch = when (platform) {
                    "All" -> true
                    else -> itemWithPred.item.preferredPlatform.equals(platform, ignoreCase = true)
                }
                categoryMatch && platformMatch
            }
            .sortedBy { it.prediction.urgencyLevel.ordinal }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Real-time multi-platform price comparison for active basket
    val platformComparisons: StateFlow<List<PlatformComparison>> = combine(
        basketItems
    ) { itemsArray ->
        val items = itemsArray[0]
        MlPredictionEngine.comparePlatforms(items)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setPlatformFilter(platform: String) {
        _selectedPlatformFilter.value = platform
    }

    fun setHouseholdSize(size: String) {
        _householdSize.value = size
        _householdMultiplier.value = when (size) {
            "1 Person" -> 0.65f
            "2 Persons" -> 1.0f
            "Family (4+)" -> 1.85f
            else -> 1.0f
        }
        showSnackbar("Household profile updated to $size. ML consumption rates adjusted.")
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showSnackbar(msg: String) {
        _snackbarMessage.value = msg
    }

    fun adjustStock(item: GroceryItem, newPercent: Float) {
        viewModelScope.launch {
            val clamped = newPercent.coerceIn(0.0f, 1.0f)
            val estimatedDaysLeft = clamped / (item.consumptionRatePerDay.coerceAtLeast(0.05f))
            repository.updateStock(item.id, clamped, estimatedDaysLeft)
            showSnackbar("Updated ${item.name} stock to ${(clamped * 100).toInt()}%")
        }
    }

    fun replenishItemDirectly(item: GroceryItem) {
        viewModelScope.launch {
            repository.markReplenished(item.id, item.averageIntervalDays)
            showSnackbar("${item.name} marked replenished (100% stock)")
        }
    }

    fun addToBasket(item: GroceryItem, urgencyReason: String) {
        viewModelScope.launch {
            repository.addToBasket(item, urgencyReason)
            showSnackbar("Added ${item.name} to Smart Basket")
        }
    }

    fun updateBasketQuantity(item: BasketItem, delta: Int) {
        viewModelScope.launch {
            val newQty = item.quantity + delta
            if (newQty <= 0) {
                repository.removeBasketItem(item.id)
                showSnackbar("Removed ${item.name} from basket")
            } else {
                repository.updateBasketItem(item.copy(quantity = newQty))
            }
        }
    }

    fun removeBasketItem(item: BasketItem) {
        viewModelScope.launch {
            repository.removeBasketItem(item.id)
            showSnackbar("Removed ${item.name} from basket")
        }
    }

    fun clearBasket() {
        viewModelScope.launch {
            repository.clearBasket()
            showSnackbar("Smart Basket cleared")
        }
    }

    fun addAllCriticalToBasket() {
        viewModelScope.launch {
            val criticalList = groceriesWithPredictions.value.filter {
                it.prediction.urgencyLevel == UrgencyLevel.CRITICAL ||
                it.prediction.urgencyLevel == UrgencyLevel.DEPLETING_SOON
            }
            if (criticalList.isEmpty()) {
                showSnackbar("No critical items detected! Your pantry is stocked.")
                return@launch
            }
            var count = 0
            for (itemWithPred in criticalList) {
                repository.addToBasket(itemWithPred.item, itemWithPred.prediction.urgencyLevel.title)
                count++
            }
            showSnackbar("Added $count urgently needed items to Smart Basket")
        }
    }

    fun placeOrder(comparison: PlatformComparison) {
        viewModelScope.launch {
            val currentBasket = basketItems.value
            if (currentBasket.isEmpty()) {
                showSnackbar("Basket is empty!")
                return@launch
            }
            val deliveryMins = if (comparison.platformName == "Zepto") 9
            else if (comparison.platformName == "Blinkit") 11
            else if (comparison.platformName == "Swiggy Instamart") 14
            else 120

            repository.checkoutBasket(
                platform = comparison.platformName,
                deliveryMins = deliveryMins,
                items = currentBasket,
                totalAmount = comparison.grandTotal
            )
            showSnackbar("Order placed on ${comparison.platformName}! Stock levels reset & ML intervals updated.")
        }
    }

    fun simulateSyncPastOrder(platform: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val orderNum = (10000..99999).random()
            val order = OrderHistory(
                orderId = "SYNC-$orderNum",
                platform = platform,
                timestampEpochMs = now,
                totalAmount = 349.0,
                itemsCount = 4,
                itemsSummary = "Amul Taaza Milk (2x), English Oven Bread, Tomatoes 1kg",
                deliveryTimeMins = if (platform == "Zepto") 10 else 12
            )
            repository.recordOrder(order)
            showSnackbar("Synced recent receipt from $platform! Updated replenishment baseline.")
        }
    }

    fun addNewGroceryItem(
        name: String,
        brand: String,
        category: String,
        unit: String,
        price: Double,
        platform: String,
        intervalDays: Float
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val consumptionRate = 1.0f / intervalDays.coerceAtLeast(1f)
            val runout = now + (intervalDays * 24 * 60 * 60 * 1000L).toLong()
            val item = GroceryItem(
                name = name,
                brand = brand,
                category = category,
                unit = unit,
                currentStockPercent = 1.0f,
                consumptionRatePerDay = consumptionRate,
                lastPurchasedEpochMs = now,
                predictedRunoutEpochMs = runout,
                preferredPlatform = platform,
                estimatedPrice = price,
                averageIntervalDays = intervalDays,
                confidenceScore = 0.85f
            )
            repository.insertGrocery(item)
            showSnackbar("Added $name to Predictive Inventory")
        }
    }

    fun askGemini(promptText: String) {
        if (promptText.isBlank()) return

        val userMsg = AiMessage(sender = "User", message = promptText)
        _aiMessages.value = _aiMessages.value + userMsg
        _isAiLoading.value = true

        viewModelScope.launch {
            try {
                val response = GeminiClient.queryGeminiPrediction(
                    userPrompt = promptText,
                    currentInventory = rawGroceries.value,
                    recentOrders = allOrders.value,
                    householdSize = _householdSize.value
                )
                val geminiMsg = AiMessage(sender = "Gemini", message = response)
                _aiMessages.value = _aiMessages.value + geminiMsg
            } catch (e: Exception) {
                val errorMsg = AiMessage(sender = "Gemini", message = "Unable to process prediction: ${e.message}")
                _aiMessages.value = _aiMessages.value + errorMsg
            } finally {
                _isAiLoading.value = false
            }
        }
    }
}
