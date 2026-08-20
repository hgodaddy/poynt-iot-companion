pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "poynt-iot-companion"

include(":shared")
include(":companion-app")

// Production Cloud Messaging is a git submodule at foundation/poynt-cloudmessaging.
// Phase 1 does not compile against it yet. After clone + mapping, include specific
// reusable modules here instead of the whole production app.
val foundationSettings = file("foundation/poynt-cloudmessaging/settings.gradle")
val foundationSettingsKts = file("foundation/poynt-cloudmessaging/settings.gradle.kts")
if (foundationSettings.exists() || foundationSettingsKts.exists()) {
    gradle.settingsEvaluated {
        println("Foundation present at foundation/poynt-cloudmessaging — map modules before include().")
    }
}
