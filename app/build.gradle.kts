import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(gaming.plugins.application)
    alias(gaming.plugins.kotlin)
    alias(gaming.plugins.compose)
    alias(gaming.plugins.ksp)
    alias(gaming.plugins.hilt)
}

val jvmVersion = JavaVersion.VERSION_17
val mainModuleName = "mygame"

android {
    namespace = "${project.group}.starterkit"

    compileSdk = gaming.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "${project.group}.wl.adhoc"

        minSdk = gaming.versions.minSdk.get().toInt()
        targetSdk = gaming.versions.targetSdk.get().toInt()

        versionCode = 1
        versionName = "1.0"

        resValue("string", "gaming_core_dev_title", rootProject.name)
        resValue("string", "gaming_core_dev_game_ids", mainModuleName)

        manifestPlaceholders.putAll(mapOf(
            "auth0Domain" to "@string/com_auth0_domain",
            "auth0Scheme" to "@string/com_auth0_scheme",
        ))

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("poc") {
            storeFile = file("keystore.jks")
            storePassword = "SndaYiANE177"
            keyAlias = "poc"
            keyPassword = "b4B4A3CUg2Hq"
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("poc")
        }
        release {
            initWith(buildTypes.getByName("debug"))
            isDebuggable = true
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("poc")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        create("pre") {
            initWith(buildTypes.getByName("debug"))
            isDebuggable = true
            signingConfig = signingConfigs.getByName("poc")
            matchingFallbacks.add("release")
        }
    }
    compileOptions {
        sourceCompatibility = jvmVersion
        targetCompatibility = jvmVersion
    }
    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.fromTarget(jvmVersion.toString())
            freeCompilerArgs.add("-Xannotation-default-target=param-property")
        }
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(gaming.compose))

    implementation(gaming.core)
    implementation(gaming.compose.foundation)

    implementation(gaming.hilt.android)
    ksp(gaming.hilt.compiler)

    gaming.versions.gaming.get()

    implementation(platform(gaming.gaming))
    implementation(gaming.gaming.dev)
    implementation(gaming.theme.core)
    //implementation("com.engagecraft.gaming.core:ui-shared-theme-todo")

    implementation(project(":$mainModuleName"))

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
