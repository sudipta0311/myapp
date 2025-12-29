package com.explainmymoney.data.database

import androidx.room.*
import com.explainmymoney.domain.model.Budget
import com.explainmymoney.domain.model.TransactionCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE isActive = 1 ORDER BY category")
    fun getActiveBudgets(): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE yearMonth = :yearMonth AND isActive = 1 ORDER BY category")
    fun getBudgetsForMonth(yearMonth: String): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE category = :category AND yearMonth = :yearMonth AND isActive = 1 LIMIT 1")
    suspend fun getBudgetForCategory(category: TransactionCategory, yearMonth: String): Budget?

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getBudgetById(id: Long): Budget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget): Long

    @Update
    suspend fun updateBudget(budget: Budget)

    @Delete
    suspend fun deleteBudget(budget: Budget)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: Long)

    @Query("UPDATE budgets SET isActive = 0 WHERE id = :id")
    suspend fun deactivateBudget(id: Long)

    @Query("SELECT DISTINCT yearMonth FROM budgets ORDER BY yearMonth DESC")
    fun getAllBudgetMonths(): Flow<List<String>>
}
