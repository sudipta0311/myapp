package com.explainmymoney.data.parser

import android.content.Context
import android.net.Uri
import com.explainmymoney.domain.model.*
import com.opencsv.CSVReader
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class StatementParser(private val context: Context) {

    init {
        PDFBoxResourceLoader.init(context)
    }

    data class ParsedTransaction(
        val date: Date,
        val description: String,
        val amount: Double,
        val type: TransactionType,
        val balance: Double? = null
    )

    fun parseFile(uri: Uri): List<Transaction> {
        val mimeType = context.contentResolver.getType(uri) ?: return emptyList()
        
        val parsedTransactions = when {
            mimeType.contains("pdf") -> parsePdf(uri)
            mimeType.contains("csv") || mimeType.contains("text") -> parseCsv(uri)
            mimeType.contains("spreadsheet") || mimeType.contains("excel") || 
            mimeType.contains("sheet") -> parseExcel(uri)
            else -> emptyList()
        }

        return parsedTransactions
            .filter { isValidTransaction(it) }
            .map { convertToTransaction(it) }
    }

    private fun parsePdf(uri: Uri): List<ParsedTransaction> {
        val transactions = mutableListOf<ParsedTransaction>()
        
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val document = PDDocument.load(inputStream)
                val stripper = PDFTextStripper()
                val text = stripper.getText(document)
                document.close()

                val lines = text.split("\n").filter { it.isNotBlank() }
                
                for (line in lines) {
                    parseLine(line)?.let { transactions.add(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return transactions
    }

    private fun parseCsv(uri: Uri): List<ParsedTransaction> {
        val transactions = mutableListOf<ParsedTransaction>()

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = CSVReader(InputStreamReader(inputStream))
                var headers: Array<String>? = null
                var line: Array<String>?

                while (reader.readNext().also { line = it } != null) {
                    if (headers == null) {
                        headers = line
                        continue
                    }

                    line?.let { row ->
                        parseRowFromHeaders(row, headers)?.let { transactions.add(it) }
                    }
                }
                reader.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return transactions
    }

    private fun parseExcel(uri: Uri): List<ParsedTransaction> {
        val transactions = mutableListOf<ParsedTransaction>()

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val workbook = WorkbookFactory.create(inputStream)
                val sheet = workbook.getSheetAt(0)
                var headers: List<String>? = null

                for (row in sheet) {
                    if (headers == null) {
                        headers = row.map { it.stringCellValue.lowercase() }
                        continue
                    }

                    val values = row.map { cell ->
                        when (cell.cellType) {
                            org.apache.poi.ss.usermodel.CellType.NUMERIC -> cell.numericCellValue.toString()
                            org.apache.poi.ss.usermodel.CellType.STRING -> cell.stringCellValue
                            else -> ""
                        }
                    }.toTypedArray()

                    parseRowFromHeaders(values, headers.toTypedArray())?.let { 
                        transactions.add(it) 
                    }
                }
                workbook.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return transactions
    }

    private fun parseLine(line: String): ParsedTransaction? {
        val datePattern = Pattern.compile(
            """(\d{1,2}[-/]\d{1,2}[-/]\d{2,4}|\d{1,2}\s+\w{3}\s+\d{2,4})"""
        )
        val amountPattern = Pattern.compile(
            """(?:Rs\.?|INR|₹)\s*([\d,]+\.?\d*)|([\d,]+\.?\d*)\s*(?:Dr|Cr|DR|CR)"""
        )

        val dateMatcher = datePattern.matcher(line)
        val amountMatcher = amountPattern.matcher(line)

        if (!dateMatcher.find() || !amountMatcher.find()) return null

        val dateStr = dateMatcher.group(1) ?: return null
        val date = parseDate(dateStr) ?: return null

        val amountStr = amountMatcher.group(1) ?: amountMatcher.group(2) ?: return null
        val amount = amountStr.replace(",", "").toDoubleOrNull() ?: return null

        val isDebit = line.contains("Dr", ignoreCase = true) || 
                      line.contains("debit", ignoreCase = true) ||
                      line.contains("paid", ignoreCase = true) ||
                      line.contains("sent", ignoreCase = true)

        val description = line
            .replace(datePattern.toRegex(), "")
            .replace(amountPattern.toRegex(), "")
            .trim()
            .take(200)

        return ParsedTransaction(
            date = date,
            description = description,
            amount = amount,
            type = if (isDebit) TransactionType.DEBIT else TransactionType.CREDIT
        )
    }

    private fun parseRowFromHeaders(row: Array<String>, headers: Array<String>): ParsedTransaction? {
        val headerMap = headers.mapIndexed { index, header -> 
            header.lowercase() to index 
        }.toMap()

        val dateIndex = headerMap.entries.find { 
            it.key.contains("date") || it.key.contains("txn") 
        }?.value ?: return null

        val descIndex = headerMap.entries.find { 
            it.key.contains("desc") || it.key.contains("narration") || 
            it.key.contains("particular") || it.key.contains("remark")
        }?.value ?: return null

        val debitIndex = headerMap.entries.find { 
            it.key.contains("debit") || it.key.contains("withdrawal") 
        }?.value

        val creditIndex = headerMap.entries.find { 
            it.key.contains("credit") || it.key.contains("deposit") 
        }?.value

        val amountIndex = headerMap.entries.find { 
            it.key.contains("amount") && !it.key.contains("balance")
        }?.value

        if (dateIndex >= row.size || descIndex >= row.size) return null

        val dateStr = row.getOrNull(dateIndex) ?: return null
        val date = parseDate(dateStr) ?: return null
        val description = row.getOrNull(descIndex) ?: return null

        var amount = 0.0
        var type = TransactionType.DEBIT

        when {
            debitIndex != null && creditIndex != null -> {
                val debitAmount = row.getOrNull(debitIndex)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                val creditAmount = row.getOrNull(creditIndex)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                
                if (debitAmount > 0) {
                    amount = debitAmount
                    type = TransactionType.DEBIT
                } else if (creditAmount > 0) {
                    amount = creditAmount
                    type = TransactionType.CREDIT
                }
            }
            amountIndex != null -> {
                amount = row.getOrNull(amountIndex)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                type = if (description.contains("credit", ignoreCase = true)) 
                    TransactionType.CREDIT else TransactionType.DEBIT
            }
        }

        if (amount <= 0) return null

        return ParsedTransaction(
            date = date,
            description = description,
            amount = amount,
            type = type
        )
    }

    private fun parseDate(dateStr: String): Date? {
        val formats = listOf(
            SimpleDateFormat("dd/MM/yyyy", Locale.US),
            SimpleDateFormat("dd-MM-yyyy", Locale.US),
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
            SimpleDateFormat("dd MMM yyyy", Locale.US),
            SimpleDateFormat("dd/MM/yy", Locale.US),
            SimpleDateFormat("MM/dd/yyyy", Locale.US)
        )

        for (format in formats) {
            try {
                format.isLenient = false
                return format.parse(dateStr)
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }

    private fun isValidTransaction(parsed: ParsedTransaction): Boolean {
        if (parsed.amount <= 0) return false
        if (parsed.description.length < 3) return false

        val skipPatterns = listOf(
            "opening balance", "closing balance", "total", "balance b/f", "balance c/f",
            "statement", "account number", "ifsc", "branch", "page"
        )

        return skipPatterns.none { parsed.description.lowercase().contains(it) }
    }

    private fun convertToTransaction(parsed: ParsedTransaction): Transaction {
        val (category, investmentType) = categorizeTransaction(parsed.description)
        val merchant = extractMerchant(parsed.description)
        val method = detectPaymentMethod(parsed.description)

        return Transaction(
            rawMessage = parsed.description,
            source = TransactionSource.STATEMENT,
            timestamp = parsed.date.time,
            category = category,
            investmentType = investmentType,
            summary = generateSummary(parsed, category, merchant),
            amount = parsed.amount,
            type = parsed.type,
            merchant = merchant,
            method = method,
            referenceNo = "STMT-${System.currentTimeMillis()}-${(1000..9999).random()}",
            balance = parsed.balance
        )
    }

    private fun categorizeTransaction(description: String): Pair<TransactionCategory, InvestmentType?> {
        val desc = description.lowercase()

        return when {
            desc.matches(Regex(".*(sip|systematic investment|mutual fund|groww|zerodha|upstox|kuvera).*")) ->
                TransactionCategory.INVESTMENT to InvestmentType.SIP
            desc.matches(Regex(".*(ppf|public provident|provident fund).*")) ->
                TransactionCategory.INVESTMENT to InvestmentType.PPF
            desc.matches(Regex(".*(nps|national pension|pension scheme).*")) ->
                TransactionCategory.INVESTMENT to InvestmentType.NPS
            desc.matches(Regex(".*(stock|share|equity|trading|bse|nse).*")) ->
                TransactionCategory.INVESTMENT to InvestmentType.STOCKS
            desc.matches(Regex(".*(bond|debenture|fixed deposit|fd).*")) ->
                TransactionCategory.INVESTMENT to InvestmentType.BONDS
            desc.matches(Regex(".*(zomato|swiggy|food|restaurant|cafe|pizza|burger|domino|mcdonald|kfc|starbucks).*")) ->
                TransactionCategory.FOOD to null
            desc.matches(Regex(".*(netflix|prime|hotstar|spotify|movie|theater|cinema|entertainment|game).*")) ->
                TransactionCategory.ENTERTAINMENT to null
            desc.matches(Regex(".*(home loan|housing loan|mortgage|hdfc home|sbi home).*")) ->
                TransactionCategory.EMI_HOME_LOAN to null
            desc.matches(Regex(".*(car loan|auto loan|vehicle loan|two wheeler|bike loan).*")) ->
                TransactionCategory.EMI_CAR_LOAN to null
            desc.matches(Regex(".*(electricity|water|gas|internet|broadband|jio|airtel|vi|bsnl|mobile recharge).*")) ->
                TransactionCategory.UTILITIES to null
            desc.matches(Regex(".*(amazon|flipkart|myntra|shopping|mall|store|purchase|retail).*")) ->
                TransactionCategory.SHOPPING to null
            desc.matches(Regex(".*(salary|wages|income|credit|received|deposit).*")) && 
                desc.matches(Regex(".*(credited|received|deposit).*")) ->
                TransactionCategory.SALARY to null
            desc.matches(Regex(".*(transfer|neft|imps|rtgs|upi).*")) ->
                TransactionCategory.TRANSFER to null
            else -> TransactionCategory.OTHER to null
        }
    }

    private fun extractMerchant(description: String): String? {
        val merchants = listOf(
            "Zomato", "Swiggy", "Amazon", "Flipkart", "Netflix", "Spotify", "Uber", "Ola",
            "Paytm", "PhonePe", "GPay", "Google Pay", "HDFC", "SBI", "ICICI", "Axis",
            "Zerodha", "Groww", "Upstox", "Myntra", "BigBasket", "Blinkit", "Dunzo"
        )

        return merchants.find { description.contains(it, ignoreCase = true) }
    }

    private fun detectPaymentMethod(description: String): PaymentMethod? {
        val desc = description.lowercase()
        return when {
            desc.contains("upi") || desc.contains("@") -> PaymentMethod.UPI
            desc.contains("neft") -> PaymentMethod.NEFT
            desc.contains("imps") -> PaymentMethod.IMPS
            desc.contains("card") || desc.contains("atm") -> PaymentMethod.CARD
            desc.contains("cheque") || desc.contains("chq") -> PaymentMethod.CHEQUE
            desc.contains("cash") -> PaymentMethod.CASH
            else -> null
        }
    }

    private fun generateSummary(
        parsed: ParsedTransaction, 
        category: TransactionCategory, 
        merchant: String?
    ): String {
        val typeWord = if (parsed.type == TransactionType.DEBIT) "paid" else "received"
        val amount = "₹${String.format("%,.2f", parsed.amount)}"
        val to = merchant ?: "merchant"

        return when (category) {
            TransactionCategory.FOOD -> "You $typeWord $amount for food at $to"
            TransactionCategory.ENTERTAINMENT -> "You $typeWord $amount for entertainment"
            TransactionCategory.INVESTMENT -> "You invested $amount"
            TransactionCategory.SHOPPING -> "You $typeWord $amount shopping at $to"
            TransactionCategory.UTILITIES -> "You $typeWord $amount for utilities"
            TransactionCategory.SALARY -> "You received $amount as salary"
            else -> "You $typeWord $amount to $to"
        }
    }
}
