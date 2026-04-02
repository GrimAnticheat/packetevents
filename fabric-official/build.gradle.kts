plugins {
    packetevents.`library-conventions`
    net.fabricmc.`fabric-loom`
}

repositories {
    maven("https://maven.fabricmc.net/")
    maven("https://repo.viaversion.com/")
}

dependencies {
    api(project(":fabric-common"))

    minecraft(libs.fabric.minecraft.official)
    compileOnly(libs.fabric.loader)
    compileOnly(libs.via.version)
}

tasks.withType<JavaCompile> {
    options.release = 25
}

loom {
    splitEnvironmentSourceSets()
    mods {
        register("packetevents-fabric-official") {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets.maybeCreate("client"))
        }
    }

    mixin {
        useLegacyMixinAp.set(false)
    }

    accessWidenerPath = sourceSets.main.get().resources.srcDirs.single()
        .resolve("packetevents.accesswidener")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}
