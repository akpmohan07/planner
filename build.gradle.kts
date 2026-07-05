plugins {
    java
    application
}

val timefoldSolverVersion = "2.2.0"
val logbackVersion = "1.5.34"

group = "org.acme"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("ai.timefold.solver:timefold-solver-bom:$timefoldSolverVersion"))
    implementation("ai.timefold.solver:timefold-solver-core")
    runtimeOnly("ch.qos.logback:logback-classic:$logbackVersion")

    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.compileJava {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.compileTestJava {
    options.encoding = "UTF-8"
}

application {
    mainClass = "dev.mohanverse.Main"
}

tasks.test {
    testLogging {
        events("passed", "skipped", "failed")
    }
}