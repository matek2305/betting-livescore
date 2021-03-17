plugins {
    id("org.jetbrains.kotlin.jvm") version "1.3.72"
    groovy
    application
}

repositories {
    jcenter()
}

val exposedVersion: String by project

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.exposed:exposed-core:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-dao:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-jdbc:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-java-time:${exposedVersion}")
    implementation("khttp:khttp:1.0.0")
    implementation("com.natpryce:konfig:1.6.10.0")

    runtimeOnly("org.postgresql:postgresql:42.2.2")

    testImplementation("org.codehaus.groovy:groovy-all:2.5.14")
    testImplementation("org.spockframework:spock-core:1.3-groovy-2.5")
    testImplementation("com.h2database:h2:1.3.176")
    testImplementation("com.github.tomakehurst:wiremock-jre8:2.27.2")
}

application {
    mainClass.set("com.github.matek2305.betting.livescore.AppKt")
}
