import org.apache.tools.ant.filters.ReplaceTokens
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-library`
    `maven-publish`
    kotlin("jvm") version "2.4.21"
    id("com.gradleup.shadow") version "9.6.1"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.24"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.azisaba.net/repository/maven-public/")
    maven("https://mvn.lumine.io/repository/maven-public/")
}

dependencies {
    implementation(kotlin("stdlib"))
    api("xyz.acrylicstyle.java-util:common:2.1.1")
    api("xyz.acrylicstyle.java-util:expression:2.1.0")
    compileOnly("net.azisaba:LifeCore:7.0.1+1.21.11")
    compileOnly("io.lumine:Mythic-Dist:5.14.0")
    compileOnly("net.azisaba.rarity:api:2.1.1") {
        exclude("org.spigotmc", "spigot-api")
    }
    compileOnly("net.azisaba:ItemStash:2.2.8") {
        exclude("org.spigotmc", "spigot-api")
    }
    compileOnly("net.azisaba.loreeditor:api:1.3.6") {
        exclude("org.spigotmc", "spigot-api")
    }
    compileOnly("xyz.acrylicstyle:StorageBox:1.6.5+1.21.11")
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
    // the dev bundle declares adventure-text-serializer-ansi without a version
    compileOnly("net.kyori:adventure-text-serializer-ansi:4.26.1")
}

group = "com.github.Mori01231"
version = "1.9.0+1.21.11"
description = "MMLuck"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        allWarningsAsErrors.set(true)
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
    }

    processResources {
        from(
            sourceSets.main
                .get()
                .resources.srcDirs,
        ) {
            include("**")
            val tokenReplacementMap =
                mapOf(
                    "version" to project.version,
                    "description" to project.description,
                )
            filter<ReplaceTokens>("tokens" to tokenReplacementMap)
        }
        filteringCharset = "UTF-8"
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        from(projectDir) { include("LICENSE") }
    }

    shadowJar {
        // let the Kotlin module metadata transformer see (and merge) every duplicate
        filesMatching("META-INF/*.kotlin_module") {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
        relocate("xyz.acrylicstyle.util", "com.github.mori01231.mmluck.lib.xyz.acrylicstyle.util")
        relocate("kotlin", "com.github.mori01231.mmluck.lib.kotlin")
        relocate("org.intellij.lang.annotations", "com.github.mori01231.mmluck.lib.org.intellij.lang.annotations")
        relocate("org.jetbrains.annotations", "com.github.mori01231.mmluck.lib.org.jetbrains.annotations")
    }
}

publishing {
    repositories {
        maven {
            name = "repo"
            credentials(PasswordCredentials::class)
            url =
                uri(
                    if (project.version.toString().endsWith("SNAPSHOT")) {
                        project.findProperty("deploySnapshotURL")
                            ?: System.getProperty("deploySnapshotURL", "https://repo.azisaba.net/repository/maven-snapshots/")
                    } else {
                        project.findProperty("deployReleasesURL")
                            ?: System.getProperty("deployReleasesURL", "https://repo.azisaba.net/repository/maven-releases/")
                    },
                )
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
