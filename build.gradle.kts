import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	idea
	java
	kotlin("jvm") version "1.6.21"
	`maven-publish`
	signing
}

val javaMajorVersion = "17"
val kotlinTargetVersion = "17"
val pluginGroup = "me.joshlarson"
val pluginName = "websocket"
val pluginVersion = "0.9.6"

project.group = pluginGroup
project.version = pluginVersion

java {
	modularity.inferModulePath.set(true)
	withJavadocJar()
	withSourcesJar()
}

repositories {
	mavenCentral()
}

sourceSets {
	main {
		dependencies {
			api(group="org.jetbrains", name="annotations", version="20.1.0")
		}
	}
	test {
		dependencies {
			testImplementation(kotlin("stdlib"))
			
			testImplementation(group="org.junit.jupiter", name="junit-jupiter-api", version="5.8.1")
			testRuntimeOnly(group="org.junit.jupiter", name="junit-jupiter-engine", version="5.8.1")
		}
	}
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
	jvmTarget = kotlinTargetVersion
}

// Create the publication with the pom configuration:
publishing {
	publications {
		create<MavenPublication>("websocketPublication") {
			groupId = pluginGroup
			artifactId = pluginName
			version = pluginVersion
			from(components["java"])
			versionMapping {
				usage("java-api") {
					fromResolutionOf("runtimeClasspath")
				}
				usage("java-runtime") {
					fromResolutionResult()
				}
			}

			pom {
				name.set("websocket")
				description.set("A Lightweight Java Websocket Client/Server Wrapper")
				url.set("https://github.com/Josh-Larson/java-websocket")
				licenses {
					license {
						name.set("The MIT License")
						url.set("https://opensource.org/licenses/MIT")
						distribution.set("repo")
					}
				}
				developers {
					developer {
						id.set("Josh-Larson")
						name.set("Josh Larson")
						email.set("joshlarson2015@gmail.com")
					}
				}
				scm {
					connection.set("scm:git:git://github.com/Josh-Larson/java-websocket.git")
					developerConnection.set("scm:git:ssh://github.com:Josh-Larson/jlcommon.git")
					url.set("https://github.com/Josh-Larson/java-websocket")
				}
			}
		}
	}
}

tasks {
	javadoc {
		if (JavaVersion.current().isJava9Compatible) {
			(options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
		}
	}
	
	artifacts {
		archives(jar)
		archives(getByName("sourcesJar"))
		archives(getByName("javadocJar"))
	}
	
	signing {
		sign(publishing.publications["websocketPublication"])
	}
}
