package dev.yukineko.ekimemo_map.data.user

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [AccessLog::class, PassingLog::class], version = 1)
abstract class UserDatabase : RoomDatabase() {
    abstract fun accessLogDao(): AccessLogDao
    abstract fun passingLogDao(): PassingLogDao
}
