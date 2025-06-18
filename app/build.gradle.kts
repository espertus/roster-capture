plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.navigation.safe.args)
    alias(libs.plugins.google.protobuf)
}

android {
    namespace = "com.ellenspertus.qroster"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.ellenspertus.qroster"
        minSdk = 34
        //noinspection OldTargetApi
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = false
        buildConfig = true
        viewBinding = true
    }
}

// Protobuf configuration
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.24.4"
    }

    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.anki.android)
    implementation(libs.datastore)
    implementation(libs.fragment.ktx)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.material)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.protobuf.javalite)
}
