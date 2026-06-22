plugins {
    packetevents.`publish-conventions`
    `maven-publish` // 1. 确保引入 maven-publish 插件
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}

// ---------------- 以下为原有的变量计算逻辑 ----------------
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
    // 1. 彻底禁用 Javadoc 生成任务，忽略所有 Javadoc 语法检查
    tasks.withType<Javadoc> {
        enabled = false
    }

    // 2. 禁用 javadocJar 打包任务
    // 因为前面的 Javadoc 生成被禁用了，如果不禁用打包任务，可能会因为找不到源文件而报错
    tasks.matching { it.name.contains("javadocJar", ignoreCase = true) }.configureEach {
        enabled = false
    }
    // 确保所有子项目都应用了 maven-publish
    apply(plugin = "maven-publish")

    // 2. 配置发布目标仓库
    publishing {
        repositories {
            maven {
                // 定义一个本地文件夹作为 Maven 仓库
                name = "LocalFolder"
                // 这里将其输出到项目根目录下的 local-maven-repo 文件夹中
                // 你可以将其修改为任意本地绝对路径，如 uri("D:/my-local-repo")
                url = uri(rootProject.layout.projectDirectory.dir("local-maven-repo"))
            }
        }
    }

    tasks {
        withType<Jar> {
            archiveBaseName = "${rootProject.name}-${project.name}"
            archiveVersion = rootProject.ext["artifactVersion"] as String
        }

        // 3. 将发布任务挂载到 build 任务结束之后
        matching { it.name == "build" }.configureEach {
            // 自动触发发布到上述定义的 LocalFolder
           // finalizedBy("publishAllPublicationsToLocalFolderRepository")

            // 【备用方案】：如果你口中的“本地的文件夹”是指 Maven 的默认本地缓存（~/.m2/repository）
            // 请将上面那行注释掉，并解开下面这行的注释：
            finalizedBy("publishToMavenLocal")
        }
    }
}