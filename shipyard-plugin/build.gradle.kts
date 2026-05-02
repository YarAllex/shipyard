plugins {
    `java-gradle-plugin`
    kotlin("jvm")
    id("com.gradle.plugin-publish")
    id("com.diffplug.spotless")
    id("org.jetbrains.kotlinx.kover")
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
    withSourcesJar()
}

kotlin {
    jvmToolchain(17)
}

val jupiterVersion = "5.11.3"

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:$jupiterVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint("1.4.1").editorConfigOverride(
            mapOf(
                "max_line_length" to "120",
                "ktlint_standard_no-wildcard-imports" to "enabled",
                "ktlint_standard_filename" to "disabled",
            ),
        )
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint("1.4.1").editorConfigOverride(
            mapOf("max_line_length" to "120"),
        )
        trimTrailingWhitespace()
        endWithNewline()
    }
}

kover {
    reports {
        filters {
            excludes {
                classes(
                    "dev.yarallex.shipyard.ShipyardPlugin",
                    "dev.yarallex.shipyard.ShipyardExtension",
                    "dev.yarallex.shipyard.ReleaseSummaryTask",
                    "dev.yarallex.shipyard.docker.*",
                    "dev.yarallex.shipyard.git.*",
                    "dev.yarallex.shipyard.log.*",
                    "dev.yarallex.shipyard.version.PrintCurrentVersionTask",
                    "dev.yarallex.shipyard.version.NextVersionTask",
                    "dev.yarallex.shipyard.version.VersionResolver*",
                    "dev.yarallex.shipyard.version.VersionValueSource*",
                )
            }
        }
        verify {
            rule {
                minBound(90)
            }
        }
    }
}

tasks.named("check") {
    dependsOn("koverVerify")
}

gradlePlugin {
    website = "https://github.com/YarAllex/shipyard"
    vcsUrl = "https://github.com/YarAllex/shipyard.git"
    plugins {
        create("shipyard") {
            id = "dev.yarallex.shipyard"
            implementationClass = "dev.yarallex.shipyard.ShipyardPlugin"
            displayName = "Shipyard"
            description = "Conventional-commits versioning + Docker image release for ghcr.io and other registries."
            tags = listOf("docker", "release", "semver", "conventional-commits", "ghcr")
        }
    }
}

publishing {
    repositories {
        mavenLocal()
    }
    publications.withType<MavenPublication>().configureEach {
        pom {
            name = "Shipyard"
            description = "Conventional-commits versioning + Docker image release."
            url = "https://github.com/YarAllex/shipyard"
            licenses {
                license {
                    name = "Apache License 2.0"
                    url = "https://www.apache.org/licenses/LICENSE-2.0"
                }
            }
            developers {
                developer {
                    id = "YarAllex"
                    name = "YarAllex"
                }
            }
            scm {
                url = "https://github.com/YarAllex/shipyard"
                connection = "scm:git:https://github.com/YarAllex/shipyard.git"
                developerConnection = "scm:git:ssh://git@github.com/YarAllex/shipyard.git"
            }
        }
    }
}
