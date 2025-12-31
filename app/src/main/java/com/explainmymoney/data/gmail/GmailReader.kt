package com.explainmymoney.data.gmail

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.GmailScopes
import com.google.api.services.gmail.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

private const val TAG = "GmailReader"

data class EmailData(
    val id: String,
    val subject: String,
    val from: String,
    val snippet: String,
    val body: String,
    val date: Long
)

sealed class GmailAuthState {
    object NotAuthenticated : GmailAuthState()
    object Authenticating : GmailAuthState()
    data class Authenticated(val email: String) : GmailAuthState()
    data class Error(val message: String) : GmailAuthState()
}

class GmailReader(private val context: Context) {
    
    companion object {
        const val REQUEST_CODE_SIGN_IN = 1001
        private val GMAIL_SCOPE = Scope(GmailScopes.GMAIL_READONLY)
        // Web Client ID from Google Cloud Console
        private const val WEB_CLIENT_ID = "49844813522-7d5dqbh8dn7kpat4oqequ0sk7ucah2uo.apps.googleusercontent.com"
    }
    
    private var googleSignInClient: GoogleSignInClient? = null
    private var gmailService: Gmail? = null
    private var currentAccount: GoogleSignInAccount? = null
    
    fun isAuthenticated(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        val hasPermissions = account != null && GoogleSignIn.hasPermissions(account, GMAIL_SCOPE)
        Log.d(TAG, "isAuthenticated: account=${account?.email}, hasPermissions=$hasPermissions")
        return hasPermissions
    }
    
    fun getSignedInEmail(): String? {
        return GoogleSignIn.getLastSignedInAccount(context)?.email
    }
    
    fun getSignInIntent(): Intent {
        Log.d(TAG, "getSignInIntent() called with Web Client ID")
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .requestScopes(GMAIL_SCOPE)
            .build()
        
        googleSignInClient = GoogleSignIn.getClient(context, gso)
        Log.d(TAG, "getSignInIntent: Created sign-in client with Web Client ID")
        return googleSignInClient!!.signInIntent
    }
    
    fun handleSignInResult(account: GoogleSignInAccount?): Boolean {
        Log.d(TAG, "handleSignInResult: account=${account?.email}")
        if (account == null) {
            Log.e(TAG, "handleSignInResult: Account is null")
            return false
        }
        
        Log.d(TAG, "handleSignInResult: Setting current account and initializing Gmail service")
        currentAccount = account
        try {
            initializeGmailService(account)
            Log.d(TAG, "handleSignInResult: Gmail service initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "handleSignInResult: Error initializing Gmail service: ${e.message}", e)
            return false
        }
        return true
    }
    
    private fun initializeGmailService(account: GoogleSignInAccount) {
        Log.d(TAG, "initializeGmailService: account=${account.email}")
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(GmailScopes.GMAIL_READONLY)
        )
        credential.selectedAccount = account.account
        Log.d(TAG, "initializeGmailService: Credential created, selectedAccount=${account.account?.name}")
        
        gmailService = Gmail.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Explain My Money")
            .build()
        Log.d(TAG, "initializeGmailService: Gmail service built successfully")
    }
    
    suspend fun readTransactionEmails(maxResults: Int = 50): List<EmailData> = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account == null || gmailService == null) {
            if (account != null) {
                initializeGmailService(account)
            } else {
                return@withContext emptyList()
            }
        }
        
        try {
            val query = buildTransactionEmailQuery()
            
            val response = gmailService!!.users().messages()
                .list("me")
                .setQ(query)
                .setMaxResults(maxResults.toLong())
                .execute()
            
            val messages = response.messages ?: return@withContext emptyList()
            
            messages.mapNotNull { msg ->
                try {
                    val fullMsg = gmailService!!.users().messages()
                        .get("me", msg.id)
                        .setFormat("full")
                        .execute()
                    
                    parseEmail(fullMsg)
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun buildTransactionEmailQuery(): String {
        val bankKeywords = listOf(
            "transaction", "debit", "credit", "payment", "transfer",
            "UPI", "NEFT", "IMPS", "withdrawal", "deposit",
            "statement", "alert", "bank", "rupees", "INR",
            "debited", "credited", "spent", "received"
        )
        
        val bankSenders = listOf(
            "from:@hdfcbank.net",
            "from:@icicibank.com", 
            "from:@axisbank.com",
            "from:@sbi.co.in",
            "from:@kotak.com",
            "from:alerts@",
            "from:noreply@",
            "from:transactions@"
        )
        
        val keywordQuery = bankKeywords.joinToString(" OR ") { "subject:$it OR body:$it" }
        val senderQuery = bankSenders.joinToString(" OR ")
        
        return "($senderQuery) OR ($keywordQuery)"
    }
    
    private fun parseEmail(message: Message): EmailData {
        val headers = message.payload?.headers ?: emptyList()
        
        val subject = headers.find { it.name == "Subject" }?.value ?: ""
        val from = headers.find { it.name == "From" }?.value ?: ""
        val dateStr = headers.find { it.name == "Date" }?.value ?: ""
        
        val body = extractBody(message)
        val date = parseDate(dateStr)
        
        return EmailData(
            id = message.id,
            subject = subject,
            from = from,
            snippet = message.snippet ?: "",
            body = body,
            date = date
        )
    }
    
    private fun extractBody(message: Message): String {
        val payload = message.payload ?: return message.snippet ?: ""
        
        if (payload.body?.data != null) {
            return try {
                String(android.util.Base64.decode(payload.body.data, android.util.Base64.URL_SAFE))
            } catch (e: Exception) {
                message.snippet ?: ""
            }
        }
        
        val parts = payload.parts ?: return message.snippet ?: ""
        for (part in parts) {
            if (part.mimeType == "text/plain" && part.body?.data != null) {
                return try {
                    String(android.util.Base64.decode(part.body.data, android.util.Base64.URL_SAFE))
                } catch (e: Exception) {
                    continue
                }
            }
        }
        
        for (part in parts) {
            if (part.mimeType == "text/html" && part.body?.data != null) {
                return try {
                    val html = String(android.util.Base64.decode(part.body.data, android.util.Base64.URL_SAFE))
                    html.replace(Regex("<[^>]*>"), " ")
                        .replace(Regex("\\s+"), " ")
                        .trim()
                } catch (e: Exception) {
                    continue
                }
            }
        }
        
        return message.snippet ?: ""
    }
    
    private fun parseDate(dateStr: String): Long {
        return try {
            java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", java.util.Locale.ENGLISH)
                .parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
    
    suspend fun signOut() = withContext(Dispatchers.IO) {
        googleSignInClient?.signOut()
        gmailService = null
        currentAccount = null
    }
    
    suspend fun revokeAccess() = withContext(Dispatchers.IO) {
        googleSignInClient?.revokeAccess()
        gmailService = null
        currentAccount = null
    }
}
