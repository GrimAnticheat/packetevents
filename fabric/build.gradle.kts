import me.modmuss50.mpp.ModPublishExtension

plugins {
    packetevents.`library-conventions`
    packetevents.`publish-conventions`
    net.fabricmc.`fabric-loom`
}

dependencies {
    include(libs.bundles.adventure)
    include(project(":api", "shadow"))
    include(project(":netty-common"))

    include(project(":fabric-common"))
    include(project(":fabric-intermediary"))
    include(project(":fabric-official"))

    minecraft(libs.fabric.minecraft.official)
}

configure<ModPublishExtension> {
    file = tasks.named<Jar>("jar").flatMap { it.archiveFile }
}

tasks {
    named<Jar>("jar") {
        destinationDirectory = rootProject.layout.buildDirectory.dir("libs")
    }
}
