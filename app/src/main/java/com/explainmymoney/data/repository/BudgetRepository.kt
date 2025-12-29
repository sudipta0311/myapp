package com.explainmymoney.data.repository

import com.explainmymoney.data.database.BudgetDao
import com.explainmymoney.data.database.TransactionDao
import com.explainmymoney.domain.model.Budget
import com.explainmymoney.domain.model.BudgetWithSpending
import com.explainmymoney.domain.model.TransactionCategory
import com.explainmymoney.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*

class BudgetRepository(
    private val budgetDao: BudgetDao,
    private val transactionDao: TransactionDao
) {
    fun getActiveBudgets(): Flow<List<Budget>> = budgetDao.getActiveBudgets()

    fun getBudgetsForMonth(yearMonth: String): Flow<List<Budget>> = 
        budgetDao.getBudgetsForMonth(yearMonth)

    suspend fun getBudgetForCategory(category: TransactionCategory, yearMonth: String): Budget? =
        budgetDao.getBudgetForCategory(category, yearMonth)

    suspend fun insertBudget(budget: Budget): Long = budgetDao.insertBudget(budget)

    suspend fun updateBudget(budget: Budget) = budgetDao.updateBudget(budget)

    suspend fun deleteBudget(id: Long) = budgetDao.deleteBudgetById(id)

    suspend fun deactivateBudget(id: Long) = budgetDao.deactivateBudget(id)

    fun getBudgetsWithSpending(yearMonth: String): Flow<List<BudgetWithSpending>> {
        val (startTime, endTime) = getMonthRange(yearMonth)
        
        return budgetDao.getBudgetsForMonth(yearMonth).combine(
            transactionDao.getTransactionsInRange(startTime, endTime)
        ) { budgets, transactions ->
            budgets.map { budget ->
                val spent = transactions
                    .filter { it.category == budget.category && it.type == TransactionType.DEBIT }
                    .sumOf { it.amount }
                
                val remaining = (budget.monthlyLimit - spent).coerceAtLeast(0.0)
                val percentUsed = if (budget.monthlyLimit > 0) {
                    (spent / budget.monthlyLimit).toFloat().coerceIn(0f, 1.5f)
                } else 0f

                BudgetWithSpending(
                    budget = budget,
                    spent = spent,
                    remaining = remaining,
                    percentUsed = percentUsed
                )
            }
        }
    }

    fun getCurrentMonthBudgetsWithSpending(): Flow<List<BudgetWithSpending>> {
        return getBudgetsWithSpending(getCurrentYearMonth())
    }

    companion object {
        fun getCurrentYearMonth(): String {
            val formatter = SimpleDateFormat("yyyy-MM", Locale.US)
            return formatter.format(Date())
        }

        fun getMonthRange(yearMonth: String): Pair<Long, Long> {
            val formatter = SimpleDateFormat("yyyy-MM", Locale.US)
            val date = formatter.parse(yearMonth) ?: Date()
            val calendar = Calendar.getInstance().apply {
                time = date
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startTime = calendar.timeInMillis
            calendar.add(Calendar.MONTH, 1)
            val endTime = calendar.timeInMillis
            return startTime to endTime
        }
    }
}
