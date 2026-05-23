plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.google.services)
}

fun versionCodeFromVersionName(versionName: String): Int {
  val parts = versionName.split(".").mapNotNull { it.toIntOrNull() }
  if (parts.size >= 3) {
    return parts[0] * 10_000 + parts[1] * 100 + parts[2]
  }
  return 1
}

val focusWellVersionName =
  providers.gradleProperty("focuswellVersionName").orNull
    ?: System.getenv("GITHUB_REF_NAME")?.takeIf { it.matches(Regex("""\d+\.\d+\.\d+""")) }
    ?: "1.0"
val focusWellVersionCode =
  providers.gradleProperty("focuswellVersionCode").orNull?.toIntOrNull()
    ?: versionCodeFromVersionName(focusWellVersionName)
val releaseStoreFile = System.getenv("FOCUSWELL_RELEASE_STORE_FILE")
val releaseStorePassword = System.getenv("FOCUSWELL_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = System.getenv("FOCUSWELL_RELEASE_KEY_ALIAS")
val releaseKeyPassword = System.getenv("FOCUSWELL_RELEASE_KEY_PASSWORD")
val hasReleaseSigning =
  !releaseStoreFile.isNullOrBlank() &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

android {
    namespace = "dev.nihildigit.focuswell"
    compileSdk = 36
    defaultConfig {
        applicationId = "dev.nihildigit.focuswell"
        minSdk = 26
        targetSdk = 36
        versionCode = focusWellVersionCode
        versionName = focusWellVersionName
        buildConfigField("String", "FOCUSWELL_BACKEND_URL", "\"https://backend-seven-eosin-45.vercel.app\"")
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.fragment.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Remote reminders
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.messaging)
}
