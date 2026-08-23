plugins {
  alias(libs.plugins.android.application)
  // AGP 9.x provides built-in Kotlin support; do NOT apply
  // org.jetbrains.kotlin.android here (it conflicts with built-in Kotlin).
  // The Compose compiler is likewise supplied automatically by AGP's
  // built-in Kotlin when buildFeatures.compose is enabled — the standalone
  // org.jetbrains.kotlin.plugin.compose plugin is intentionally NOT applied
  // because it breaks lintDebug/assembleDebug with AGP 9.1 built-in Kotlin.
  alias(libs.plugins.google.devtools.ksp)
  // Roborazzi 1.59 is incompatible with AGP 9.x; re-enable after it ships
  // an AGP 9 compatible release. The screenshot test is also disabled below.
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  // SHADOW_NET_ABIS lets CI (and local developers) target a single ABI.
  // When unset we fall back to the full device matrix used during development.
  val shadowNetAbis: List<String> = (System.getenv("SHADOW_NET_ABIS") ?: "")
    .split(',', ' ')
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .ifEmpty { listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64") }

  defaultConfig {
    applicationId = "com.aistudio.shadownet.qzkvtr"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    ndk { abiFilters.addAll(shadowNetAbis) }
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  sourceSets.getByName("main").jniLibs.srcDir("src/main/jniLibs")
  packaging {
    jniLibs.useLegacyPackaging = true
  }

  splits {
    abi {
      isEnable = true
      reset()
      include(*shadowNetAbis.toTypedArray())
      isUniversalApk = false
    }
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// google-services.json is intentionally absent in this repository; the
// plugin is configured in passthrough mode via gradle.properties so builds
// do not require a Firebase configuration file.

// AGP 9.x ships built-in Kotlin support. The `kotlin` extension is provided
// by that built-in plugin and lets us align the Kotlin JVM target with the
// Java source/target compatibility above.
kotlin {
  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
  }
}

val fetchSingBoxCore by tasks.registering(Exec::class) {
  group = "build setup"
  description = "Fetches the pinned sing-box Android executables"
  commandLine("bash", rootProject.file("scripts/fetch-vpn-cores.sh").absolutePath)
  inputs.file(rootProject.file("scripts/fetch-vpn-cores.sh"))
  outputs.file(project.file(".vpn-core-versions"))
  outputs.dir(project.file("src/main/jniLibs"))
}

tasks.named("preBuild") { dependsOn(fetchSingBoxCore) }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  // Xray owns the Android TUN; sing-box runs as an optional local proxy core
  // behind that TUN, avoiding conflicting gomobile runtimes.
  implementation("com.github.2dust:libv2ray:v26.8.20@aar")
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  // Roborazzi 1.59 is incompatible with AGP 9.x; re-enable after a
  // compatible release is available.
  // testImplementation(libs.roborazzi)
  // testImplementation(libs.roborazzi.compose)
  // testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
