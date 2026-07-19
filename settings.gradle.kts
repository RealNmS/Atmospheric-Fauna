pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    // Check the latest version on https://stonecutter.kikugie.dev/blog/changes/0.9
    id("dev.kikugie.stonecutter") version "0.9.6"

    // Used for cross-compat for 26.1+ and older versions (https://codeberg.org/KikuGie/loom-back-compat)
    id("dev.kikugie.loom-back-compat") version "0.4"

    // Sometimes it is needed to make Gradle run at all, so it doesn't hurt to have
    // (https://github.com/gradle/foojay-toolchains)
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        // See https://stonecutter.kikugie.dev/wiki/start/#choosing-minecraft-versions
        version("1.21.2.x", "1.21.2")
        version("1.21.4.x", "1.21.4")
        version("1.21.5.x", "1.21.5")
        version("1.21.6.x", "1.21.6")
        version("1.21.9.x", "1.21.9")
        version("1.21.11.x", "1.21.11")
        version("26.1.x", "26.1")
        version("26.2.x", "26.2")
        vcsVersion = "26.1.x"
    }
}

rootProject.name = "Atmospheric Fauna"
