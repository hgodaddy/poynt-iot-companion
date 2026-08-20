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
        buildConfigField("boolean", "IOT_PROTOCOL_BOUND", "true")
        buildConfigField("String", "PRODUCTION_PACKAGE", "\"co.poynt.cloudmessaging\"")
        buildConfigField("String", "PRODUCTION_SERVICE", "\"co.poynt.cloudmessaging/.PcmService\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")
    api("software.amazon.awssdk.iotdevicesdk:aws-iot-device-sdk-android:1.27.4")
}
