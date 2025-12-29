package com.explainmymoney.data.parser

import com.explainmymoney.data.gmail.EmailData
import com.explainmymoney.domain.model.Transaction
import com.explainmymoney.domain.model.TransactionCategory
import com.explainmymoney.domain.model.TransactionSource
import com.explainmymoney.domain.model.TransactionType
import java.util.regex.Pattern

class EmailParser {
    
    private val amountPatterns = listOf(
        Pattern.compile("(?:Rs\\.?|INR|₹)\\s*([\\d,]+\\.?\\d*)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("([\\d,]+\\.?\\d*)\\s*(?:Rs\\.?|INR|₹)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:amount|amt)\\s*(?:of)?\\s*(?:Rs\\.?|INR|₹)?\\s*([\\d,]+\\.?\\d*)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:debited|credited|paid|received|spent)\\s*(?:Rs\\.?|INR|₹)?\\s*([\\d,]+\\.?\\d*)", Pattern.CASE_INSENSITIVE)
    )
    
    private val debitKeywords = listOf(
        "debited", "debit", "spent", "paid", "payment", "purchase",
        "withdrawn", "withdrawal", "sent", "transfer to", "bill payment",
        "emi", "subscription", "charged"
    )
    
    private val creditKeywords = listOf(
        "credited", "credit", "received", "deposit", "refund",
        "cashback", "salary", "transfer from", "income", "interest"
    )
    
    private val merchantPatterns = listOf(
        Pattern.compile("(?:to|at|from|merchant|payee)\\s+([A-Za-z][A-Za-z0-9\\s&.-]{2,30}?)(?:\\s|\\.|,|$)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:UPI|VPA)\\s*[-:]?\\s*([a-zA-Z0-9._-]+@[a-zA-Z]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:paid to|received from|transfer to|transfer from)\\s+([A-Za-z][A-Za-z0-9\\s.-]{2,30})", Pattern.CASE_INSENSITIVE)
    )
    
    fun parseTransactionEmail(email: EmailData): Transaction? {
        val content = "${email.subject} ${email.body}".lowercase()
        
        if (!isTransactionEmail(content)) {
            return null
        }
        
        val amount = extractAmount(content) ?: return null
        if (amount < 1.0 || amount > 100000000.0) {
            return null
        }
        
        val type = determineTransactionType(content)
        val merchant = extractMerchant(content, email.from)
        val category = categorizeTransaction(content, merchant)
        
        val summary = generateSummary(type, amount, merchant, category)
        
        return Transaction(
            rawMessage = email.body.take(500),
            source = TransactionSource.EMAIL,
            amount = amount,
            type = type,
            category = category,
            merchant = merchant,
            summary = summary,
            timestamp = email.date
        )
    }
    
    private fun isTransactionEmail(content: String): Boolean {
        val hasAmount = amountPatterns.any { it.matcher(content).find() }
        val hasTransactionWord = (debitKeywords + creditKeywords).any { content.contains(it) }
        return hasAmount && hasTransactionWord
    }
    
    private fun extractAmount(content: String): Double? {
        for (pattern in amountPatterns) {
            val matcher = pattern.matcher(content)
            if (matcher.find()) {
                val amountStr = matcher.group(1)?.replace(",", "") ?: continue
                val amount = amountStr.toDoubleOrNull()
                if (amount != null && amount > 0) {
                    return amount
                }
            }
        }
        return null
    }
    
    private fun determineTransactionType(content: String): TransactionType {
        val debitScore = debitKeywords.count { content.contains(it) }
        val creditScore = creditKeywords.count { content.contains(it) }
        
        return if (creditScore > debitScore) TransactionType.CREDIT else TransactionType.DEBIT
    }
    
    private fun extractMerchant(content: String, from: String): String? {
        for (pattern in merchantPatterns) {
            val matcher = pattern.matcher(content)
            if (matcher.find()) {
                val merchant = matcher.group(1)?.trim()
                if (merchant != null && merchant.length >= 2 && merchant.length <= 50) {
                    return cleanMerchantName(merchant)
                }
            }
        }
        
        val senderName = from
            .replace(Regex("<.*>"), "")
            .replace(Regex("@.*"), "")
            .trim()
        
        if (senderName.isNotEmpty() && !senderName.contains("noreply", ignoreCase = true)
            && !senderName.contains("alert", ignoreCase = true)) {
            return senderName.take(30)
        }
        
        return null
    }
    
    private fun cleanMerchantName(name: String): String {
        return name
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[^a-zA-Z0-9\\s&.-]"), "")
            .trim()
            .take(30)
    }
    
    private fun categorizeTransaction(content: String, merchant: String?): TransactionCategory {
        val searchText = "$content ${merchant ?: ""}".lowercase()
        
        return when {
            containsAny(searchText, listOf("food", "restaurant", "zomato", "swiggy", "uber eats", "domino", 
                "pizza", "burger", "cafe", "coffee", "tea", "breakfast", "lunch", "dinner", "meal")) -> 
                TransactionCategory.FOOD
            
            containsAny(searchText, listOf("emi", "loan", "housing", "mortgage", "home loan", "property")) -> 
                TransactionCategory.EMI_HOME_LOAN
            
            containsAny(searchText, listOf("car loan", "vehicle loan", "auto loan", "car emi")) -> 
                TransactionCategory.EMI_CAR_LOAN
            
            containsAny(searchText, listOf("electricity", "water bill", "gas bill", "utility", "broadband", 
                "internet", "mobile recharge", "phone bill", "dth", "airtel", "jio", "vodafone", "bsnl")) -> 
                TransactionCategory.UTILITIES
            
            containsAny(searchText, listOf("netflix", "prime", "hotstar", "spotify", "movie", "cinema", 
                "entertainment", "gaming", "playstation", "xbox", "steam", "concert", "theatre")) -> 
                TransactionCategory.ENTERTAINMENT
            
            containsAny(searchText, listOf("amazon", "flipkart", "myntra", "ajio", "shopping", "purchase", 
                "order", "mart", "mall", "store", "retail", "nykaa", "meesho")) -> 
                TransactionCategory.SHOPPING
            
            containsAny(searchText, listOf("mutual fund", "sip", "investment", "stocks", "shares", 
                "zerodha", "groww", "upstox", "ppf", "nps", "fd", "fixed deposit", "dividend")) -> 
                TransactionCategory.INVESTMENT
            
            else -> TransactionCategory.OTHER
        }
    }
    
    private fun containsAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it) }
    }
    
    private fun generateSummary(
        type: TransactionType,
        amount: Double,
        merchant: String?,
        category: TransactionCategory
    ): String {
        val action = if (type == TransactionType.DEBIT) "Paid" else "Received"
        val categoryName = category.name.replace("_", " ").lowercase()
            .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        
        return if (merchant != null) {
            "$action ₹${String.format("%,.0f", amount)} to $merchant ($categoryName)"
        } else {
            "$action ₹${String.format("%,.0f", amount)} - $categoryName"
        }
    }
    
    fun parseEmails(emails: List<EmailData>): List<Transaction> {
        return emails.mapNotNull { parseTransactionEmail(it) }
            .sortedByDescending { it.timestamp }
    }
}
