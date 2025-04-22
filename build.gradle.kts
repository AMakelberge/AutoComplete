plugins {
    kotlin("jvm") version "1.8.22"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(kotlin("stdlib", "1.8.22"))
    runtimeOnly("org.jetbrains.kotlin:kotlin-stdlib:1.8.22:sources@jar")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("autocomplete.MainKt")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "autocomplete.MainKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}