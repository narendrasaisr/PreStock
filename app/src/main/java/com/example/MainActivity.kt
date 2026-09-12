package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.AiAdvisorScreen
import com.example.ui.screens.OrderHistoryScreen
import com.example.ui.screens.PredictionsScreen
import com.example.ui.screens.SmartBasketScreen
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldOnContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.GroceryPredictorViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: GroceryPredictorViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
                val groceries by viewModel.groceriesWithPredictions.collectAsStateWithLifecycle()
                val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
                val selectedPlatformFilter by viewModel.selectedPlatformFilter.collectAsStateWithLifecycle()
                val householdSize by viewModel.householdSize.collectAsStateWithLifecycle()
                val basketItems by viewModel.basketItems.collectAsStateWithLifecycle()
                val comparisons by viewModel.platformComparisons.collectAsStateWithLifecycle()
                val orders by viewModel.allOrders.collectAsStateWithLifecycle()
                val aiMessages by viewModel.aiMessages.collectAsStateWithLifecycle()
                val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
                val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()

                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(snackbarMsg) {
                    snackbarMsg?.let {
                        snackbarHostState.showSnackbar(it)
                        viewModel.clearSnackbar()
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = EmeraldPrimary,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.padding(6.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Grocery Predictor",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = EmeraldContainer
                                    ) {
                                        Text(
                                            text = "ML ENGINE",
                                            color = EmeraldOnContainer,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { viewModel.setTab(AppTab.SMART_BASKET) },
                                    modifier = Modifier.testTag("top_bar_basket_button")
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (basketItems.isNotEmpty()) {
                                                Badge(containerColor = EmeraldPrimary) {
                                                    Text("${basketItems.sumOf { it.quantity }}")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ShoppingBag,
                                            contentDescription = "Smart Basket",
                                            tint = if (currentTab == AppTab.SMART_BASKET) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .testTag("bottom_nav_bar"),
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp
                        ) {
                            NavigationBarItem(
                                selected = currentTab == AppTab.PREDICTIONS,
                                onClick = { viewModel.setTab(AppTab.PREDICTIONS) },
                                icon = { Icon(Icons.Default.Inventory2, contentDescription = "Pantry & AI") },
                                label = { Text("Pantry & AI", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = EmeraldContainer,
                                    selectedIconColor = EmeraldOnContainer
                                ),
                                modifier = Modifier.testTag("nav_predictions")
                            )

                            NavigationBarItem(
                                selected = currentTab == AppTab.SMART_BASKET,
                                onClick = { viewModel.setTab(AppTab.SMART_BASKET) },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (basketItems.isNotEmpty()) {
                                                Badge(containerColor = EmeraldPrimary) {
                                                    Text("${basketItems.size}")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.ShoppingBag, contentDescription = "Smart Basket")
                                    }
                                },
                                label = { Text("Smart Basket", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = EmeraldContainer,
                                    selectedIconColor = EmeraldOnContainer
                                ),
                                modifier = Modifier.testTag("nav_basket")
                            )

                            NavigationBarItem(
                                selected = currentTab == AppTab.HISTORY,
                                onClick = { viewModel.setTab(AppTab.HISTORY) },
                                icon = { Icon(Icons.Default.History, contentDescription = "Receipts & Baseline") },
                                label = { Text("Receipts", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = EmeraldContainer,
                                    selectedIconColor = EmeraldOnContainer
                                ),
                                modifier = Modifier.testTag("nav_history")
                            )

                            NavigationBarItem(
                                selected = currentTab == AppTab.AI_ADVISOR,
                                onClick = { viewModel.setTab(AppTab.AI_ADVISOR) },
                                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Gemini Insights") },
                                label = { Text("Gemini AI", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = EmeraldContainer,
                                    selectedIconColor = EmeraldOnContainer
                                ),
                                modifier = Modifier.testTag("nav_ai_advisor")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentTab) {
                            AppTab.PREDICTIONS -> PredictionsScreen(
                                viewModel = viewModel,
                                groceries = groceries,
                                selectedCategory = selectedCategory,
                                selectedPlatformFilter = selectedPlatformFilter,
                                householdSize = householdSize
                            )
                            AppTab.SMART_BASKET -> SmartBasketScreen(
                                viewModel = viewModel,
                                basketItems = basketItems,
                                comparisons = comparisons
                            )
                            AppTab.HISTORY -> OrderHistoryScreen(
                                viewModel = viewModel,
                                orders = orders
                            )
                            AppTab.AI_ADVISOR -> AiAdvisorScreen(
                                viewModel = viewModel,
                                messages = aiMessages,
                                isLoading = isAiLoading
                            )
                        }
                    }
                }
            }
        }
    }
}
