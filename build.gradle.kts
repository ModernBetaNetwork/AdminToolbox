plugins {
	id("java")
	id("com.github.johnrengelman.shadow") version "8.1.1"
	id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "org.modernbeta.admintoolbox"
version = "1.1.2"

java {
	toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
	maven("https://repo.papermc.io/repository/maven-public/") {
		name = "papermc"
	}
    maven("https://repo.bluecolored.de/releases") {
        name = "bluemap"
    }
	maven("https://jitpack.io") {
		name = "jitpack"
	}
}

dependencies {
	compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
	compileOnly("org.bstats:bstats-bukkit:3.0.2")
	implementation("com.github.LeonMangler:SuperVanish:6.2.18-3")
	implementation("de.bluecolored:bluemap-api:2.7.4")
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

val plugins = runPaper.downloadPluginsSpec {
	modrinth("viaversion", "5.3.2") // makes testing much easier
	modrinth("bluemap", "5.5-paper")
}

// Paper (non-Folia!) server
tasks.runServer {
	minecraftVersion("1.20.4")
	downloadPlugins {
		from(plugins)
		// SuperVanish does not support Folia, so it only goes in the plain Paper server
		github("LeonMangler", "SuperVanish", "6.2.18", "SuperVanish-6.2.18.jar")
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
