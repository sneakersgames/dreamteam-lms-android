# `ghbridge` iOS to Android migration report

Port of the DreamTeam Last Man Standing native web-game bridge from the iOS POC
(`Sources/Classes/Web/*.swift`) to Kotlin/Compose in the `epllastmanstanding` library module.

## File mapping

| iOS | Android |
|---|---|
| `GameConfig.swift` | `web/GameConfig.kt` |
| `GHBridge.swift` | `web/GhBridge.kt` |
| `BridgePayloads.swift` | `web/BridgePayloads.kt` |
| `GameWebViewController.swift` | `web/GameWebView.kt` |
| `Game.swift` | `Game.kt` |
| `FeaturedCard.swift` | `Card.kt` |
| `GameBody` (DEBUG demo screen) | deliberately not ported |

Toolchain matches the starter kit unchanged: Gradle 8.13, AGP 8.13.1, Kotlin 2.2.20, Compose BOM
2025.10.00, minSdk 24, compileSdk/targetSdk 36, JVM 17. The gaming catalog is
`com.engagecraft.gaming.core:catalog-shared:2026.08.01`, which is newer than the `2026.08.00` in
the platform doc, so it was left alone. `androidx.webkit:webkit:1.14.0` was added to
`gradle/libs.versions.toml`; it is in neither the local nor the gaming catalog.

## 1. Transport: the shim, and why there was no choice

**Decision: register the Kotlin interface as `__ghBridgeNative` and inject a document-start shim
that recreates the WebKit-shaped API on top of it. No web-side change is required.**

This was not a judgement call. The web app's minified bundle
(`_next/static/chunks/281-70952de745d19734.js`) decides native mode like this:

```js
let s = window.webkit?.messageHandlers?.[i.$4] ?? null;   // i.$4 === "ghbridge"
...
return y ? { mode: "native", api: y } : window.ghComponent ? { mode: "web", api: window.ghComponent } : null;
```

There is no Android branch and no `window.ghbridge` path. The app only ever looks under
`window.webkit.messageHandlers`, and it posts a JS **object**, which `addJavascriptInterface`
cannot marshal. The shim both supplies the object the detection looks for and stringifies the
payload on the way across.

Two consequences worth knowing:

- **`gh_native` plays no part in native-mode detection.** Presence of the bridge object is the
  entire test (see §5).
- **The shim must land before hydration.** Detection runs inside a React effect that otherwise
  falls back to `mode: "web"`, and a page that falls back still renders and still appears to
  work, just permanently anonymous. `WebViewCompat.addDocumentStartJavaScript` is therefore used
  and is scoped to our origin. The `onPageStarted` fallback for devices without
  `DOCUMENT_START_SCRIPT` is genuinely racy; it logs a warning when taken.

The shim is scoped to the env-specific origin (`https://lms.dreamteamfc.com` on `PROD`,
`https://lms.uat-dreamteamfc.com` on `PRE` and `INT`) and the interface is removed on teardown,
since `addJavascriptInterface` otherwise exposes the object to every page in the WebView.

## 2. Verified Android core-lib symbol table

Taken from `com.engagecraft.gaming.core:lib:1.11.0` (`lib-1.11.0-pre.aar`), decompiled rather
than assumed. **Three entries contradict the platform doc.**

| iOS | Android | Notes |
|---|---|---|
| `GamingHubCards.environment.environment` | `GamingConfig.config.env` | `GamingEnv.{PROD,PRE,INT}`, a plain enum with no raw value |
| `GamingHubCards.environment.language` | `GamingLocale.getLanguage()` | |
| `GamingHubCards.environment.clientId` | `GamingConfig.config.clientId` | |
| `GamingHubCards.environment.appVersion` | `GamingConfig.config.appVersion` | |
| `GamingHubCards.user` | `GamingAuthManager.getUser()` | `LiveData<User>` |
| `GamingHubCards.isLoggedIn` | `User.isLoggedIn()` | extension in `model.UserKt` |
| `GamingHubCards.user.token?.token` | `GamingAuthManager.getToken()` | `LiveData<Token>`, `Token.accessToken` |
| `GamingHubCards.login(gameId)` | `Gaming.login(gameId)` | second `Activity` param is optional |
| `GamingHubCards.register(gameId)` | `Gaming.register(gameId)` | **exists** — the doc listing `login` twice was a typo |
| `GamingHubCards.logout()` | `Gaming.logout()` | **exists** — the doc omits it entirely |
| `GamingHubCards.openUefaProfile()` | `Gaming.openProfile()` | **exists** — no need to fall back to the menu |
| `GamingHubCards.openMenu()` | `Gaming.openMenu()` | |
| `GamingHubCards.openLink(url, gameId:)` | `Gaming.openLink(url)` | capital `L`; the doc writes `openlink` |
| `GamingHubCards.open(gameId, data:)` | `Gaming.open(gameId, data)` | |
| — | `Gaming.close()` | used for system back once the game has no screen to go back to |
| `.ghLoggedIn` / `.ghLoggedOut` | `GamingAuthManager.getUser()` / `getToken()` observation | |
| `.ghOpenLink` | `GamingEvent.onLink()` | `Link.link` |
| `webView.preloadConsent(from:)` | `Gaming.setupWebView(webView)` | delegates to the host's `GamingListener`; default is a no-op |

