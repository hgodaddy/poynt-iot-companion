plugins {
    id("com.android.library")
}

android {
    namespace = "co.poynt.iot.companion.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("boolean", "FOUNDATION_BOUND", "false")
        buildConfigField("String", "PRODUCTION_PACKAGE", "\"co.poynt.cloudmessaging\"")
        buildConfigField("String", "PRODUCTION_SERVICE", "\"co.poynt.cloudmessaging/.PcmService\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.annotation:annotation:1.9.1")
}
