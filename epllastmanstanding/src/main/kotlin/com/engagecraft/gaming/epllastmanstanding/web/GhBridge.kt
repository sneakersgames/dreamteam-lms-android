package com.engagecraft.gaming.epllastmanstanding.web

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.engagecraft.gaming.core.lib.GamingUtil
import org.json.JSONObject

private const val LOG_TAG = "ghbridge"

internal fun bridgeLog(message: String) = GamingUtil.log("[$LOG_TAG] $message")

/**
 * A single inbound message from the web app.
 *
 * Only [type] is required; anything without one is not addressed to this protocol.
 */
internal class BridgeMessage private constructor(
    val version: Int,
    val id: String?,
    val type: String,
    val oneWay: Boolean,
    val gameId: String?,
    val payload: JSONObject?,
) {
    /** A request expects exactly one reply. Everything else is a command and expects none. */
    val expectsReply: Boolean get() = !oneWay && id != null

    companion object {
        fun parse(json: String): BridgeMessage? {
            val body = runCatching { JSONObject(json) }.getOrElse {
                bridgeLog("Discarding unparseable inbound message: $json")
                return null
            }
            val type = body.optString("type").takeIf { it.isNotEmpty() } ?: run {
                bridgeLog("Discarding inbound message with no type: $json")
                return null
            }
            return BridgeMessage(
                version = body.optInt("v", GameConfig.PROTOCOL_VERSION),
                id = if (body.isNull("id")) null else body.optString("id").takeIf { it.isNotEmpty() },
                type = type,
                oneWay = body.optBoolean("oneWay", false),
                gameId = if (body.isNull("gameId")) null else body.optString("gameId").takeIf { it.isNotEmpty() },
                payload = body.optJSONObject("payload"),
            )
        }
    }
}

/**
 * Inbound message parsing plus the outbound transport to `window.__ghBridge.receive`.
 *
 * The web app posts to `window.webkit.messageHandlers.ghbridge`, which is recreated on top of
 * [GameConfig.NATIVE_INTERFACE_NAME] by [GameConfig.WEBKIT_SHIM_SCRIPT].
 */
internal class GhBridge(private val onMessage: (BridgeMessage) -> Unit) {

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var webView: WebView? = null

    fun attach(webView: WebView) {
        this.webView = webView
    }

    fun detach() {
        webView = null
    }

    /** Success reply: `{"id": <echoed>, "ok": true, "payload": <any|null>}`. */
    fun reply(message: BridgeMessage, payload: Any?) {
        val id = message.id ?: return
        send(
            JSONObject()
                .put("id", id)
                .put("ok", true)
                .put("payload", payload ?: JSONObject.NULL),
        )
    }

    /** Failure reply: `{"id": <echoed>, "ok": false, "error": "<string>"}`. */
    fun fail(message: BridgeMessage, error: String) {
        val id = message.id ?: return
        send(
            JSONObject()
                .put("id", id)
                .put("ok", false)
                .put("error", error),
        )
    }

    /** Event: `{"type": "<name>", "payload": <any|null>}`. */
    fun emit(type: String, payload: Any?) {
        send(
            JSONObject()
                .put("type", type)
                .put("payload", payload ?: JSONObject.NULL),
        )
    }

    private fun send(body: JSONObject) {
        val json = body.toString()
        // JSONObject.quote produces a correctly escaped JS string literal, including the U+2028
        // and U+2029 cases that are legal JSON but terminate a JS string.
        val script =
            "window.${GameConfig.WEB_BRIDGE_OBJECT} && " +
                "window.${GameConfig.WEB_BRIDGE_OBJECT}.receive(${JSONObject.quote(json)})"
        onMain {
            // The bridge object only exists once the web app's first client code has run, so its
            // absence is normal rather than an error.
            webView?.evaluateJavascript(script, null)
        }
    }

    /**
     * Entry point for the web app. Invoked on a background WebView thread, so everything is
     * moved to the main thread before any UI or WebView state is touched.
     */
    @JavascriptInterface
    fun postMessage(json: String) {
        onMain { dispatch(json) }
    }

    private fun dispatch(json: String) {
        val message = BridgeMessage.parse(json) ?: return

        if (message.gameId != null && message.gameId != GameConfig.GAME_ID) {
            bridgeLog("Message addressed to gameId ${message.gameId}, handling anyway")
        }

        // Every request must get a reply, even an error one: an unanswered request makes the web
        // side time out and permanently degrade to an anonymous session.
        runCatching { onMessage(message) }.onFailure { error ->
            bridgeLog("Handler for ${message.type} threw: $error")
            if (message.expectsReply) {
                fail(message, error.message ?: error.toString())
            }
        }
    }

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }
}
