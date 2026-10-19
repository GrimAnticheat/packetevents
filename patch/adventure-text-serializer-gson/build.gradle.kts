plugins {
    packetevents.`patching-conventions`
}

// Compile only the additive API adapter against Adventure 5. Runtime dependencies stay on 4.x.
val adventure5 by sourceSets.creating
sourceSets.main {
    compileClasspath += adventure5.output
    output.dir(mapOf("builtBy" to adventure5.classesTaskName), adventure5.java.classesDirectory)
}

dependencies {
    add(adventure5.compileOnlyConfigurationName, "net.kyori:adventure-api:5.2.0")
    api(libs.adventure.text.serializer.gson)
    api(libs.adventure.text.serializer.json.legacy)
    api(libs.adventure.text.serializer.commons)
}

tasks {
    sourcesJar { from(adventure5.allSource) }
    shadowJar {
        dependencies {
            exclude(dependency("net.kyori:adventure-api:.*"))
            exclude(dependency("net.kyori:adventure-key:.*"))
            exclude(dependency("net.kyori:adventure-nbt:.*"))
            exclude(dependency("net.kyori:examination-api:.*"))
            exclude(dependency("net.kyori:examination-string:.*"))
            exclude(dependency("com.google.code.gson:gson:.*"))
            exclude("META-INF/services/**")
        }
    }
}
