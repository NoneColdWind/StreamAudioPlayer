plugins {
    id("java")
}

group = "cn.ncw.music"
version = "1.0.2-hotfix2"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    compileOnly("org.projectlombok:lombok:1.18.20")
    annotationProcessor("org.projectlombok:lombok:1.18.20")
    implementation("ws.schild:jave-all-deps:3.5.0")

    // SLF4J API
    implementation("org.slf4j:slf4j-api:1.7.32")
    // Log4j2 核心库
    implementation("org.apache.logging.log4j:log4j-core:2.25.3")
    implementation("org.apache.logging.log4j:log4j-api:2.25.3")
    // SLF4J 与 Log4j2 的桥接
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.25.3")
}

tasks.test {
    useJUnitPlatform()
}

