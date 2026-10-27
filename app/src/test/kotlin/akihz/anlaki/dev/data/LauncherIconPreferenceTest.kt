package akihz.anlaki.dev.data

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherIconPreferenceTest {
    @Test
    fun `existing installs default to material and can persist the normal icon`() {
        val context = mockk<Context>()
        val prefs = mockk<SharedPreferences>(relaxed = true)
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        val booleans = mutableMapOf<String, Boolean>()
        every { context.getSharedPreferences(any(), Context.MODE_PRIVATE) } returns prefs
        every { prefs.getFloat("last_rate", 60f) } returns 60f
        every { prefs.getBoolean(any(), any()) } answers {
            booleans[firstArg<String>()] ?: secondArg<Boolean>()
        }
        every { prefs.edit() } returns editor
        every { editor.putBoolean(any(), any()) } answers {
            booleans[firstArg()] = secondArg()
            editor
        }
        PreferencesHelper.init(context)

        assertTrue(PreferencesHelper.themedAppIcon)
        PreferencesHelper.themedAppIcon = false
        assertFalse(PreferencesHelper.themedAppIcon)
        verify { editor.apply() }
        PreferencesHelper.themedAppIcon = true
        assertTrue(PreferencesHelper.themedAppIcon)
    }
}
