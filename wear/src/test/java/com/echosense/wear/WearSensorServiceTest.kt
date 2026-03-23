package com.echosense.wear

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WearSensorServiceTest {

    @Test
    fun testServiceStarts() {
        val controller: ServiceController<WearSensorService> = Robolectric.buildService(WearSensorService::class.java)
        val service = controller.create().get()
        assertNotNull(service)
    }

    @Test
    fun testStartCommand() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, WearSensorService::class.java)
        val controller = Robolectric.buildService(WearSensorService::class.java, intent)
        
        controller.create().startCommand(0, 0)
        // We can't easily check foreground status in Robolectric without more setup,
        // but we verify it doesn't crash.
    }
}
