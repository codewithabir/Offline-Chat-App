plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.offlinechat"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.offlinechat"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Android Components API ব্যবহার করে APK নাম OnAir.apk করা (Android Studio Recommended)
androidComponents {
    onVariants { variant ->
        val mainOutput = variant.outputs.singleOrNull()
        mainOutput?.outputFileName?.set("OnAir.apk")
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
    implementation("com.github.bumptech.glide:glide:4.16.0")
}