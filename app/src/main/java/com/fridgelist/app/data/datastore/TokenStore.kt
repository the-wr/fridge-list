package com.fridgelist.app.data.datastore

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.fridgelist.app.data.model.ProviderType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "fridge_list_tokens",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveTokens(provider: ProviderType, accessToken: String, refreshToken: String?) {
        prefs.edit()
            .putString("${provider.name}_access", accessToken)
            .apply {
                if (refreshToken != null) putString("${provider.name}_refresh", refreshToken)
                else remove("${provider.name}_refresh")
            }
            .apply()
    }

    fun getAccessToken(provider: ProviderType): String? =
        prefs.getString("${provider.name}_access", null)

    fun getRefreshToken(provider: ProviderType): String? =
        prefs.getString("${provider.name}_refresh", null)

    fun clearTokens(provider: ProviderType) {
        prefs.edit()
            .remove("${provider.name}_access")
            .remove("${provider.name}_refresh")
            .apply()
    }
}
