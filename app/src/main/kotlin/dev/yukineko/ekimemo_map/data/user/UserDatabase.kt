package dev.yukineko.ekimemo_map.data.user

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        AccessLog::class,
        PassingLog::class,
        MoveLogSession::class,
        MoveLog::class,
        IntervalTimer::class,
    ],
    version = 1,
)
abstract class UserDatabase : RoomDatabase() {
    abstract fun accessLogDao(): AccessLogDao
    abstract fun passingLogDao(): PassingLogDao
    abstract fun moveLogSessionDao(): MoveLogSessionDao
    abstract fun moveLogDao(): MoveLogDao
    abstract fun intervalTimerDao(): IntervalTimerDao
}
