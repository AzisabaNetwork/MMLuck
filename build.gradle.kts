import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    `java-library`
    `maven-publish`
    id("com.gradleup.shadow") version "9.4.1"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.azisaba.net/repository/maven-public/")
    maven("https://mvn.lumine.io/repository/maven-public/")
}

dependencies {
    api("xyz.acrylicstyle.java-util:common:2.0.0-SNAPSHOT")
    api("xyz.acrylicstyle.java-util:expression:2.0.0-SNAPSHOT")
    compileOnly("net.azisaba:LifeCore:6.18.4+1.21.11")
    compileOnly("io.lumine:Mythic-Dist:5.12.0")
    compileOnly("net.azisaba.rarity:api:2.1.1") {
        exclude("org.spigotmc", "spigot-api")
    }
    compileOnly("net.azisaba:ItemStash:1.0.0-SNAPSHOT") {
        exclude("org.spigotmc", "spigot-api")
    }
    compileOnly("net.azisaba.loreeditor:api:1.3.4-SNAPSHOT") {
        exclude("org.spigotmc", "spigot-api")
    }
    compileOnly("xyz.acrylicstyle:StorageBox:1.6.2+1.21.11")
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
}

group = "com.github.Mori01231"
version = "1.8.0+1.21.11"
description = "MMLuck"

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
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
        relocate("xyz.acrylicstyle.util", "com.github.mori01231.mmluck.lib.xyz.acrylicstyle.util")
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
            artifact(tasks.reobfJar)
        }
    }
}
