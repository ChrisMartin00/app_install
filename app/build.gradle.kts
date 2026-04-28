plugins {
    id("com.android.application")
}

android {
    namespace = "com.christmartin.appinstall"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.christmartin.appinstall"
        minSdk = 19
        targetSdk = 19
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
}
