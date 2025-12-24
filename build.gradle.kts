import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.9"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "org.modernbeta.admintoolbox"

val baseVersion = "1.4.1"
version = run {
    // CI: on release tag - use that version
    val refType = System.getenv("GITHUB_REF_TYPE")
    val ref = System.getenv("GITHUB_REF")
    if (refType == "tag" && ref?.startsWith("refs/tags/v") == true) {
        return@run ref.removePrefix("refs/tags/v")
    }

    // git: get latest commit hash
    val gitHash = runCatching {
        ProcessBuilder("git", "rev-parse", "--short", "HEAD")
            .redirectErrorStream(true)
            .start()
            .let { proc ->
                proc.inputStream.bufferedReader().readText().trim()
                    .takeIf { proc.waitFor() == 0 && it.matches(Regex("[a-f0-9]+")) }
            }
    }.getOrNull()

    // git: not in repository, use "-dev"
    if (gitHash == null) {
        return@run "$baseVersion-dev"
    }

    // CI: use commit hash, working tree is always clean
    if (System.getenv("CI") != null) {
        return@run "$baseVersion+$gitHash"
    }

    // local: if working tree is clean, use hash, else use "-dev"
    val isClean = runCatching {
        ProcessBuilder("git", "status", "--porcelain")
            .redirectErrorStream(true)
            .start()
            .let { proc ->
                val output = proc.inputStream.bufferedReader().readText()
                proc.waitFor() == 0 && output.isBlank()
            }
    }.getOrDefault(false)

    if (isClean) "$baseVersion+$gitHash"
    else "$baseVersion-dev"
}

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

tasks.jar {
    enabled = false
}

tasks.shadowJar {
    archiveClassifier.set("")

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
