package com.explainmymoney.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

@Composable
fun TransactionCard(
    transaction: Transaction,
    currencySymbol: String = "₹",
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showDetails by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = { showDetails = !showDetails }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(getCategoryColor(transaction.category).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(transaction.category),
                            contentDescription = null,
                            tint = getCategoryColor(transaction.category),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = transaction.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(2.dp))
                        
                        Text(
                            text = formatDate(transaction.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    val amountColor = if (transaction.type == TransactionType.CREDIT) 
                        Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                    val amountPrefix = if (transaction.type == TransactionType.CREDIT) "+" else "-"

                    Text(
                        text = "$amountPrefix$currencySymbol${formatAmount(transaction.amount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = amountColor
                    )

                    transaction.method?.let { method ->
                        Text(
                            text = method.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (showDetails) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow(label = "Category", value = formatCategory(transaction.category))
                    transaction.merchant?.let { DetailRow(label = "Merchant", value = it) }
                    transaction.referenceNo?.let { DetailRow(label = "Reference", value = it) }
                    transaction.balance?.let { 
                        DetailRow(label = "Balance After", value = "$currencySymbol${formatAmount(it)}") 
                    }
                    DetailRow(label = "Source", value = transaction.source.name)
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Original Message:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = transaction.rawMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    onDelete?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = it,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete Transaction")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
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

private fun formatCategory(category: TransactionCategory): String {
    return category.name
        .replace("_", " ")
        .lowercase()
        .split(" ")
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

private fun formatAmount(amount: Double): String {
    return String.format(Locale.getDefault(), "%,.2f", amount)
}
