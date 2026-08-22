package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ServerEntity::class, LogEntryEntity::class, AppSettingsEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
    abstract fun logDao(): LogDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** Removes the generated demo state and introduces dual-core preferences. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE servers ADD COLUMN method TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN preferredCore TEXT NOT NULL DEFAULT 'AUTO'")
                db.execSQL("DELETE FROM servers WHERE address LIKE '%.shadownet.core'")
                db.execSQL(
                    """
                    UPDATE servers SET isSelected = 1
                    WHERE id = (SELECT id FROM servers ORDER BY id ASC LIMIT 1)
                      AND NOT EXISTS (SELECT 1 FROM servers WHERE isSelected = 1)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    UPDATE app_settings
                    SET isConnected = 0,
                        connectedSeconds = 0,
                        dlRateMbps = 0.0,
                        ulRateMbps = 0.0,
                        totalUsageGb = 0.0,
                        preferredCore = 'AUTO'
                    WHERE id = 1
                    """.trimIndent()
                )
                // Version 1 shipped fabricated daemon logs. Do not present them
                // as if they came from either native core after the upgrade.
                db.execSQL("DELETE FROM system_logs")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shadow_net_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
