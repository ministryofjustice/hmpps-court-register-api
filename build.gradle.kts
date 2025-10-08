plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "9.1.2"
  kotlin("plugin.spring") version "2.2.20"
  kotlin("plugin.jpa") version "2.2.20"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")

  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

  implementation("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql:42.7.8")

  implementation("javax.transaction:javax.transaction-api:1.3")
  implementation("javax.xml.bind:jaxb-api:2.3.1")

  // AWS
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.5.0")

  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.13")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.wiremock:wiremock-standalone:3.13.1")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:4.1.1")
  testImplementation("org.testcontainers:localstack:1.21.3")
  testImplementation("org.testcontainers:postgresql:1.21.3")
  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.10.2")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.13.0")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.13.0")
  testImplementation("org.mockito:mockito-inline:5.2.0")
}

kotlin {
  jvmToolchain(21)
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
  }
}
