plugins {
    alias(libs.plugins.kotlinJvm)
}

// Pure-JVM audio engine used by the desktop app: decodes AAC/M4A, MP3 and WAV/OGG and mixes any
// number of looping tracks through one output line. It has no dependency on the rest of the app,
// so it is built and unit-tested on its own.

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(libs.jcodec)
    implementation(libs.jlayer)
    runtimeOnly(libs.vorbis.spi)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
}

sourceSets {
    test {
        // Real-world fixtures live in src/test/resources/sounds (a few original M4A/MP3 files);
        // the Vorbis copies the desktop app ships are tested straight from the app's resources.
        resources.srcDir("../desktopApp/src/main/resources")
    }
}

tasks.test {
    useJUnit()
}
