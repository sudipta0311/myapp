package com.explainmymoney.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.explainmymoney.domain.model.Transaction
import com.explainmymoney.domain.model.TransactionCategory
import com.explainmymoney.domain.model.TransactionType
import kotlinx.coroutines.launch
import java.util.*

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    transactions: List<Transaction>,
    currencySymbol: String,
    isSlmEnabled: Boolean = false,
    onSlmQuery: suspend (String) -> String = { "" },
    onLocalQuery: (String) -> String = { "" },
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    
    val welcomeMessage = if (isSlmEnabled) {
        "Hi! I'm your AI financial assistant running locally on your device. I can have natural conversations about your spending. Ask me anything like:\n\n• Explain my spending patterns\n• Why did I spend so much on food?\n• Compare my top categories\n• Give me a financial summary"
    } else {
        "Hi! I'm your local financial assistant. I can help you understand your spending. Try asking:\n\n• How much did I spend on food?\n• What's my biggest expense?\n• Show my recent transactions\n• How much did I invest this month?"
    }
    
    var messages by remember { 
        mutableStateOf(
            listOf(
                ChatMessage(
                    content = welcomeMessage,
                    isUser = false
                )
            )
        )
    }
    var isTyping by remember { mutableStateOf(false) }

    fun processQuery(query: String) {
        scope.launch {
            isTyping = true
            kotlinx.coroutines.delay(300)
            
            val response = if (isSlmEnabled) {
                val slmResponse = onSlmQuery(query)
                slmResponse.ifEmpty { 
                    onLocalQuery(query).ifEmpty {
                        generateLocalResponse(query, transactions, currencySymbol)
                    }
                }
            } else {
                val localResponse = onLocalQuery(query)
                localResponse.ifEmpty {
                    generateLocalResponse(query, transactions, currencySymbol)
                }
            }
            
            messages = messages + ChatMessage(
                content = response,
                isUser = false
            )
            isTyping = false
        }
    }

    fun sendMessage() {
        val text = inputText.trim()
        if (text.isEmpty()) return
        messages = messages + ChatMessage(content = text, isUser = true)
        inputText = ""
        processQuery(text)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSlmEnabled) MaterialTheme.colorScheme.tertiary 
                                    else MaterialTheme.colorScheme.primary
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isSlmEnabled) Icons.Default.Psychology else Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = if (isSlmEnabled) MaterialTheme.colorScheme.onTertiary 
                                       else MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isSlmEnabled) "AI Assistant" else "Money Assistant",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isSlmEnabled) "Powered by on-device AI" else "Runs locally on your device",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (isSlmEnabled) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ) {
                            Text("AI", style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
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
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    ChatBubble(message = message, isSlmEnabled = isSlmEnabled)
                }

                if (isTyping) {
                    item {
                        TypingIndicator(isSlmEnabled = isSlmEnabled)
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                if (isSlmEnabled) "Ask anything about your finances..." 
                                else "Ask about your spending...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    FilledIconButton(
                        onClick = { sendMessage() },
                        enabled = inputText.isNotBlank()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, isSlmEnabled: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSlmEnabled) MaterialTheme.colorScheme.tertiary 
                        else MaterialTheme.colorScheme.primary
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isSlmEnabled) Icons.Default.Psychology else Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = if (isSlmEnabled) MaterialTheme.colorScheme.onTertiary 
                           else MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = if (message.isUser) 16.dp else 4.dp,
                topEnd = if (message.isUser) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            color = if (message.isUser) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = if (message.isUser) 
                    MaterialTheme.colorScheme.onPrimary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp)
            )
        }

        if (message.isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun TypingIndicator(isSlmEnabled: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    if (isSlmEnabled) MaterialTheme.colorScheme.tertiary 
                    else MaterialTheme.colorScheme.primary
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isSlmEnabled) Icons.Default.Psychology else Icons.Default.SmartToy,
                contentDescription = null,
                tint = if (isSlmEnabled) MaterialTheme.colorScheme.onTertiary 
                       else MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}

