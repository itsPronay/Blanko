import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.ksp)
}

kotlin {
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Compose Multiplatform UI
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.ui)
            api(compose.components.resources)
            // @Preview annotation used by the composable previews in common code.
            implementation(compose.components.uiToolingPreview)
            implementation(compose.materialIconsExtended)

            // Multiplatform builds of the AndroidX libraries
            api(libs.jetbrains.lifecycle.viewmodel)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.jetbrains.lifecycle.runtime.compose)
            implementation(libs.jetbrains.navigation.compose)
            api(libs.androidx.room.runtime)
            api(libs.androidx.datastore.preferences)
            implementation(libs.okio)

            // Coroutines + DI
            api(libs.kotlinx.coroutines.core)
            api(libs.koin.core)
            api(libs.koin.compose)
            api(libs.koin.compose.viewmodel)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        val desktopMain by getting {
            dependencies {
                // Dispatchers.Main for Compose Desktop (the Swing event thread).
                api(libs.kotlinx.coroutines.swing)
                // Room's desktop driver: SQLite compiled into a native library bundled with the app.
                api(libs.androidx.sqlite.bundled)
            }
        }
    }
}

compose.resources {
    packageOfResClass = "com.pronaycoding.blanko.resources"
    // Public: the desktop app (tray menu) reuses the shared strings.
    publicResClass = true
}

dependencies {
    // Room generates its database implementation.
    add("kspDesktop", libs.androidx.room.compiler)
}
