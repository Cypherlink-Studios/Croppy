plugins {
    `java-library`
}

dependencies {
    implementation(project(":croppy-api"))
    implementation(project(":croppy-integrations:common"))

    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    testImplementation("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")

    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.12") {
        exclude(group = "*")
    }
    compileOnly("com.sk89q.worldguard:worldguard-core:7.0.12") {
        exclude(group = "*")
    }
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.10") {
        exclude(group = "*")
    }
    compileOnly("com.sk89q.worldedit:worldedit-core:7.3.10") {
        exclude(group = "*")
    }

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
