package com.explainmymoney.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

enum class TransactionType {
    DEBIT, CREDIT
}

enum class TransactionCategory {
    FOOD, ENTERTAINMENT, EMI_HOME_LOAN, EMI_CAR_LOAN, 
    UTILITIES, SHOPPING, INVESTMENT, SALARY, TRANSFER, OTHER
}

enum class InvestmentType {
    SIP, MUTUAL_FUND, STOCKS, PPF, NPS, BONDS, OTHER
}

enum class PaymentMethod {
    UPI, NEFT, IMPS, CARD, CASH, CHEQUE, OTHER
}

enum class TransactionSource {
    SMS, EMAIL, STATEMENT
}

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rawMessage: String,
    val source: TransactionSource,
    val timestamp: Long,
    val category: TransactionCategory,
    val investmentType: InvestmentType? = null,
    val summary: String,
    val amount: Double,
    val currency: String = "₹",
    val type: TransactionType,
    val merchant: String? = null,
    val method: PaymentMethod? = null,
    val referenceNo: String? = null,
    val balance: Double? = null
)
