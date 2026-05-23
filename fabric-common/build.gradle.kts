plugins {
    packetevents.`library-conventions`
}

repositories {
    maven("https://maven.fabricmc.net/")
    maven("https://repo.viaversion.com/")
}

dependencies {
    compileOnly(libs.bundles.adventure)
    compileOnly(project(":api", "shadow"))
    compileOnly(project(":netty-common"))

    compileOnly("net.fabricmc:fabric-loader:${rootProject.findProperty("loader_version") ?: "0.16.14"}")
    compileOnly(libs.via.version)
    compileOnly("org.slf4j:slf4j-api:2.0.16")
    compileOnly("org.apache.logging.log4j:log4j-api:2.24.3")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
