import net.fabricmc.loom.task.RemapJarTask

// Top-level fabric aggregator — produces the published `packetevents-fabric-<version>.jar`
// by JiJ-nesting the intermediary and official variant outputs along with the shared
// fabric-common library. This jar is a "meta" Fabric mod: its own fabric.mod.json carries
// the version + depends declarations, and Fabric Loader extracts the nested per-variant
// mods at runtime and gates them by their declared minecraft version ranges.

plugins {
    packetevents.`library-conventions`
    packetevents.`publish-conventions`
    net.fabricmc.`fabric-loom-remap`
}

repositories {
    mavenCentral()
    maven("https://repo.viaversion.com/")
}

val minecraft_version: String by project
val yarn_mappings: String by project
val loader_version: String by project

dependencies {
    // Bind to the oldest MC version we support so Loom remap is happy with a 1.16.1 floor.
    minecraft("com.mojang:minecraft:$minecraft_version")
    mappings("net.fabricmc:yarn:$yarn_mappings")
    modImplementation("net.fabricmc:fabric-loader:$loader_version")

    // The aggregator does not contribute MC-typed code itself, but pulls the variants
    // and fabric-common in as nested JiJ dependencies.
    include(project(":fabric-common"))
    include(project(":fabric-intermediary", configuration = "namedElements"))
    include(project(":fabric-official", configuration = "namedElements"))
}

loom {
    mods {
        register("packetevents") {
            sourceSet(sourceSets.main.get())
        }
    }
}

evaluationDependsOn(":fabric-intermediary")
evaluationDependsOn(":fabric-official")

tasks {
    withType<JavaCompile> {
        options.release = 17
    }

    remapJar {
        destinationDirectory = rootProject.layout.buildDirectory.dir("libs")
        archiveBaseName = "${rootProject.name}-fabric"
        archiveVersion = rootProject.ext["artifactVersion"] as String

        // Pull the variant remapJars as nested mods. evaluationDependsOn above
        // guarantees the Loom tasks in the variant projects are registered before
        // this configuration block runs.
        val intermediaryRemap = project(":fabric-intermediary").tasks.named<RemapJarTask>("remapJar")
        val officialRemap = project(":fabric-official").tasks.named<RemapJarTask>("remapJar")
        dependsOn(intermediaryRemap)
        dependsOn(officialRemap)
        nestedJars.from(intermediaryRemap)
        nestedJars.from(officialRemap)
    }
}
