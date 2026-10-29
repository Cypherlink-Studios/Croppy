plugins {
    `java-library`
    id("com.gradleup.shadow")
}

dependencies {
    implementation(project(":croppy-api"))
    implementation(project(":croppy-core"))

    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")

    // Libby runtime library loader
    implementation("net.byteflux:libby-paper:1.3.2")

    // Compile-only database dependencies (loaded dynamically at runtime via Libby)
    compileOnly("com.zaxxer:HikariCP:7.1.0")
    compileOnly("org.xerial:sqlite-jdbc:3.49.1.0")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
    }
    build {
        dependsOn(shadowJar)
    }
}
