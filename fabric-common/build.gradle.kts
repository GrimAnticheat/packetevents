plugins {
    packetevents.`library-conventions`
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
    maven("https://repo.viaversion.com/")
    maven("https://jitpack.io")
    maven("https://repo.spongepowered.org/repository/maven-public/")
}

dependencies {
    api(libs.bundles.adventure)
    api(project(":api"))
    api(project(":netty-common"))

    compileOnly(libs.netty)
    compileOnly(libs.fabric.loader)
    compileOnly(libs.slf4j.api)
    compileOnly(libs.via.version)
    compileOnly("org.apache.logging.log4j:log4j-api:2.22.1")
    compileOnly("org.spongepowered:mixin:0.8.7")
    compileOnly("com.github.Fallen-Breath.conditional-mixin:conditional-mixin-fabric:0.6.4")
}

tasks.withType<JavaCompile> {
    options.release = 17
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
