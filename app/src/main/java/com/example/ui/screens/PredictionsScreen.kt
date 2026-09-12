package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ml.UrgencyLevel
import com.example.ui.components.AddItemDialog
import com.example.ui.components.GroceryItemCard
import com.example.ui.components.HeroPredictionHeader
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.GroceryPredictorViewModel
import com.example.ui.viewmodel.GroceryWithPrediction

@Composable
fun PredictionsScreen(
    viewModel: GroceryPredictorViewModel,
    groceries: List<GroceryWithPrediction>,
    selectedCategory: String,
    selectedPlatformFilter: String,
    householdSize: String,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    val criticalCount = groceries.count { it.prediction.urgencyLevel == UrgencyLevel.CRITICAL }

    val categories = listOf(
        "All",
        "🚨 Critical",
        "Dairy & Eggs",
        "Fresh Produce",
        "Bakery",
        "Pantry Staples",
        "Beverages",
        "Snacks & Drinks",
        "Household"
    )

    val platforms = listOf("All", "Zepto", "Blinkit", "Swiggy Instamart", "Amazon Fresh")

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // Hero Visual & Summary
            item {
                HeroPredictionHeader(
                    criticalCount = criticalCount,
                    householdSize = householdSize,
                    onHouseholdSizeChange = { viewModel.setHouseholdSize(it) },
                    onAutoBuildBasketClick = { viewModel.addAllCriticalToBasket() }
                )
            }

            // Category Chips Row
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = cat == selectedCategory
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setCategory(cat) },
                                label = { Text(cat, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.testTag("filter_chip_$cat")
                            )
                        }
                    }
                }
            }

            // Platform Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Store Filter:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    platforms.forEach { plat ->
                        val isSelected = plat == selectedPlatformFilter
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setPlatformFilter(plat) },
                            label = { Text(plat, fontSize = 11.sp) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // List of Groceries with Real-Time ML Predictions
            if (groceries.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No items matching filter",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try selecting 'All' or add a new grocery item to monitor.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(groceries, key = { it.item.id }) { itemWithPred ->
                    GroceryItemCard(
                        itemWithPred = itemWithPred,
                        onAdjustStock = { newPercent -> viewModel.adjustStock(itemWithPred.item, newPercent) },
                        onReplenishDirectly = { viewModel.replenishItemDirectly(itemWithPred.item) },
                        onAddToBasket = { viewModel.addToBasket(itemWithPred.item, itemWithPred.prediction.urgencyLevel.title) }
                    )
                }
            }
        }

        // Floating Action Button to Add New Item
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("fab_add_item"),
            containerColor = EmeraldPrimary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Item")
        }
    }

    if (showAddDialog) {
        AddItemDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, brand, category, unit, price, platform, interval ->
                viewModel.addNewGroceryItem(name, brand, category, unit, price, platform, interval)
            }
        )
    }
}
