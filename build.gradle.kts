import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.4.21"
    id("com.gradleup.shadow") version "9.6.1"
}

group = "com.github.Mori01231"
version = "1.1.1"
description = "Trade ontime for items"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.rosewooddev.io/repository/public/")
    maven("https://repo.tcoded.com/releases")
}

dependencies {
    compileOnly("com.destroystokyo.paper:paper-api:1.13.2-R0.1-SNAPSHOT")
    compileOnly("org.black_ixx:playerpoints:3.2.3")
    implementation("com.tcoded:FoliaLib:0.5.1")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

tasks {
    processResources {
        val props = mapOf("version" to project.version, "description" to project.description)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        archiveClassifier.set("")
        val prefix = "com.github.mori01231.tradeontime.libs"
        relocate("kotlin", "$prefix.kotlin")
        relocate("org.jetbrains.annotations", "$prefix.jetbrains.annotations")
        relocate("org.intellij.lang.annotations", "$prefix.intellij.annotations")
        relocate("com.tcoded.folialib", "$prefix.folialib")
        exclude("META-INF/*.kotlin_module", "META-INF/versions/**")
        minimize {
            // FoliaLib picks its implementation at runtime, so keep all of it
            exclude(dependency("com.tcoded:FoliaLib:.*"))
        }
    }

    jar {
        enabled = false
    }

    build {
        dependsOn(shadowJar)
    }
}
