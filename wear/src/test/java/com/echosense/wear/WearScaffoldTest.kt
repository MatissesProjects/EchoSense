package com.echosense.wear

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class WearScaffoldTest {

    @Test
    fun testContextAvailability() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNotNull(context)
    }

    @Test
    fun testPackageName() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val packageName = context.packageName
        println("DEBUG: Package Name is $packageName")
        assertNotNull(packageName)
        // Robolectric with Config.NONE often returns "org.robolectric.default" 
        // if no other configuration is provided.
        // We just want to see it run for now.
    }
}
