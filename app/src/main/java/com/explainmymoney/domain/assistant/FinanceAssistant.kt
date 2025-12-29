package com.explainmymoney.domain.assistant

import com.explainmymoney.domain.model.Transaction
import com.explainmymoney.domain.model.TransactionCategory
import com.explainmymoney.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.*

sealed class QueryIntent {
    data class CategorySpending(val category: TransactionCategory, val timeRange: TimeRange) : QueryIntent()
    data class TotalSpending(val timeRange: TimeRange) : QueryIntent()
    data class TotalIncome(val timeRange: TimeRange) : QueryIntent()
    data class TotalInvestment(val timeRange: TimeRange) : QueryIntent()
    data class TopExpenses(val limit: Int, val timeRange: TimeRange) : QueryIntent()
    data class CategoryBreakdown(val timeRange: TimeRange) : QueryIntent()
    data class RecentTransactions(val limit: Int) : QueryIntent()
    data class CompareCategories(val timeRange: TimeRange) : QueryIntent()
    data class BiggestExpense(val timeRange: TimeRange) : QueryIntent()
    data class SearchMerchant(val merchant: String) : QueryIntent()
    object Summary : QueryIntent()
    object Help : QueryIntent()
    object Unknown : QueryIntent()
}

enum class TimeRange {
    TODAY, THIS_WEEK, THIS_MONTH, LAST_MONTH, THIS_YEAR, ALL_TIME
}

class FinanceAssistant {
    
    fun detectIntent(query: String): QueryIntent {
        val lowerQuery = query.lowercase().trim()
        
        val timeRange = detectTimeRange(lowerQuery)
        
        return when {
            // Help queries
            lowerQuery.contains("help") || lowerQuery.contains("what can you") || 
            lowerQuery.contains("how do") -> QueryIntent.Help

            // Category-specific spending
            detectCategory(lowerQuery) != null -> {
                val category = detectCategory(lowerQuery)!!
                QueryIntent.CategorySpending(category, timeRange)
            }

            // Investment queries
            lowerQuery.contains("invest") || lowerQuery.contains("sip") || 
            lowerQuery.contains("mutual fund") || lowerQuery.contains("stock") -> 
                QueryIntent.TotalInvestment(timeRange)

            // Income queries
            lowerQuery.contains("income") || lowerQuery.contains("earned") || 
            lowerQuery.contains("received") || lowerQuery.contains("salary") -> 
                QueryIntent.TotalIncome(timeRange)

            // Biggest expense
            lowerQuery.contains("biggest") || lowerQuery.contains("largest") || 
            lowerQuery.contains("highest") || lowerQuery.contains("most expensive") -> 
                QueryIntent.BiggestExpense(timeRange)

            // Top expenses
            lowerQuery.contains("top") && (lowerQuery.contains("expense") || lowerQuery.contains("spent")) -> 
                QueryIntent.TopExpenses(5, timeRange)

            // Category breakdown
            lowerQuery.contains("category") || lowerQuery.contains("breakdown") || 
            lowerQuery.contains("by category") || lowerQuery.contains("expenses by") -> 
                QueryIntent.CategoryBreakdown(timeRange)

            // Compare categories
            lowerQuery.contains("compare") -> QueryIntent.CompareCategories(timeRange)

            // Recent transactions
            lowerQuery.contains("recent") || lowerQuery.contains("last") || 
            lowerQuery.contains("latest") || lowerQuery.contains("show me") -> 
                QueryIntent.RecentTransactions(5)

            // Total spending
            lowerQuery.contains("total") || lowerQuery.contains("spent") || 
            lowerQuery.contains("spending") || lowerQuery.contains("how much") -> 
                QueryIntent.TotalSpending(timeRange)

            // Summary
            lowerQuery.contains("summary") || lowerQuery.contains("overview") -> 
                QueryIntent.Summary

            else -> QueryIntent.Unknown
        }
    }

    private fun detectTimeRange(query: String): TimeRange {
        return when {
            query.contains("today") -> TimeRange.TODAY
            query.contains("this week") || query.contains("week") -> TimeRange.THIS_WEEK
            query.contains("last month") -> TimeRange.LAST_MONTH
            query.contains("this year") || query.contains("year") -> TimeRange.THIS_YEAR
            query.contains("all time") || query.contains("ever") || query.contains("total") -> TimeRange.ALL_TIME
            else -> TimeRange.THIS_MONTH
        }
    }

