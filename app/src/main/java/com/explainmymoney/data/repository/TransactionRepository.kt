package com.explainmymoney.data.repository

import com.explainmymoney.data.database.TransactionDao
import com.explainmymoney.domain.model.Transaction
import com.explainmymoney.domain.model.TransactionCategory
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

    fun getTransactionsThisMonth(): Flow<List<Transaction>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfMonth = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        val endOfMonth = calendar.timeInMillis

        return transactionDao.getTransactionsInRange(startOfMonth, endOfMonth)
    }

    suspend fun getTotalSpentThisMonth(): Double {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfMonth = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        val endOfMonth = calendar.timeInMillis

        return transactionDao.getTotalAmountByType(TransactionType.DEBIT, startOfMonth, endOfMonth) ?: 0.0
    }

    suspend fun getTotalIncomeThisMonth(): Double {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfMonth = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        val endOfMonth = calendar.timeInMillis

        return transactionDao.getTotalAmountByType(TransactionType.CREDIT, startOfMonth, endOfMonth) ?: 0.0
    }

    suspend fun getCategoryBreakdown(): Map<TransactionCategory, Double> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfMonth = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        val endOfMonth = calendar.timeInMillis

        return TransactionCategory.entries.associateWith { category ->
            transactionDao.getTotalByCategory(category, startOfMonth, endOfMonth) ?: 0.0
        }.filterValues { it > 0 }
    }

    suspend fun insertTransaction(transaction: Transaction): Long =
        transactionDao.insertTransaction(transaction)

    suspend fun insertTransactions(transactions: List<Transaction>) =
        transactionDao.insertTransactions(transactions)

    suspend fun deleteTransaction(id: Long) =
        transactionDao.deleteTransactionById(id)

    suspend fun getTransactionCount(): Int =
        transactionDao.getTransactionCount()
}
