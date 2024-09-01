import ca.solostudios.nyx.util.soloStudios

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)

    alias(libs.plugins.nyx)

    alias(libs.plugins.axion.release)

    application
}

application {
    mainClass = "io.github.septicake.LaunchKt"
}

nyx {
    compile {
        sourcesJar = true

        allWarnings = true
        distributeLicense = true
        buildDependsOnJar = true
        jvmTarget = 21
        reproducibleBuilds = true
    }

    info {
        group = "io.github.septicake"
        version = scmVersion.version
        repository.fromGithub("Septicake", "PokeSmashJava")
    }
}

repositories {
    soloStudios()
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.bundles.kotlinx.coroutines.core)
    testImplementation(libs.bundles.kotlinx.coroutines.debugging)

    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.uuid)

    implementation(libs.bundles.fuel)

    implementation(libs.slf4j)
    implementation(libs.slf4k)
    implementation(libs.logback)

    implementation(libs.guava)
    implementation(libs.guava.kotlin)

    implementation(libs.classgraph)

    implementation(libs.jda)
    implementation(libs.jda.ktx)

    implementation(libs.hikaricp)
    implementation(libs.sqlite)
    implementation(libs.mariadb)

    implementation(libs.bundles.exposed)
    implementation(libs.kotlinx.uuid.exposed)

    implementation(libs.bundles.cloud)
}

tasks.register<JavaExec>("genMap") {
    mainClass = "io.github.septicake.pokeapi.GenerateMapFileKt"
    classpath = sourceSets.main.get().runtimeClasspath
}

tasks.register<JavaExec>("launch") {
    mainClass = "io.github.septicake.LaunchKt"
    classpath = sourceSets.main.get().runtimeClasspath
}