    private fun detectCategory(query: String): TransactionCategory? {
        return when {
            query.contains("food") || query.contains("restaurant") || query.contains("eat") || 
            query.contains("dining") || query.contains("zomato") || query.contains("swiggy") -> 
                TransactionCategory.FOOD

            query.contains("entertainment") || query.contains("movie") || query.contains("netflix") || 
            query.contains("spotify") || query.contains("fun") -> 
                TransactionCategory.ENTERTAINMENT

            query.contains("shopping") || query.contains("amazon") || query.contains("flipkart") || 
            query.contains("myntra") || query.contains("bought") -> 
                TransactionCategory.SHOPPING

            query.contains("utility") || query.contains("utilities") || query.contains("electricity") || 
            query.contains("water") || query.contains("gas") || query.contains("bill") -> 
                TransactionCategory.UTILITIES

            query.contains("health") || query.contains("medical") || query.contains("doctor") || 
            query.contains("hospital") || query.contains("medicine") || query.contains("pharmacy") -> 
                TransactionCategory.HEALTH

            query.contains("travel") || query.contains("trip") || query.contains("flight") || 
            query.contains("hotel") || query.contains("uber") || query.contains("ola") -> 
                TransactionCategory.TRAVEL

            query.contains("education") || query.contains("school") || query.contains("college") || 
            query.contains("course") || query.contains("tuition") -> 
                TransactionCategory.EDUCATION

            query.contains("groceries") || query.contains("grocery") || query.contains("supermarket") || 
            query.contains("bigbasket") || query.contains("blinkit") -> 
                TransactionCategory.GROCERIES

            query.contains("fuel") || query.contains("petrol") || query.contains("diesel") || 
            query.contains("gas station") -> 
                TransactionCategory.FUEL

            query.contains("rent") || query.contains("lease") -> 
                TransactionCategory.RENT

            query.contains("subscription") || query.contains("subscriptions") -> 
                TransactionCategory.SUBSCRIPTIONS

            query.contains("insurance") -> TransactionCategory.INSURANCE

            query.contains("emi") || query.contains("loan") -> 
                if (query.contains("home") || query.contains("house")) 
                    TransactionCategory.EMI_HOME_LOAN 
                else 
                    TransactionCategory.EMI_CAR_LOAN

            query.contains("transfer") || query.contains("sent money") -> 
                TransactionCategory.TRANSFER

            else -> null
        }
    }

    fun generateResponse(
        intent: QueryIntent,
        transactions: List<Transaction>,
        currencySymbol: String
    ): String {
        if (transactions.isEmpty() && intent !is QueryIntent.Help) {
            return "You don't have any transactions yet. Scan your SMS or import a bank statement to get started!"
        }

        return when (intent) {
            is QueryIntent.CategorySpending -> handleCategorySpending(intent, transactions, currencySymbol)
            is QueryIntent.TotalSpending -> handleTotalSpending(intent, transactions, currencySymbol)
            is QueryIntent.TotalIncome -> handleTotalIncome(intent, transactions, currencySymbol)
            is QueryIntent.TotalInvestment -> handleTotalInvestment(intent, transactions, currencySymbol)
            is QueryIntent.TopExpenses -> handleTopExpenses(intent, transactions, currencySymbol)
            is QueryIntent.CategoryBreakdown -> handleCategoryBreakdown(intent, transactions, currencySymbol)
            is QueryIntent.RecentTransactions -> handleRecentTransactions(intent, transactions, currencySymbol)
            is QueryIntent.CompareCategories -> handleCompareCategories(intent, transactions, currencySymbol)
            is QueryIntent.BiggestExpense -> handleBiggestExpense(intent, transactions, currencySymbol)
            is QueryIntent.SearchMerchant -> handleSearchMerchant(intent, transactions, currencySymbol)
            is QueryIntent.Summary -> handleSummary(transactions, currencySymbol)
            is QueryIntent.Help -> getHelpText()
            is QueryIntent.Unknown -> handleUnknown(transactions, currencySymbol)
        }
    }

    private fun handleCategorySpending(
        intent: QueryIntent.CategorySpending,
        transactions: List<Transaction>,
        currencySymbol: String
    ): String {
        val filtered = filterByTimeRange(transactions, intent.timeRange)
            .filter { it.category == intent.category && it.type == TransactionType.DEBIT }
        
        val total = filtered.sumOf { it.amount }
        val count = filtered.size
        val categoryName = formatCategory(intent.category)
        val timeText = formatTimeRange(intent.timeRange)

        return if (filtered.isEmpty()) {
            "I don't see any $categoryName expenses $timeText."
        } else {
            val avg = total / count
            "You've spent $currencySymbol${formatAmount(total)} on $categoryName $timeText across $count transactions. That's an average of $currencySymbol${formatAmount(avg)} per transaction."
        }
    }

