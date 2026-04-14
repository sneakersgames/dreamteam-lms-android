import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(gaming.plugins.library)
    alias(gaming.plugins.kotlin)
    alias(gaming.plugins.serialization)
    alias(gaming.plugins.compose)
    alias(gaming.plugins.ksp)
    id("maven-publish")
}

val jvmVersion = JavaVersion.VERSION_17

fun getProp(key: String): String = (project.findProperty(key) as? String)
    ?: System.getProperty(key)
    ?: System.getenv(key.replace(".", "_").uppercase())

android {
    // project name -> gameId -> code namespace
    namespace = "${project.group}.${project.name}"

    // project name -> gameId -> resource prefix
    resourcePrefix = "${project.name}_"

    compileSdk = gaming.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = gaming.versions.minSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        debug {

        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        create("pre") {

        }
    }
    publishing {
        singleVariant("release")
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
    testOptions {
        targetSdk = gaming.versions.targetSdk.get().toInt()
    }
    lint {
        targetSdk = gaming.versions.targetSdk.get().toInt()
    }
}

publishing {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/WL-Gaming/packages-android-todo")
            credentials {
                username = getProp("gpr.wlgaming.usr")
                password = getProp("gpr.wlgaming.key")
            }
        }
    }
    publications {
        register<MavenPublication>("release") {
            version = if (project.hasProperty("snapshot")) "0.0.0-SNAPSHOT" else (project.version as String)
            afterEvaluate {
                from(components.findByName("release"))
            }
        }
    }
}

dependencies {
    implementation(platform(gaming.compose))

    implementation(gaming.core)
    implementation(gaming.compose.material3)

    implementation(gaming.paging.compose)
    implementation(gaming.compose.livedata)
    implementation(gaming.viewmodel.compose)
    implementation(gaming.navigation.compose)
    implementation(gaming.hilt.compose)

    implementation(gaming.appcompat)
    implementation(gaming.activity.compose)

    debugImplementation(gaming.compose.tooling)
    implementation(gaming.compose.preview)

    implementation(gaming.okhttp.logging)
    implementation(gaming.retrofit.core)
    implementation(gaming.retrofit.serialization)
    implementation(gaming.serialization)

    ksp(gaming.room.compiler)
    implementation(gaming.room.runtime)
    implementation(gaming.room.ktx)

    implementation(platform(gaming.gaming))
    implementation(gaming.gaming.lib)
    implementation(gaming.gaming.components)
    implementation(gaming.gaming.editorial)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}