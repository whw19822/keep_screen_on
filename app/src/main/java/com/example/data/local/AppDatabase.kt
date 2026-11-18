package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.KeepAwakeRuleEntity
import com.example.data.model.WakeMode

class WakeModeConverter {
  @TypeConverter
  fun fromWakeMode(mode: WakeMode): String = mode.name

  @TypeConverter
  fun toWakeMode(value: String): WakeMode =
    runCatching { WakeMode.valueOf(value) }.getOrDefault(WakeMode.BRIGHT)
}

@Database(
  entities = [KeepAwakeRuleEntity::class],
  version = 1,
  exportSchema = false,
)
@TypeConverters(WakeModeConverter::class)
abstract class AppDatabase : RoomDatabase() {
  abstract fun keepAwakeRuleDao(): KeepAwakeRuleDao

  companion object {
    @Volatile private var INSTANCE: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase {
      return INSTANCE
        ?: synchronized(this) {
          INSTANCE
            ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "screenkeep.db",
              )
              .build()
              .also { INSTANCE = it }
        }
    }
  }
}
