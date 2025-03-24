plugins {
	id("java")
	id("com.github.johnrengelman.shadow") version "8.1.1"
	id("maven-publish")
}

group = "org.modernbeta.admintoolbox"
version = "0.1"

repositories {
	mavenCentral()
	maven("https://repo.papermc.io/repository/maven-public/") {
		name = "papermc"
	}
	maven("https://oss.sonatype.org/content/groups/public/") {
		name = "sonatype"
	}
}

dependencies {
	compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
}

java {
	toolchain.languageVersion.set(JavaLanguageVersion.of(21))
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

publishing {
	publications {
		create<MavenPublication>("maven") {
			groupId = "org.modernbeta"
			artifactId = "AdminToolbox"

			from(components["java"])
		}
	}
}
