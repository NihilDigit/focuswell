import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.google.services)
  alias(libs.plugins.ksp)
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
val localReleaseSigningFile = rootProject.file("release-signing.properties")
val localReleaseSigning = Properties().apply {
    if (localReleaseSigningFile.isFile) {
        localReleaseSigningFile.inputStream().use(::load)
    }
}

fun releaseSigningValue(envName: String, propertyName: String): String? =
    System.getenv(envName)?.takeIf { it.isNotBlank() }
        ?: localReleaseSigning.getProperty(propertyName)?.takeIf { it.isNotBlank() }

val releaseStoreFile = releaseSigningValue("FOCUSWELL_RELEASE_STORE_FILE", "storeFile")
val releaseStorePassword = releaseSigningValue("FOCUSWELL_RELEASE_STORE_PASSWORD", "storePassword")
val releaseKeyAlias = releaseSigningValue("FOCUSWELL_RELEASE_KEY_ALIAS", "keyAlias")
val releaseKeyPassword = releaseSigningValue("FOCUSWELL_RELEASE_KEY_PASSWORD", "keyPassword")
val hasReleaseSigning =
  !releaseStoreFile.isNullOrBlank() &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

gradle.taskGraph.whenReady {
    val buildsRelease = allTasks.any { task ->
        task.name.contains("Release") &&
            (task.name.startsWith("assemble") || task.name.startsWith("package") || task.name.startsWith("install"))
    }
    if (buildsRelease && !hasReleaseSigning) {
        throw GradleException(
            "Release signing is required. Provide FOCUSWELL_RELEASE_* environment variables " +
                "or app/release-signing.properties with the same keystore used by GitHub Actions.",
        )
    }
}

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
            isMinifyEnabled = true
            isShrinkResources = true
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

ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
  arg("room.incremental", "true")
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
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  implementation(libs.kotlinx.datetime)
  implementation(libs.kotlinx.serialization.json)
  ksp(libs.androidx.room.compiler)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.material.color.utilities)
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
  androidTestImplementation(libs.androidx.room.testing)
  androidTestImplementation(libs.kotlinx.coroutines.test)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Remote reminders
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.messaging)
}
