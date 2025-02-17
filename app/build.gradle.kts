plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "matos.csu.group3"
    compileSdk = 35

    defaultConfig {
        applicationId = "matos.csu.group3"
        minSdk = 33
        this.targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildToolsVersion = "35.0.0"
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.room.common.jvm)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation (libs.room.runtime)
    annotationProcessor (libs.room.compiler)

    implementation (libs.glide)
    annotationProcessor (libs.compiler)

    implementation(libs.ucrop)
}