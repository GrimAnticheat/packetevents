import me.modmuss50.mpp.ModPublishExtension
import me.modmuss50.mpp.PublishModTask
import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.task.RemapSourcesJarTask
import net.fabricmc.loom.task.prod.ServerProductionRunTask

plugins {
    packetevents.`library-conventions`
    net.fabricmc.`fabric-loom-remap`
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

    include(project(":api", "shadow"))
    include(project(":netty-common"))
    include(project(":fabric-common"))

    minecraft("com.mojang:minecraft:$minecraft_version")
    // MC 26.X server/client jars ship pre-deobfuscated with Mojang's official names but
    // FabricMC has not published a tiny intermediary for 26.X yet. The 0.0.0:v2 stub
    // satisfies Loom's mapping requirement without renaming anything — source code in
    // this module must not reference net.minecraft.* compile-time types, because the
    // empty mapping resolves them to nothing. Per-version Mojang-typed code lives in
    // mc261 (or future mc26X) subprojects, which configure their own Loom remap.
    mappings("net.fabricmc:intermediary:0.0.0:v2")
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
}

allprojects {
    apply(plugin = "fabric-loom")
    apply(plugin = "packetevents.publish-conventions")

    repositories {
        maven("https://repo.codemc.io/repository/maven-snapshots/")
    }

    dependencies {
        modImplementation("net.fabricmc:fabric-loader:$loader_version")
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    tasks {
        // Intermediary 0.0.0 has no "named" namespace, so source remap fails. Disable
        // it where these tasks exist (root fabric-official has them; mc261 subproject
        // may not).
        matching { it.name == "remapSourcesJar" || it.name == "sourcesJar" }
            .configureEach { enabled = false }

        withType<JavaCompile> {
            val targetJavaVersion = 25
            options.release = targetJavaVersion
        }

        remapJar {
            destinationDirectory = rootProject.layout.buildDirectory.dir("libs")
            archiveBaseName = if (project == project(":fabric-official")) {
                "${rootProject.name}-fabric-official"
            } else {
                "${rootProject.name}-fabric-${project.name}"
            }
            archiveVersion = rootProject.ext["artifactVersion"] as String
        }

        remapSourcesJar {
            archiveBaseName = if (project == project(":fabric-official")) {
                "${rootProject.name}-fabric-official"
            } else {
                "${rootProject.name}-fabric-${project.name}"
            }
            archiveVersion = rootProject.ext["artifactVersion"] as String
        }
    }

    loom {
        mixin {
            useLegacyMixinAp.set(false)
        }

        val accessWidenerFile = sourceSets["main"].resources.srcDirs.first()
            .resolve("${rootProject.name}.accesswidener")

        if (accessWidenerFile.exists()) {
            accessWidenerPath.set(accessWidenerFile)
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
        compileOnly(project(":fabric-official", configuration = "namedElements"))
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

    tasks.register<ServerProductionRunTask>("prodServer") {
        javaLauncher = javaToolchains.launcherFor {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }
}

subprojects.forEach {
    tasks.named("remapJar").configure {
        dependsOn("${it.path}:remapJar")
    }
}

tasks.remapJar.configure {
    subprojects.forEach { subproject ->
        subproject.tasks.matching { it.name == "remapJar" }.configureEach {
            nestedJars.from(this)
        }
    }
}

tasks.withType<PublishModTask> {
    dependsOn(tasks.named<RemapJarTask>("remapJar"))
    dependsOn(tasks.named<RemapSourcesJarTask>("remapSourcesJar"))
}

configure<ModPublishExtension> {
    file = tasks.named<RemapJarTask>("remapJar").flatMap { it.archiveFile }
    additionalFiles.from(tasks.named<RemapSourcesJarTask>("remapSourcesJar").flatMap { it.archiveFile })
}
