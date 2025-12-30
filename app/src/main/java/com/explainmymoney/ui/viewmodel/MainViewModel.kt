package com.explainmymoney.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.explainmymoney.data.database.AppDatabase
import com.explainmymoney.data.gmail.GmailReader
import com.explainmymoney.data.parser.EmailParser
import com.explainmymoney.data.parser.SmsParser
import com.explainmymoney.data.parser.StatementParser
import com.explainmymoney.data.repository.BudgetRepository
import com.explainmymoney.data.repository.TransactionRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.explainmymoney.domain.assistant.FinanceAssistant
import com.explainmymoney.domain.model.Budget
import com.explainmymoney.domain.model.BudgetWithSpending
import com.explainmymoney.domain.model.Country
import com.explainmymoney.domain.model.Transaction
import com.explainmymoney.domain.model.TransactionCategory
import com.explainmymoney.domain.model.UserSettings
import com.explainmymoney.domain.slm.DeviceCapability
import com.explainmymoney.domain.slm.SlmDownloadState
import com.explainmymoney.domain.slm.SlmManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val transactionDao = database.transactionDao()
    private val userSettingsDao = database.userSettingsDao()
    private val budgetDao = database.budgetDao()
    
    val repository = TransactionRepository(transactionDao)
    val budgetRepository = BudgetRepository(budgetDao, transactionDao)
    private val smsParser = SmsParser()
    private val statementParser = StatementParser(application)
    val slmManager = SlmManager(application)
    val gmailReader = GmailReader(application)
    private val emailParser = EmailParser()
    private val financeAssistant = FinanceAssistant()
    
    val transactions: StateFlow<List<Transaction>> = repository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val investmentTransactions: StateFlow<List<Transaction>> = repository.getInvestmentTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    private val _userSettings = MutableStateFlow<UserSettings?>(null)
    val userSettings: StateFlow<UserSettings?> = _userSettings.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _scanResult = MutableStateFlow<String?>(null)
    val scanResult: StateFlow<String?> = _scanResult.asStateFlow()
    
    private val _totalSpentThisMonth = MutableStateFlow(0.0)
    val totalSpentThisMonth: StateFlow<Double> = _totalSpentThisMonth.asStateFlow()
    
    private val _totalIncomeThisMonth = MutableStateFlow(0.0)
    val totalIncomeThisMonth: StateFlow<Double> = _totalIncomeThisMonth.asStateFlow()
    
    private val _categoryBreakdown = MutableStateFlow<Map<TransactionCategory, Double>>(emptyMap())
    val categoryBreakdown: StateFlow<Map<TransactionCategory, Double>> = _categoryBreakdown.asStateFlow()
    
    private val _slmDownloadProgress = MutableStateFlow(0f)
    val slmDownloadProgress: StateFlow<Float> = _slmDownloadProgress.asStateFlow()
    
    val slmDownloadState: StateFlow<SlmDownloadState> = slmManager.downloadState
    val slmIsReady: StateFlow<Boolean> = slmManager.isModelReady
    
    private val _isGmailScanning = MutableStateFlow(false)
    val isGmailScanning: StateFlow<Boolean> = _isGmailScanning.asStateFlow()
    
    private val _isGmailConnected = MutableStateFlow(false)
    val isGmailConnected: StateFlow<Boolean> = _isGmailConnected.asStateFlow()
    
    private val _totalSpentThisYear = MutableStateFlow(0.0)
    val totalSpentThisYear: StateFlow<Double> = _totalSpentThisYear.asStateFlow()
    
    private val _totalIncomeThisYear = MutableStateFlow(0.0)
    val totalIncomeThisYear: StateFlow<Double> = _totalIncomeThisYear.asStateFlow()
    
    private val _totalInvestedThisYear = MutableStateFlow(0.0)
    val totalInvestedThisYear: StateFlow<Double> = _totalInvestedThisYear.asStateFlow()
    
    val budgetsWithSpending: StateFlow<List<BudgetWithSpending>> = budgetRepository
        .getCurrentMonthBudgetsWithSpending()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    private val _pendingStatementTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val pendingStatementTransactions: StateFlow<List<Transaction>> = _pendingStatementTransactions.asStateFlow()
    
    private val _pendingStatementFileName = MutableStateFlow<String?>(null)
    val pendingStatementFileName: StateFlow<String?> = _pendingStatementFileName.asStateFlow()
    
    init {
        loadUserSettings()
        loadAnalytics()
        loadYearlyAnalytics()
        // Initialize Gmail connection state
        _isGmailConnected.value = gmailReader.isAuthenticated()
    }
    
    private fun loadUserSettings() {
        viewModelScope.launch {
            userSettingsDao.getSettings().collect { settings ->
                if (settings == null) {
                    val defaultSettings = UserSettings()
                    userSettingsDao.saveSettings(defaultSettings)
                    _userSettings.value = defaultSettings
                } else {
                    _userSettings.value = settings
                }
            }
        }
    }
    
    fun loadAnalytics() {
        viewModelScope.launch {
            _totalSpentThisMonth.value = repository.getTotalSpentThisMonth()
            _totalIncomeThisMonth.value = repository.getTotalIncomeThisMonth()
            _categoryBreakdown.value = repository.getCategoryBreakdown()
        }
    }
    
    private fun loadYearlyAnalytics() {
        viewModelScope.launch {
            _totalSpentThisYear.value = repository.getTotalSpentThisYear()
            _totalIncomeThisYear.value = repository.getTotalIncomeThisYear()
            _totalInvestedThisYear.value = repository.getTotalInvestedThisYear()
        }
    }
    
    fun updateCountry(country: Country) {
        viewModelScope.launch {
            userSettingsDao.updateCountryCurrency(
                country.code,
                country.name,
                country.currencyCode,
                country.currencySymbol
            )
        }
    }
    
    fun login(displayName: String, email: String, profileImageUrl: String?) {
        viewModelScope.launch {
            userSettingsDao.updateLoginStatus(true, displayName, email, profileImageUrl)
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            userSettingsDao.logout()
        }
    }
    
    fun insertTransactions(transactions: List<Transaction>) {
        viewModelScope.launch {
            repository.insertTransactions(transactions)
            loadAnalytics()
            loadYearlyAnalytics()
        }
    }
    
    fun insertTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.insertTransaction(transaction)
            loadAnalytics()
            loadYearlyAnalytics()
            _scanResult.value = "Transaction added successfully"
        }
    }
    
    fun insertTransactionsWithDeduplication(transactions: List<Transaction>) {
        viewModelScope.launch {
            val insertedCount = repository.insertTransactionsWithDeduplication(transactions)
            loadAnalytics()
            loadYearlyAnalytics()
            _scanResult.value = "Imported $insertedCount new transactions (${transactions.size - insertedCount} duplicates skipped)"
        }
    }
    
    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
            loadAnalytics()
            loadYearlyAnalytics()
        }
    }
    
    fun clearScanResult() {
        _scanResult.value = null
    }
    
    // Budget functions
    fun addBudget(category: TransactionCategory, monthlyLimit: Double) {
        viewModelScope.launch {
            val budget = Budget(
                category = category,
                monthlyLimit = monthlyLimit,
                yearMonth = BudgetRepository.getCurrentYearMonth()
            )
            budgetRepository.insertBudget(budget)
        }
    }
    
    fun updateBudget(budget: Budget) {
        viewModelScope.launch {
            budgetRepository.updateBudget(budget)
        }
    }
    
    fun deleteBudget(budgetId: Long) {
        viewModelScope.launch {
            budgetRepository.deleteBudget(budgetId)
        }
    }
    
    // Statement preview functions
    fun parseStatementForPreview(uri: Uri, fileName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val parsed = withContext(Dispatchers.IO) {
                    statementParser.parseFile(uri)
                }
                _pendingStatementTransactions.value = parsed
                _pendingStatementFileName.value = fileName
            } catch (e: Exception) {
                _scanResult.value = "Error parsing file: ${e.message}"
                _pendingStatementTransactions.value = emptyList()
                _pendingStatementFileName.value = null
            }
            _isLoading.value = false
        }
    }
    
    fun confirmStatementImport(selectedTransactions: List<Transaction>) {
        viewModelScope.launch {
            _isLoading.value = true
            val insertedCount = repository.insertTransactionsWithDeduplication(selectedTransactions)
            loadAnalytics()
            loadYearlyAnalytics()
            _scanResult.value = "Imported $insertedCount transactions"
            clearPendingStatement()
            _isLoading.value = false
        }
    }
    
    fun clearPendingStatement() {
        _pendingStatementTransactions.value = emptyList()
        _pendingStatementFileName.value = null
    }
    
    // Enhanced assistant response
    fun generateAssistantResponse(query: String): String {
        val intent = financeAssistant.detectIntent(query)
        return financeAssistant.generateResponse(intent, transactions.value, getCurrencySymbol())
    }
    
    fun scanSmsMessages(context: Context, hasPermission: Boolean) {
        if (!hasPermission) {
            _scanResult.value = "SMS permission required"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val parsedTransactions = withContext(Dispatchers.IO) {
                    val smsUri = Telephony.Sms.CONTENT_URI
                    val projection = arrayOf(
                        Telephony.Sms._ID,
                        Telephony.Sms.ADDRESS,
                        Telephony.Sms.BODY,
                        Telephony.Sms.DATE
                    )

                    val cursor: Cursor? = try {
                        context.contentResolver.query(
                            smsUri,
                            projection,
                            null,
                            null,
                            "${Telephony.Sms.DATE} DESC LIMIT 500"
                        )
                    } catch (e: SecurityException) {
                        null
                    }

                    val transactions = mutableListOf<Transaction>()
                    cursor?.use {
                        val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
                        val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
                        val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)

                        if (addressIndex < 0 || bodyIndex < 0 || dateIndex < 0) {
                            return@use
                        }

                        while (it.moveToNext()) {
                            val address = it.getString(addressIndex) ?: continue
                            val body = it.getString(bodyIndex) ?: continue
                            val date = it.getLong(dateIndex)

                            smsParser.parseTransactionSms(address, body, date)?.let { tx ->
                                transactions.add(tx)
                            }
                        }
                    }
                    transactions
                }

                if (parsedTransactions.isNotEmpty()) {
                    repository.insertTransactions(parsedTransactions)
                    loadAnalytics()
                    _scanResult.value = "Found ${parsedTransactions.size} transaction SMS messages"
                } else {
                    _scanResult.value = "No transaction messages found"
                }
            } catch (e: Exception) {
                _scanResult.value = "Error scanning SMS: ${e.message ?: "Unknown error"}"
            }
            _isLoading.value = false
        }
    }
    
    fun parseStatementFile(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val parsed = withContext(Dispatchers.IO) {
                    statementParser.parseFile(uri)
                }
                if (parsed.isNotEmpty()) {
                    repository.insertTransactions(parsed)
                    loadAnalytics()
                    _scanResult.value = "Imported ${parsed.size} transactions from statement"
                } else {
                    _scanResult.value = "No transactions found in file"
                }
            } catch (e: Exception) {
                _scanResult.value = "Error parsing file: ${e.message}"
            }
            _isLoading.value = false
        }
    }
    
    fun getCurrencySymbol(): String {
        return _userSettings.value?.currencySymbol ?: "₹"
    }
    
    fun checkSlmCapability(): DeviceCapability {
        return slmManager.checkDeviceCapability()
    }
    
    fun isSlmModelDownloaded(): Boolean {
        return slmManager.isModelDownloaded()
    }
    
    fun toggleSlmEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userSettingsDao.updateSlmEnabled(enabled)
            
            if (enabled && slmManager.isModelDownloaded()) {
                slmManager.initializeModel()
            } else if (!enabled) {
                slmManager.close()
            }
        }
    }
    
    fun downloadSlmModel() {
        viewModelScope.launch {
            val result = slmManager.downloadModel { progress ->
                _slmDownloadProgress.value = progress
            }
            
            result.onSuccess { modelPath ->
                userSettingsDao.updateSlmModelStatus(true, modelPath)
                
                if (_userSettings.value?.slmEnabled == true) {
                    slmManager.initializeModel()
                }
            }
        }
    }
    
    fun deleteSlmModel() {
        viewModelScope.launch {
            slmManager.deleteModel()
            userSettingsDao.updateSlmModelStatus(false, null)
            userSettingsDao.updateSlmEnabled(false)
        }
    }
    
    suspend fun generateSlmResponse(query: String): String {
        val inference = slmManager.getInference()
        return if (inference != null && _userSettings.value?.slmEnabled == true) {
            inference.generateResponse(query, transactions.value, getCurrencySymbol())
        } else {
            ""
        }
    }
    
    fun isSlmEnabled(): Boolean {
        return _userSettings.value?.slmEnabled == true && slmManager.isModelReady.value
    }
    
    // Permission functions
    fun hasSmsPermission(): Boolean {
        return android.content.pm.PackageManager.PERMISSION_GRANTED == 
            getApplication<Application>().checkSelfPermission(android.Manifest.permission.READ_SMS)
    }
    
    // Login functions
    fun getLoginSignInIntent(): Intent {
        return try {
            val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
            )
                .requestEmail()
                .requestProfile()
                .build()
            com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(getApplication(), gso).signInIntent
        } catch (e: Exception) {
            Intent()
        }
    }
    
    // Gmail functions
    fun checkGmailConnected(): Boolean {
        val connected = gmailReader.isAuthenticated()
        _isGmailConnected.value = connected
        return connected
    }
    
    fun getGmailSignInIntent() = gmailReader.getSignInIntent()
    
    fun handleGmailSignInResult(account: GoogleSignInAccount?) {
        viewModelScope.launch {
            val success = gmailReader.handleSignInResult(account)
            if (success && account != null) {
                _isGmailConnected.value = true
                userSettingsDao.updateGmailStatus(true, account.email)
            }
        }
    }
    
    fun scanGmailEmails() {
        viewModelScope.launch {
            _isGmailScanning.value = true
            _isLoading.value = true
            try {
                val emails = gmailReader.readTransactionEmails(50)
                val parsedTransactions = emailParser.parseEmails(emails)
                
                if (parsedTransactions.isNotEmpty()) {
                    repository.insertTransactions(parsedTransactions)
                    loadAnalytics()
                    _scanResult.value = "Found ${parsedTransactions.size} transactions from ${emails.size} emails"
                } else {
                    _scanResult.value = "No transaction emails found"
                }
            } catch (e: Exception) {
                _scanResult.value = "Error scanning emails: ${e.message ?: "Unknown error"}"
            }
            _isLoading.value = false
            _isGmailScanning.value = false
        }
    }
    
    fun disconnectGmail() {
        viewModelScope.launch {
            gmailReader.signOut()
            userSettingsDao.updateGmailStatus(false, null)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        slmManager.close()
    }
}
