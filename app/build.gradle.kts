val baseVersion = "0.0"

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "akihz.anlaki.dev"
    compileSdk = 36

    defaultConfig {
        applicationId = "akihz.anlaki.dev"
        minSdk = 30
        targetSdk = 34
        versionCode = (findProperty("versionCode") as? String)?.toIntOrNull() ?: 1
        versionName = (findProperty("versionName") as? String) ?: baseVersion
    }

    signingConfigs {
        val keystorePath = System.getenv("KEYSTORE_FILE")
        if (keystorePath != null && file(keystorePath).exists()) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
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
            if (signingConfigs.findByName("release") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
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

    val isCiBuild = project.hasProperty("ciBuild")
    applicationVariants.all {
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val versionName = versionName
            val versionCode = versionCode
            val buildType = buildType.name

            val newName = if (isCiBuild) {
                "app-release.apk"
            } else if (buildType == "debug") {
                "app-v${versionName}(${versionCode})-debug.apk"
            } else {
                "app-v${versionName}(${versionCode}).apk"
            }

            output.outputFileName = newName
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation("androidx.compose.material3:material3:1.4.0-alpha14")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation(libs.activity.compose)
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.core:core:1.12.0")
}
