// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  // Kotlin compilation itself is built into AGP 9.x (no kotlin-android
  // plugin), but the Compose compiler Gradle plugin is still required when
  // buildFeatures.compose is enabled. Its version must match AGP's
  // embedded Kotlin (2.2.10).
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  // Roborazzi is temporarily disabled while waiting for AGP 9 compatibility.
  // alias(libs.plugins.roborazzi) apply false
}
