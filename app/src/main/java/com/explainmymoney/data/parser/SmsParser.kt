package com.explainmymoney.data.parser

import com.explainmymoney.domain.model.*
import java.util.regex.Pattern

class SmsParser {

    fun parseTransactionSms(sender: String, body: String, timestamp: Long): Transaction? {
        if (!isTransactionSms(sender, body)) return null

        val amount = extractAmount(body) ?: return null
        val type = determineTransactionType(body)
        val merchant = extractMerchant(body)
        val method = detectPaymentMethod(body)
        val balance = extractBalance(body)
        val referenceNo = extractReferenceNumber(body)
        val (category, investmentType) = categorizeTransaction(body, merchant)

        return Transaction(
            rawMessage = body,
            source = TransactionSource.SMS,
            timestamp = timestamp,
            category = category,
            investmentType = investmentType,
            summary = generateSummary(amount, type, merchant, category),
            amount = amount,
            type = type,
            merchant = merchant,
            method = method,
            referenceNo = referenceNo,
            balance = balance
        )
    }

    private fun isTransactionSms(sender: String, body: String): Boolean {
        val bankSenders = listOf(
            "HDFCBK", "SBIINB", "ICICIB", "AXISBK", "KOTAKB", "PNBSMS", "BOIIND",
            "IABORATNMOBILE", "YESBNK", "INDUSB", "CABORATIN", "FEDERALB",
            "PAYTMB", "IABORAT", "CANBNK", "UNIONB", "IABORATNIN", "RBLBNK",
            "IABORATOR", "IDBIBNK", "CITIBNK", "SCBANK", "DLOBNK", "AUFBNK",
            "PHONPE", "GPAY", "AMAZONPAY", "MOBIKWIK"
        )

        val transactionKeywords = listOf(
            "debited", "credited", "sent", "received", "paid", "payment",
            "withdrawn", "deposited", "transfer", "upi", "neft", "imps",
            "a/c", "acct", "account", "rs.", "inr", "bal", "txn", "ref"
        )

        val senderMatch = bankSenders.any { sender.uppercase().contains(it) } ||
                sender.matches(Regex(".*[A-Z]{2}-[A-Z]{3,}.*")) ||
                sender.matches(Regex(".*[A-Z]{5,}.*"))

        val lowerBody = body.lowercase()
        val bodyMatch = transactionKeywords.any { lowerBody.contains(it) }

        val hasAmount = body.matches(Regex(".*(?:Rs\\.?|INR|₹)\\s*[\\d,]+.*", RegexOption.IGNORE_CASE))

        return senderMatch && bodyMatch && hasAmount
    }

