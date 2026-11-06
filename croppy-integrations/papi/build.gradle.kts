plugins {
    `java-library`
}

dependencies {
    implementation(project(":croppy-api"))
    implementation(project(":croppy-core"))
    implementation(project(":croppy-integrations:common"))

    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    testImplementation("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")

    compileOnly("me.clip:placeholderapi:2.12.3")
    testImplementation("me.clip:placeholderapi:2.12.3")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
