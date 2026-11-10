plugins {
    `java-library`
    id("com.gradleup.shadow")
}

dependencies {
    implementation(project(":croppy-api"))
    implementation(project(":croppy-core"))
    implementation(project(":croppy-integrations:common"))
    implementation(project(":croppy-integrations:worldguard"))
    implementation(project(":croppy-integrations:papi"))
    implementation(project(":croppy-integrations:vault"))

    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    testImplementation("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    testImplementation("net.kyori:adventure-text-serializer-plain:4.17.0")

    // PacketEvents network and protocol library (provided by server plugin)
    compileOnly("com.github.retrooper:packetevents-spigot:2.14.0")
    testImplementation("com.github.retrooper:packetevents-spigot:2.14.0")

    // Libby runtime library loader
    implementation("net.byteflux:libby-paper:1.3.2")

    // Incendo Cloud 2 Command Framework
    implementation("org.incendo:cloud-paper:2.0.1")
    implementation("org.incendo:cloud-annotations:2.1.0")

    // Compile-only database dependencies (loaded dynamically at runtime via Libby)
    compileOnly("com.zaxxer:HikariCP:7.1.0")
    compileOnly("org.xerial:sqlite-jdbc:3.49.1.0")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    shadowJar {
        archiveBaseName.set("Croppy-paper")
        archiveClassifier.set("")
        relocate("org.incendo.cloud", "dev.darkblade.croppy.libs.cloud")
        relocate("io.leangen.geantyref", "dev.darkblade.croppy.libs.geantyref")
        relocate("net.byteflux.libby", "dev.darkblade.croppy.libs.libby")
    }
    jar {
        archiveClassifier.set("plain")
    }
    assemble {
        dependsOn(shadowJar)
    }
    build {
        dependsOn(shadowJar)
    }
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching(listOf("plugin.yml", "paper-plugin.yml")) {
        expand(props)
    }
}