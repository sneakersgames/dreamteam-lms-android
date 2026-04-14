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
    version = "0.0.1"

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
