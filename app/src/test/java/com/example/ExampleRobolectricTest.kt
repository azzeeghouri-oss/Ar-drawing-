package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.ARTransform
import com.example.model.EdgeFilterMode
import com.example.model.SketchTemplates
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
    assertEquals("AR Draw Trace", appName)
  }

  @Test
  fun `default templates are populated`() {
    assertTrue(SketchTemplates.defaultTemplates.isNotEmpty())
    val first = SketchTemplates.defaultTemplates.first()
    assertNotNull(first.drawableResId)
  }

  @Test
  fun `transform initial state values`() {
    val transform = ARTransform()
    assertEquals(0.50f, transform.opacity, 0.001f)
    assertEquals(1.0f, transform.scale, 0.001f)
    assertEquals(EdgeFilterMode.SKETCH_OUTLINE, transform.edgeFilter)
  }
}
