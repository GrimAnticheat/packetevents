import me.modmuss50.mpp.ModPublishExtension
import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.task.prod.ServerProductionRunTask

// Aggregator: JiJ-nests fabric-intermediary + fabric-official + fabric-common into the
// published packetevents-fabric jar. Fabric Loader gates each nested variant by its
// declared minecraft range at runtime.

plugins {
    packetevents.`library-conventions`
    packetevents.`publish-conventions`
    net.fabricmc.`fabric-loom-remap`
}

repositories {
    mavenCentral()
    maven("https://repo.viaversion.com/")
    maven("https://jitpack.io") // Conditional Mixin
}

val minecraft_version: String by project
val yarn_mappings: String by project
val loader_version: String by project

dependencies {
    // Floor at oldest supported MC so Loom remap accepts 1.16.1.
    minecraft("com.mojang:minecraft:$minecraft_version")
    mappings("net.fabricmc:yarn:$yarn_mappings")
    modImplementation("net.fabricmc:fabric-loader:$loader_version")

    // api(): re-export so consumers' POMs see them transitively.
    // include(): JiJ at runtime. Both are needed.
    api(project(":fabric-common"))
    api(libs.bundles.adventure)
    api(project(":api", "shadow"))
    api(project(":netty-common"))

    include(project(":fabric-common"))
    include(libs.bundles.adventure)
    api(libs.adventure.text.logger.slf4j)
    include(libs.adventure.text.logger.slf4j)
    include(project(":api", "shadow"))
    include(project(":netty-common"))
    // Hoisted from variant modules to avoid duplicating the 28KB JiJ.
    include("com.github.Fallen-Breath.conditional-mixin:conditional-mixin-fabric:0.6.4")
}

loom {
    mods {
        register("packetevents") {
            sourceSet(sourceSets.main.get())
        }
    }
}

tasks {
    withType<JavaCompile> {
        options.release = 17
    }

    register<ServerProductionRunTask>("prodServer") {
        // always run this task if asked to
        outputs.upToDateWhen { false }

        minecraftVersion = "26.2-rc-2"
        loaderVersion = libs.versions.fabric.loader
        runDir = project.layout.projectDirectory.dir("run").dir(minecraftVersion.get())

        javaLauncher = project.javaToolchains.launcherFor {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    remapJar {
        destinationDirectory = rootProject.layout.buildDirectory.dir("libs")
        archiveBaseName = "${rootProject.name}-fabric"
        archiveVersion = rootProject.ext["artifactVersion"] as String

        // Nest via file path (not project deps) to skip variant project configuration,
        // which would otherwise inject dev/namedElements jars into `include`.
        // intermediary → remapJar (LoomRemap); official → jar (LoomNoRemap).
        dependsOn(":fabric-intermediary:remapJar", ":fabric-official:jar")
        nestedJars.from(
            rootProject.layout.buildDirectory.file("libs/${rootProject.name}-fabric-intermediary-${rootProject.ext["artifactVersion"]}.jar")
        )
        nestedJars.from(
            rootProject.layout.buildDirectory.file("libs/${rootProject.name}-fabric-official-${rootProject.ext["artifactVersion"]}.jar")
        )
    }
}

// publishMods demands `file` set even when publishing.skip_files=true.
configure<ModPublishExtension> {
    file = tasks.named<RemapJarTask>("remapJar").flatMap { it.archiveFile }
}
