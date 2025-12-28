package com.explainmymoney.domain.slm

import android.content.Context
import com.explainmymoney.domain.model.Transaction
import com.explainmymoney.domain.model.TransactionCategory
import com.explainmymoney.domain.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class SlmInference(
    private val context: Context,
    private val modelPath: String
) {
    private var isInitialized = false
    
    suspend fun initialize() = withContext(Dispatchers.Default) {
        isInitialized = true
    }
    
    suspend fun generateResponse(
        query: String,
        transactions: List<Transaction>,
        currencySymbol: String
    ): String = withContext(Dispatchers.Default) {
        if (!isInitialized) {
            return@withContext "Model not initialized"
        }
        
        val context = buildTransactionContext(transactions, currencySymbol)
        processWithSlm(query, context, transactions, currencySymbol)
    }
    
    private fun buildTransactionContext(transactions: List<Transaction>, currencySymbol: String): String {
        if (transactions.isEmpty()) {
            return "No transactions available."
        }
        
        val totalSpent = transactions.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
        val totalIncome = transactions.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }
        val transactionCount = transactions.size
        
        val categoryBreakdown = transactions
            .filter { it.type == TransactionType.DEBIT }
            .groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
        
        val categoryText = categoryBreakdown.take(5).joinToString("\n") { (cat, amount) ->
            "- ${formatCategory(cat)}: $currencySymbol${formatAmount(amount)}"
        }
        
        return """
            Transaction Summary:
            - Total transactions: $transactionCount
            - Total spent: $currencySymbol${formatAmount(totalSpent)}
            - Total income: $currencySymbol${formatAmount(totalIncome)}
            - Net: $currencySymbol${formatAmount(totalIncome - totalSpent)}
            
            Top spending categories:
            $categoryText
        """.trimIndent()
    }
    
    private fun processWithSlm(
        query: String,
        transactionContext: String,
        transactions: List<Transaction>,
        currencySymbol: String
    ): String {
        val lowerQuery = query.lowercase()
        
        return when {
            containsAny(lowerQuery, listOf("how", "why", "what if", "should", "could", "would", "explain")) -> {
                generateExplanatoryResponse(query, transactions, currencySymbol)
            }
            containsAny(lowerQuery, listOf("compare", "versus", "vs", "difference", "between")) -> {
                generateComparisonResponse(query, transactions, currencySymbol)
            }
            containsAny(lowerQuery, listOf("trend", "pattern", "habit", "behavior", "usually")) -> {
                generatePatternResponse(transactions, currencySymbol)
            }
            containsAny(lowerQuery, listOf("advice", "suggest", "recommend", "tip", "help me")) -> {
                generateInsightResponse(transactions, currencySymbol)
            }
            containsAny(lowerQuery, listOf("summarize", "summary", "overview", "tell me about")) -> {
                generateSummaryResponse(transactions, currencySymbol)
            }
            else -> {
                generateGeneralResponse(query, transactions, currencySymbol)
            }
        }
    }
    
    private fun generateExplanatoryResponse(
        query: String,
        transactions: List<Transaction>,
        currencySymbol: String
    ): String {
        val lowerQuery = query.lowercase()
        
        if (transactions.isEmpty()) {
            return "I don't have any transaction data to analyze yet. Try scanning your SMS messages or importing a bank statement first."
        }
        
        val debits = transactions.filter { it.type == TransactionType.DEBIT }
        val totalSpent = debits.sumOf { it.amount }
        
        if (lowerQuery.contains("spending") || lowerQuery.contains("spent")) {
            val byCategory = debits.groupBy { it.category }
                .mapValues { (_, txs) -> txs.sumOf { it.amount } }
                .toList()
                .sortedByDescending { it.second }
            
            val topCategory = byCategory.firstOrNull()
            val explanation = if (topCategory != null) {
                val percentage = (topCategory.second / totalSpent * 100).toInt()
                """
                    Looking at your spending, I can see that ${formatCategory(topCategory.first)} is your biggest expense category, accounting for $percentage% of your total spending ($currencySymbol${formatAmount(topCategory.second)}).
                    
                    Here's why this might be:
                    ${getSpendingExplanation(topCategory.first)}
                    
                    Your total spending across ${debits.size} transactions is $currencySymbol${formatAmount(totalSpent)}.
                """.trimIndent()
            } else {
                "You haven't had any spending transactions yet."
            }
            return explanation
        }
        
        return generateGeneralResponse(query, transactions, currencySymbol)
    }
    
    private fun getSpendingExplanation(category: TransactionCategory): String {
        return when (category) {
            TransactionCategory.FOOD -> "Food expenses often add up quickly with daily meals, groceries, and occasional dining out."
            TransactionCategory.SHOPPING -> "Shopping can include both essentials and discretionary purchases. Consider tracking what you buy most."
            TransactionCategory.ENTERTAINMENT -> "Entertainment spending reflects your leisure activities. These are often areas where small reductions can add up."
            TransactionCategory.EMI_HOME_LOAN -> "Your home loan EMI is a fixed recurring expense. This is typically a planned expense for building equity."
            TransactionCategory.EMI_CAR_LOAN -> "Car loan EMIs are fixed commitments. Ensure this aligns with your transportation needs."
            TransactionCategory.UTILITIES -> "Utilities are essential expenses. Look for energy-saving opportunities to optimize these costs."
            TransactionCategory.INVESTMENT -> "Investment transactions are positive for wealth building. Keep tracking your portfolio growth."
            else -> "This category represents various transactions that may need closer review."
        }
    }
    
    private fun generateComparisonResponse(
        query: String,
        transactions: List<Transaction>,
        currencySymbol: String
    ): String {
        val debits = transactions.filter { it.type == TransactionType.DEBIT }
        val byCategory = debits.groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
        
        if (byCategory.size < 2) {
            return "I need more transaction categories to make a comparison. Keep tracking your expenses!"
        }
        
        val top2 = byCategory.take(2)
        val diff = top2[0].second - top2[1].second
        
        return """
            Comparing your top spending categories:
            
            ${formatCategory(top2[0].first)}: $currencySymbol${formatAmount(top2[0].second)}
            ${formatCategory(top2[1].first)}: $currencySymbol${formatAmount(top2[1].second)}
            
            You're spending $currencySymbol${formatAmount(diff)} more on ${formatCategory(top2[0].first)} than ${formatCategory(top2[1].first)}.
            
            This difference represents ${((diff / top2[1].second) * 100).toInt()}% more spending in your top category.
        """.trimIndent()
    }
    
    private fun generatePatternResponse(
        transactions: List<Transaction>,
        currencySymbol: String
    ): String {
        if (transactions.size < 5) {
            return "I need more transactions to identify patterns. Keep tracking your expenses for better insights!"
        }
        
        val debits = transactions.filter { it.type == TransactionType.DEBIT }
        val avgTransaction = debits.sumOf { it.amount } / debits.size
        val largeTransactions = debits.filter { it.amount > avgTransaction * 2 }
        
        val mostCommonCategory = debits.groupBy { it.category }
            .maxByOrNull { it.value.size }
            ?.key
        
        return """
            Based on your transaction patterns:
            
            Your most frequent spending category is ${mostCommonCategory?.let { formatCategory(it) } ?: "unknown"}, which appears in ${debits.count { it.category == mostCommonCategory }} of your ${debits.size} transactions.
            
            Average transaction size: $currencySymbol${formatAmount(avgTransaction)}
            
            You have ${largeTransactions.size} transactions that are significantly larger than average (over $currencySymbol${formatAmount(avgTransaction * 2)}).
            
            This suggests your spending follows a pattern of regular smaller purchases with occasional larger expenses.
        """.trimIndent()
    }
    
    private fun generateInsightResponse(
        transactions: List<Transaction>,
        currencySymbol: String
    ): String {
        val debits = transactions.filter { it.type == TransactionType.DEBIT }
        val credits = transactions.filter { it.type == TransactionType.CREDIT }
        
        val totalSpent = debits.sumOf { it.amount }
        val totalIncome = credits.sumOf { it.amount }
        val savingsRate = if (totalIncome > 0) ((totalIncome - totalSpent) / totalIncome * 100).toInt() else 0
        
        val insights = mutableListOf<String>()
        
        if (savingsRate < 10) {
            insights.add("Your savings rate is currently at $savingsRate%. Consider aiming for at least 20% savings.")
        } else if (savingsRate >= 20) {
            insights.add("Great job! Your savings rate of $savingsRate% is healthy.")
        }
        
        val byCategory = debits.groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
        
        val discretionary = listOf(TransactionCategory.ENTERTAINMENT, TransactionCategory.SHOPPING)
        val discretionarySpend = discretionary.sumOf { byCategory[it] ?: 0.0 }
        val discretionaryPercent = if (totalSpent > 0) (discretionarySpend / totalSpent * 100).toInt() else 0
        
        if (discretionaryPercent > 30) {
            insights.add("Discretionary spending (entertainment + shopping) is $discretionaryPercent% of your expenses. Consider if all purchases are necessary.")
        }
        
        val investmentSpend = byCategory[TransactionCategory.INVESTMENT] ?: 0.0
        if (investmentSpend > 0) {
            insights.add("You're investing $currencySymbol${formatAmount(investmentSpend)} which is great for long-term wealth building.")
        } else {
            insights.add("Consider setting aside some amount for investments if possible.")
        }
        
        return """
            Here are some observations based on your transactions:
            
            ${insights.joinToString("\n\n")}
            
            Remember: This is based purely on your transaction data. For personalized financial advice, consult a qualified financial advisor.
        """.trimIndent()
    }
    
    private fun generateSummaryResponse(
        transactions: List<Transaction>,
        currencySymbol: String
    ): String {
        if (transactions.isEmpty()) {
            return "No transactions to summarize. Start by scanning SMS or importing a statement."
        }
        
        val debits = transactions.filter { it.type == TransactionType.DEBIT }
        val credits = transactions.filter { it.type == TransactionType.CREDIT }
        
        val totalSpent = debits.sumOf { it.amount }
        val totalIncome = credits.sumOf { it.amount }
        
        val topCategories = debits.groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(3)
            .joinToString("\n") { (cat, amount) -> 
                "  ${formatCategory(cat)}: $currencySymbol${formatAmount(amount)}" 
            }
        
        return """
            Here's your financial summary:
            
            Total Income: $currencySymbol${formatAmount(totalIncome)} (${credits.size} transactions)
            Total Spending: $currencySymbol${formatAmount(totalSpent)} (${debits.size} transactions)
            Net Flow: $currencySymbol${formatAmount(totalIncome - totalSpent)}
            
            Top spending categories:
            $topCategories
            
            Total transactions analyzed: ${transactions.size}
        """.trimIndent()
    }
    
    private fun generateGeneralResponse(
        query: String,
        transactions: List<Transaction>,
        currencySymbol: String
    ): String {
        val context = buildTransactionContext(transactions, currencySymbol)
        
        return """
            Based on your question about "${query.take(50)}":
            
            $context
            
            Feel free to ask more specific questions like:
            - "Explain my spending patterns"
            - "Compare my top categories"
            - "Give me a summary of my finances"
            - "What patterns do you see in my transactions?"
        """.trimIndent()
    }
    
    private fun containsAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it) }
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
    
    fun close() {
        isInitialized = false
    }
}
