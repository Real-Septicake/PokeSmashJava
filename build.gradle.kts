import ca.solostudios.nyx.util.soloStudios
import org.gradle.kotlin.dsl.scmVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)

    alias(libs.plugins.nyx)

    alias(libs.plugins.axion.release)
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

    // testImplementation(platform("org.junit:junit-bom:5.11.0"))
    // testImplementation("org.junit.jupiter:junit-jupiter")
    // // implementation("com.github.PokeAPI:pokekotlin:2.3.1")
    // implementation("mysql:mysql-connector-java:8.0.33")
    // implementation("org.springframework:spring-jdbc:6.1.12")
}
