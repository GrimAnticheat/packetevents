plugins {
    packetevents.`publish-conventions`
    `maven-publish` // 1. 确保引入 maven-publish 插件
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}


ext["snapshot"] = ext["snapshot"].toString().toBooleanStrict()
ext["includeBranchName"] = ext["includeBranchName"].toString().toBooleanStrict()
ext["mainBranchName"] = ext["mainBranchName"].toString()
ext["commitHash"] = providers.exec {
    commandLine("git", "rev-parse", "--short", "HEAD")
}.standardOutput.asText.map { it.trim() }.getOrElse("unknown")
ext["gitBranch"] = providers.exec {
    commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
}.standardOutput.asText.map {
    it.trim()
        .replace(Regex("[^a-zA-Z0-9_.-]+"), "_")
        .replace(Regex("_{2,}"), "_")
        .replace(Regex("^[ ._-]+|[ ._-]+$"), "")
        .replace(Regex("^heads_"), "")
}.getOrElse("")
ext["branchName"] = when {
    ext["includeBranchName"] == false ||
            ext["gitBranch"].toString().isBlank() ||
            ext["gitBranch"].toString().contentEquals(ext["mainBranchName"].toString()) -> ""
    else -> "${ext["gitBranch"]}"
}
ext["versionMeta"] = if (ext["snapshot"] == true) "-SNAPSHOT" else ""
ext["versionMetaWithHash"] = "+${ext["commitHash"]}${ext["versionMeta"]}"
ext["artifactVersion"] = buildString {
    append(ext["fullVersion"])
    append(ext[if (ext["snapshot"] == true) "versionMetaWithHash" else "versionMeta"])
}

group = "com.github.retrooper"
description = rootProject.name
version = buildString {
    append(ext["fullVersion"])
    append(ext[if (ext["snapshot"] == true) "versionMetaWithHash" else "versionMeta"])
}
// --------------------------------------------------------

tasks {
    val taskSubModules: (String) -> Array<Task> = { task ->
        subprojects.filterNot { it.path == ":patch" }.map { it.tasks[task] }.toTypedArray()
    }

    register<Delete>("clean") {
        dependsOn(*taskSubModules("clean"))
        delete(rootProject.layout.buildDirectory)
    }

    register("printVersion") {
        println("Project Version: " + project.version)
        println("Artifact Version: " + project.ext["artifactVersion"])
    }

    defaultTasks("build")
}

allprojects {
    tasks.withType<Javadoc> {
        enabled = false
    }

    tasks.matching { it.name.contains("javadocJar", ignoreCase = true) }.configureEach {
        enabled = false
    }
    apply(plugin = "maven-publish")

    publishing {
        repositories {
            maven {
                name = "LocalFolder"
                url = uri(rootProject.layout.projectDirectory.dir("local-maven-repo"))
            }
        }
    }

    tasks {
        withType<Jar> {
            archiveBaseName = "${rootProject.name}-${project.name}"
            archiveVersion = rootProject.ext["artifactVersion"] as String
        }

        matching { it.name == "build" }.configureEach {
            finalizedBy("publishToMavenLocal")
        }
    }
}