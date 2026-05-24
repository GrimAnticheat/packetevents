dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }

        create("testlibs") {
            from(files("testlibs.versions.toml"))
        }
    }
}

pluginManagement {
    repositories {
        maven {
            name = "FabricMC"
            url = uri("https://maven.fabricmc.net/")
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "packetevents"
include("api")
include("netty-common")
// Platform modules
include("spigot")
include("bungeecord")
include("velocity")
include("sponge")
include("fabric")
include("fabric-common")
include("fabric-intermediary")
include(":fabric-intermediary:mc1140")
include(":fabric-intermediary:mc1194")
include(":fabric-intermediary:mc1202")
include(":fabric-intermediary:mc1211")
include(":fabric-intermediary:mc1216")
include("fabric-official")
include(":fabric-official:mc261")
// Patch modules
include(":patch:adventure-text-serializer-gson")
include(":patch:adventure-text-serializer-legacy")

// Workspace composite override (grim.sh writes this file on clone/pull to rename
// rootProject when the workspace pulls multiple sibling repos with the same name).
if (file("workspace.gradle.kts").exists()) apply(from = "workspace.gradle.kts")