private fun generateLocalResponse(query: String, transactions: List<Transaction>, currencySymbol: String): String {
    val lowerQuery = query.lowercase()

    if (transactions.isEmpty()) {
        return "You don't have any transactions yet. Scan your SMS or import a bank statement to get started!"
    }

    return when {
        lowerQuery.contains("food") || lowerQuery.contains("restaurant") || lowerQuery.contains("eat") -> {
            val foodTxs = transactions.filter { it.category == TransactionCategory.FOOD }
            val total = foodTxs.sumOf { it.amount }
            if (foodTxs.isEmpty()) {
                "I don't see any food-related transactions in your history."
            } else {
                "You've spent $currencySymbol${formatAmount(total)} on food across ${foodTxs.size} transactions. " +
                "That's an average of $currencySymbol${formatAmount(total / foodTxs.size)} per transaction."
            }
        }

        lowerQuery.contains("biggest") || lowerQuery.contains("largest") || lowerQuery.contains("highest") -> {
            val biggest = transactions.filter { it.type == TransactionType.DEBIT }.maxByOrNull { it.amount }
            biggest?.let {
                "Your biggest expense was $currencySymbol${formatAmount(it.amount)} to ${it.merchant ?: "a merchant"} " +
                "in the ${formatCategory(it.category)} category."
            } ?: "I couldn't find any expenses to analyze."
        }

        lowerQuery.contains("invest") -> {
            val investments = transactions.filter { it.category == TransactionCategory.INVESTMENT }
            val total = investments.sumOf { it.amount }
            if (investments.isEmpty()) {
                "I don't see any investment transactions yet."
            } else {
                "You've invested $currencySymbol${formatAmount(total)} across ${investments.size} transactions. Keep building your wealth!"
            }
        }

        lowerQuery.contains("total") || lowerQuery.contains("spent") || lowerQuery.contains("spending") -> {
            val debits = transactions.filter { it.type == TransactionType.DEBIT }
            val total = debits.sumOf { it.amount }
            "Your total spending is $currencySymbol${formatAmount(total)} across ${debits.size} transactions."
        }

        lowerQuery.contains("income") || lowerQuery.contains("received") || lowerQuery.contains("earned") -> {
            val credits = transactions.filter { it.type == TransactionType.CREDIT }
            val total = credits.sumOf { it.amount }
            if (credits.isEmpty()) {
                "I don't see any income transactions recorded."
            } else {
                "You've received $currencySymbol${formatAmount(total)} in income from ${credits.size} transactions."
            }
        }

        lowerQuery.contains("recent") || lowerQuery.contains("last") || lowerQuery.contains("latest") -> {
            val recent = transactions.take(3)
            if (recent.isEmpty()) {
                "You don't have any transactions yet."
            } else {
                val summaries = recent.mapIndexed { i, tx ->
                    "${i + 1}. ${tx.summary}"
                }.joinToString("\n")
                "Here are your most recent transactions:\n\n$summaries"
            }
        }

        lowerQuery.contains("category") || lowerQuery.contains("breakdown") -> {
            val byCategory = transactions
                .filter { it.type == TransactionType.DEBIT }
                .groupBy { it.category }
                .mapValues { (_, txs) -> txs.sumOf { it.amount } }
                .toList()
                .sortedByDescending { it.second }
                .take(5)

            if (byCategory.isEmpty()) {
                "No spending categories to show yet."
            } else {
                val breakdown = byCategory.mapIndexed { i, (cat, amount) ->
                    "${i + 1}. ${formatCategory(cat)}: $currencySymbol${formatAmount(amount)}"
                }.joinToString("\n")
                "Your top spending categories:\n\n$breakdown"
            }
        }

        lowerQuery.contains("shopping") -> {
            val shopping = transactions.filter { it.category == TransactionCategory.SHOPPING }
            val total = shopping.sumOf { it.amount }
            if (shopping.isEmpty()) {
                "I don't see any shopping transactions."
            } else {
                "You've spent $currencySymbol${formatAmount(total)} on shopping across ${shopping.size} purchases."
            }
        }

        lowerQuery.contains("entertainment") || lowerQuery.contains("movie") || lowerQuery.contains("netflix") -> {
            val entertainment = transactions.filter { it.category == TransactionCategory.ENTERTAINMENT }
            val total = entertainment.sumOf { it.amount }
            if (entertainment.isEmpty()) {
                "No entertainment expenses found."
            } else {
                "You've spent $currencySymbol${formatAmount(total)} on entertainment."
            }
        }

        lowerQuery.contains("help") || lowerQuery.contains("what can you") -> {
            "I can help you understand your spending! Try asking:\n\n" +
            "• How much did I spend on food?\n" +
            "• What's my biggest expense?\n" +
            "• Show my recent transactions\n" +
            "• How much did I invest?\n" +
            "• Give me a category breakdown"
        }

        else -> {
            val total = transactions.sumOf { it.amount }
            val count = transactions.size
            "You have $count transactions totaling $currencySymbol${formatAmount(total)}. " +
            "Try asking about specific categories like food, shopping, or investments!"
        }
    }
}

private fun formatAmount(amount: Double): String {
    return String.format(Locale.getDefault(), "%,.0f", amount)
}

private fun formatCategory(category: TransactionCategory): String {
    return category.name
        .replace("_", " ")
        .lowercase()
        .split(" ")
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}
