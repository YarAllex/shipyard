plugins {
    kotlin("jvm") version "2.0.21" apply false
    id("com.gradle.plugin-publish") version "1.3.0" apply false
    id("com.diffplug.spotless") version "6.25.0" apply false
    id("org.jetbrains.kotlinx.kover") version "0.9.1" apply false
}

allprojects {
    group = "dev.yarallex"
    version = providers.gradleProperty("shipyard.version").getOrElse("0.1.0")
}
