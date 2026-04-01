import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.task.RemapSourcesJarTask

plugins {
    packetevents.`library-conventions`
    packetevents.`publish-conventions`
    // Root aggregate project uses remap tasks while subprojects apply full fabric-loom below.
    net.fabricmc.`fabric-loom-remap`
}

repositories {
    mavenCentral()
    maven("https://repo.viaversion.com/")
    maven("https://jitpack.io")
}

val minecraft_version: String by project
val yarn_mappings: String by project
val loader_version: String by project

dependencies {
    api(project(":fabric-common"))

    modApi("com.github.Fallen-Breath.conditional-mixin:conditional-mixin-fabric:0.6.4")
    include("com.github.Fallen-Breath.conditional-mixin:conditional-mixin-fabric:0.6.4")

    minecraft("com.mojang:minecraft:$minecraft_version")
    mappings("net.fabricmc:yarn:$yarn_mappings")

    compileOnly(libs.via.version)
    compileOnly("org.slf4j:slf4j-simple:2.0.16")
}

loom {
    mods {
        register("packetevents-${project.name}") {
            sourceSet(sourceSets.main.get())
        }
    }
}

tasks.named<ProcessResources>("processResources") {
    inputs.property("version", project.version)
    inputs.property("modName", "packetevents-${project.name}")
    inputs.property("minecraft_version", minecraft_version)

    filesMatching("fabric.mod.json") {
        expand(
            mapOf(
                "version" to project.version,
                "modName" to "packetevents-${project.name}",
                "minecraft_version" to minecraft_version
            )
        )
    }
}

allprojects {
    // Subprojects (mc1140, mc1194, ...) need full fabric-loom for mappings/mod tasks.
    apply(plugin = "fabric-loom")
    apply(plugin = "packetevents.publish-conventions")

    repositories {
        maven("https://repo.codemc.io/repository/maven-snapshots/")
    }

    dependencies {
        "modImplementation"("net.fabricmc:fabric-loader:$loader_version")
    }

    tasks {
        withType<JavaCompile> {
            val targetJavaVersion = 17
            options.release = targetJavaVersion
        }

        named<RemapJarTask>("remapJar") {
            destinationDirectory = rootProject.layout.buildDirectory.dir("libs")
            archiveBaseName = "${rootProject.name}-fabric${if (project.name != "fabric-intermediary") "-${project.name}" else "-intermediary"}"
            archiveVersion = rootProject.ext["artifactVersion"] as String
        }

        named<RemapSourcesJarTask>("remapSourcesJar") {
            archiveBaseName = "${rootProject.name}-fabric${if (project.name != "fabric-intermediary") "-${project.name}" else "-intermediary"}"
            archiveVersion = rootProject.ext["artifactVersion"] as String
        }
    }

    extensions.configure<net.fabricmc.loom.api.LoomGradleExtensionAPI>("loom") {
        mixin {
            useLegacyMixinAp.set(false)
        }

        val resourceDir = the<SourceSetContainer>()["main"].resources.srcDirs.firstOrNull()
        val accessWidenerFile = resourceDir?.resolve("${rootProject.name}.accesswidener")

        if (accessWidenerFile != null && accessWidenerFile.exists()) {
            accessWidenerPath.set(accessWidenerFile)
        }
    }
}

subprojects {
    version = rootProject.version
    val minecraft_version: String by project

    repositories {
        maven {
            name = "ParchmentMC"
            url = uri("https://maven.parchmentmc.org")
        }
    }

    dependencies {
        "compileOnly"(project(":api", "shadow"))
        "compileOnly"(project(":netty-common"))
        "compileOnly"(project(":fabric-common"))
        "compileOnly"(project(":fabric-intermediary", configuration = "namedElements"))
    }

    tasks {
        named<ProcessResources>("processResources") {
            inputs.property("version", project.version)
            inputs.property("modName", "packetevents-${project.name}")
            inputs.property("minecraft_version", minecraft_version)

            filesMatching("fabric.mod.json") {
                expand(
                    mapOf(
                        "version" to project.version,
                        "modName" to "packetevents-${project.name}",
                        "minecraft_version" to minecraft_version
                    )
                )
            }
        }
    }

    extensions.configure<net.fabricmc.loom.api.LoomGradleExtensionAPI>("loom") {
    }
}

subprojects.forEach {
    tasks.named("remapJar").configure {
        dependsOn("${it.path}:remapJar")
    }
}

tasks.named<RemapJarTask>("remapJar").configure {
    subprojects.forEach { subproject ->
        subproject.tasks.matching { it.name == "remapJar" }.configureEach {
            nestedJars.from(this)
        }
    }
}
