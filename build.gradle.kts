plugins {
    alias(gaming.plugins.application) apply false
    alias(gaming.plugins.library) apply false
    alias(gaming.plugins.kotlin) apply false
    alias(gaming.plugins.serialization) apply false
    alias(gaming.plugins.compose) apply false
    alias(gaming.plugins.ksp) apply false
    alias(gaming.plugins.hilt) apply false
    alias(gaming.plugins.gaming.tools.initgame) apply true
}

allprojects {
    group = "com.engagecraft.gaming"
    version = providers.gradleProperty("game.version").get()

    repositories {
        configurations.configureEach {
            resolutionStrategy {
                // refresh SNAPSHOTS more frequently
                //cacheChangingModulesFor 60, 'seconds'
                // force snapshots for CI builds
                //force("com.engagecraft.gaming.core:bom:0.0.0-SNAPSHOT")
            }
        }
    }
}

fun parseSemVer(version: String): Triple<Int, Int, Int> {
    val parts = version.split(".")
    require(parts.size == 3 && parts.all { it.toIntOrNull() != null }) {
        "game.version must be major.minor.patch (got '$version')"
    }
    return Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
}

fun bumpSemVer(version: String, part: String): String {
    val (major, minor, patch) = parseSemVer(version)
    return when (part) {
        "major" -> "${major + 1}.0.0"
        "minor" -> "$major.${minor + 1}.0"
        "patch" -> "$major.$minor.${patch + 1}"
        else -> error("Unknown -Ppart='$part'. Use major, minor or patch.")
    }
}

fun writeGameVersion(newVersion: String) {
    parseSemVer(newVersion)
    val file = rootProject.file("gradle.properties")
    val text = file.readText()
    val pattern = Regex("""^game\.version=.*$""", RegexOption.MULTILINE)
    require(pattern.containsMatchIn(text)) { "game.version is missing from gradle.properties" }
    file.writeText(pattern.replace(text, "game.version=$newVersion"))
}

tasks.register("currentVersion") {
    group = "publishing"
    description = "Print the current game library Maven coordinates"
    doLast {
        val version = providers.gradleProperty("game.version").get()
        println("com.engagecraft.gaming:epllastmanstanding:$version")
        println("Snapshot: com.engagecraft.gaming:epllastmanstanding:0.0.0-SNAPSHOT")
        println("Repo: https://maven.pkg.github.com/WL-Gaming/packages-android-dt")
    }
}

tasks.register("bumpVersion") {
    group = "publishing"
    description = "Bump game.version (patch by default). Use -Ppart=major|minor|patch or -Pto=x.y.z"
    doLast {
        val current = providers.gradleProperty("game.version").get()
        val explicit = project.findProperty("to") as String?
        val part = (project.findProperty("part") as String?) ?: "patch"
        val next = explicit ?: bumpSemVer(current, part)
        writeGameVersion(next)
        println("Bumped version $current -> $next")
        println("Publish this version with a separate Gradle invocation:")
        println("  ./gradlew publish")
    }
}
