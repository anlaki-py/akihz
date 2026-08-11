val baseVersion = "0.0"

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "akihz.anlaki.dev"
    compileSdk = 36

    defaultConfig {
        applicationId = "akihz.anlaki.dev"
        minSdk = 30
        targetSdk = 36
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

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            isUniversalApk = true
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

    lint {
        disable += setOf("ProtectedPermissions")
    }

    kapt {
        correctErrorTypes = true
    }

    val isCiBuild = project.hasProperty("ciBuild")
    applicationVariants.all {
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val versionName = versionName
            val versionCode = versionCode
            val buildType = buildType.name
            val abi = getFilter(com.android.build.OutputFile.ABI) ?: "universal"

            val newName = if (isCiBuild) {
                "app-$abi-$buildType.apk"
            } else if (buildType == "debug") {
                "app-v${versionName}(${versionCode})-$abi-debug.apk"
            } else {
                "app-v${versionName}(${versionCode})-$abi.apk"
            }

            output.outputFileName = newName
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.coroutines.android)
    implementation(libs.timber)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
