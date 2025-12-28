package com.explainmymoney.ui.screens.investments

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
import com.explainmymoney.domain.model.InvestmentType
import com.explainmymoney.domain.model.Transaction
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentsScreen(
    investmentTransactions: List<Transaction>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val totalInvested = investmentTransactions.sumOf { it.amount }
    
    val investmentsByType = investmentTransactions.groupBy { it.investmentType ?: InvestmentType.OTHER }
        .mapValues { (_, txs) -> txs.sumOf { it.amount } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Investments",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Track your wealth building",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "TOTAL INVESTED",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$currencySymbol${formatAmount(totalInvested)}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${investmentTransactions.size} investment transactions tracked",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            item {
                Text(
                    text = "INVESTMENT BREAKDOWN",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }

            if (investmentsByType.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.TrendingUp,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No investments tracked yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Investment transactions from your SMS will appear here",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(investmentsByType.entries.sortedByDescending { it.value }.toList()) { (type, amount) ->
                    InvestmentTypeCard(
                        type = type,
                        amount = amount,
                        total = totalInvested,
                        count = investmentTransactions.count { it.investmentType == type },
                        currencySymbol = currencySymbol
                    )
                }
            }

            if (investmentTransactions.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "RECENT INVESTMENTS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(investmentTransactions.take(10)) { investment ->
                    InvestmentRow(investment = investment, currencySymbol = currencySymbol)
                }
            }
        }
    }
}

@Composable
private fun InvestmentTypeCard(
    type: InvestmentType,
    amount: Double,
    total: Double,
    count: Int,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val percentage = if (total > 0) (amount / total * 100).toInt() else 0
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(getInvestmentColor(type).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getInvestmentIcon(type),
                    contentDescription = null,
                    tint = getInvestmentColor(type),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatInvestmentType(type),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$count transactions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$currencySymbol${formatAmount(amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "$percentage% of total",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InvestmentRow(
    investment: Transaction,
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
                    text = investment.merchant ?: formatInvestmentType(investment.investmentType ?: InvestmentType.OTHER),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatDate(investment.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = "$currencySymbol${formatAmount(investment.amount)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun getInvestmentIcon(type: InvestmentType): ImageVector {
    return when (type) {
        InvestmentType.SIP -> Icons.Default.Autorenew
        InvestmentType.MUTUAL_FUND -> Icons.Default.PieChart
        InvestmentType.STOCKS -> Icons.Default.ShowChart
        InvestmentType.PPF -> Icons.Default.Savings
        InvestmentType.NPS -> Icons.Default.AccountBalance
        InvestmentType.BONDS -> Icons.Default.Description
        InvestmentType.OTHER -> Icons.Default.TrendingUp
    }
}

private fun getInvestmentColor(type: InvestmentType): Color {
    return when (type) {
        InvestmentType.SIP -> Color(0xFF4ECDC4)
        InvestmentType.MUTUAL_FUND -> Color(0xFF845EF7)
        InvestmentType.STOCKS -> Color(0xFF51CF66)
        InvestmentType.PPF -> Color(0xFF339AF0)
        InvestmentType.NPS -> Color(0xFFFCC419)
        InvestmentType.BONDS -> Color(0xFFFF922B)
        InvestmentType.OTHER -> Color(0xFF868E96)
    }
}

private fun formatInvestmentType(type: InvestmentType): String {
    return when (type) {
        InvestmentType.SIP -> "SIP"
        InvestmentType.MUTUAL_FUND -> "Mutual Funds"
        InvestmentType.STOCKS -> "Stocks"
        InvestmentType.PPF -> "PPF"
        InvestmentType.NPS -> "NPS"
        InvestmentType.BONDS -> "Bonds"
        InvestmentType.OTHER -> "Other"
    }
}

private fun formatAmount(amount: Double): String {
    return String.format(Locale.getDefault(), "%,.0f", amount)
}

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
