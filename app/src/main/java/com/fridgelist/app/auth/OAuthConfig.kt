package com.fridgelist.app.auth

import android.net.Uri
import com.fridgelist.app.BuildConfig
import com.fridgelist.app.data.model.ProviderType
import net.openid.appauth.AuthorizationServiceConfiguration

data class OAuthConfig(
    val serviceConfig: AuthorizationServiceConfiguration,
    val clientId: String,
    /** Null for providers that use PKCE instead of a client secret. */
    val clientSecret: String?,
    val scopes: String,
    val redirectUri: Uri = REDIRECT_URI,
) {
    companion object {
        val REDIRECT_URI: Uri = Uri.parse("com.fridgelist.auth://callback")
    }
}

object OAuthConfigs {
    fun forProvider(provider: ProviderType): OAuthConfig = when (provider) {
        ProviderType.TODOIST -> OAuthConfig(
            serviceConfig = AuthorizationServiceConfiguration(
                Uri.parse("https://todoist.com/oauth/authorize"),
                Uri.parse("https://todoist.com/oauth/access_token"),
            ),
            clientId = BuildConfig.TODOIST_CLIENT_ID,
            // Todoist requires a client_secret; no PKCE support.
            clientSecret = BuildConfig.TODOIST_CLIENT_SECRET.takeIf { it.isNotEmpty() },
            scopes = "task:add data:read data:delete",
        )

        ProviderType.MICROSOFT_TODO -> OAuthConfig(
            serviceConfig = AuthorizationServiceConfiguration(
                Uri.parse("https://login.microsoftonline.com/common/oauth2/v2.0/authorize"),
                Uri.parse("https://login.microsoftonline.com/common/oauth2/v2.0/token"),
            ),
            clientId = BuildConfig.MICROSOFT_CLIENT_ID,
            // Public mobile client — uses PKCE, no secret.
            clientSecret = null,
            scopes = "Tasks.ReadWrite offline_access",
        )

        ProviderType.GOOGLE_TASKS -> OAuthConfig(
            serviceConfig = AuthorizationServiceConfiguration(
                Uri.parse("https://accounts.google.com/o/oauth2/v2/auth"),
                Uri.parse("https://oauth2.googleapis.com/token"),
            ),
            clientId = BuildConfig.GOOGLE_CLIENT_ID,
            // Installed-app / mobile client — uses PKCE, no secret.
            clientSecret = null,
            scopes = "https://www.googleapis.com/auth/tasks",
        )

        ProviderType.TICKTICK -> OAuthConfig(
            serviceConfig = AuthorizationServiceConfiguration(
                Uri.parse("https://ticktick.com/oauth/authorize"),
                Uri.parse("https://ticktick.com/oauth/token"),
            ),
            clientId = BuildConfig.TICKTICK_CLIENT_ID,
            clientSecret = BuildConfig.TICKTICK_CLIENT_SECRET.takeIf { it.isNotEmpty() },
            scopes = "tasks:read tasks:write",
        )
    }
}
