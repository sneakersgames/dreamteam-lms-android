# DreamTeam Game Development  Android

Gaming Core Lib latest version: **2026.08.00**

To be able to start developing game you have to provide list of GitHub usernames to EngageCraft team. Then you need to create personal access token (classic) for your GitHub accounts.

More info can be found here: [https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens#creating-a-personal-access-token-classic](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens#creating-a-personal-access-token-classic)

It is recommended to start developing game using our preconfigured StarterKit (POC) Android project.

---



## Terminology

1. **Host App** - any app that can integrate *Gaming Core Library* and load the *Game library*.
2. **Gaming Core Library** - a library that manages interaction between *Game library* and the *Host App*.
3. **gameId** - every game on Gaming system must have an unique game id (e.g. `eplpredictor`).
4. **Game library** - Android library that contains *Game* (and optionally *Card*) components and is distributed on *Maven* repository.
5. **Game component** - main component on *Game library* that renders the game.
6. **Card component (Featured Card)** - an optional component that may be used on *Host App *as an entry point to the game.

---



## 1. Using StarterKit (POC) project

1. Clone StarterKit (POC) project:

```
git@github.com:WL-Gaming/starterkit-android.git
```

1. Remove origin as you will not be able to push the chages to this repository.
2. Open it on AndroidStudio and initialize the game using Gradle task ( **do not forget to specify your game id!**):

```
./gradlew initgame --gameId=mygameid --publishingUrl=https://maven.pkg.github.com/WL-Gaming/packages-android-dt
```

```
`mygameid` - is game id assigned to the game (it will be used for published game library to the provided publishing url)
```

1. Sync gradle and the POC project will be ready for use.
2. Check **README.md** for details how to publish game for release or CI builds.



## 2. General information

*Gaming Core Lib* is distributed as Gradle dependency through Maven repositories. As mentioned, game is also a library that should also be distributed through Maven repository.

Game library:

1. must be named as *gameId*
2. must not expose any components except for *Game* or *Card* compoenents (all other functions, classes, interfaces and etc. must be *internal* or *private*)
3. must have all the resources (drawables, strings, plurals, xmls and etc.) defined as private and they all must be prefixed with *gameId*.

All these points are done on POC project initiaziation you just need to follow the pattern once you start developing.

## 3. Developing the game



### 3.1. Environment information

*Gaming Core Lib* provides some usefull information that can be used on the *Game library*.

```
// to get environement
GamingConfig.config.env // GamingEnv.INT | GamingEnv.PRE | GamingEnv.PROD
// to get app language
GamingLocale.getLanguage() // e.g. en, de and etc.
```



### 3.2. User information

*Gaming Core Library* provides Gaming user obejct and game must listen for user obejct changes. User object change means:

1. If user id is not changed - some data on user oject has achged (chaged username).
2. If user id is changed  - user logged out ir logged in. Most definatelly game must be fully reloaded.

In some very rare situations user obejct may be *null*. It can happed if user has no internet connection and has never opened the app before. In this case we can say that user is not logged in.

Example:

```
val user by GamingAuthManager.getUser().observeAsState()

if (user.isLoggedIn()) {
   // user is logged in
   // user?.id - Gaming user id
   // user?.refId - Authentication system user id
   // user?.username - Gaming username
   // ...
} else {
   // user is logged out
}
```

To authorize game backed with *Gaming* backed you can use provided user token:

```
val token by GamingAuthManager.getToken().observeAsState()
// token?.accessToken
```



### 3.3. Login/Registration

Game can ask to start login/registration flow.

```
// to start login flow
Gaming.login(gameId: String)

// to start registration flow
Gaming.login(gameId: String)
```

There are no callbacks for these actions. Game must always observe user object and in case it changes (`user?.id` changes) it should act accordingly.

### 3.4. Game component

Game library must have *Game* component. It is the component that should render the game.

*Game* component is full-screen component so it must take into account window insets!

Do not remove *@Keep* annotation. Otherwise *Host App* will not be able to load the game.

```
@Keep
@Composable
fun Game(data: Bundle? = null) {
    ...
}
```

Where:

1. `data` - an optional data Bundle. It may contain:
  1. `data?.getBundle(Gaming.PROP_DATA)` - will contant data that is passed from the Card (if any) or data passed from push notification
  2. `data?.getString(Gaming.PROP_GAME_ID)` - gameId to load
  3. `data?.getBundle(Gaming.PROP_DATA)?.getString(Gaming.PROP_LINK)` - the actual deep-link if game is opened from the deep-link



### 3.5. Card component

Game library can have an **optional** *Card* compomnent. This card may be used on Homefeed and act as an entry point to the game. It can also reflect game status, e.g. reminder for the user to do some actions show live/total points and etc.

*Card* component does not have any height or width constraints. It should try to fill the width and to wrap height.

Do not remove *@Keep* annotation. Otherwise *Host App* will not be able to load the card.

```
@Keep
@Composable
fun Card(data: Bundle? = null) {
    ...
}
```

Where:

1. `data` - an optional data Bundle. It may contain:
  1. `data?.getBundle(Gaming.PROP_DATA)` - custom data that may be passed from the backed to the card (in case it needs to have some remote configuration)



### 3.6. Game requests



#### 3.6.1. Open game

If game library provides *Featured Game Card* and it does have CTA to open the game, *Card* cannot directly open the *Game*. It mus always ask *Host App* to open the *Game* using:

```
Gaming.open(gameId: String, data: Bnundle? = null)
```

Where:

1. `gameId` - gameId of the game requesting login
2. `data` - optional data *Bundle* object that will be passed to the Game (in case you want to pass some data from *Card* to *Game*)



#### 3.6.2. Close game

Game can ask *Host App* to close itself:

```
Gaming.close()
```



#### 3.6.3. Open link

When the game needs to open a link (it may be handled by the host app (deep-link) or it can be opened externally) it must always use *Gaming* interface using:

```
Gaming.openlink(url: String)
```



#### 3.6.4. Open menu

When the game is running it should provide a way to open host app menu. As game rendering *TopAppBar* it should include navigation action (hamburger menu). To open the menu use this:

```
Gaming.openMenu()
```



### 3.7. Actions



#### 3.7.1. Deeplinks

Deeplinks will follow this format:

`https://{host}/{2-letter-language}/{gameId}/{optional-game-segments}`

The whole link will be passsed to the game and game must handle it.

As mentioned before if the game is opened from the deep-link the actual deeplink will be passed on `data` parameter. But if the game is running it will not be recreated and an event will be fired instead:

```
val linkEvent by GamingEvent.onLink().asState()
LaunchedEffect(linkEvent) {
    linkEvent?.link?.let {
        // process link
    }
}
```



### 3.8. Updating Gaming Core Lib

From time to time a new version of Gaming Core Lib will be released. To updated Gaming Core Lib just bump the version on **settings.gradle.kts**:

```
    versionCatalogs {
        create("gaming") {
            from("com.engagecraft.gaming.core:catalog-shared:XXXX.XX.XX") // <-- update the version
        }
    }
```



### 3.9. Publishing Game library



#### 3.9.1. Publishing a new release (for release/productions builds)

Once you decide to release a new version do not forget to bump Game library version and then run this command:

```
./gradlew publish
```

Once it finishes provide a new version for EngageCraft team.

#### 3.9.1. Publishing a new snapshot (for CI builds)

You can publish a **SNAPSHOT** that can be will be automatically included on the next CI build. No need to change **SNAPSHOT** version, just run this command:

```
./gradlew -Psnapshot publish
```



## 4.  Additional information

Due to specific auth setup with TheSun authorization system, POC project needs to be modifier so it could be authorized with TheSun.

Open `app/build.gradle` file and make these adjustments:

1. Change applicationId:

```
//applicationId = "${project.group}.wl.adhoc"
applicationId = "com.dreamteam.adhoc"
```

and update auth0 setup.

```
manifestPlaceholders.putAll(mapOf(
    "auth0Domain" to "@string/gaming_core_ui_theme_dt_auth0_domain_pre",
    "auth0Scheme" to "@string/gaming_core_ui_theme_dt_auth0_scheme_pre",
))
```

 And always user `pre` build type. There is no setup for `debug` build type yet and `release` setup will not work for now.

## 5. Consent sharing with WebViews

`Gaming.setupWebView(it)` will not provide anything on POC project as Consent Manager is not integrated on POC. It will work on CI builds (UAT) only.

*Gaming Core Lib* provides interface that allows to access Consent Manager information on the WebView. The recommended way to setup it is on `WebViewClient.onPageFinished()` callback:

```
  webView.webViewClient = object: WebViewClient() {
      override fun onPageFinished(view: WebView?, url: String?) {
          view?.let { Gaming.setupWebView(it) }
      }
  }
```

