package com.explainmymoney.ui.screens.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Categories", "Trends", "Forecast")
    
    // Month/Year filter state
    val calendar = Calendar.getInstance()
    var selectedMonth by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) }
    var selectedYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    var showMonthPicker by remember { mutableStateOf(false) }
    
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val years = (2020..calendar.get(Calendar.YEAR)).toList()
    
    // Filter transactions by selected month/year
    val filteredTransactions = remember(transactions, selectedMonth, selectedYear) {
        transactions.filter { tx ->
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            txCal.get(Calendar.MONTH) == selectedMonth && txCal.get(Calendar.YEAR) == selectedYear
        }
    }
    
    // Recalculate totals for filtered transactions
    val filteredTotalSpent = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
    }
    val filteredTotalIncome = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }
    }
    val filteredCategoryBreakdown = remember(filteredTransactions) {
        filteredTransactions
            .filter { it.type == TransactionType.DEBIT }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }
    
    val dailyAverage = if (calendar.get(Calendar.DAY_OF_MONTH) > 0) {
        filteredTotalSpent / calendar.get(Calendar.DAY_OF_MONTH)
    } else 0.0

    val daysRemaining = calendar.getActualMaximum(Calendar.DAY_OF_MONTH) - 
                       calendar.get(Calendar.DAY_OF_MONTH)
    val projectedMonthlySpend = filteredTotalSpent + (dailyAverage * daysRemaining)
    
    // EMI calculation (recurring debits)
    val emiTotal = remember(filteredTransactions) {
        filteredTransactions
            .filter { it.category == TransactionCategory.EMI_HOME_LOAN || 
                     it.category == TransactionCategory.EMI_CAR_LOAN ||
                     it.category == TransactionCategory.RENT }
            .sumOf { it.amount }
    }

    LaunchedEffect(Unit) {
        onRefresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Spending Analytics",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Track your spending patterns and trends",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Month/Year Filter
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { showMonthPicker = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${months[selectedMonth]} $selectedYear",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Select month"
                    )
                }
            }
            
            // Summary Cards Row
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryCard(
                            title = "Total Spend",
                            amount = filteredTotalSpent,
                            currencySymbol = currencySymbol,
                            subtitle = "Last 7 days avg: ${currencySymbol}${formatAmount(dailyAverage)}/day",
                            subtitleColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCard(
                            title = "EMI Obligations",
                            amount = emiTotal,
                            currencySymbol = currencySymbol,
                            subtitle = "Monthly recurring",
                            subtitleColor = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Investment Total",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$currencySymbol${formatAmount(totalInvestedThisYear)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "SIP + Stock purchases tracked",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Tab Row
                item {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title) }
                            )
                        }
                    }
                }
                
                // Tab Content
                when (selectedTab) {
                    0 -> { // Categories Tab
                        item {
                            if (filteredCategoryBreakdown.isEmpty()) {
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
                                            text = "No spending data for ${months[selectedMonth]} $selectedYear",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                // Pie Chart
                                PieChart(
                                    categoryBreakdown = filteredCategoryBreakdown,
                                    totalSpent = filteredTotalSpent,
                                    currencySymbol = currencySymbol,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(250.dp)
                                )
                            }
                        }
                        
                        // Category list
                        items(filteredCategoryBreakdown.entries.sortedByDescending { it.value }.toList()) { (category, amount) ->
                            CategoryRow(
                                category = category,
                                amount = amount,
                                total = filteredTotalSpent,
                                currencySymbol = currencySymbol
                            )
                        }
                    }
                    1 -> { // Trends Tab
                        item {
                            TrendsContent(
                                transactions = filteredTransactions,
                                currencySymbol = currencySymbol,
                                dailyAverage = dailyAverage
                            )
                        }
                    }
                    2 -> { // Forecast Tab
                        item {
                            ForecastContent(
                                projectedMonthlySpend = projectedMonthlySpend,
                                dailyAverage = dailyAverage,
                                daysRemaining = daysRemaining,
                                currencySymbol = currencySymbol
                            )
                        }
                    }
                }
            }
        }
        
        // Month Picker Dialog
        if (showMonthPicker) {
            AlertDialog(
                onDismissRequest = { showMonthPicker = false },
                title = { Text("Select Month & Year") },
                text = {
                    Column {
                        Text("Year", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            years.takeLast(5).forEach { year ->
                                FilterChip(
                                    selected = selectedYear == year,
                                    onClick = { selectedYear = year },
                                    label = { Text(year.toString()) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Month", style = MaterialTheme.typography.labelMedium)
                        Column {
                            (0..11 step 4).forEach { startIndex ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    (startIndex until minOf(startIndex + 4, 12)).forEach { monthIndex ->
                                        FilterChip(
                                            selected = selectedMonth == monthIndex,
                                            onClick = { selectedMonth = monthIndex },
                                            label = { Text(months[monthIndex]) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showMonthPicker = false }) {
                        Text("Done")
                    }
                }
            )
        }
    }
}

@Composable
private fun PieChart(
    categoryBreakdown: Map<TransactionCategory, Double>,
    totalSpent: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val sortedCategories = categoryBreakdown.entries.sortedByDescending { it.value }
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pie Chart Canvas
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    var startAngle = -90f
                    val strokeWidth = 35f
                    
                    sortedCategories.forEach { (category, amount) ->
                        val sweepAngle = if (totalSpent > 0) (amount / totalSpent * 360f).toFloat() else 0f
                        drawArc(
                            color = getCategoryColor(category),
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = strokeWidth),
                            size = Size(size.width - strokeWidth, size.height - strokeWidth),
                            topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                        )
                        startAngle += sweepAngle
                    }
                }
                
                // Center text
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$currencySymbol${formatAmount(totalSpent)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Legend
            Column(
                modifier = Modifier.weight(1f).padding(start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                sortedCategories.take(5).forEach { (category, amount) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(getCategoryColor(category))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatCategoryName(category),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${if (totalSpent > 0) (amount / totalSpent * 100).toInt() else 0}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                if (sortedCategories.size > 5) {
                    Text(
                        text = "+${sortedCategories.size - 5} more",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendsContent(
    transactions: List<Transaction>,
    currencySymbol: String,
    dailyAverage: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Daily Spending Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Past 30 days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Simple bar representation
            val last30Days = transactions
                .filter { it.type == TransactionType.DEBIT }
                .groupBy { 
                    val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                    cal.get(Calendar.DAY_OF_MONTH)
                }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
            
            if (last30Days.isEmpty()) {
                Text(
                    text = "No spending data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            } else {
                val maxSpend = last30Days.values.maxOrNull() ?: 1.0
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    (1..30).forEach { day ->
                        val spend = last30Days[day] ?: 0.0
                        val height = if (maxSpend > 0) (spend / maxSpend * 80).toFloat() else 0f
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(height.dp.coerceAtLeast(2.dp))
                                .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                .background(
                                    if (spend > dailyAverage * 1.5) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary
                                )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1", style = MaterialTheme.typography.labelSmall)
                    Text("Daily average: $currencySymbol${formatAmount(dailyAverage)}", 
                         style = MaterialTheme.typography.labelSmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("30", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun ForecastContent(
    projectedMonthlySpend: Double,
    dailyAverage: Double,
    daysRemaining: Int,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "These are scenario-based estimates showing \"if-this-continues\" ranges based on your past spending. Not predictions or recommendations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "How This Works",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "We take your spending from the past few weeks and extrapolate forward. The range (conservative to higher) accounts for natural variation in your spending patterns.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Projection cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Conservative", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "$currencySymbol${formatAmount(projectedMonthlySpend * 0.85)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                    
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Expected", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "$currencySymbol${formatAmount(projectedMonthlySpend)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Higher", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "$currencySymbol${formatAmount(projectedMonthlySpend * 1.15)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "$daysRemaining days remaining • Daily avg: $currencySymbol${formatAmount(dailyAverage)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    amount: Double,
    currencySymbol: String,
    subtitle: String,
    subtitleColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$currencySymbol${formatAmount(amount)}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = subtitleColor
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
                progress = (percentage / 100f).coerceIn(0f, 1f),
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
