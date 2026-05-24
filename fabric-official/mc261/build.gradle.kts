val minecraft_version: String by project

plugins {
    net.fabricmc.`fabric-loom`
}

repositories {
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft_version")
    // No mappings(): LoomNoRemap uses the pre-deobfuscated 26.1.2 jar's Mojang names
    // directly. Source code in this subproject references net.minecraft.* names that
    // exist verbatim in the server.jar (net.minecraft.world.item.Item, etc.).
}

loom {
    mods {
        register("packetevents-${project.name}") {
            sourceSet(sourceSets.main.get())
        }
    }
}
