import java.util.Properties
import java.io.FileInputStream

// Function to load properties from local.properties
fun loadLocalProperties(): Properties {
    val properties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")

    if (localPropertiesFile.exists()) {
        FileInputStream(localPropertiesFile).use { inputStream ->
            properties.load(inputStream)
        }
    }
    return properties
}

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "matos.csu.group3"
    compileSdk = 35

    defaultConfig {
        applicationId = "matos.csu.group3"
        manifestPlaceholders["appAuthRedirectScheme"] = "matos.csu.group3"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Load local.properties manually
        val localProperties = loadLocalProperties()

        val googleClientId = localProperties.getProperty("GOOGLE_CLIENT_ID", "")

        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"$googleClientId\"")
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
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
    implementation(libs.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation (libs.room.runtime)
    annotationProcessor (libs.room.compiler)

    implementation (libs.glide)
    annotationProcessor (libs.compiler)

    implementation(libs.ucrop)
    implementation (libs.material.v161)
    implementation(libs.play.services.auth)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.preference.ktx)
    implementation(libs.appauth.v0111)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.flexbox)
    implementation(libs.work.runtime.ktx)
    implementation(libs.swiperefreshlayout)
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.libraries.places:places:3.3.0")

}