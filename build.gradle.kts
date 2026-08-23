// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  // AGP 9.x: Kotlin (and the Compose compiler for buildFeatures.compose)
  // is supplied by AGP's built-in Kotlin. Do NOT apply
  // org.jetbrains.kotlin.plugin.compose — the standalone Compose compiler
  // Gradle plugin conflicts with built-in Kotlin and breaks
  // lintDebug/assembleDebug on AGP 9.1 even with no Composable sources.
  alias(libs.plugins.google.devtools.ksp) apply false
  // Roborazzi is temporarily disabled while waiting for AGP 9 compatibility.
  // alias(libs.plugins.roborazzi) apply false
}
