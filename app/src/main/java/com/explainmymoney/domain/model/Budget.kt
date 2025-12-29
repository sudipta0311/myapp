package com.explainmymoney.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: TransactionCategory,
    val monthlyLimit: Double,
    val yearMonth: String, // Format: "2024-01" for January 2024
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class BudgetWithSpending(
    val budget: Budget,
    val spent: Double,
    val remaining: Double,
    val percentUsed: Float
) {
    val isOverBudget: Boolean get() = spent > budget.monthlyLimit
    val isNearLimit: Boolean get() = percentUsed >= 0.8f && !isOverBudget
}

data class MonthlyBudgetSummary(
    val yearMonth: String,
    val totalBudget: Double,
    val totalSpent: Double,
    val categoryBreakdown: List<BudgetWithSpending>
)
