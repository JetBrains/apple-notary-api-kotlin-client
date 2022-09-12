import java.net.URI

plugins {
    val kotlinVersion = "1.7.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
}

repositories {
    mavenCentral()
    maven { url = URI("https://jitpack.io") }
}

dependencies {
    val ktorVersion = "2.1.1"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    implementation("aws.sdk.kotlin:s3:0.17.5-beta")

    implementation("com.github.PhilJay:JWT:1.2.6")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
