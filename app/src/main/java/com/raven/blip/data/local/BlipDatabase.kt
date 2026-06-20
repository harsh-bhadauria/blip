package com.raven.blip.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.raven.blip.data.model.Task

@Database(
    entities = [Task::class],
    version = 1,
    exportSchema = false
)
abstract class BlipDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}
