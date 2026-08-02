plugins {
    java
    id("com.gradleup.shadow") version "9.5.1"
    kotlin("jvm") version "2.2.20"
    kotlin("kapt") version "2.2.20"
}

group = "dev.dead"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // project Reactor
    implementation("io.projectreactor:reactor-core:3.6.9")
    // kotlinx.coroutines — .await() on CompletableFuture ships in core since 1.7.0,
    // no separate kotlinx-coroutines-jdk8 dependency needed
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.8.1")

    // JMH Dependencies
    implementation("org.openjdk.jmh:jmh-core:1.37")
    annotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")
    kapt("org.openjdk.jmh:jmh-generator-annprocess:1.37")

    // JUnit 6 BOM (requires Java 17+)
    testImplementation(platform("org.junit:junit-bom:6.1.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Kotlin Stdlib
    implementation(kotlin("stdlib-jdk8"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.shadowJar {
    archiveBaseName = "benchmarks"
    archiveClassifier = ""
    archiveVersion = ""
    manifest {
        attributes("Main-Class" to "org.openjdk.jmh.Main")
    }
    relocate("org.objectweb.asm", "jmh.org.objectweb.asm")
    mergeServiceFiles()
}

tasks.jar {
    // Disabled in favor of shadowJar fat-jar generation
    enabled = false
}

artifacts {
    add("archives", tasks.shadowJar)
}
// Disable target validation mismatch check between Java (25) and Kotlin (24 fallback)
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTargetValidationMode.set(org.jetbrains.kotlin.gradle.dsl.jvm.JvmTargetValidationMode.IGNORE)
    }
}

// Ensure Kapt also ignores the target mismatch
tasks.withType<org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask>().configureEach {
    compilerOptions {
        jvmTargetValidationMode.set(org.jetbrains.kotlin.gradle.dsl.jvm.JvmTargetValidationMode.IGNORE)
    }
}