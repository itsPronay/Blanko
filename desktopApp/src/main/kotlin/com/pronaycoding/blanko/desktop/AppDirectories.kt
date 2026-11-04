package com.pronaycoding.blanko.desktop

import java.io.File

/** Where the desktop app keeps its database, preferences and imported sounds. */
object AppDirectories {
    val data: File by lazy {
        val osName = System.getProperty("os.name").lowercase()
        val home = System.getProperty("user.home")
        val base =
            when {
                "win" in osName -> System.getenv("APPDATA")?.let(::File) ?: File(home, "AppData/Roaming")
                "mac" in osName -> File(home, "Library/Application Support")
                else -> System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() }?.let(::File) ?: File(home, ".local/share")
            }
        val folder = if ("win" in osName || "mac" in osName) "Blanko" else "blanko"
        File(base, folder).also { it.mkdirs() }
    }
}
