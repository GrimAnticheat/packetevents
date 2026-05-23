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
    maven("https://repo.viaversion.com/")
    maven("https://jitpack.io") // Conditional Mixin
}

val minecraft_version: String by project
val yarn_mappings: String by project
val loader_version: String by project

dependencies {
    api(libs.bundles.adventure)
    api(project(":api", "shadow"))
    api(project(":netty-common"))
    api(project(":fabric-common"))
    modApi("com.github.Fallen-Breath.conditional-mixin:conditional-mixin-fabric:0.6.4")

    include(libs.bundles.adventure)
    include(project(":api", "shadow"))
    include(project(":netty-common"))
    include(project(":fabric-common"))
    include("com.github.Fallen-Breath.conditional-mixin:conditional-mixin-fabric:0.6.4")

    // To change the versions, see the gradle.properties file
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

allprojects {
    apply(plugin = "fabric-loom")
    apply(plugin = "packetevents.publish-conventions")

    repositories {
        maven("https://repo.codemc.io/repository/maven-snapshots/")
    }

    dependencies {
        modImplementation("net.fabricmc:fabric-loader:$loader_version")
    }

    tasks {
        withType<JavaCompile> {
            val targetJavaVersion = 17
            if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
                options.release = targetJavaVersion
            }
        }

        remapJar {
            destinationDirectory = rootProject.layout.buildDirectory.dir("libs")
            archiveBaseName = "${rootProject.name}-fabric${if (project.name != "fabric") "-${project.name}" else ""}"
            archiveVersion = rootProject.ext["artifactVersion"] as String
        }

        remapSourcesJar {
            archiveBaseName = "${rootProject.name}-fabric${if (project.name != "fabric") "-${project.name}" else ""}"
            archiveVersion = rootProject.ext["artifactVersion"] as String
        }
    }

    loom {
        mixin {
            // Replaces strings in annotations instead of using refmap
            // This allows us to write mixins that target methodName* and have them work across versions
            // Even as the signature changes without having to use @Dynamic and intermediary names
            // This preserves some compile-time safety, reduces jar size but be careful to not inject into wrong methods
            useLegacyMixinAp.set(false)
        }

        // accesswidener file is literally named "packetevents.accesswidener" (matches
        // fabric.mod.json's accessWidener key), not "${rootProject.name}.accesswidener".
        // rootProject.name resolves to "packetevents-public" in the workspace composite.
        val accessWidenerFile = sourceSets["main"].resources.srcDirs.first()
            .resolve("packetevents.accesswidener")

        if (accessWidenerFile.exists()) {
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
        compileOnly(project(":api", "shadow"))
        compileOnly(project(":netty-common"))
        compileOnly(project(":fabric-common"))
        compileOnly(project(":fabric-intermediary", configuration = "namedElements"))
    }

    // version replacement already processed for :fabric in packetevents.`library-conventions`
    tasks {
        processResources {
            // Declare the inputs to allow Gradle to track changes
            inputs.property("version", project.version)
            inputs.property("modName", "packetevents-${project.name}")
            inputs.property("minecraft_version", minecraft_version) // Add if you use this

            // Match and expand variables in fabric.mod.json
            filesMatching("fabric.mod.json") {
                expand(
                    mapOf(
                        "version" to project.version,
                        "modName" to "packetevents-${project.name}",
                        "minecraft_version" to minecraft_version // Or pull from a variable
                    )
                )
            }
        }
    }

    tasks.register<ServerProductionRunTask>("prodServer") {
        javaLauncher = javaToolchains.launcherFor {
            languageVersion = JavaLanguageVersion.of(21)
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
