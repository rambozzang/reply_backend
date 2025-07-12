import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.spring") version "1.9.20"
    kotlin("plugin.jpa") version "1.9.20"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
}

group = "com.comdeply"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    implementation("io.jsonwebtoken:jjwt-impl:0.11.5")
    implementation("io.jsonwebtoken:jjwt-jackson:0.11.5")

    // MariaDB
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client")

    // WebSocket for real-time comments
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // Redis for caching and session management
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // CORS support
    implementation("org.springframework.boot:spring-boot-starter-web")

    // File upload support
    implementation("commons-fileupload:commons-fileupload:1.5")
    implementation("commons-io:commons-io:2.11.0")

    // Image processing
    implementation("org.apache.commons:commons-imaging:1.0.0-alpha5")

    // AWS S3 SDK for Cloudflare R2
    implementation("software.amazon.awssdk:s3:2.20.162")
    implementation("software.amazon.awssdk:auth:2.20.162")

    // HTTP client for Cloudflare
    implementation("software.amazon.awssdk:url-connection-client:2.20.162")

    // Swagger/OpenAPI documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")

    // WebFlux for HTTP client (포트원 API 호출용)
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // macOS DNS resolver fix
    if (System.getProperty("os.name").lowercase().contains("mac")) {
        implementation("io.netty:netty-resolver-dns-native-macos:4.1.100.Final:osx-x86_64")
        implementation("io.netty:netty-resolver-dns-native-macos:4.1.100.Final:osx-aarch_64")
    }

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.7.3")

    // JSON processing
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.core:jackson-databind")

    // 이메일 보내기
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    // Date/Time processing
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

ktlint {
    disabledRules.set(setOf("no-wildcard-imports", "filename"))
    filter {
        exclude("**/.history/**")
        exclude("**/.gradle/**")
        exclude("**/.idea/**")
        exclude("**/.git/**")
        exclude("**/.svn/**")
        exclude("**/.hg/**")
        exclude("**/.bzr/**")
        exclude("**/.gitignore/**")
        exclude("**/bin/**")
    }
}
