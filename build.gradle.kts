plugins {
    kotlin("jvm") version "1.3.72"
    maven
    `project-report`
}

group = "com.github.seniorjoinu"
version = "0.1-SNAPSHOT"

repositories {
    mavenCentral()
    maven { setUrl("https://jitpack.io") }
    maven { setUrl("https://dl.bintray.com/hotkeytlt/maven") }
}

dependencies {
    implementation("co.nstant.in:cbor:0.9")
    implementation("com.github.h0tk3y.betterParse:better-parse-jvm:0.4.0")
    implementation("com.github.kittinunf.fuel:fuel:2.2.3")
    implementation("com.github.kittinunf.fuel:fuel-coroutines:2.2.3")
    implementation("com.github.square:kotlinpoet:1.6.0")
    implementation("commons-codec:commons-codec:1.15")
    implementation("org.jetbrains.kotlin:kotlin-reflect") { version { strictly("1.3.72") } }
    implementation("org.jetbrains.kotlin:kotlin-stdlib") { version { strictly("1.3.72") } }
    implementation("org.jetbrains.kotlin:kotlin-stdlib-common") { version { strictly("1.3.72") } }
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8") { version { strictly("1.3.72") } }
    implementation("net.i2p.crypto:eddsa:0.3.0")

    testImplementation("com.github.h0tk3y.betterParse:better-parse-jvm:0.4.0")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.2.9")
    testImplementation("io.kotest:kotest-runner-junit5:4.2.5")
    testImplementation("io.kotest:kotest-assertions-core:4.2.5")
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.2")
}

tasks.test {
    useJUnitPlatform()
}

val compileKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
