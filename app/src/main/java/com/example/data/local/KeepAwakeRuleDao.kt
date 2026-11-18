package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.KeepAwakeRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KeepAwakeRuleDao {
  @Query("SELECT * FROM keep_awake_rules ORDER BY createdAt DESC")
  fun observeAllRules(): Flow<List<KeepAwakeRuleEntity>>

  @Query("SELECT * FROM keep_awake_rules WHERE isEnabled = 1")
  fun observeEnabledRules(): Flow<List<KeepAwakeRuleEntity>>

  @Query("SELECT * FROM keep_awake_rules WHERE packageName = :packageName LIMIT 1")
  suspend fun getRule(packageName: String): KeepAwakeRuleEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertRule(rule: KeepAwakeRuleEntity)

  @Query("UPDATE keep_awake_rules SET isEnabled = :enabled WHERE packageName = :packageName")
  suspend fun setRuleEnabled(packageName: String, enabled: Boolean)

  @Query("DELETE FROM keep_awake_rules WHERE packageName = :packageName")
  suspend fun deleteRule(packageName: String)

  @Query("DELETE FROM keep_awake_rules")
  suspend fun clearAllRules()
}
