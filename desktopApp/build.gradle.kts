import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
}

// Desktop (Windows + Linux) app. The UI, view models and data layer come from :shared; audio
// playback comes from :desktop-audio (pure-JVM decoders + a looping software mixer).

val appVersion = "1.1.6"

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":desktop-audio"))

    implementation(compose.desktop.currentOs)
    implementation(libs.jetbrains.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.koin.core)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
}

tasks.test {
    useJUnit()
}

compose.desktop {
    application {
        mainClass = "com.pronaycoding.blanko.desktop.MainKt"

        // Decoded audio is held in memory while a sound is loaded (about 10 MB per minute).
        jvmArgs += listOf("-Xmx768m")

        nativeDistributions {
            // Windows: .exe installer and .msi. Linux: .deb and .rpm. macOS: .dmg.
            // jpackage only builds the formats of the OS it runs on.
            targetFormats(TargetFormat.Exe, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.Dmg)

            packageName = "Blanko"
            packageVersion = appVersion
            description = "Listen to different sounds"
            copyright = "Blanko contributors. Licensed under GPL-3.0."
            vendor = "Blanko"
            licenseFile.set(rootProject.file("LICENSE"))

            // Modules the app needs at runtime that the jlink-trimmed runtime would otherwise drop.
            modules("java.sql", "java.naming", "jdk.unsupported", "java.management")

            windows {
                iconFile.set(project.file("icons/blanko.ico"))
                menuGroup = "Blanko"
                shortcut = true
                dirChooser = true
                perUserInstall = true
                // Keep this value forever: Windows uses it to recognise upgrades of the same app.
                upgradeUuid = "5d6ab4c1-7a38-4d0e-9b1f-2c0e8f7a3b51"
            }

            macOS {
                iconFile.set(project.file("icons/blanko.icns"))
                bundleID = "com.pronaycoding.blanko.desktop"
                dockName = "Blanko"
                // Builds are unsigned. To sign and notarize, configure `signing { }` and
                // `notarization { }` here (needs an Apple Developer account).
            }

            linux {
                iconFile.set(project.file("icons/blanko.png"))
                packageName = "blanko"
                menuGroup = "Blanko"
                appCategory = "Audio"
                debMaintainer = "itsPronay"
                rpmLicenseType = "GPL-3.0"
            }
        }

        buildTypes.release.proguard {
            // The app is small and relies on service loading (Java Sound, Room, Koin): do not shrink.
            isEnabled.set(false)
        }
    }
}
