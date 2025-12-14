import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.9"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "org.modernbeta.admintoolbox"
version = "1.3.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
    maven("https://repo.bluecolored.de/releases") {
        name = "bluemap"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("net.luckperms:api:5.4")
    compileOnly("de.bluecolored:bluemap-api:2.7.4")
    implementation("org.bstats:bstats-bukkit:3.1.0")
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.named<ShadowJar>("shadowJar") {
    configurations = listOf(project.configurations.runtimeClasspath.get())
    relocate("org.bstats", project.group.toString())
}

val plugins = runPaper.downloadPluginsSpec {
    modrinth("viaversion", "5.6.0") // makes testing much easier
    modrinth("bluemap", "5.5-paper")
}

// Paper (non-Folia!) server
tasks.runServer {
    minecraftVersion("1.20.4")
    downloadPlugins {
        from(plugins)
        // Add Folia-incompatible plugins below
        modrinth("luckperms", "v5.5.0-bukkit") // they are working on Folia support but it's not ready yet!
    }
}

// Folia server
runPaper.folia.registerTask {
    minecraftVersion("1.20.4")
    downloadPlugins.from(plugins)
}

// better IntelliJ IDEA debugging
// see: https://github.com/jpenilla/run-task/wiki/Debugging
tasks.withType(xyz.jpenilla.runtask.task.AbstractRun::class) {
    javaLauncher = javaToolchains.launcherFor {
        vendor = JvmVendorSpec.JETBRAINS
        languageVersion = JavaLanguageVersion.of(21)
    }
    jvmArgs("-XX:+AllowEnhancedClassRedefinition")
}