`Token` carries `expiresIn` as a duration, not an absolute instant, so `expirationDate` is read
from the JWT `exp` claim — the same source the core lib's own `Token.isExpired()` uses.

## 3. `env.get` fields that needed a fallback

Android's `GamingConfig.Config` exposes only `env`, `gamingApp`, `clientId`, `appVersion`,
`appLink` and `languages`. Four of the eleven keys have no counterpart. **The web team should
confirm these are acceptable**; the web app currently only reads `sessionKey`, `gameId`,
`environment` and `appId`, and only the first is functionally load-bearing.

| Key | Source | Status |
|---|---|---|
| `environment` | `GamingConfig.config.env.name.lowercase()` | derived — the Android enum has no raw value, so the lowercased name stands in for the iOS `rawValue`. Worth confirming the casing matches iOS. |
| `language` | `GamingLocale.getLanguage()` | real |
| `competition` | `GamingCompetition.EPL.slug` → `"premierleague"` | **fallback** — not on `GamingConfig` |
| `season` | `GamingCompetition.EPL.season` → `"2027"` | **fallback** — not on `GamingConfig`, and the lib's own value for EPL looks like a default branch rather than a real season |
| `timezone` | `TimeZone.getDefault().id` | **fallback** |
| `appId` | `context.packageName` | **fallback** — a library `BuildConfig` has no `APPLICATION_ID` |
| `clientId` | `GamingConfig.config.clientId` | real, but empty unless the host calls `GamingConfig.setup(...)`; the POC's `Gaming.init` does not |
| `appVersion` | `GamingConfig.config.appVersion`, falling back to `PackageManager` `versionName` | real with fallback, same caveat |
| `gameId` | constant | native addition |
| `sessionKey` | `BuildConfig.GH_NATIVE_SESSION_KEY` | native addition, `null` when unconfigured |
| `launchUrl` | cold-start deep link | native addition, `null` when absent |

## 4. Consent: the gap is total

**The Android core lib has no readable consent or TCF state of any kind.** A full-text search of
`lib-1.11.0-pre.aar` finds no reference to consent, SourcePoint, GDPR or `IABTCF`. There is no
`ConsentManager`, no SourcePoint dependency, and nothing reading `IABTCF_*` preferences.
`Gaming.setupWebView(webView)` is a one-line delegate to `GamingListener.setupWebView`, whose
default implementation does nothing, which matches the platform doc's note that it is inert on
the POC and only functions on CI/UAT builds.

Accordingly:

- `Gaming.setupWebView(view)` is wired into `onPageFinished`, so it starts working the moment the
  host provides a real listener.
- `consent.get` always replies `{"hasConsent": false}`. It always replies — an unanswered request
  is worse than a negative one, because the web side times out rather than simply treating
  consent as absent.
- `_sp_pass_consent=true` is never appended, because a false positive leaves the web CMP banner
  waiting forever for data that never arrives.
- `consent.changed` is never emitted, matching iOS, where the payload builder exists but is never
  used.

This is the largest functional difference between the two platforms. On iOS, `AdsConsentManager`
supplies a real `SPUserData` and `webView.preloadConsent(from:)` injects it.

## 5. For the web and backend teams

1. **`gh_native=android` needs nothing from you.** The server returns byte-identical HTML for
   `gh_native=android`, `=ios`, `=1` and absent, and the client never reads the parameter. We
   still send `gh_native=android` for parity and server-side logging. No whitelist change is
   required, contrary to the original concern.
