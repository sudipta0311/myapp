package com.explainmymoney.data.parser

import com.explainmymoney.domain.model.InvestmentType
import com.explainmymoney.domain.model.TransactionCategory

/**
 * Rule-based investment classification module.
 * No machine learning - purely deterministic rules for explainability.
 */
class InvestmentClassifier {
    
    data class ClassificationResult(
        val isInvestment: Boolean,
        val confidence: ConfidenceLevel,
        val investmentType: InvestmentType?,
        val reasons: List<String>
    )
    
    enum class ConfidenceLevel {
        NONE, LOW, MEDIUM, HIGH
    }
    
    // High confidence keywords - UMRN/Mandate related
    private val mandateKeywords = listOf(
        "UMRN", "NACH", "ECS", "MANDATE", "AUTODEBIT", "AUTO-DEBIT",
        "AUTOPAY", "AUTO-PAY", "STANDING INSTRUCTION", "SI-"
    )
    
    // High confidence - Clearing/Market infrastructure
    private val clearingCorpKeywords = listOf(
        "INDIAN CLEARING CORP", "ICCL", "BSE", "NSE", "MCX",
        "CAMS", "KFINTECH", "KARVY", "NSDL", "CDSL",
        "SEBI", "AMFI", "MFUTILITY", "MF UTILITY"
    )
    
    // Medium confidence - Investment specific keywords
    private val investmentKeywords = listOf(
        "SIP", "MUTUAL FUND", "MF", "NAV", "UNITS", "ISIN",
        "FOLIO", "ETF", "EQUITY", "STOCK", "SHARE", "DEMAT",
        "PPF", "NPS", "ELSS", "INDEX FUND", "DEBT FUND",
        "LIQUID FUND", "FLEXI CAP", "LARGE CAP", "MID CAP",
        "SMALL CAP", "HYBRID FUND", "BALANCED FUND",
        "SYSTEMATIC", "REDEMPTION", "DIVIDEND", "GROWTH OPTION",
        "DIRECT PLAN", "REGULAR PLAN"
    )
    
    // Known investment AMC/Platform names
    private val investmentPlatformKeywords = listOf(
        "GROWW", "ZERODHA", "UPSTOX", "PAYTM MONEY", "KUVERA",
        "ET MONEY", "COIN BY ZERODHA", "MIRAE ASSET", "HDFC AMC",
        "SBI MF", "ICICI PRUDENTIAL", "AXIS MF", "KOTAK MF",
        "NIPPON", "ADITYA BIRLA", "UTI MF", "TATA MF", "DSP",
        "FRANKLIN", "MOTILAL OSWAL", "EDELWEISS", "INVESCO",
        "PGIM", "CANARA ROBECO", "BANDHAN MF", "QUANT MF",
        "PARAG PARIKH", "PPFAS"
    )
    
    // PPF/NPS specific
    private val retirementKeywords = listOf(
        "PPF", "PUBLIC PROVIDENT", "NPS", "NATIONAL PENSION",
        "PENSION FUND", "PFRDA", "TIER I", "TIER II"
    )
    
    /**
     * Classify if a transaction text indicates an investment.
     * Returns classification result with confidence and reasons.
     */
    fun classify(text: String): ClassificationResult {
        val upperText = text.uppercase()
        val reasons = mutableListOf<String>()
        var confidenceScore = 0
        var detectedType: InvestmentType? = null
        
        // Check mandate keywords (high confidence)
        val foundMandateKeywords = mandateKeywords.filter { upperText.contains(it) }
        if (foundMandateKeywords.isNotEmpty()) {
            confidenceScore += 40
            reasons.add("Mandate/AutoDebit: ${foundMandateKeywords.joinToString(", ")}")
            detectedType = InvestmentType.SIP
        }
        
        // Check clearing corp keywords (high confidence)
        val foundClearingKeywords = clearingCorpKeywords.filter { upperText.contains(it) }
        if (foundClearingKeywords.isNotEmpty()) {
            confidenceScore += 35
            reasons.add("Market Infrastructure: ${foundClearingKeywords.joinToString(", ")}")
            if (detectedType == null) {
                detectedType = InvestmentType.MUTUAL_FUND
            }
        }
        
        // Check investment keywords (medium confidence)
        val foundInvestmentKeywords = investmentKeywords.filter { upperText.contains(it) }
        if (foundInvestmentKeywords.isNotEmpty()) {
            confidenceScore += 25
            reasons.add("Investment Terms: ${foundInvestmentKeywords.joinToString(", ")}")
            
            // Determine specific type
            if (detectedType == null) {
                detectedType = when {
                    foundInvestmentKeywords.any { it in listOf("SIP", "SYSTEMATIC") } -> InvestmentType.SIP
                    foundInvestmentKeywords.any { it in listOf("MUTUAL FUND", "MF", "NAV", "FOLIO", "UNITS") } -> InvestmentType.MUTUAL_FUND
                    foundInvestmentKeywords.any { it in listOf("STOCK", "SHARE", "EQUITY", "DEMAT") } -> InvestmentType.STOCKS
                    foundInvestmentKeywords.any { it in listOf("ETF", "INDEX FUND") } -> InvestmentType.MUTUAL_FUND
                    else -> InvestmentType.OTHER
                }
            }
        }
        
        // Check platform keywords (medium confidence)
        val foundPlatformKeywords = investmentPlatformKeywords.filter { upperText.contains(it) }
        if (foundPlatformKeywords.isNotEmpty()) {
            confidenceScore += 20
            reasons.add("Investment Platform: ${foundPlatformKeywords.joinToString(", ")}")
            if (detectedType == null) {
                detectedType = InvestmentType.MUTUAL_FUND
            }
        }
        
        // Check retirement keywords
        val foundRetirementKeywords = retirementKeywords.filter { upperText.contains(it) }
        if (foundRetirementKeywords.isNotEmpty()) {
            confidenceScore += 30
            reasons.add("Retirement Scheme: ${foundRetirementKeywords.joinToString(", ")}")
            detectedType = when {
                foundRetirementKeywords.any { it.contains("PPF") || it.contains("PROVIDENT") } -> InvestmentType.PPF
                foundRetirementKeywords.any { it.contains("NPS") || it.contains("PENSION") } -> InvestmentType.NPS
                else -> detectedType
            }
        }
        
        // Determine confidence level
        val confidenceLevel = when {
            confidenceScore >= 50 -> ConfidenceLevel.HIGH
            confidenceScore >= 30 -> ConfidenceLevel.MEDIUM
            confidenceScore >= 15 -> ConfidenceLevel.LOW
            else -> ConfidenceLevel.NONE
        }
        
        // Only classify as investment if confidence is at least MEDIUM
        val isInvestment = confidenceLevel >= ConfidenceLevel.MEDIUM
        
        return ClassificationResult(
            isInvestment = isInvestment,
            confidence = confidenceLevel,
            investmentType = if (isInvestment) (detectedType ?: InvestmentType.OTHER) else null,
            reasons = reasons
        )
    }
    
    /**
     * Quick check if text likely contains investment indicators.
     */
    fun isLikelyInvestment(text: String): Boolean {
        return classify(text).isInvestment
    }
    
    /**
     * Get the appropriate category based on classification.
     */
    fun getCategory(text: String): TransactionCategory? {
        val result = classify(text)
        return if (result.isInvestment) TransactionCategory.INVESTMENT else null
    }
}
