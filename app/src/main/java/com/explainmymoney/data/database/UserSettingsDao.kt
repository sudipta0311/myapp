package com.explainmymoney.data.database

import androidx.room.*
import com.explainmymoney.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun getSettings(): Flow<UserSettings?>

    @Query("SELECT * FROM user_settings WHERE id = 1")
    suspend fun getSettingsOnce(): UserSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: UserSettings)

    @Query("UPDATE user_settings SET isLoggedIn = :isLoggedIn, displayName = :displayName, email = :email, profileImageUrl = :profileImageUrl WHERE id = 1")
    suspend fun updateLoginStatus(isLoggedIn: Boolean, displayName: String?, email: String?, profileImageUrl: String?)

    @Query("UPDATE user_settings SET countryCode = :countryCode, countryName = :countryName, currencyCode = :currencyCode, currencySymbol = :currencySymbol WHERE id = 1")
    suspend fun updateCountryCurrency(countryCode: String, countryName: String, currencyCode: String, currencySymbol: String)

    @Query("UPDATE user_settings SET isLoggedIn = 0, displayName = NULL, email = NULL, profileImageUrl = NULL WHERE id = 1")
    suspend fun logout()

    @Query("UPDATE user_settings SET slmEnabled = :enabled WHERE id = 1")
    suspend fun updateSlmEnabled(enabled: Boolean)

    @Query("UPDATE user_settings SET slmModelDownloaded = :downloaded, slmModelPath = :modelPath WHERE id = 1")
    suspend fun updateSlmModelStatus(downloaded: Boolean, modelPath: String?)

    @Query("UPDATE user_settings SET gmailConnected = :connected, gmailEmail = :email WHERE id = 1")
    suspend fun updateGmailStatus(connected: Boolean, email: String?)
}