2. **`NATIVE_SESSION_SECRET` must match the key we ship.** It reaches the web app only through
   `env.get` → `sessionKey`, and becomes the `x-gh-native-key` header on
   `POST /api/auth/native-session`. If it does not match, login completes natively but the web
   session is never created.
3. **`menu.open` sends three targets**, not one: `profile`, `editProfile` and `support`. Only
   `profile` has a host equivalent (`Gaming.openProfile()`); the other two fall back to the main
   menu, as they do on iOS. Tell us if either deserves its own destination.
4. **Please confirm the `environment` casing.** We send `"pre"` / `"prod"` / `"int"`; the Android
   enum has no raw value so this is inferred from the iOS convention.
5. **`competition` and `season` are locally derived** (`premierleague` / `2027`) because the
   Android core lib does not carry them. Confirm the values, or confirm they are unused.
6. **System back needs two new bridge messages.** The web app sends the one-way command
   `navigation.changed` with `{ "canGoBack": boolean }` whenever its in-memory router moves, and
   handles the event `navigation.back` by calling `navigate(-1)`. Both are additive, so
   `protocolVersion` stays at 1. Until a web build with them is deployed, back on Android closes
   the game from any screen.

## 6. Notable Android-specific decisions

- **`@JavascriptInterface` runs on a background thread.** `GhBridge.postMessage` hops to the main
  thread before anything touches the WebView or UI state.
- **`onCreateWindow` returns `true`, not `false`.** Returning `false` never yields a URL, which
  defeats the requirement to forward `target="_blank"` links to the host. The pop-up navigation
  is instead routed into a throwaway WebView purely to learn its URL, which is handed to
  `Gaming.openLink` before the throwaway is destroyed. No window is ever shown.
- **`hasGesture()` is weaker than iOS's `linkActivated`.** It is true for any user-initiated
  navigation, not only an anchor tap, so external-link interception is additionally gated on a
  host mismatch. Scripted same-origin navigation is unaffected. Worth re-checking if the web app
  ever starts navigating cross-origin from script during a gesture.
- **System back unwinds the game before closing it.** The web app locks its URL at `/` and routes
  in memory, so `webView.canGoBack()` cannot see its screens. The web side reports
  `navigation.changed { canGoBack }`; while that is true, back emits `navigation.back` and the web
  app calls `navigate(-1)`. Otherwise back falls through to `webView.goBack()` for any real
  document history, and only then to `Gaming.close()`. Against a web build without these
  messages, back closes the game as before.
- **Rotation does not recreate the WebView.** The host `GamingDevMainActivity` declares
  `configChanges="...|orientation|screenSize"`, so the Activity is not recreated and the
  `remember`ed host survives.
- **An R8 keep rule ships in `consumer-rules.pro`.** R8 cannot see the bridge's call sites, so
  without it a minifying host would rename the method away and every request would go unanswered.

## 7. Verification status

Built and linted clean: `./gradlew :app:assemblePre :epllastmanstanding:lintPre`.

The wire contract was verified offline against the web app's **own** minified code, by extracting
the shim from `GameConfig.kt` and running it against the detection expression, the type guards
from module 5432 and the user adapter from module 281. 24/24 checks pass, including:

- the shim puts the web app into `mode: "native"`;
- a posted JS object arrives natively as parseable JSON with `type`, `id` and `oneWay` intact;
- the success, failure and event envelopes satisfy the real `isReply` / `isEvent` guards;
- a signed-in payload yields an authenticated session with `user_id`, `access_token` and
  `favourites` populated;
- setting `anonymous: true` or dropping the token defeats the session, which is exactly why
  `anonymous` is computed as `!(isLoggedIn && hasNonEmptyToken)` instead of being forwarded.

**Not yet done: the on-device run.** No emulator image or physical device was available, so the
eleven runtime checks are outstanding. In priority order:

1. `chrome://inspect` → confirm `useGamingHub().isNative` is `true`. If the shim loses the race to
   hydration the app silently runs in web mode and everything else still looks fine.
2. `env.get`, `user.get` and `consent.get` each get exactly one reply; `env` carries all eleven keys.
3. Login: `user.changed` fires, `anonymous` is `false`, `userId` and `token.token` are present,
   and `POST /api/auth/native-session` returns 2xx (this is what proves the session key matches).
4. Logout, hamburger and web-sent `menu.open`, cold-start and warm deep links, external and
   `target="_blank"` links, kill-and-relaunch cookie survival, rotation, airplane-mode retry.
