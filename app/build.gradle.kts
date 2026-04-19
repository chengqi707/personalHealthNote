import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt")
}

android {
    namespace = "com.chengqi.personalhealthnote"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.chengqi.personalhealthnote"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 读取local.properties配置
        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(localPropertiesFile.inputStream())
        }

        // 注入配置到BuildConfig
        buildConfigField("String", "DOUBAO_API_KEY", "\"${properties.getProperty("DOUBAO_API_KEY", "")}\"")
        buildConfigField("String", "DOUBAO_API_URL", "\"${properties.getProperty("DOUBAO_API_URL", "")}\"")
        buildConfigField("String", "DOUBAO_MODEL", "\"${properties.getProperty("DOUBAO_MODEL", "")}\"")
        buildConfigField("String", "SERVER_BASE_URL", "\"${properties.getProperty("SERVER_BASE_URL", "")}\"")
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
    kotlinOptions {
        jvmTarget = "11"
    }

    // 启用 ViewBinding 和 BuildConfig
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.glide)
    kapt(libs.glide)
    implementation(libs.okhttp)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}