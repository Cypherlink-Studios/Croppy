plugins {
    `java-library`
}

dependencies {
    api(project(":croppy-api"))

    implementation("org.yaml:snakeyaml:2.2")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
