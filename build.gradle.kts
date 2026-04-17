plugins {
    id("java")
    id("checkstyle")
    id("pmd")
    id("com.github.spotbugs") version "6.0.13"
}

group = "cn.ncw.music"
version = "1.0.4-hotfix5"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    flatDir {
        dirs("libs")
    }

    // 使用国内镜像源加速依赖下载
    maven {
        url = uri("https://maven.aliyun.com/repository/public")
        isAllowInsecureProtocol = true
    }
    maven {
        url = uri("https://maven.aliyun.com/repository/gradle-plugin")
        isAllowInsecureProtocol = true
    }

    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    implementation("ws.schild:jave-all-deps:3.5.0")

    // NCW Logger
    implementation(name ,"NCW-Logger-1.0.4")

    // SLF4J API
    implementation("org.slf4j:slf4j-api:2.0.16")
    // Log4j2 核心库
    implementation("org.apache.logging.log4j:log4j-core:2.24.0")
    implementation("org.apache.logging.log4j:log4j-api:2.24.0")
    // SLF4J 与 Log4j2 的桥接
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.24.0")

    // 添加 Spotbugs 插件
    spotbugsMain {
        ignoreFailures = false
    }
    spotbugsTest {
        ignoreFailures = false
    }
}

tasks.test {
    useJUnitPlatform()
}

// 编译选项
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:all")
    options.compilerArgs.add("-Werror")
}

// Checkstyle 配置
checkstyle {
    toolVersion = "10.16.0"
    configFile = file("${rootProject.projectDir}/config/checkstyle/checkstyle.xml")
}

// PMD 配置
pmd {
    toolVersion = "7.14.0"
    ruleSets = listOf(
        "category/java/bestpractices.xml",
        "category/java/codestyle.xml",
        "category/java/design.xml",
        "category/java/documentation.xml",
        "category/java/errorprone.xml",
        "category/java/performance.xml",
        "category/java/security.xml"
    )
}

