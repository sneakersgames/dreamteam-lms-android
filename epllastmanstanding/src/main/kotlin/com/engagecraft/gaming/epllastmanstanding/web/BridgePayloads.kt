package com.engagecraft.gaming.epllastmanstanding.web

import android.content.Context
import android.util.Base64
import com.engagecraft.gaming.core.lib.GamingCompetition
import com.engagecraft.gaming.core.lib.GamingConfig
import com.engagecraft.gaming.core.lib.GamingLocale
import com.engagecraft.gaming.core.lib.model.Token
import com.engagecraft.gaming.core.lib.model.User
import com.engagecraft.gaming.core.lib.model.isLoggedIn
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Builds the JSON payloads the web app expects.
 *
 * The key names are the web app's API and are shared with the iOS implementation, so they are
 * reproduced exactly -- including the `isMiniRegistartionCompleted` typo, which the web side
 * reads under that spelling.
 */
internal object BridgePayloads {

    private val iso8601: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }

    /**
     * Reply to `env.get`.
     *
     * `gameId`, `sessionKey` and `launchUrl` are native additions the core lib does not provide.
     * `competition`, `season`, `timezone` and `appId` have no counterpart on Android's
     * `GamingConfig.Config` and are derived locally -- see the migration report.
     */
    fun environment(context: Context, launchUrl: String?): JSONObject {
        val config = GamingConfig.config
        return JSONObject()
            .put("environment", config.env.name.lowercase(Locale.US))
            .put("language", GamingLocale.getLanguage())
            .put("competition", GamingCompetition.EPL.slug)
            .put("season", GamingCompetition.EPL.season)
            .put("timezone", TimeZone.getDefault().id)
            .put("appId", context.packageName)
            .put("clientId", config.clientId)
            .put("appVersion", config.appVersion.ifBlank { appVersionName(context) })
            .put("gameId", GameConfig.GAME_ID)
            .put("sessionKey", GameConfig.sessionKey ?: JSONObject.NULL)
            .put("launchUrl", launchUrl ?: JSONObject.NULL)
    }

    /**
     * Reply to `user.get` and payload for the `user.changed` event.
     *
     * Built field by field rather than through the core lib's serializer: the web adapter needs
     * both `userId` and `token.token` to treat the session as authenticated, and it refuses any
     * payload with `anonymous: true` even when `isLoggedIn` is true.
     */
    fun user(user: User?, token: Token?): JSONObject {
        val isLoggedIn = user?.isLoggedIn() == true
        val accessToken = token?.accessToken?.takeIf { it.isNotEmpty() }
        val hasToken = accessToken != null

        if (isLoggedIn && !hasToken) {
            bridgeLog("User is logged in but has no access token; web will fall back to anonymous")
        }

        // User.id is a non-null Int, so 0 stands in for "absent".
        var userId: Any = user?.id?.takeIf { it != 0 } ?: JSONObject.NULL
        if (userId == JSONObject.NULL) {
            if (isLoggedIn) {
                bridgeLog("User is logged in but has a null user id")
            }
            jwtSubject(accessToken)?.let { sub ->
                // Display fallback only; the server still verifies the token itself.
                userId = sub.toIntOrNull() ?: sub
            }
        }

        val encoded = JSONObject()
            .put("userId", userId)
            .put("uefaId", user?.refId ?: JSONObject.NULL)
            .put("username", user?.username ?: JSONObject.NULL)
            .put("anonymous", !(isLoggedIn && hasToken))
            .put("nextLevelXP", user?.nextLevelXp ?: JSONObject.NULL)
            .put("nextLevel", user?.nextLevel ?: JSONObject.NULL)
            .put("levelName", user?.levelName ?: JSONObject.NULL)
            .put("level", user?.level ?: JSONObject.NULL)
            .put("levelColor", user?.levelColor ?: JSONObject.NULL)
            .put("xp", user?.xp ?: JSONObject.NULL)
            .put("startingXP", user?.startingXp ?: JSONObject.NULL)
            .put("avatar", avatarPayload(user))
            .put("countryCode", user?.refCountryCode ?: JSONObject.NULL)
            .put("favouriteClub", user?.refFavClubName ?: JSONObject.NULL)
            .put("isFirstSeason", user?.let { it.isFirstSeason != 0 } ?: JSONObject.NULL)
            .put("isMiniRegistartionCompleted", user?.registrationCompleted ?: JSONObject.NULL)
            .put("token", tokenPayload(accessToken))

        return JSONObject()
            .put("isLoggedIn", isLoggedIn)
            .put("user", encoded)
    }

    /**
     * Reply to `consent.get`.
     *
     * The Android core lib exposes no readable consent or TCF state, so this is always a
     * negative answer. Replying negatively is required: an unanswered request is worse, because
     * the web side times out rather than simply treating consent as absent.
     */
    fun consent(): JSONObject = JSONObject().put("hasConsent", false)

    private fun avatarPayload(user: User?): Any {
        val avatar = user?.avatar ?: return JSONObject.NULL
        return JSONObject()
            .put("id", avatar.id)
            .put("url", avatar.url)
    }

    private fun tokenPayload(accessToken: String?): Any {
        if (accessToken == null) return JSONObject.NULL
        return JSONObject()
            .put("token", accessToken)
            .put("expirationDate", jwtExpiry(accessToken)?.let { iso8601.format(it) } ?: JSONObject.NULL)
    }

    private fun appVersionName(context: Context): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull().orEmpty()

    private fun jwtClaims(token: String?): JSONObject? {
        if (token == null) return null
        val parts = token.split(".")
        if (parts.size < 2) return null
        val decoded = runCatching {
            Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        }.getOrNull() ?: return null
        return runCatching { JSONObject(String(decoded, Charsets.UTF_8)) }.getOrNull()
    }

    /** The unverified `sub` claim, used only when the core lib omits the user id. */
    private fun jwtSubject(token: String?): String? {
        val claims = jwtClaims(token) ?: return null
        if (claims.isNull("sub")) return null
        return claims.optString("sub").takeIf { it.isNotEmpty() }
    }

    /**
     * The core lib's `Token` carries `expiresIn` as a duration rather than an absolute instant,
     * so the absolute expiry is read from the JWT `exp` claim -- the same source the core lib's
     * own `isExpired()` uses.
     */
    private fun jwtExpiry(token: String): Date? {
        val exp = jwtClaims(token)?.optLong("exp", 0L) ?: 0L
        return if (exp > 0L) Date(exp * 1000L) else null
    }
}
