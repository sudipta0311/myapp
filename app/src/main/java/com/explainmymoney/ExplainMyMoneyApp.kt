package com.explainmymoney

import android.app.Application
import com.explainmymoney.data.database.AppDatabase

class ExplainMyMoneyApp : Application() {
    
    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }
    
    override fun onCreate() {
        super.onCreate()
    }
}
