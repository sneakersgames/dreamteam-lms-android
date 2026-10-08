package com.engagecraft.gaming.epllastmanstanding.web

import android.annotation.SuppressLint
import android.content.Context
import android.os.Message
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.engagecraft.gaming.core.lib.Gaming
import com.engagecraft.gaming.core.lib.GamingConfig
import com.engagecraft.gaming.core.lib.model.Token
import com.engagecraft.gaming.core.lib.model.User
import com.engagecraft.gaming.epllastmanstanding.R

/**
 * Renders the game, or the retry state when the document failed to load.
 *
 * The WebView instance itself lives in [host] and is deliberately not recreated here.
 */
@Composable
internal fun GameWebView(host: GameWebViewHost, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        AndroidView(
            factory = { host.webView },
            modifier = Modifier.fillMaxSize(),
        )
        if (host.hasError) {
            GameErrorState(
                onRetry = host::load,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun GameErrorState(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                text = stringResource(R.string.epllastmanstanding_error_message),
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(onClick = onRetry) {
                Text(stringResource(R.string.epllastmanstanding_error_retry))
            }
        }
    }
}

/**
 * Owns the WebView that hosts the remote game and wires it to [GhBridge].
 *
 * Held across recomposition and configuration changes so the document -- and with it the
 * session -- survives rotation.
 */
internal class GameWebViewHost(context: Context, private val launchUrl: String?) {

    /** Drives the Compose error state; set on main-frame load failures only. */
    var hasError by mutableStateOf(false)
        private set

    /**
     * Set by `app.ready`. Tracked for diagnostics only: the web side queues and replays events
     * received before it is ready, so emission is deliberately not gated on this.
     */
    private var isBridgeReady = false

    /**
     * Set by `navigation.changed`. The web app routes in memory with the URL locked at `/`, so
     * its history is invisible to [WebView.canGoBack] and has to be reported over the bridge.
     */
    private var webCanGoBack = false

    private var currentUser: User? = null
    private var currentToken: Token? = null
    private var destroyed = false

    private val bridge = GhBridge(::handleMessage)

    private val diagnostics = TouchDiagnostics.isEnabled(context)

    @SuppressLint("SetJavaScriptEnabled")
    val webView: WebView = GameWebViewView(context, diagnostics).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.userAgentString = "${settings.userAgentString} ${GameConfig.USER_AGENT_SUFFIX}"
        // Required for onCreateWindow to fire for target="_blank".
        settings.setSupportMultipleWindows(true)
        settings.javaScriptCanOpenWindowsAutomatically = true

        webViewClient = GameWebViewClient()
        webChromeClient = GameWebChromeClient()

        // The interface is exposed to every page in this WebView, so only our own origin is ever
        // loaded and the interface is removed again on teardown.
        addJavascriptInterface(bridge, GameConfig.NATIVE_INTERFACE_NAME)
    }

    init {
        // A non-ephemeral cookie store is what makes the first paint after a relaunch signed in.
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }
        if (diagnostics) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
        bridge.attach(webView)
        installShim()
    }

    /**
     * The web app decides it is in native mode by looking for
     * `window.webkit.messageHandlers.ghbridge`, and it does so from a React effect that
     * otherwise falls back to web mode. The shim therefore has to be in place before the app's
     * own scripts run.
     */
    private fun installShim() {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addDocumentStartJavaScript(
                webView,
                GameConfig.WEBKIT_SHIM_SCRIPT,
                setOf(GameConfig.baseOrigin),
            )
            if (diagnostics) {
                WebViewCompat.addDocumentStartJavaScript(
                    webView,
                    TouchDiagnostics.SCRIPT,
                    setOf(GameConfig.baseOrigin),
                )
            }
        } else {
            bridgeLog(
                "DOCUMENT_START_SCRIPT unsupported; falling back to onPageStarted injection, " +
                    "which races the web app's own scripts and may leave it in web mode",
            )
        }
    }

    private val needsPageStartShim: Boolean
        get() = !WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)

    fun load() {
        hasError = false
        bridgeLog("Loading ${GameConfig.baseUrl} for env ${GamingConfig.config.env}")
        // Consent is never held on this build -- see BridgePayloads.consent.
        webView.loadUrl(GameConfig.launchUrl(passConsent = false))
    }

    fun onAuthChanged(user: User?, token: Token?) {
        currentUser = user
        currentToken = token
        bridge.emit("user.changed", BridgePayloads.user(user, token))
    }

    /** Warm deep links are only forwarded when the link is addressed to this game. */
    fun onDeepLink(url: String) {
        if (!url.contains(GameConfig.GAME_ID)) return
        bridge.emit("deeplink", org.json.JSONObject().put("url", url))
    }

    /**
     * Steps back through the web app's in-memory router first, then through document history.
     * Returns false when there is nowhere left to go, so the caller can close the game.
     */
    fun goBack(): Boolean = when {
        webCanGoBack -> {
            bridge.emit("navigation.back", null)
            true
        }
        webView.canGoBack() -> {
            webView.goBack()
            true
        }
        else -> false
    }

    fun destroy() {
        if (destroyed) return
        destroyed = true
        bridge.detach()
        webView.removeJavascriptInterface(GameConfig.NATIVE_INTERFACE_NAME)
        webView.stopLoading()
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.destroy()
        CookieManager.getInstance().flush()
    }

    private fun handleMessage(message: BridgeMessage) {
        when (message.type) {
            "env.get" ->
                bridge.reply(message, BridgePayloads.environment(webView.context, launchUrl))

            "user.get" ->
                bridge.reply(message, BridgePayloads.user(currentUser, currentToken))

            "consent.get" ->
                bridge.reply(message, BridgePayloads.consent())

            "app.ready" -> isBridgeReady = true

            "auth.login" -> Gaming.login(GameConfig.GAME_ID)

            "auth.register" -> Gaming.register(GameConfig.GAME_ID)

            "auth.logout" -> Gaming.logout()

            "menu.open" -> openMenu(message.payload?.optString("target"))

            "navigation.changed" ->
                webCanGoBack = message.payload?.optBoolean("canGoBack") == true

            else -> {
                bridgeLog("Unsupported message type: ${message.type}")
                if (message.expectsReply) {
                    bridge.fail(message, "Unsupported message type: ${message.type}")
                }
            }
        }
    }

    /**
     * The web app sends `profile`, `editProfile` and `support`. Only a profile screen exists on
     * the host, so everything else falls back to the menu, as on iOS.
     */
    private fun openMenu(target: String?) {
        when (target) {
            "profile" -> Gaming.openProfile()
            else -> Gaming.openMenu()
        }
    }

    private fun handleExternalLink(url: String) {
        bridgeLog("Handing link to host: $url")
        Gaming.openLink(url)
    }

    private inner class GameWebViewClient : WebViewClient() {

        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
            super.onPageStarted(view, url, favicon)
            // A fresh document means a fresh window.__ghBridge and a fresh in-memory router.
            isBridgeReady = false
            webCanGoBack = false
            if (needsPageStartShim) {
                view?.evaluateJavascript(GameConfig.WEBKIT_SHIM_SCRIPT, null)
                if (diagnostics) view?.evaluateJavascript(TouchDiagnostics.SCRIPT, null)
            }
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            view?.let { Gaming.setupWebView(it) }
        }

        /**
         * `hasGesture` is a weaker signal than iOS's `linkActivated`: it is true for any
         * navigation started from a user interaction, not only an anchor tap. Scripted
         * same-origin navigations are unaffected because of the host check.
         */
        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest,
        ): Boolean {
            if (!request.isForMainFrame || !request.hasGesture()) return false
            val host = request.url.host ?: return false
            if (host.equals(GameConfig.baseHost, ignoreCase = true)) return false
            handleExternalLink(request.url.toString())
            return true
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?,
        ) {
            super.onReceivedError(view, request, error)
            // Subresource failures are noise; only a failed main frame means no game.
            if (request?.isForMainFrame == true) {
                bridgeLog("Main frame load failed: ${error?.description}")
                hasError = true
            }
        }
    }

    private inner class GameWebChromeClient : WebChromeClient() {

        override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
            if (!diagnostics) return super.onConsoleMessage(consoleMessage)
            val message = consoleMessage.message()
            if (message.startsWith("[${TouchDiagnostics.LOG_TAG}]")) {
                TouchDiagnostics.log("web ${message.removePrefix("[${TouchDiagnostics.LOG_TAG}] ")}")
            } else {
                TouchDiagnostics.log(
                    "console ${consoleMessage.messageLevel()}: $message " +
                        "(${consoleMessage.sourceId()}:${consoleMessage.lineNumber()})",
                )
            }
            return true
        }

        /**
         * There is no window for a pop-up to open into, so the navigation is routed into a
         * throwaway WebView purely to learn its URL, which is then handed to the host.
         */
        override fun onCreateWindow(
            view: WebView,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message?,
        ): Boolean {
            val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
            val catcher = WebView(view.context).apply {
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        popup: WebView,
                        request: WebResourceRequest,
                    ): Boolean {
                        handleExternalLink(request.url.toString())
                        popup.post { popup.destroy() }
                        return true
                    }
                }
            }
            transport.webView = catcher
            resultMsg.sendToTarget()
            return true
        }
    }
}
