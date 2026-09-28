package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DepthWallpaper
import com.example.domain.compositor.ClockStyles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("DepthLock", appName)
  }

  @Test
  fun `clock styles list is non empty and contains rounded style`() {
    val styles = ClockStyles.getStyles()
    assertTrue(styles.isNotEmpty())
    val rounded = ClockStyles.getStyleById("rounded")
    assertNotNull(rounded)
    assertEquals("rounded", rounded.id)
  }

  @Test
  fun `depth wallpaper entity default values are valid`() {
    val wp = DepthWallpaper(
      title = "Test Wallpaper",
      imagePath = "/data/test.png",
      maskPath = "/data/mask.png"
    )
    assertEquals("Test Wallpaper", wp.title)
    assertTrue(wp.depthOverlapEnabled)
    assertEquals(96f, wp.clockSizeSp, 0.01f)
  }
}
