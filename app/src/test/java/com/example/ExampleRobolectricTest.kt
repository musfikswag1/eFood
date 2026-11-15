package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Coupon
import com.example.data.model.formatBdt
import com.example.data.model.isValidBangladeshPhone
import com.example.data.model.normalizeBangladeshPhone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    assertEquals("eFood", appName)
  }

  @Test
  fun `bangladesh phone validation and normalization`() {
    assertTrue(isValidBangladeshPhone("01711223344"))
    assertTrue(isValidBangladeshPhone("+8801811223344"))
    assertFalse(isValidBangladeshPhone("12345"))
    assertEquals("+8801711223344", normalizeBangladeshPhone("01711-223344"))
  }

  @Test
  fun `bdt coupon discount and formatting`() {
    val percentCoupon = Coupon(
      code = "EFOOD20",
      discountType = "PERCENT",
      discountValue = 20.0,
      minOrderBdt = 300.0,
      maxDiscountBdt = 150.0,
      isActive = true
    )
    assertEquals(100.0, percentCoupon.calculateDiscount(500.0), 0.01)
    assertEquals(150.0, percentCoupon.calculateDiscount(1200.0), 0.01)
    assertEquals(0.0, percentCoupon.calculateDiscount(200.0), 0.01)
    assertEquals("৳450", formatBdt(450.0))
  }
}
