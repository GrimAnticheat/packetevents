plugins {
    packetevents.`library-conventions`
}

repositories {
    maven("https://maven.fabricmc.net/")
    maven("https://repo.viaversion.com/")
    maven("https://repo.spongepowered.org/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly(libs.bundles.adventure)
    compileOnly(project(":api", "shadow"))
    compileOnly(project(":netty-common"))

    compileOnly("net.fabricmc:fabric-loader:${rootProject.findProperty("loader_version") ?: "0.16.14"}")
    compileOnly(libs.via.version)
    compileOnly("org.slf4j:slf4j-api:2.0.16")
    compileOnly("org.apache.logging.log4j:log4j-api:2.24.3")
    // PacketEventsMixinManager extends a conditional-mixin base class, which itself
    // extends Sponge Mixin's IMixinConfigPlugin.
    compileOnly("com.github.Fallen-Breath.conditional-mixin:conditional-mixin-fabric:0.6.4")
    compileOnly("org.spongepowered:mixin:0.8.7")
}

// Override library-conventions' release=8: bridge code uses switch expressions
// and pattern-matching instanceof.
tasks.withType<JavaCompile> {
    options.release = 17
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
