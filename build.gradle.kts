plugins {
    val kotlinVersion = "1.8.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("org.jetbrains.dokka") version kotlinVersion
    id("maven-publish")
    id("signing")
    id("org.hildan.github.changelog") version "1.11.1"
}

group = "org.jetbrains"
description = "Apple Notary API client for Kotlin"

repositories {
    mavenCentral()
}

dependencies {
    val ktorVersion = "2.1.1"
    api("io.ktor:ktor-client-core:$ktorVersion")
    api("io.ktor:ktor-client-logging:$ktorVersion")
    api("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    api("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    implementation("com.amazonaws:aws-java-sdk-s3:1.12.431")

    implementation("com.auth0:java-jwt:4.3.0")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

changelog {
    futureVersionTag = project.version.toString()
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val dokkaJavadocJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles Kotlin docs with Dokka into a Javadoc jar"
    archiveClassifier.set("javadoc")
    from(tasks.dokkaJavadoc)
}

val spaceUsername: String? by project
val spacePassword: String? by project

publishing {
    repositories {
        maven {
            url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
            credentials {
                username = spaceUsername
                password = spacePassword
            }
        }
    }

    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            artifact(sourcesJar)
            artifact(dokkaJavadocJar)

            val githubUser: String by project
            val githubSlug = "$githubUser/${rootProject.name}"
            val githubRepoUrl = "https://github.com/$githubSlug"

            pom {
                name.set(project.name)
                description.set(project.description)
                url.set(githubRepoUrl)
                licenses {
                    license {
                        name.set("Apache 2.0 License")
                        url.set("https://opensource.org/licenses/Apache-2.0")
                    }
                }
                developers {
                    developer {
                        id.set("ArchangelX360")
                        name.set("Titouan BION")
                        email.set("titouan.bion@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:$githubRepoUrl.git")
                    developerConnection.set("scm:git:git@github.com:$githubSlug.git")
                    url.set(githubRepoUrl)
                }
            }
        }
    }
}

// signing {
//     val signingKey: String? by project
//     val signingPassword: String? by project
//     useInMemoryPgpKeys(signingKey, signingPassword)
//     sign(publishing.publications["maven"])
// }
