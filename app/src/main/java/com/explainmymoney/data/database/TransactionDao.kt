package com.explainmymoney.data.database

import androidx.room.*
import com.explainmymoney.domain.model.Transaction
import com.explainmymoney.domain.model.TransactionCategory
import com.explainmymoney.domain.model.TransactionSource
import com.explainmymoney.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): Transaction?

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY timestamp DESC")
    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY timestamp DESC")
    fun getTransactionsByCategory(category: TransactionCategory): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getTransactionsInRange(startTime: Long, endTime: Long): Flow<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getTotalAmountByType(type: TransactionType, startTime: Long, endTime: Long): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE category = :category AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getTotalByCategory(category: TransactionCategory, startTime: Long, endTime: Long): Double?

    @Query("SELECT * FROM transactions WHERE category = 'INVESTMENT' ORDER BY timestamp DESC")
    fun getInvestmentTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE rawMessage LIKE '%' || :query || '%' OR merchant LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchTransactions(query: String): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int

    @Query("SELECT * FROM transactions WHERE amount = :amount AND timestamp BETWEEN :startTime AND :endTime AND type = :type LIMIT 1")
    suspend fun findDuplicate(amount: Double, startTime: Long, endTime: Long, type: TransactionType): Transaction?

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'DEBIT' AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getTotalSpentInRange(startTime: Long, endTime: Long): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'CREDIT' AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getTotalIncomeInRange(startTime: Long, endTime: Long): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE category = 'INVESTMENT' AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getTotalInvestedInRange(startTime: Long, endTime: Long): Double?

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startTime AND :endTime AND type = 'DEBIT' ORDER BY amount DESC LIMIT :limit")
    suspend fun getTopExpensesInRange(startTime: Long, endTime: Long, limit: Int): List<Transaction>

    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE type = 'DEBIT' AND timestamp BETWEEN :startTime AND :endTime GROUP BY category ORDER BY total DESC")
    suspend fun getCategoryTotalsInRange(startTime: Long, endTime: Long): List<CategoryTotal>

    @Query("SELECT * FROM transactions WHERE source = :source ORDER BY timestamp DESC")
    fun getTransactionsBySource(source: TransactionSource): Flow<List<Transaction>>

    @Query("SELECT DISTINCT strftime('%Y-%m', datetime(timestamp/1000, 'unixepoch')) as month FROM transactions ORDER BY month DESC")
    fun getDistinctMonths(): Flow<List<String>>
}

data class CategoryTotal(
    val category: TransactionCategory,
    val total: Double
)
