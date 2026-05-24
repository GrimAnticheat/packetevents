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

    // Re-export everything the variant modules used to expose so downstream consumers
    // (e.g. Grim) that depend on `packetevents-fabric` get the FQNs transitively.
    // Without these api() entries the published POM lists only fabric-loader and
    // mc-typed consumers fail to compile against the now-fabric-common bridge.
    api(project(":fabric-common"))
    api(libs.bundles.adventure)
    api(project(":api", "shadow"))
    api(project(":netty-common"))

    // JiJ side: ship the same artifacts inside the published mod jar so Fabric Loader
    // has them at runtime even when the consumer didn't pull the maven POM.
    include(project(":fabric-common"))
    include(libs.bundles.adventure)
    include(project(":api", "shadow"))
    include(project(":netty-common"))
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

    remapJar {
        destinationDirectory = rootProject.layout.buildDirectory.dir("libs")
        archiveBaseName = "${rootProject.name}-fabric"
        archiveVersion = rootProject.ext["artifactVersion"] as String

        // Nest the variant remapJars without triggering full project configuration
        // (which would inject dev/namedElements jars into the include config).
        dependsOn(":fabric-intermediary:remapJar", ":fabric-official:remapJar")
        nestedJars.from(
            rootProject.layout.buildDirectory.file("libs/${rootProject.name}-fabric-intermediary-${rootProject.ext["artifactVersion"]}.jar")
        )
        nestedJars.from(
            rootProject.layout.buildDirectory.file("libs/${rootProject.name}-fabric-official-${rootProject.ext["artifactVersion"]}.jar")
        )
    }
}
