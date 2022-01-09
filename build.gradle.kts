plugins {
    java
    kotlin("jvm") version embeddedKotlinVersion
    `maven-publish`
    `project-report`
}

group = "com.github.seniorjoinu"
version = "0.1-rc25"

repositories {
    mavenCentral()
    maven { setUrl("https://jitpack.io") }
    maven { setUrl("https://dl.bintray.com/hotkeytlt/maven") }
}

dependencies {
    implementation("co.nstant.in:cbor:0.9")
    implementation("com.github.h0tk3y.betterParse:better-parse-jvm:0.4.3")
    implementation("com.github.kittinunf.fuel:fuel:2.2.3")
    implementation("com.github.kittinunf.fuel:fuel-coroutines:2.2.3")
    implementation("com.github.square:kotlinpoet:1.6.0")
    implementation("commons-codec:commons-codec:1.15")
    implementation("org.jetbrains.kotlin:kotlin-reflect") { version { strictly(embeddedKotlinVersion) } }
    implementation("org.jetbrains.kotlin:kotlin-stdlib") { version { strictly(embeddedKotlinVersion) } }
    implementation("org.jetbrains.kotlin:kotlin-stdlib-common") { version { strictly(embeddedKotlinVersion) } }
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8") { version { strictly(embeddedKotlinVersion) } }
    implementation("net.i2p.crypto:eddsa:0.3.0")

    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.4.7")
    testImplementation("io.kotest:kotest-assertions-core:4.3.2")
    testImplementation("io.kotest:kotest-runner-junit5:4.3.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
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
