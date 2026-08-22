package com.example

import android.content.Context
import androidx.core.content.res.ResourcesCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("هواشناسی کوهستان", appName)
  }

  @Test
  fun `test load font resources`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    println("--- Robolectric Load Font Test ---")
    try {
      val regular = ResourcesCompat.getFont(context, R.font.vazirmatn_regular)
      println("vazirmatn_regular loaded: $regular")
      assertNotNull(regular)

      val medium = ResourcesCompat.getFont(context, R.font.vazirmatn_medium)
      println("vazirmatn_medium loaded: $medium")
      assertNotNull(medium)

      val bold = ResourcesCompat.getFont(context, R.font.vazirmatn_bold)
      println("vazirmatn_bold loaded: $bold")
      assertNotNull(bold)
    } catch (e: Exception) {
      println("Failed to load: ${e.message}")
      e.printStackTrace()
      throw e
    }
  }
}

