package com.example.data.model

import android.graphics.drawable.Drawable
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class WakeMode {
  BRIGHT,
  DIM,
}

@Entity(tableName = "keep_awake_rules")
data class KeepAwakeRuleEntity(
  @PrimaryKey val packageName: String,
  val appLabel: String,
  val isEnabled: Boolean = true,
  val wakeMode: WakeMode = WakeMode.BRIGHT,
  val maxDurationMinutes: Int = 0,
  val createdAt: Long = System.currentTimeMillis(),
)

data class InstalledAppItem(
  val packageName: String,
  val label: String,
  val isSystemApp: Boolean,
  val icon: Drawable?,
)

data class AppWithRule(
  val app: InstalledAppItem,
  val rule: KeepAwakeRuleEntity?,
)
