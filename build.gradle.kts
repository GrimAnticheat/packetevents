plugins {
    packetevents.`publish-conventions`;
    id("maven-publish")
}

// properties are all set as string, convert to boolean
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
        .replace(Regex("[^a-zA-Z0-9_.-]+"), "_") // Other invalid chars become underscores
        .replace(Regex("_{2,}"), "_") // Collapse multiple underscores
        .replace(Regex("^[ ._-]+|[ ._-]+$"), "") // Remove leading/trailing underscores/dots/hyphens
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
    tasks {
        withType<Jar> {
            archiveBaseName = "${rootProject.name}-${project.name}"
            archiveVersion = rootProject.ext["artifactVersion"] as String
        }
    }
    apply(plugin = "maven-publish")

    publishing {
        repositories {
            maven {
                name = "hezhong-repository"
                url =
                    uri("https://mvn.hezhongkj.top/${if (rootProject.ext["snapshot"] == true) "snapshots" else "releases"}")
                credentials(PasswordCredentials::class) {
                    username = findProperty("hezhongRepoUsername") as String? ?: ""
                    password = findProperty("hezhongRepoPassword") as String? ?: ""
                    println("username = '$username', password length = ${password?.length}") // 调试
                }
            }
        }
    }
}



