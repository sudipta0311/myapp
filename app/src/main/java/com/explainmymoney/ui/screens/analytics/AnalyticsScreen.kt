package com.explainmymoney.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.explainmymoney.domain.model.Transaction
import com.explainmymoney.domain.model.TransactionCategory
import com.explainmymoney.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    transactions: List<Transaction>,
    totalSpent: Double,
    totalIncome: Double,
    categoryBreakdown: Map<TransactionCategory, Double>,
    currencySymbol: String,
    totalSpentThisYear: Double = 0.0,
    totalIncomeThisYear: Double = 0.0,
    totalInvestedThisYear: Double = 0.0,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dailyAverage = if (Calendar.getInstance().get(Calendar.DAY_OF_MONTH) > 0) {
        totalSpent / Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    } else 0.0

    val daysRemaining = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH) - 
                       Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    val projectedMonthlySpend = totalSpent + (dailyAverage * daysRemaining)

    LaunchedEffect(Unit) {
        onRefresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Analytics",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = getCurrentMonthYear(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        title = "Total Spent",
                        amount = totalSpent,
                        currencySymbol = currencySymbol,
                        icon = Icons.Default.ArrowUpward,
                        iconColor = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "Total Income",
                        amount = totalIncome,
                        currencySymbol = currencySymbol,
                        icon = Icons.Default.ArrowDownward,
                        iconColor = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "IF THIS CONTINUES...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Projected Monthly Spend",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "$currencySymbol${formatAmount(projectedMonthlySpend)}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Daily Average",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "$currencySymbol${formatAmount(dailyAverage)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "$daysRemaining days remaining in this month",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            item {
                Text(
                    text = "SPENDING BY CATEGORY",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }

            if (categoryBreakdown.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No spending data yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(categoryBreakdown.entries.sortedByDescending { it.value }.toList()) { (category, amount) ->
                    CategoryRow(
                        category = category,
                        amount = amount,
                        total = totalSpent,
                        currencySymbol = currencySymbol
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "YEARLY SUMMARY (${Calendar.getInstance().get(Calendar.YEAR)})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Total Spent",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "$currencySymbol${formatAmount(totalSpentThisYear)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Total Income",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "$currencySymbol${formatAmount(totalIncomeThisYear)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Total Invested",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "$currencySymbol${formatAmount(totalInvestedThisYear)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Net Savings",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                )
                                val netSavings = totalIncomeThisYear - totalSpentThisYear
                                Text(
                                    text = "${if (netSavings >= 0) "+" else ""}$currencySymbol${formatAmount(netSavings)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (netSavings >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "RECENT ACTIVITY",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }

            items(transactions.take(5)) { transaction ->
                MiniTransactionRow(transaction = transaction, currencySymbol = currencySymbol)
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    amount: Double,
    currencySymbol: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$currencySymbol${formatAmount(amount)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CategoryRow(
    category: TransactionCategory,
    amount: Double,
    total: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val percentage = if (total > 0) (amount / total * 100).toInt() else 0
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(getCategoryColor(category).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(category),
                            contentDescription = null,
                            tint = getCategoryColor(category),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = formatCategoryName(category),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$currencySymbol${formatAmount(amount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$percentage%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { (percentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = getCategoryColor(category),
                trackColor = getCategoryColor(category).copy(alpha = 0.15f)
            )
        }
    }
}

@Composable
private fun MiniTransactionRow(
    transaction: Transaction,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchant ?: transaction.summary.take(30),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatCategoryName(transaction.category),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            val amountColor = if (transaction.type == TransactionType.CREDIT) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
            val prefix = if (transaction.type == TransactionType.CREDIT) "+" else "-"
            
            Text(
                text = "$prefix$currencySymbol${formatAmount(transaction.amount)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = amountColor
            )
        }
    }
}

private fun getCategoryIcon(category: TransactionCategory): ImageVector {
    return when (category) {
        TransactionCategory.FOOD -> Icons.Default.Restaurant
        TransactionCategory.ENTERTAINMENT -> Icons.Default.Movie
        TransactionCategory.EMI_HOME_LOAN -> Icons.Default.Home
        TransactionCategory.EMI_CAR_LOAN -> Icons.Default.DirectionsCar
        TransactionCategory.UTILITIES -> Icons.Default.Bolt
        TransactionCategory.SHOPPING -> Icons.Default.ShoppingBag
        TransactionCategory.INVESTMENT -> Icons.Default.TrendingUp
        TransactionCategory.SALARY -> Icons.Default.AccountBalance
        TransactionCategory.TRANSFER -> Icons.Default.SwapHoriz
        TransactionCategory.HEALTH -> Icons.Default.LocalHospital
        TransactionCategory.TRAVEL -> Icons.Default.Flight
        TransactionCategory.EDUCATION -> Icons.Default.School
        TransactionCategory.INSURANCE -> Icons.Default.Security
        TransactionCategory.SUBSCRIPTIONS -> Icons.Default.Subscriptions
        TransactionCategory.GROCERIES -> Icons.Default.ShoppingCart
        TransactionCategory.FUEL -> Icons.Default.LocalGasStation
        TransactionCategory.RENT -> Icons.Default.Apartment
        TransactionCategory.PERSONAL_CARE -> Icons.Default.Spa
        TransactionCategory.GIFTS -> Icons.Default.CardGiftcard
        TransactionCategory.OTHER -> Icons.Default.MoreHoriz
    }
}

private fun getCategoryColor(category: TransactionCategory): Color {
    return when (category) {
        TransactionCategory.FOOD -> Color(0xFFFF6B6B)
        TransactionCategory.ENTERTAINMENT -> Color(0xFF845EF7)
        TransactionCategory.EMI_HOME_LOAN -> Color(0xFF339AF0)
        TransactionCategory.EMI_CAR_LOAN -> Color(0xFF20C997)
        TransactionCategory.UTILITIES -> Color(0xFFFCC419)
        TransactionCategory.SHOPPING -> Color(0xFFFF922B)
        TransactionCategory.INVESTMENT -> Color(0xFF51CF66)
        TransactionCategory.SALARY -> Color(0xFF22B8CF)
        TransactionCategory.TRANSFER -> Color(0xFF748FFC)
        TransactionCategory.HEALTH -> Color(0xFFFA5252)
        TransactionCategory.TRAVEL -> Color(0xFF15AABF)
        TransactionCategory.EDUCATION -> Color(0xFF7950F2)
        TransactionCategory.INSURANCE -> Color(0xFF40C057)
        TransactionCategory.SUBSCRIPTIONS -> Color(0xFFE64980)
        TransactionCategory.GROCERIES -> Color(0xFF82C91E)
        TransactionCategory.FUEL -> Color(0xFFFD7E14)
        TransactionCategory.RENT -> Color(0xFF4C6EF5)
        TransactionCategory.PERSONAL_CARE -> Color(0xFFBE4BDB)
        TransactionCategory.GIFTS -> Color(0xFFF783AC)
        TransactionCategory.OTHER -> Color(0xFF868E96)
    }
}

private fun formatCategoryName(category: TransactionCategory): String {
    return category.name
        .replace("_", " ")
        .lowercase()
        .split(" ")
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}

private fun formatAmount(amount: Double): String {
    return String.format(Locale.getDefault(), "%,.0f", amount)
}

private fun getCurrentMonthYear(): String {
    val formatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    return formatter.format(Date())
}
