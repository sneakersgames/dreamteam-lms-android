# Gaming Starter Kit (POC) for game development for Android

This is a sample (POC) project to start developing a game for Gaming platform for Android.
You can use it as a base project for your game or use it just as en example.

1. [Start using starter kit](#1-start-using-starter-kit)
2. [Developing the game](#2-developing-the-game)
    1. [Components](#1-components)
    2. [Build variants (environments)](#2-build-variants-environments)
    3. [Publishing game library](#3-publishing-game-library)
        1. [Maven coordinates](#maven-coordinates)
        2. [Credentials](#credentials)
        3. [Bump the version](#bump-the-version)
        4. [Publish a release](#publish-a-release)
        5. [Publish a snapshot](#publish-a-snapshot)
        6. [Consume the library](#consume-the-library)
3. [DreamTeam Last Man Standing setup](#3-dreamteam-last-man-standing-setup)

***

## 1. Start using starter kit

1. Clone this repo.
2. **Remove the current origin** and add your own repo origin.
3. Setup access to the **EngageCraft Maven repositories** on your global Gradle properties (**systemProp**) or set them as ENV variables.

![gradle_properties.png](documentation/img/gradle_properties.png)

4. Initialize game project by running the following command (`gameId` and `publishingUrl` will be provided to you by EngageCraft team):
```
./gradlew initgame --gameId=mygame --publishingUrl=https://example.com/maven
```

> ❗ **IMPORTANT**
>
> Game namespace pattern: `com.engagecraft.gaming.__GAME_ID__`
>
> Every game **must** use this package namespace so we can dynamically load the game components on Gaming app or on Gaming developing tools.

***

## 2. Developing the game

ℹ️ [DEVELOPMENT DOCUMENTATION](https://example.com/Android+Game+development)

***

### 1. Components

Every game **must** have at least one public component. If the game is a full-featured game it must have `Game` component. If game must provide Featured Card component it must have `Card` component. They are two main entry points to the game.

Those two components can be a native views (extends View, FrameView or etc.) or they can be `@Composable` components (💡 **preferred**).

The starter kit provides sample `@Composable Game` and `@Composable Card` components.

> ❗ **IMPORTANT**
>
> All other game components, classes, constant etc. must be either `internal` or `private`. Game resources (drawables, strings, layout files and etc.) must have [prefix](https://developer.android.com/studio/projects/android-library#Considerations) (e.g. `gameid_`) and must be [private too](https://developer.android.com/studio/projects/android-library#PrivateResources). In general game library should not expose anything except for `Card` and/or `Game` components.
>
> 
> If the game project has more than one module (multiple game themes) and it does have a shared module it should use prefix too. As it cannot use gameId as a prefix it may be constructed like this: `gaming_fantasy`, or `gaming_fantasy_core` (gaming prefix followed by generic game name without competition prefix).

***

### 2. Build variants (environments)

There are 3 preconfigured build variants for the dev app:
1. `debug` - A build variant on **INT** environment. Intended for developing/testing purposes.
2. `pre` - A build variant on **PRE** environment. Intended for CI builds and testing before releasing a new version of the app.
3. `release` - A build variant on **PROD** environment. Intended for checking on production. ⚠️ **CAUTION** while using it.

You can change variant (environment) the using build variant selection on Android Studio:

![build_variants.png](documentation/img/build_variants.png)

***

### 3. Publishing game library

> ❗ **IMPORTANT**
>
> Every game must publish on a private EngageCraft Maven repo so it could be added to the Gaming app as a Gradle dependency.
>
> GitHub Packages **does not allow overwriting** a released version. Bump `game.version` before every release publish. Snapshots (`0.0.0-SNAPSHOT`) can be republished.

#### Maven coordinates

- **Group:** `com.engagecraft.gaming`
- **Artifact:** `epllastmanstanding`
- **Release version:** `game.version` in [`gradle.properties`](gradle.properties)
- **Snapshot version:** `0.0.0-SNAPSHOT` (fixed; do not bump)
- **Repository:** `https://maven.pkg.github.com/WL-Gaming/packages-android-dt`

Print the current coordinates:

```
./gradlew currentVersion
```

#### Credentials

Publishing uses the same GitHub Packages credentials as dependency resolution. Set them in **`~/.gradle/gradle.properties`** (preferred, so they are not committed) or as environment variables:

```properties
systemProp.gpr.wlgaming.usr=YOUR_GITHUB_USERNAME
systemProp.gpr.wlgaming.key=YOUR_GITHUB_PAT
```

```
export GPR_WLGAMING_USR=YOUR_GITHUB_USERNAME
export GPR_WLGAMING_KEY=YOUR_GITHUB_PAT
```

The PAT needs `read:packages` to resolve dependencies and `write:packages` to publish. It must have access to `WL-Gaming/packages-android` and `WL-Gaming/packages-android-dt`.

> ❗ Never commit the PAT. `local.properties` is not read for these credentials.

#### Bump the version

Release versions live in `gradle.properties` as `game.version` (semver `major.minor.patch`). Use a **separate** Gradle invocation from `publish` so the new value is picked up.

Patch (default), e.g. `0.0.1` → `0.0.2`:

```
./gradlew bumpVersion
```

Minor, e.g. `0.0.2` → `0.1.0`:

```
./gradlew bumpVersion -Ppart=minor
```

Major, e.g. `0.1.0` → `1.0.0`:

```
./gradlew bumpVersion -Ppart=major
```

Set an explicit version:

```
./gradlew bumpVersion -Pto=1.2.3
```

#### Publish a release

1. Bump `game.version` (see above). Do **not** combine this with `publish` in the same `./gradlew` command.
2. Publish the `:epllastmanstanding` AAR:

```
./gradlew bumpVersion
./gradlew publish
```

3. Send the new version (for example `0.0.2`) to the EngageCraft team so they can wire it into the Gaming app.

Typical first-time / next release sequence:

```
./gradlew currentVersion
./gradlew bumpVersion
./gradlew publish
```

#### Publish a snapshot

For CI / nightly builds we publish Maven snapshots. Snapshots do not require a version bump; the published version is always `0.0.0-SNAPSHOT` and can be overwritten.

```
./gradlew -Psnapshot publish
```

#### Consume the library

In the host app (or any consumer) add the DreamTeam packages repo and the game dependency:

```kotlin
maven {
    url = uri("https://maven.pkg.github.com/WL-Gaming/packages-android-dt")
    credentials {
        username = System.getProperty("gpr.wlgaming.usr") ?: System.getenv("GPR_WLGAMING_USR")
        password = System.getProperty("gpr.wlgaming.key") ?: System.getenv("GPR_WLGAMING_KEY")
    }
}

implementation("com.engagecraft.gaming:epllastmanstanding:0.0.1")
// or, for CI:
implementation("com.engagecraft.gaming:epllastmanstanding:0.0.0-SNAPSHOT")
```

***

## 3. DreamTeam Last Man Standing setup

This project hosts the DreamTeam **Last Man Standing** game. The game itself is not native: it
is a remote web app at `https://lms.uat-dreamteamfc.com/` running in a WebView. The Kotlin in
`epllastmanstanding/src/main/kotlin/.../web/` is only the `ghbridge` bridge that answers the web
app's questions about the user, environment and consent, and relays a few commands to the host.

See [MIGRATION.md](MIGRATION.md) for the porting notes, the verified core-lib symbol table and
the open items for the web and backend teams.

### local.properties

The bridge passes the web app a shared secret that authorises `POST /api/auth/native-session`.
Without it the web app cannot complete a login. It is read from `local.properties`, which is
gitignored, into `BuildConfig.GH_NATIVE_SESSION_KEY`:

```properties
# must equal the web server's NATIVE_SESSION_SECRET
gh.nativeSessionKey=<64-char secret>
```

`GH_NATIVE_SESSION_KEY` in the environment is used as a fallback, which is how CI should supply
it. A blank or missing value is treated as absent and sent to the web app as `null`; the build
still succeeds, but login will not work.

> ❗ Never commit the key, the GitHub PAT or any Auth0 secret.

### Build type

Build and run the **`pre`** variant only.

```
./gradlew :app:assemblePre
```

`debug` has no auth setup and `release` is not configured, so neither will sign in. The DreamTeam
Auth0 configuration is applied in `app/build.gradle.kts` via `applicationId = "com.dreamteam.adhoc"`
and the `gaming_core_ui_theme_dt_auth0_*_pre` manifest placeholders.

### Debugging the bridge

`WebView.setWebContentsDebuggingEnabled` is on for every non-release build, so the web app can be
inspected from `chrome://inspect`. Bridge activity is logged through `GamingUtil.log` under the
`[ghbridge]` tag.
