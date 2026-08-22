pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    // AndroidLibXrayLite publishes its AAR as a pinned GitHub release asset
    // rather than a Maven module. Ivy artifact-only resolution lets Gradle
    // fetch it reproducibly without committing a 59 MB binary to Git.
    ivy {
      name = "AndroidLibXrayLite"
      url = uri("https://github.com/2dust/AndroidLibXrayLite/releases/download")
      patternLayout { artifact("[revision]/[artifact].[ext]") }
      metadataSources { artifact() }
      content { includeGroup("com.github.2dust") }
    }
  }
}

rootProject.name = "SHADOW_NET"

include(":app")
