plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "akihz.anlaki.dev"
    compileSdk = 36

    defaultConfig {
        applicationId = "akihz.anlaki.dev"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget("17"))
            freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
        }
    }

    packaging {
        resources {
            excludes += setOf("META-INF/**", "kotlin/**")
        }
    }

    applicationVariants.all {
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val versionName = versionName
            val versionCode = versionCode
            val buildType = buildType.name
            
            val newName = if (buildType == "debug") {
                "app-v${versionName}(${versionCode})-debug.apk"
            } else {
                "app-v${versionName}(${versionCode}).apk"
            }
            
            output.outputFileName = newName
        }
    }
}

dependencies {
implementation("dev.rikka.shizuku:api:13.1.5")
implementation("dev.rikka.shizuku:provider:13.1.5")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
implementation("androidx.core:core:1.12.0")
}