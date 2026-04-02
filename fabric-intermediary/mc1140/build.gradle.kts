val minecraft_version: String by project
val yarn_mappings: String by project

plugins {
    net.fabricmc.`fabric-loom-remap`
}

repositories {
    mavenCentral()
}

dependencies {
    // To change the versions, see the gradle.properties file
    minecraft("com.mojang:minecraft:$minecraft_version")
    mappings("net.fabricmc:yarn:$yarn_mappings")
}

loom {
    mods {
        register("packetevents-${project.name}") {
            sourceSet(sourceSets.main.get())
        }
    }
}
