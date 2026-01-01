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
import android.util.Log
import android.widget.Toast

private const val TAG = "MainViewModel"

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    // Safe toast helper to prevent crashes during navigation
    private fun showSafeToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        try {
            val appContext = getApplication<Application>().applicationContext
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                try {
                    Toast.makeText(appContext, message, duration).show()
                } catch (e: Exception) {
                    Log.w(TAG, "Toast failed: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Toast helper failed: ${e.message}")
        }
    }
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
    
    // Debug messages for on-screen debugging
    private val _debugMessages = MutableStateFlow<List<String>>(emptyList())
    val debugMessages: StateFlow<List<String>> = _debugMessages.asStateFlow()
    
    private fun addDebugMessage(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
        _debugMessages.value = _debugMessages.value + "[$timestamp] $message"
        Log.d(TAG, message)
    }
    
    fun addDebugMessagePublic(message: String) {
        addDebugMessage(message)
    }
    
    fun clearDebugMessages() {
        _debugMessages.value = emptyList()
    }
    
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
    
    fun scanSmsMessages() {
        try {
            addDebugMessage("SMS: scanSmsMessages() called")
            val appContext = getApplication<Application>().applicationContext
            
            // Check permission first
            val hasPermission = android.content.pm.PackageManager.PERMISSION_GRANTED == 
                androidx.core.content.ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.READ_SMS)
            addDebugMessage("SMS: Permission check = $hasPermission")
            
            if (!hasPermission) {
                addDebugMessage("SMS ERROR: No SMS permission")
                _scanResult.value = "SMS permission required"
                showSafeToast("SMS permission not granted", Toast.LENGTH_LONG)
                return
            }
            
            viewModelScope.launch {
                addDebugMessage("SMS: Starting coroutine")
                showSafeToast("Starting SMS scan...")
                _isLoading.value = true
                _scanResult.value = "Scanning SMS messages..."
                
                // Add delay to let UI update
                kotlinx.coroutines.delay(1000)
                
                try {
                    addDebugMessage("SMS: Got app context")
                    
                    // First, read all SMS messages
                    val allSmsData = withContext(Dispatchers.IO) {
                        addDebugMessage("SMS: In IO dispatcher")
                        val smsUri = Telephony.Sms.CONTENT_URI
                        val projection = arrayOf(
                            Telephony.Sms._ID,
                            Telephony.Sms.ADDRESS,
                            Telephony.Sms.BODY,
                            Telephony.Sms.DATE
                        )

                        val cursor: Cursor? = try {
                            addDebugMessage("SMS: Querying SMS content provider")
                            appContext.contentResolver.query(
                                smsUri,
                                projection,
                                null,
                                null,
                                "${Telephony.Sms.DATE} DESC LIMIT 500"
                            )
                        } catch (e: SecurityException) {
                            addDebugMessage("SMS ERROR: SecurityException - ${e.message}")
                            null
                        } catch (e: Exception) {
                            addDebugMessage("SMS ERROR: Query exception - ${e.javaClass.simpleName}: ${e.message}")
                            null
                        }

                        val smsDataList = mutableListOf<Triple<String, String, Long>>()
                        cursor?.use {
                            addDebugMessage("SMS: Cursor count = ${it.count}")
                            val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
                            val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
                            val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)

                            if (addressIndex < 0 || bodyIndex < 0 || dateIndex < 0) {
                                addDebugMessage("SMS ERROR: Invalid column indices")
                                return@use
                            }

                            while (it.moveToNext()) {
                                try {
                                    val address = it.getString(addressIndex) ?: continue
                                    val body = it.getString(bodyIndex) ?: continue
                                    val date = it.getLong(dateIndex)
                                    smsDataList.add(Triple(address, body, date))
                                } catch (e: Exception) {
                                    // Skip this SMS
                                }
                            }
                        }
                        smsDataList
                    }
                    
                    addDebugMessage("SMS: Read ${allSmsData.size} SMS messages")
                    
                    // Process in batches - smaller for background stability
                    val batchSize = 25
                    val batches = allSmsData.chunked(batchSize)
                    var totalTransactions = 0
                    
                    for ((index, batch) in batches.withIndex()) {
                        addDebugMessage("SMS: Processing batch ${index + 1}/${batches.size}")
                        showSafeToast("Processing SMS batch ${index + 1}/${batches.size}...")
                        
                        val parsedTransactions = withContext(Dispatchers.IO) {
                            batch.mapNotNull { (address, body, date) ->
                                try {
                                    smsParser.parseTransactionSms(address, body, date)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        }
                        
                        if (parsedTransactions.isNotEmpty()) {
                            try {
                                withContext(Dispatchers.IO) {
                                    repository.insertTransactions(parsedTransactions)
                                }
                                totalTransactions += parsedTransactions.size
                                addDebugMessage("SMS: Saved ${parsedTransactions.size} from batch ${index + 1}")
                            } catch (e: Exception) {
                                addDebugMessage("SMS ERROR: DB insert failed batch ${index + 1}: ${e.message}")
                            }
                        }
                        
                        // Update scan result progressively
                        _scanResult.value = "Scanning SMS... saved $totalTransactions transactions so far"
                        
                        // Longer delay between batches for background stability
                        if (index < batches.size - 1) {
                            kotlinx.coroutines.delay(800)
                        }
                    }
                    
                    loadAnalytics()

                    addDebugMessage("SMS: Total transactions saved: $totalTransactions")
                    if (totalTransactions > 0) {
                        _scanResult.value = "Found $totalTransactions transaction SMS messages"
                        addDebugMessage("SMS SUCCESS: Saved $totalTransactions transactions")
                        showSafeToast("SUCCESS: Saved $totalTransactions transactions!", Toast.LENGTH_LONG)
                    } else {
                        _scanResult.value = "No transaction messages found"
                        addDebugMessage("SMS: No transaction SMS found")
                        showSafeToast("No transaction SMS found")
                    }
                } catch (e: Exception) {
                    addDebugMessage("SMS ERROR: Coroutine exception - ${e.javaClass.simpleName}: ${e.message}")
                    _scanResult.value = "Error scanning SMS: ${e.message ?: "Unknown error"}"
                    showSafeToast("ERROR: ${e.message}", Toast.LENGTH_LONG)
                }
                _isLoading.value = false
                addDebugMessage("SMS: Completed")
            }
        } catch (e: Exception) {
            addDebugMessage("SMS ERROR: Outer exception - ${e.javaClass.simpleName}: ${e.message}")
            _scanResult.value = "Error: ${e.message}"
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
    
    // Login functions - using AccountPicker (no OAuth required)
    fun getLoginSignInIntent(): Intent {
        addDebugMessage("LOGIN: getLoginSignInIntent() using AccountPicker")
        return try {
            val intent = android.accounts.AccountManager.newChooseAccountIntent(
                null,  // selectedAccount
                null,  // allowableAccounts
                arrayOf("com.google"),  // allowableAccountTypes - Google accounts only
                null,  // descriptionOverrideText
                null,  // addAccountAuthTokenType
                null,  // addAccountRequiredFeatures
                null   // addAccountOptions
            )
            addDebugMessage("LOGIN: AccountPicker intent created")
            intent
        } catch (e: Exception) {
            addDebugMessage("LOGIN ERROR: ${e.javaClass.simpleName}: ${e.message}")
            Intent()
        }
    }
    
    // Handle login from AccountPicker result
    fun handleLoginResult(accountName: String?, accountType: String?) {
        addDebugMessage("LOGIN: handleLoginResult - name=$accountName, type=$accountType")
        if (accountName != null && accountType == "com.google") {
            addDebugMessage("LOGIN: Valid Google account selected")
            login(accountName, accountName, null)
            addDebugMessage("LOGIN SUCCESS: Logged in as $accountName")
        } else {
            addDebugMessage("LOGIN ERROR: Invalid account - name=$accountName, type=$accountType")
        }
    }
    
    // Gmail functions
    fun checkGmailConnected(): Boolean {
        addDebugMessage("EMAIL: checkGmailConnected() called")
        val connected = gmailReader.isAuthenticated()
        addDebugMessage("EMAIL: isAuthenticated=$connected")
        _isGmailConnected.value = connected
        return connected
    }
    
    fun getGmailSignInIntent(): Intent {
        addDebugMessage("EMAIL: getGmailSignInIntent() using OAuth with Web Client ID")
        return try {
            val intent = gmailReader.getSignInIntent()
            addDebugMessage("EMAIL: OAuth sign-in intent created")
            intent
        } catch (e: Exception) {
            addDebugMessage("EMAIL ERROR: ${e.javaClass.simpleName}: ${e.message}")
            Intent()
        }
    }
    
    fun handleGmailSignInResult(account: GoogleSignInAccount?) {
        addDebugMessage("EMAIL: handleGmailSignInResult() called, account=${account?.email ?: "null"}")
        val appContext = getApplication<Application>().applicationContext
        viewModelScope.launch {
            try {
                showSafeToast("Email: Processing sign-in result...")
                addDebugMessage("EMAIL: Calling gmailReader.handleSignInResult")
                val success = try {
                    withContext(Dispatchers.IO) {
                        gmailReader.handleSignInResult(account)
                    }
                } catch (e: Exception) {
                    addDebugMessage("EMAIL ERROR: handleSignInResult exception: ${e.javaClass.simpleName}: ${e.message}")
                    false
                }
                addDebugMessage("EMAIL: handleSignInResult returned success=$success")
                if (success && account != null) {
                    addDebugMessage("EMAIL: Updating state - connected=true, email=${account.email}")
                    _isGmailConnected.value = true
                    try {
                        userSettingsDao.updateGmailStatus(true, account.email)
                    } catch (e: Exception) {
                        addDebugMessage("EMAIL ERROR: DB update failed: ${e.message}")
                    }
                    addDebugMessage("EMAIL SUCCESS: State updated successfully")
                    showSafeToast("SUCCESS: Connected to ${account.email}", Toast.LENGTH_LONG)
                    // Auto-scan emails after successful permission grant
                    addDebugMessage("EMAIL: Starting auto-scan after permission granted")
                    // Small delay to ensure Gmail service is ready
                    kotlinx.coroutines.delay(500)
                    scanGmailEmails()
                } else {
                    addDebugMessage("EMAIL ERROR: Failed - success=$success, account=${account?.email}")
                    showSafeToast("ERROR: Email sign-in failed", Toast.LENGTH_LONG)
                }
            } catch (e: Exception) {
                addDebugMessage("EMAIL ERROR: Unexpected error in handleGmailSignInResult: ${e.javaClass.simpleName}: ${e.message}")
                showSafeToast("Error: ${e.message}", Toast.LENGTH_LONG)
            }
        }
    }
    
    fun scanGmailEmails() {
        addDebugMessage("EMAIL SCAN: Starting email scan...")
        val appContext = getApplication<Application>().applicationContext
        
        viewModelScope.launch {
            try {
                addDebugMessage("EMAIL SCAN: Setting loading state")
                _isGmailScanning.value = true
                _isLoading.value = true
                
                showSafeToast("Starting email scan...")
                
                // Add delay to let UI update
                kotlinx.coroutines.delay(1000)
                
                addDebugMessage("EMAIL SCAN: Checking authentication")
                val isAuth = try {
                    gmailReader.isAuthenticated()
                } catch (e: Exception) {
                    addDebugMessage("EMAIL SCAN ERROR: Auth check failed: ${e.message}")
                    false
                }
                
                if (!isAuth) {
                    addDebugMessage("EMAIL SCAN ERROR: Not authenticated")
                    _scanResult.value = "Please connect Gmail first"
                    showSafeToast("Please connect Gmail first", Toast.LENGTH_LONG)
                    _isLoading.value = false
                    _isGmailScanning.value = false
                    return@launch
                }
                
                addDebugMessage("EMAIL SCAN: Auth OK, calling Gmail API...")
                showSafeToast("Fetching emails...")
                
                // Process emails in smaller batches to prevent ANR - run in background
                val batchSize = 5  // Smaller batches for stability
                val maxEmails = 50
                var totalTransactions = 0
                var totalEmails = 0
                
                val emails = try {
                    withContext(Dispatchers.IO) {
                        gmailReader.readTransactionEmails(maxEmails)
                    }
                } catch (e: Exception) {
                    addDebugMessage("EMAIL SCAN CRASH: ${e.javaClass.simpleName}: ${e.message}")
                    showSafeToast("Gmail API error: ${e.message}", Toast.LENGTH_LONG)
                    _scanResult.value = "Error reading emails: ${e.message}"
                    _isLoading.value = false
                    _isGmailScanning.value = false
                    return@launch
                }
                
                totalEmails = emails.size
                addDebugMessage("EMAIL SCAN: Got ${emails.size} emails, processing in batches of $batchSize")
                
                // Process in batches
                val batches = emails.chunked(batchSize)
                for ((index, batch) in batches.withIndex()) {
                    addDebugMessage("EMAIL SCAN: Processing batch ${index + 1}/${batches.size}")
                    showSafeToast("Processing email batch ${index + 1}/${batches.size}...")
                    
                    val parsedTransactions = try {
                        withContext(Dispatchers.IO) {
                            emailParser.parseEmails(batch)
                        }
                    } catch (e: Exception) {
                        addDebugMessage("EMAIL SCAN ERROR: Parse failed batch ${index + 1}: ${e.message}")
                        continue // Skip this batch and continue with next
                    }
                    
                    if (parsedTransactions.isNotEmpty()) {
                        try {
                            withContext(Dispatchers.IO) {
                                repository.insertTransactions(parsedTransactions)
                            }
                            totalTransactions += parsedTransactions.size
                            addDebugMessage("EMAIL SCAN: Saved ${parsedTransactions.size} from batch ${index + 1}")
                        } catch (e: Exception) {
                            addDebugMessage("EMAIL SCAN ERROR: DB insert failed batch ${index + 1}: ${e.message}")
                        }
                    }
                    
                    // Update scan result progressively
                    _scanResult.value = "Scanning... saved $totalTransactions transactions so far"
                    
                    // Longer delay between batches for background stability
                    if (index < batches.size - 1) {
                        kotlinx.coroutines.delay(1000)
                    }
                }
                
                loadAnalytics()
                
                if (totalTransactions > 0) {
                    _scanResult.value = "Found $totalTransactions transactions from $totalEmails emails"
                    addDebugMessage("EMAIL SCAN SUCCESS: Saved $totalTransactions transactions total")
                    showSafeToast("Found $totalTransactions transactions!", Toast.LENGTH_LONG)
                } else {
                    _scanResult.value = "No transaction emails found"
                    addDebugMessage("EMAIL SCAN: No transactions found in $totalEmails emails")
                    showSafeToast("No transaction emails found")
                }
            } catch (e: Exception) {
                addDebugMessage("EMAIL SCAN ERROR: ${e.javaClass.simpleName}: ${e.message}")
                _scanResult.value = "Error scanning emails: ${e.message ?: "Unknown error"}"
                showSafeToast("Error: ${e.message}", Toast.LENGTH_LONG)
            } finally {
                _isLoading.value = false
                _isGmailScanning.value = false
                addDebugMessage("EMAIL SCAN: Completed")
            }
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
