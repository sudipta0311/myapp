package com.explainmymoney.ui.screens.`import`

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.explainmymoney.domain.model.Transaction
import com.explainmymoney.domain.model.TransactionCategory
import com.explainmymoney.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatementPreviewScreen(
    fileName: String,
    transactions: List<Transaction>,
    currencySymbol: String,
    isLoading: Boolean,
    onConfirmImport: (List<Transaction>) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalDebits = transactions.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
    val totalCredits = transactions.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }
    
    var selectedTransactions by remember { mutableStateOf(transactions.map { it.id to true }.toMap()) }
    
    val selectedCount = selectedTransactions.count { it.value }
    val selectedForImport = transactions.filter { selectedTransactions[it.id] == true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Import Preview",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = fileName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onConfirmImport(selectedForImport) },
                        enabled = selectedCount > 0 && !isLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import $selectedCount")
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "No transactions found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "The file could not be parsed or contains no valid transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "SUMMARY",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "${transactions.size} transactions",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "$selectedCount selected for import",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "$currencySymbol${formatAmount(totalDebits)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.ArrowDownward,
                                            contentDescription = null,
                                            tint = Color(0xFF4CAF50),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "$currencySymbol${formatAmount(totalCredits)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TRANSACTIONS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = {
                                val allSelected = selectedTransactions.all { it.value }
                                selectedTransactions = transactions.associate { it.id to !allSelected }
                            }
                        ) {
                            Text(
                                if (selectedTransactions.all { it.value }) "Deselect All" else "Select All"
                            )
                        }
                    }
                }

                items(transactions, key = { it.id }) { transaction ->
                    PreviewTransactionCard(
                        transaction = transaction,
                        currencySymbol = currencySymbol,
                        isSelected = selectedTransactions[transaction.id] == true,
                        onToggleSelection = {
                            selectedTransactions = selectedTransactions.toMutableMap().apply {
                                this[transaction.id] = !(this[transaction.id] ?: true)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewTransactionCard(
    transaction: Transaction,
    currencySymbol: String,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.surface 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() }
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(getCategoryColor(transaction.category).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (transaction.type == TransactionType.CREDIT) 
                        Icons.Default.ArrowDownward 
                    else 
                        Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = if (transaction.type == TransactionType.CREDIT) 
                        Color(0xFF4CAF50) 
                    else 
                        getCategoryColor(transaction.category),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchant ?: transaction.rawMessage.take(30),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateFormatter.format(Date(transaction.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCategory(transaction.category),
                        style = MaterialTheme.typography.labelSmall,
                        color = getCategoryColor(transaction.category)
                    )
                }
            }
            
            val amountColor = if (transaction.type == TransactionType.CREDIT) 
                Color(0xFF4CAF50) 
            else 
                MaterialTheme.colorScheme.onSurface
            val prefix = if (transaction.type == TransactionType.CREDIT) "+" else "-"
            
            Text(
                text = "$prefix$currencySymbol${formatAmount(transaction.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = amountColor
            )
        }
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

private fun formatAmount(amount: Double): String {
    return String.format(Locale.getDefault(), "%,.0f", amount)
}
