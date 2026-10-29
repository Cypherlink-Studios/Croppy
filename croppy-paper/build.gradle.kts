plugins {
    `java-library`
    id("com.gradleup.shadow")
}

dependencies {
    implementation(project(":croppy-api"))
    implementation(project(":croppy-core"))

    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
    }
    build {
        dependsOn(shadowJar)
    }
}