    private fun extractAmount(body: String): Double? {
        val patterns = listOf(
            Pattern.compile("""(?:Rs\.?|INR|₹)\s*([\d,]+\.?\d*)""", Pattern.CASE_INSENSITIVE),
            Pattern.compile("""([\d,]+\.?\d*)\s*(?:Rs|INR|₹)""", Pattern.CASE_INSENSITIVE),
            Pattern.compile("""(?:amount|amt)[:\s]*([\d,]+\.?\d*)""", Pattern.CASE_INSENSITIVE)
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                val amountStr = matcher.group(1)?.replace(",", "") ?: continue
                return amountStr.toDoubleOrNull()
            }
        }
        return null
    }

    private fun determineTransactionType(body: String): TransactionType {
        val debitKeywords = listOf(
            "debited", "debit", "sent", "paid", "payment", "withdrawn", 
            "purchase", "spent", "transferred to", "dr"
        )

        val creditKeywords = listOf(
            "credited", "credit", "received", "deposited", "refund",
            "transferred from", "cr"
        )

        val lowerBody = body.lowercase()

        return when {
            debitKeywords.any { lowerBody.contains(it) } -> TransactionType.DEBIT
            creditKeywords.any { lowerBody.contains(it) } -> TransactionType.CREDIT
            else -> TransactionType.DEBIT
        }
    }

    private fun extractMerchant(body: String): String? {
        val merchantPatterns = listOf(
            Pattern.compile("""(?:to|at|from)\s+([A-Za-z0-9\s&]+?)(?:\s+(?:on|via|ref|upi)|[.\d])""", Pattern.CASE_INSENSITIVE),
            Pattern.compile("""(?:VPA|UPI)[:\s]*([a-zA-Z0-9.@]+)""", Pattern.CASE_INSENSITIVE),
            Pattern.compile("""(?:IMPS|NEFT|RTGS)[/\s]*[A-Za-z]+/([A-Za-z\s]+)/""", Pattern.CASE_INSENSITIVE)
        )

        for (pattern in merchantPatterns) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                val merchant = matcher.group(1)?.trim()
                if (!merchant.isNullOrBlank() && merchant.length > 2) {
                    return cleanMerchantName(merchant)
                }
            }
        }

        val knownMerchants = listOf(
            "Zomato", "Swiggy", "Amazon", "Flipkart", "Netflix", "Spotify", "Uber", "Ola",
            "Paytm", "PhonePe", "GPay", "Google Pay", "Zerodha", "Groww", "Upstox"
        )

        return knownMerchants.find { body.contains(it, ignoreCase = true) }
    }

    private fun cleanMerchantName(name: String): String {
        return name
            .replace(Regex("[^A-Za-z0-9\\s]"), "")
            .trim()
            .split("\\s+".toRegex())
            .take(3)
            .joinToString(" ")
            .take(30)
    }

    private fun detectPaymentMethod(body: String): PaymentMethod? {
        val lowerBody = body.lowercase()
        return when {
            lowerBody.contains("upi") || lowerBody.contains("@") -> PaymentMethod.UPI
            lowerBody.contains("neft") -> PaymentMethod.NEFT
            lowerBody.contains("imps") -> PaymentMethod.IMPS
            lowerBody.contains("rtgs") -> PaymentMethod.OTHER
            lowerBody.contains("card") || lowerBody.contains("atm") -> PaymentMethod.CARD
            lowerBody.contains("cheque") || lowerBody.contains("chq") -> PaymentMethod.CHEQUE
            else -> null
        }
    }

    private fun extractBalance(body: String): Double? {
        val patterns = listOf(
            Pattern.compile("""(?:bal|balance|avl bal|available)[:\s]*(?:Rs\.?|INR|₹)?\s*([\d,]+\.?\d*)""", Pattern.CASE_INSENSITIVE),
            Pattern.compile("""(?:Rs\.?|INR|₹)\s*([\d,]+\.?\d*)\s*(?:bal|balance)""", Pattern.CASE_INSENSITIVE)
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                val balanceStr = matcher.group(1)?.replace(",", "") ?: continue
                return balanceStr.toDoubleOrNull()
            }
        }
        return null
    }

    private fun extractReferenceNumber(body: String): String? {
        val patterns = listOf(
            Pattern.compile("""(?:ref|reference|txn|transaction)[:\s#]*([A-Za-z0-9]+)""", Pattern.CASE_INSENSITIVE),
            Pattern.compile("""(?:UPI)[:\s]*([0-9]+)""", Pattern.CASE_INSENSITIVE)
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                return matcher.group(1)?.take(20)
            }
        }
        return null
    }

    private fun categorizeTransaction(body: String, merchant: String?): Pair<TransactionCategory, InvestmentType?> {
        val combined = "${body.lowercase()} ${merchant?.lowercase() ?: ""}"

        return when {
            combined.matches(Regex(".*(sip|systematic investment|mutual fund|groww|zerodha|upstox).*")) ->
                TransactionCategory.INVESTMENT to InvestmentType.SIP
            combined.matches(Regex(".*(ppf|public provident).*")) ->
                TransactionCategory.INVESTMENT to InvestmentType.PPF
            combined.matches(Regex(".*(nps|national pension).*")) ->
                TransactionCategory.INVESTMENT to InvestmentType.NPS
            combined.matches(Regex(".*(zomato|swiggy|food|restaurant|cafe|pizza).*")) ->
                TransactionCategory.FOOD to null
            combined.matches(Regex(".*(netflix|prime|hotstar|spotify|movie|entertainment).*")) ->
                TransactionCategory.ENTERTAINMENT to null
            combined.matches(Regex(".*(home loan|housing loan|mortgage).*")) ->
                TransactionCategory.EMI_HOME_LOAN to null
            combined.matches(Regex(".*(car loan|auto loan|vehicle).*")) ->
                TransactionCategory.EMI_CAR_LOAN to null
            combined.matches(Regex(".*(electricity|water|gas|internet|broadband|mobile|recharge).*")) ->
                TransactionCategory.UTILITIES to null
            combined.matches(Regex(".*(amazon|flipkart|myntra|shopping).*")) ->
                TransactionCategory.SHOPPING to null
            combined.matches(Regex(".*(salary|credited.*account).*")) ->
                TransactionCategory.SALARY to null
            else -> TransactionCategory.OTHER to null
        }
    }

    private fun generateSummary(
        amount: Double,
        type: TransactionType,
        merchant: String?,
        category: TransactionCategory
    ): String {
        val typeWord = if (type == TransactionType.DEBIT) "paid" else "received"
        val amountStr = "₹${String.format("%,.2f", amount)}"
        val to = merchant ?: "someone"

        return when (category) {
            TransactionCategory.FOOD -> "You $typeWord $amountStr for food at $to"
            TransactionCategory.INVESTMENT -> "You invested $amountStr"
            TransactionCategory.SHOPPING -> "You $typeWord $amountStr at $to"
            TransactionCategory.SALARY -> "You received $amountStr as salary"
            else -> "You $typeWord $amountStr to $to"
        }
    }
}
