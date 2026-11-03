package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AlarmEntity

@Database(entities = [AlarmEntity::class], version = 1, exportSchema = false)
abstract class AnkitaDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    companion object {
        @Volatile
        private var INSTANCE: AnkitaDatabase? = null

        fun getDatabase(context: Context): AnkitaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AnkitaDatabase::class.java,
                    "ankita_voice_assistant.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
