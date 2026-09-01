package com.engagecraft.gaming.epllastmanstanding.web

import androidx.core.net.toUri
import com.engagecraft.gaming.epllastmanstanding.BuildConfig

/**
 * Static configuration for the remote web game. Values are the Android counterparts of the
 * iOS `GameConfig.swift` and are verified against the web app's own constants module.
 */
internal object GameConfig {

    const val GAME_ID = "epllastmanstanding"

    /** HTTPS is deliberate: the host 301-redirects plain HTTP. */
    const val BASE_URL = "https://lms.uat-dreamteamfc.com/"

    /** Must match `protocolVersion` in the web app's `app/lib/native/messages.ts`. */
    const val PROTOCOL_VERSION = 1

    /** The name the web app looks for under `window.webkit.messageHandlers`. */
    const val MESSAGE_HANDLER_NAME = "ghbridge"

    /** The bridge object the web app installs on `window` for native -> web delivery. */
    const val WEB_BRIDGE_OBJECT = "__ghBridge"

    /**
     * The Kotlin `@JavascriptInterface` is registered under a private name and the WebKit-shaped
     * API is recreated on top of it by [WEBKIT_SHIM_SCRIPT], so the web app needs no changes.
     */
    const val NATIVE_INTERFACE_NAME = "__ghBridgeNative"

    /** Appended to, never replacing, the stock WebView user agent. */
    const val USER_AGENT_SUFFIX = "DreamTeamNative/1.0 (android)"

    /**
     * Shared secret authorising `POST /api/auth/native-session`. Reaches the web app only through
     * the bridge, so it never appears in a URL or in the document. Blank means absent.
     */
    val sessionKey: String? = BuildConfig.GH_NATIVE_SESSION_KEY?.takeIf { it.isNotBlank() }

    val baseHost: String? = BASE_URL.toUri().host

    /** Origin the document-start shim is scoped to, so it is never injected into a foreign page. */
    val baseOrigin: String = BASE_URL.toUri().let { "${it.scheme}://${it.host}" }

    /**
     * `_sp_pass_consent` is only ever appended when the host genuinely holds consent: claiming
     * otherwise leaves the web CMP waiting forever for data that never arrives.
     */
    fun launchUrl(passConsent: Boolean): String {
        val builder = BASE_URL.toUri().buildUpon().appendQueryParameter("gh_native", "android")
        if (passConsent) {
            builder.appendQueryParameter("_sp_pass_consent", "true")
        }
        return builder.build().toString()
    }

    /**
     * Recreates `window.webkit.messageHandlers.ghbridge` on top of the Kotlin interface. The web
     * app posts a JS object; `addJavascriptInterface` can only marshal primitives, so the shim
     * stringifies before crossing the boundary.
     */
    val WEBKIT_SHIM_SCRIPT: String = """
        (function () {
          window.webkit = window.webkit || {};
          window.webkit.messageHandlers = window.webkit.messageHandlers || {};
          window.webkit.messageHandlers.$MESSAGE_HANDLER_NAME = {
            postMessage: function (msg) {
              window.$NATIVE_INTERFACE_NAME.postMessage(JSON.stringify(msg));
            }
          };
        })();
    """.trimIndent()
}
