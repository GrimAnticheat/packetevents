// fabric-official targets MC 26.X+, whose server/client jars ship pre-deobfuscated with
// Mojang's official names. Loom's NoRemap variant (`net.fabricmc.fabric-loom`, distinct
// from `-remap`) accepts the jar as already in the target namespace, so source code
// references Mojang names directly (net.minecraft.world.item.Item, etc.). No mappings()
// configuration is needed — intermediary == named == official == jar contents.

plugins {
    packetevents.`library-conventions`
    net.fabricmc.`fabric-loom`
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
    maven("https://repo.viaversion.com/")
    maven("https://jitpack.io")
}

val minecraft_version: String by project
val loader_version: String by project

dependencies {
    api(project(":api", "shadow"))
    api(project(":netty-common"))
    api(project(":fabric-common"))
    api("com.github.Fallen-Breath.conditional-mixin:conditional-mixin-fabric:0.6.4")

    include(project(":api", "shadow"))
    include(project(":netty-common"))
    include(project(":fabric-common"))
    include("com.github.Fallen-Breath.conditional-mixin:conditional-mixin-fabric:0.6.4")
    // Nest the per-version variants (mc261, future mc26X) directly inside fabric-official.
    include(project(":fabric-official:mc261"))

    minecraft("com.mojang:minecraft:$minecraft_version")
    // No mappings() block: LoomNoRemap uses the MC jar's own (Mojang) namespace
    // throughout, so source can compile against net.minecraft.* directly.
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

loom {
    mods {
        register("packetevents-${project.name}") {
            sourceSet(sourceSets.main.get())
        }
    }
    mixin {
        useLegacyMixinAp.set(false)
    }

    val accessWidenerFile = sourceSets["main"].resources.srcDirs.first()
        .resolve("packetevents.accesswidener")
    if (accessWidenerFile.exists()) {
        accessWidenerPath.set(accessWidenerFile)
    }
}

allprojects {
    // Fully-qualified id maps to LoomNoRemapGradlePlugin; the short "fabric-loom" maps
    // to the older all-in-one LoomGradlePlugin and conflicts when both are applied.
    apply(plugin = "net.fabricmc.fabric-loom")
    apply(plugin = "packetevents.publish-conventions")

    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://repo.codemc.io/repository/maven-snapshots/")
        maven("https://jitpack.io")
    }

    dependencies {
        // LoomNoRemap exposes plain configurations; mod* configs are LoomRemap-only.
        compileOnly("net.fabricmc:fabric-loader:$loader_version")
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    tasks {
        withType<JavaCompile> {
            options.release = 25
        }

        // LoomNoRemap publishes via the plain `jar` task (the jar is already in the
        // target namespace). Route it to the rootProject libs/ so the top-level fabric
        // aggregator can JiJ it alongside fabric-intermediary's remapJar output.
        jar {
            destinationDirectory = rootProject.layout.buildDirectory.dir("libs")
            archiveBaseName = if (project == project(":fabric-official")) {
                "${rootProject.name}-fabric-official"
            } else {
                "${rootProject.name}-fabric-${project.name}"
            }
            archiveVersion = rootProject.ext["artifactVersion"] as String
        }
    }
}

subprojects {
    version = rootProject.version
    val minecraft_version: String by project

    dependencies {
        compileOnly(project(":api", "shadow"))
        compileOnly(project(":netty-common"))
        compileOnly(project(":fabric-common"))
        // LoomNoRemap doesn't publish a "namedElements" variant; depend on the
        // standard apiElements via the default project() form so MC types from
        // fabric-official's classpath (which include the deobfuscated 26.X jar)
        // resolve for mc261 source.
        compileOnly(project(":fabric-official"))
    }

    loom {
        mixin {
            useLegacyMixinAp.set(false)
        }

        val accessWidenerFile = sourceSets["main"].resources.srcDirs.first()
            .resolve("packetevents.accesswidener")
        if (accessWidenerFile.exists()) {
            accessWidenerPath.set(accessWidenerFile)
        }
    }

    tasks {
        processResources {
            inputs.property("version", project.version)
            inputs.property("modName", "packetevents-${project.name}")
            inputs.property("minecraft_version", minecraft_version)

            filesMatching("fabric.mod.json") {
                expand(
                    mapOf(
                        "version" to project.version,
                        "modName" to "packetevents-${project.name}",
                        "minecraft_version" to minecraft_version,
                    )
                )
            }
        }
    }
}

// Top-level fabric-official jar JiJs the mc261 (and future 26.X) variant jars beside
// the common shell. LoomNoRemap exposes `Jar` (not `AbstractRemapJarTask`), so we
// nest via fabric-loom's `include` instead — declared up in the dependencies block.
subprojects.forEach { sub ->
    tasks.named("jar").configure {
        dependsOn("${sub.path}:jar")
    }
}
