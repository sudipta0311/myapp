package com.explainmymoney.data.repository

import com.explainmymoney.data.database.CategoryTotal
import com.explainmymoney.data.database.TransactionDao
import com.explainmymoney.domain.model.Transaction
import com.explainmymoney.domain.model.TransactionCategory
import com.explainmymoney.domain.model.TransactionSource
import com.explainmymoney.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class TransactionRepository(private val transactionDao: TransactionDao) {

    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> =
        transactionDao.getTransactionsByType(type)

    fun getTransactionsByCategory(category: TransactionCategory): Flow<List<Transaction>> =
        transactionDao.getTransactionsByCategory(category)

    fun getInvestmentTransactions(): Flow<List<Transaction>> =
        transactionDao.getInvestmentTransactions()

    fun searchTransactions(query: String): Flow<List<Transaction>> =
        transactionDao.searchTransactions(query)

    fun getTransactionsBySource(source: TransactionSource): Flow<List<Transaction>> =
        transactionDao.getTransactionsBySource(source)

    fun getDistinctMonths(): Flow<List<String>> = transactionDao.getDistinctMonths()

    fun getTransactionsInRange(startTime: Long, endTime: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsInRange(startTime, endTime)

    fun getTransactionsThisMonth(): Flow<List<Transaction>> {
        val (startOfMonth, endOfMonth) = getMonthRange()
        return transactionDao.getTransactionsInRange(startOfMonth, endOfMonth)
    }

    suspend fun getTotalSpentThisMonth(): Double {
        val (startOfMonth, endOfMonth) = getMonthRange()
        return transactionDao.getTotalAmountByType(TransactionType.DEBIT, startOfMonth, endOfMonth) ?: 0.0
    }

    suspend fun getTotalIncomeThisMonth(): Double {
        val (startOfMonth, endOfMonth) = getMonthRange()
        return transactionDao.getTotalAmountByType(TransactionType.CREDIT, startOfMonth, endOfMonth) ?: 0.0
    }

    suspend fun getTotalSpentInRange(startTime: Long, endTime: Long): Double =
        transactionDao.getTotalSpentInRange(startTime, endTime) ?: 0.0

    suspend fun getTotalIncomeInRange(startTime: Long, endTime: Long): Double =
        transactionDao.getTotalIncomeInRange(startTime, endTime) ?: 0.0

    suspend fun getTotalInvestedInRange(startTime: Long, endTime: Long): Double =
        transactionDao.getTotalInvestedInRange(startTime, endTime) ?: 0.0

    suspend fun getTopExpensesInRange(startTime: Long, endTime: Long, limit: Int = 5): List<Transaction> =
        transactionDao.getTopExpensesInRange(startTime, endTime, limit)

    suspend fun getCategoryTotalsInRange(startTime: Long, endTime: Long): List<CategoryTotal> =
        transactionDao.getCategoryTotalsInRange(startTime, endTime)

    suspend fun getCategoryBreakdown(): Map<TransactionCategory, Double> {
        val (startOfMonth, endOfMonth) = getMonthRange()
        return TransactionCategory.entries.associateWith { category ->
            transactionDao.getTotalByCategory(category, startOfMonth, endOfMonth) ?: 0.0
        }.filterValues { it > 0 }
    }

    suspend fun insertTransaction(transaction: Transaction): Long =
        transactionDao.insertTransaction(transaction)

    suspend fun insertTransactions(transactions: List<Transaction>) =
        transactionDao.insertTransactions(transactions)

    suspend fun insertTransactionsWithDeduplication(transactions: List<Transaction>): Int {
        var insertedCount = 0
        for (transaction in transactions) {
            if (!isDuplicate(transaction)) {
                transactionDao.insertTransaction(transaction)
                insertedCount++
            }
        }
        return insertedCount
    }

    suspend fun isDuplicate(transaction: Transaction): Boolean {
        val timeWindow = 60 * 60 * 1000L // 1 hour window
        val startTime = transaction.timestamp - timeWindow
        val endTime = transaction.timestamp + timeWindow
        
        val existing = transactionDao.findDuplicate(
            amount = transaction.amount,
            startTime = startTime,
            endTime = endTime,
            type = transaction.type
        )
        return existing != null
    }

    suspend fun deleteTransaction(id: Long) =
        transactionDao.deleteTransactionById(id)

    suspend fun getTransactionCount(): Int =
        transactionDao.getTransactionCount()

    private fun getMonthRange(monthOffset: Int = 0): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, monthOffset)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        val endOfMonth = calendar.timeInMillis

        return startOfMonth to endOfMonth
    }

    fun getYearRange(year: Int = Calendar.getInstance().get(Calendar.YEAR)): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, Calendar.JANUARY)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfYear = calendar.timeInMillis

        calendar.add(Calendar.YEAR, 1)
        val endOfYear = calendar.timeInMillis

        return startOfYear to endOfYear
    }

    suspend fun getTotalSpentThisYear(): Double {
        val (start, end) = getYearRange()
        return transactionDao.getTotalSpentInRange(start, end) ?: 0.0
    }

    suspend fun getTotalIncomeThisYear(): Double {
        val (start, end) = getYearRange()
        return transactionDao.getTotalIncomeInRange(start, end) ?: 0.0
    }

    suspend fun getTotalInvestedThisYear(): Double {
        val (start, end) = getYearRange()
        return transactionDao.getTotalInvestedInRange(start, end) ?: 0.0
    }
}
