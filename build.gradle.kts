plugins {
	java
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"
	id("jacoco")
    id("com.github.spotbugs") version "6.5.12"
	id("org.owasp.dependencycheck") version "13.0.0"
}

group = "com.lukete"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.flywaydb:flyway-database-postgresql")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.0")
	runtimeOnly("org.postgresql:postgresql")
	compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:testcontainers-junit-jupiter")
	testImplementation("org.testcontainers:testcontainers-postgresql")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

dependencyCheck {
    nvd {
        apiKey = System.getenv("NVD_API_KEY")
    }
}

tasks.named<com.github.spotbugs.snom.SpotBugsTask>("spotbugsMain") {
    reports {
        create("html") {
            required = true
        }
    }
}

tasks.withType<JavaCompile> {
    options.release = 21
}