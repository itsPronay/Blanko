package com.pronaycoding.blanko.core.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.pronaycoding.blanko.core.database.dao.CustomSoundDao
import com.pronaycoding.blanko.core.database.dao.PresetDao
import com.pronaycoding.blanko.core.database.entities.CustomSoundEntity
import com.pronaycoding.blanko.core.database.entities.PresetEntity

/**
 * Room database for the Blanko application.
 *
 * This database manages persistent storage for:
 * - Custom sound files added by users ([CustomSoundEntity])
 * - Sound presets (combinations of sounds with specific volumes) ([PresetEntity])
 *
 * The database is a Kotlin Multiplatform Room database: the platform source sets create the builder
 * (see `createBlankoDatabase`) and Koin keeps a single instance for the whole app.
 * All database access is performed through Data Access Objects (DAOs) which provide type-safe database queries.
 *
 * @see CustomSoundDao for custom sound database operations
 * @see PresetDao for preset database operations
 * @see CustomSoundEntity for custom sound data model
 * @see PresetEntity for preset data model
 */
@Database(
    entities = [
        CustomSoundEntity::class,
        PresetEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@ConstructedBy(BlankoDatabaseConstructor::class)
abstract class BlankoDatabase : RoomDatabase() {
    abstract fun customSoundDao(): CustomSoundDao

    abstract fun presetDao(): PresetDao

    companion object {
        /** Database file name. */
        const val DATABASE_NAME = "blanko_database"
    }
}

// Room's KSP processor generates the `actual` implementation for each target.
@Suppress("KotlinNoActualForExpect", "NO_ACTUAL_FOR_EXPECT")
expect object BlankoDatabaseConstructor : RoomDatabaseConstructor<BlankoDatabase> {
    override fun initialize(): BlankoDatabase
}
