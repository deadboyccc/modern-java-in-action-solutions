plugins {
    java
    // Shadow has moved to GradleUp org — new plugin ID, new version
    id("com.gradleup.shadow") version "9.4.1"
    kotlin("jvm")
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
    // JMH — 1.37 is still the latest stable release
    implementation("org.openjdk.jmh:jmh-core:1.37")
    annotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")

    // JUnit — pick ONE of the two lines below:
    // Option A: JUnit 5 stable (conservative, widest ecosystem support)
//    testImplementation(platform("org.junit:junit-bom:5.14.4"))
    // Option B: JUnit 6 GA (new, requires Java 17+, breaking API changes from 5.x)
    testImplementation(platform("org.junit:junit-bom:6.1.0"))

    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation(kotlin("stdlib-jdk8"))
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
    enabled = false
}

artifacts {
    add("archives", tasks.shadowJar)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}