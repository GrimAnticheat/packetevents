val minecraft_version: String by project

plugins {
    net.fabricmc.`fabric-loom-remap`
}

repositories {
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft_version")
    // See fabric-official/build.gradle.kts for why this isn't officialMojangMappings().
    mappings("net.fabricmc:intermediary:0.0.0:v2")
}

loom {
    mods {
        register("packetevents-${project.name}") {
            sourceSet(sourceSets.main.get())
        }
    }
}
