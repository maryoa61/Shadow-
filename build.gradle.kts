// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  // Kotlin support is built into AGP 9.x; no separate kotlin-android plugin.
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  // Roborazzi is temporarily disabled while waiting for AGP 9 compatibility.
  // alias(libs.plugins.roborazzi) apply false
}
