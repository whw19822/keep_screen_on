package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.KeepAwakeRuleEntity
import com.example.data.model.WakeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("常亮助手", appName)
  }

  @Test
  fun `room dao inserts updates and deletes keep awake rules`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db =
      Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    try {
      val dao = db.keepAwakeRuleDao()
      val rule =
        KeepAwakeRuleEntity(
          packageName = "com.example.reader",
          appLabel = "Reader",
          isEnabled = true,
          wakeMode = WakeMode.DIM,
          maxDurationMinutes = 30,
        )
      dao.upsertRule(rule)

      val enabled = dao.observeEnabledRules().first()
      assertEquals(1, enabled.size)
      assertEquals(WakeMode.DIM, enabled.first().wakeMode)
      assertEquals(30, enabled.first().maxDurationMinutes)

      dao.setRuleEnabled("com.example.reader", false)
      assertEquals(0, dao.observeEnabledRules().first().size)

      dao.deleteRule("com.example.reader")
      assertNull(dao.getRule("com.example.reader"))
    } finally {
      db.close()
    }
  }
}