    private fun handleTotalSpending(
        intent: QueryIntent.TotalSpending,
        transactions: List<Transaction>,
        currencySymbol: String
    ): String {
        val filtered = filterByTimeRange(transactions, intent.timeRange)
            .filter { it.type == TransactionType.DEBIT }
        
        val total = filtered.sumOf { it.amount }
        val timeText = formatTimeRange(intent.timeRange)

        return "Your total spending $timeText is $currencySymbol${formatAmount(total)} across ${filtered.size} transactions."
    }

    private fun handleTotalIncome(
        intent: QueryIntent.TotalIncome,
        transactions: List<Transaction>,
        currencySymbol: String
    ): String {
        val filtered = filterByTimeRange(transactions, intent.timeRange)
            .filter { it.type == TransactionType.CREDIT }
        
        val total = filtered.sumOf { it.amount }
        val timeText = formatTimeRange(intent.timeRange)

        return if (filtered.isEmpty()) {
            "I don't see any income transactions $timeText."
        } else {
            "You've received $currencySymbol${formatAmount(total)} in income $timeText from ${filtered.size} transactions."
        }
    }

    private fun handleTotalInvestment(
        intent: QueryIntent.TotalInvestment,
        transactions: List<Transaction>,
        currencySymbol: String
    ): String {
        val filtered = filterByTimeRange(transactions, intent.timeRange)
            .filter { it.category == TransactionCategory.INVESTMENT }
        
        val total = filtered.sumOf { it.amount }
        val timeText = formatTimeRange(intent.timeRange)

        return if (filtered.isEmpty()) {
            "I don't see any investment transactions $timeText."
        } else {
            "You've invested $currencySymbol${formatAmount(total)} $timeText across ${filtered.size} transactions."
        }
    }

    private fun handleTopExpenses(
        intent: QueryIntent.TopExpenses,
        transactions: List<Transaction>,
        currencySymbol: String
    ): String {
        val filtered = filterByTimeRange(transactions, intent.timeRange)
            .filter { it.type == TransactionType.DEBIT }
            .sortedByDescending { it.amount }
            .take(intent.limit)

        val timeText = formatTimeRange(intent.timeRange)

        return if (filtered.isEmpty()) {
            "No expenses found $timeText."
        } else {
            val list = filtered.mapIndexed { i, tx ->
                "${i + 1}. ${tx.merchant ?: formatCategory(tx.category)}: $currencySymbol${formatAmount(tx.amount)}"
            }.joinToString("\n")
            "Your top ${filtered.size} expenses $timeText:\n\n$list"
        }
    }

    private fun handleCategoryBreakdown(
        intent: QueryIntent.CategoryBreakdown,
        transactions: List<Transaction>,
        currencySymbol: String
    ): String {
        val filtered = filterByTimeRange(transactions, intent.timeRange)
            .filter { it.type == TransactionType.DEBIT }
        
        val byCategory = filtered
            .groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(7)

        val timeText = formatTimeRange(intent.timeRange)
        val total = filtered.sumOf { it.amount }

        return if (byCategory.isEmpty()) {
            "No spending categories to show $timeText."
        } else {
            val breakdown = byCategory.mapIndexed { i, (cat, amount) ->
                val pct = if (total > 0) ((amount / total) * 100).toInt() else 0
                "${i + 1}. ${formatCategory(cat)}: $currencySymbol${formatAmount(amount)} ($pct%)"
            }.joinToString("\n")
            "Your spending breakdown $timeText:\n\n$breakdown"
        }
    }

    private fun handleRecentTransactions(
        intent: QueryIntent.RecentTransactions,
        transactions: List<Transaction>,
        currencySymbol: String
    ): String {
        val recent = transactions.take(intent.limit)

        return if (recent.isEmpty()) {
            "You don't have any transactions yet."
        } else {
            val list = recent.mapIndexed { i, tx ->
                val prefix = if (tx.type == TransactionType.CREDIT) "+" else "-"
                "${i + 1}. ${tx.merchant ?: tx.summary.take(25)}: $prefix$currencySymbol${formatAmount(tx.amount)}"
            }.joinToString("\n")
            "Your most recent transactions:\n\n$list"
        }
    }

    private fun handleCompareCategories(
        intent: QueryIntent.CompareCategories,
        transactions: List<Transaction>,
        currencySymbol: String
    ): String {
        return handleCategoryBreakdown(
            QueryIntent.CategoryBreakdown(intent.timeRange),
            transactions,
            currencySymbol
        )
    }

