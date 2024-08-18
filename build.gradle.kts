import java.net.URI

plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = URI.create("https://jitpack.io") }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("com.github.PokeAPI:pokekotlin:2.3.1")
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("org.springframework:spring-jdbc:6.1.12")
}

tasks.test {
    useJUnitPlatform()
}