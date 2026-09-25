val minecraft_version: String by project

plugins {
    net.fabricmc.`fabric-loom`
}

repositories {
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft_version")
}

loom {
    mods {
        register("packetevents-${project.name}") {
            sourceSet(sourceSets.main.get())
        }
    }
}
