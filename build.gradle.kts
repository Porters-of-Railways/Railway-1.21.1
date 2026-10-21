/*
 * Steam 'n' Rails
 * Copyright (c) 2022-2025 The Railways Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import me.modmuss50.mpp.ModPublishExtension
import java.util.*
import java.io.ByteArrayOutputStream
import dev.ithundxr.silk.ChangelogText
import me.modmuss50.mpp.ReleaseType

plugins {
    java
    `maven-publish`
    id("net.neoforged.moddev") version "2.0.141"
    id("me.modmuss50.mod-publish-plugin") version "2.0.0-beta.2"
    id("dev.ithundxr.silk") version "1.0.1"
    id("net.kyori.blossom") version "2.2.0"
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.4.1"
}

println("Steam 'n' Rails v${"mod_version"()}")

val isRelease = System.getenv("RELEASE_BUILD")?.toBoolean() ?: false
val buildNumber = System.getenv("GITHUB_RUN_NUMBER")?.toInt()
val gitHash = "\"${calculateGitHash() + (if (hasUnstaged()) "-modified" else "")}\""

extra["gitHash"] = gitHash

// Resolve commonly used Gradle properties eagerly to avoid task name ambiguities inside task scopes
val modId: String = (rootProject.findProperty("mod_id") as String)
val minecraftVersion: String = (rootProject.findProperty("minecraft_version") as String)
val neoforgeVersion: String = (rootProject.findProperty("neoforge_version") as String)
val createForgeVersion: String = (rootProject.findProperty("create_forge_version") as String)
val ponderVersion: String = (rootProject.findProperty("ponder_version") as String)
val flywheelVersion: String = (rootProject.findProperty("flywheel_version") as String)
val registrateForgeVersion: String = (rootProject.findProperty("registrate_forge_version") as String)
val mixinExtrasVersion: String = (rootProject.findProperty("mixin_extras_version") as String)
val voicechatApiVersion: String = (rootProject.findProperty("voicechat_api_version") as String)
val voicechatVersion: String = (rootProject.findProperty("voicechat_version") as String)
val modName: String = (rootProject.findProperty("mod_name") as String)

base {
    archivesName.set(modId)
}

group = "maven_group"()

val build = buildNumber?.let { "-build.${it}" } ?: "-local"

var gitBranchLabel = ""
if (!isRelease && "mod_version"().endsWith("-alpha")) {
    gitBranchLabel = "-" + calculateGitBranch().replace("/", "_")
}

version = "${"mod_version"()}${gitBranchLabel}+neoforge-mc${"minecraft_version"() + if (isRelease) "" else build}"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

// Local maven for vendored dependencies
repositories {
    maven {
        url = uri("${rootProject.projectDir}/local-maven")
    }
    mavenCentral()
    maven("https://maven.neoforged.net/releases")
    maven("https://maven.createmod.net")
    maven("https://mvn.devos.one/snapshots/")
    maven("https://maven.blamejared.com/")
    maven("https://maven.tterrag.com/")
    maven("https://maven.ftb.dev/")
    // CC: Tweaked (required transitively by Create 6.x)
    maven("https://maven.squiddev.cc")
    maven("https://maven.architectury.dev/")
    maven("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/")
    maven("https://jitpack.io")
    maven("https://maven.parchmentmc.org")
    maven("https://modmaven.dev/")
    maven("https://maven.theillusivec4.top/")
    maven("https://maven.maxhenkel.de/releases")
    maven("https://api.modrinth.com/maven")
    maven("https://jm.gserv.me/repository/maven-public/") // JourneyMap API
}

// NeoForge ModDev configuration
neoForge {
    // NeoForge version from properties
    version = neoforgeVersion

    // Note: Parchment overlay disabled for now; fall back to Mojang mappings (stable in ModDev)
    // To re-enable later, ensure a valid parchment artifact exists for the current MC version
    // parchment {
    //     minecraftVersion.set("minecraft_version"())
    //     mappingsVersion.set("parchment_version"())
    // }

    // Add access transformers and mixins
    accessTransformers {
        file("src/main/resources/META-INF/accesstransformer.cfg")
    }

    runs {
        // Client run configuration
        create("client") {
            client()
        }

        // Server run configuration
        create("server") {
            server()
            programArgument("--nogui")
        }

        // Data generation run
        create("data") {
            data()
            environment("DATAGEN", "TRUE")
            programArguments.addAll(
                "--mod", modId,
                "--all",
                "--output", file("src/generated/resources").absolutePath,
                "--existing", file("src/main/resources").absolutePath,
                "--existing-mod", "create"
            )
        }

        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            systemProperty("forge.logging.console.level", "debug")
        }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
        }
    }

    unitTest {
        enable()
        testedMod = mods.getByName(modId)
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // NeoForge
    implementation("net.neoforged:neoforge:${neoforgeVersion}")

    // Create and its dependencies (NeoForge)
    // Use compileOnly for compile-time API access; runtime implementation still comes from the mod
    compileOnly("com.simibubi.create:create-${minecraftVersion}:${createForgeVersion}") {
        exclude(group = "dev.ftb.mods")
        exclude(group = "net.createmod.ponder")
        exclude(group = "maven.modrinth", module = "journeymap")
        exclude(group = "info.journeymap")
    }
    runtimeOnly("com.simibubi.create:create-${minecraftVersion}:${createForgeVersion}") {
        exclude(group = "dev.ftb.mods")
        exclude(group = "net.createmod.ponder")
        exclude(group = "maven.modrinth", module = "journeymap")
        exclude(group = "info.journeymap")
    }

    // Ponder
    implementation("net.createmod.ponder:ponder-neoforge:$ponderVersion+mc$minecraftVersion")

    // Flywheel
    implementation("dev.engine-room.flywheel:flywheel-neoforge-${minecraftVersion}:${flywheelVersion}")

    // Registrate
    implementation("com.tterrag.registrate:Registrate:${registrateForgeVersion}")

    // Architectury
    implementation("dev.architectury:architectury-neoforge:13.0.8")

    // MixinExtras
    implementation("io.github.llamalad7:mixinextras-neoforge:${mixinExtrasVersion}")

    // Annotations
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    // JourneyMap (explicit version to avoid Create pulling in mismatched beta.46)
    // The API jar is bundled inside JourneyMap via JarJar — do NOT add it separately
    val journeymapVersion: String = (rootProject.findProperty("journeymap_version") as String)
    runtimeOnly("maven.modrinth:journeymap:${journeymapVersion}+neoforge") {
        exclude(group = "info.journeymap") // JarJar-bundled API is authoritative
    }

    // Voice chat API
    compileOnly("de.maxhenkel.voicechat:voicechat-api:${voicechatApiVersion}")
    if ((rootProject.findProperty("enable_simple_voice_chat") as String).toBoolean()) {
        compileOnly("maven.modrinth:simple-voice-chat:neoforge-${voicechatVersion}")
    }
}

sourceSets.main {
    // Generated resources first (higher priority), then hand-crafted overrides
    resources.srcDir("src/generated/resources")
    resources.srcDir("src/resources")
}

tasks {
    test {
        useJUnitPlatform()
    }

    processResources {
        // Prefer generated resources over hand-crafted duplicates
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        val props = mapOf(
            "version" to project.version.toString(),
            "minecraft_version" to minecraftVersion,
            "neoforge_version" to neoforgeVersion,
            "mod_id" to modId,
            "mod_name" to modName,
            "create_forge_version" to createForgeVersion,
            "voicechat_api_version" to voicechatApiVersion
        )
        inputs.properties(props)
        filesMatching("META-INF/neoforge.mods.toml") {
            expand(props)
        }
    }

    // Handle duplicates in sourcesJar too
    withType<Jar> {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    jar {
        manifest {
            attributes(mapOf(
                "Specification-Title" to modId,
                "Specification-Vendor" to "The Railways Team",
                "Specification-Version" to "1",
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Implementation-Vendor" to "The Railways Team"
            ))
        }
    }
}

// Utility extension operator
operator fun String.invoke(): String = rootProject.ext[this] as? String ?: error("Property $this not found")

fun calculateGitHash(): String {
    return try {
        val stdout = ByteArrayOutputStream()
        // Gradle 8+ replacement for deprecated Project.exec
        providers.exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
            standardOutput = stdout
        }
        stdout.toString().trim()
    } catch (e: Exception) {
        "unknown"
    }
}

fun calculateGitBranch(): String {
    return try {
        val stdout = ByteArrayOutputStream()
        // Gradle 8+ replacement for deprecated Project.exec
        providers.exec {
            commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
            standardOutput = stdout
        }
        stdout.toString().trim()
    } catch (e: Exception) {
        "unknown"
    }
}

fun hasUnstaged(): Boolean {
    return try {
        val stdout = ByteArrayOutputStream()
        // Gradle 8+ replacement for deprecated Project.exec
        providers.exec {
            commandLine("git", "status", "--porcelain")
            standardOutput = stdout
        }
        stdout.toString().trim().isNotEmpty()
    } catch (e: Exception) {
        false
    }
}

// IDE configuration
apply(plugin = "org.jetbrains.gradle.plugin.idea-ext")