    private fun handleBiggestExpense(
        intent: QueryIntent.BiggestExpense,
        transactions: List<Transaction>,
        currencySymbol: String
    ): String {
        val filtered = filterByTimeRange(transactions, intent.timeRange)
            .filter { it.type == TransactionType.DEBIT }
        
        val biggest = filtered.maxByOrNull { it.amount }
        val timeText = formatTimeRange(intent.timeRange)

        return biggest?.let {
            val date = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(it.timestamp))
            "Your biggest expense $timeText was $currencySymbol${formatAmount(it.amount)} to ${it.merchant ?: "a merchant"} on $date (${formatCategory(it.category)})."
        } ?: "I couldn't find any expenses $timeText."
    }

    private fun handleSearchMerchant(
        intent: QueryIntent.SearchMerchant,
        transactions: List<Transaction>,
        currencySymbol: String
    ): String {
        val matched = transactions.filter { 
            it.merchant?.contains(intent.merchant, ignoreCase = true) == true ||
            it.rawMessage.contains(intent.merchant, ignoreCase = true)
        }

        return if (matched.isEmpty()) {
            "I couldn't find any transactions with '${intent.merchant}'."
        } else {
            val total = matched.sumOf { it.amount }
            "You have ${matched.size} transactions with '${intent.merchant}' totaling $currencySymbol${formatAmount(total)}."
        }
    }

    private fun handleSummary(transactions: List<Transaction>, currencySymbol: String): String {
        val thisMonth = filterByTimeRange(transactions, TimeRange.THIS_MONTH)
        val debits = thisMonth.filter { it.type == TransactionType.DEBIT }
        val credits = thisMonth.filter { it.type == TransactionType.CREDIT }
        
        val totalSpent = debits.sumOf { it.amount }
        val totalIncome = credits.sumOf { it.amount }
        val netSavings = totalIncome - totalSpent

        val topCategory = debits
            .groupBy { it.category }
            .maxByOrNull { (_, txs) -> txs.sumOf { it.amount } }

        val summary = StringBuilder()
        summary.append("📊 This Month's Summary:\n\n")
        summary.append("• Total Spent: $currencySymbol${formatAmount(totalSpent)}\n")
        summary.append("• Total Income: $currencySymbol${formatAmount(totalIncome)}\n")
        summary.append("• Net: ${if (netSavings >= 0) "+" else ""}$currencySymbol${formatAmount(netSavings)}\n")
        
        topCategory?.let { (cat, txs) ->
            val catTotal = txs.sumOf { it.amount }
            summary.append("• Top Category: ${formatCategory(cat)} ($currencySymbol${formatAmount(catTotal)})")
        }

        return summary.toString()
    }

    private fun handleUnknown(transactions: List<Transaction>, currencySymbol: String): String {
        val total = transactions.sumOf { it.amount }
        return "You have ${transactions.size} transactions totaling $currencySymbol${formatAmount(total)}. " +
               "Try asking about specific categories like food, shopping, or investments!"
    }

    private fun getHelpText(): String {
        return """I can help you understand your spending! Try asking:

• How much did I spend on food this month?
• What's my biggest expense?
• Show my recent transactions
• How much did I invest this year?
• Give me a category breakdown
• How much did I spend on entertainment?
• What's my total spending this month?
• Show me my income this month

I'll give you factual information about your transactions. I don't provide financial advice."""
    }

    private fun filterByTimeRange(transactions: List<Transaction>, timeRange: TimeRange): List<Transaction> {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        val startTime = when (timeRange) {
            TimeRange.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            TimeRange.THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            TimeRange.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            TimeRange.LAST_MONTH -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            TimeRange.THIS_YEAR -> {
                calendar.set(Calendar.MONTH, Calendar.JANUARY)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            TimeRange.ALL_TIME -> 0L
        }

        val endTime = when (timeRange) {
            TimeRange.LAST_MONTH -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.timeInMillis
            }
            else -> now
        }

        return transactions.filter { it.timestamp in startTime..endTime }
    }

    private fun formatTimeRange(timeRange: TimeRange): String {
        return when (timeRange) {
            TimeRange.TODAY -> "today"
            TimeRange.THIS_WEEK -> "this week"
            TimeRange.THIS_MONTH -> "this month"
            TimeRange.LAST_MONTH -> "last month"
            TimeRange.THIS_YEAR -> "this year"
            TimeRange.ALL_TIME -> "in total"
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
}
