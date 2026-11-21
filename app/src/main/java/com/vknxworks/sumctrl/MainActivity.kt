package com.vknxworks.sumctrl

import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.sumctrl.ui.lock.AppLockGate
import com.vknxworks.sumctrl.ui.lock.GuardWindow
import com.vknxworks.sumctrl.ui.lock.AppLock
import com.vknxworks.sumctrl.ui.lock.lockCovering
import com.vknxworks.sumctrl.ui.nav.AppRoot
import com.vknxworks.sumctrl.ui.theme.SumCtrlTheme

class MainActivity : ComponentActivity() {

    override fun onResume() {
        super.onResume()
        // Coming here straight from the "Calculate" pop-up still has to pass the lock's time-away check.
        val c = container
        AppLock.onMainScreenShown(c.vault.enabled, c.settings.settings.value.lockTimeout.millis)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val c = container
        // Hidden from screenshots until the saved settings say otherwise, and no frame drawn before they're in.
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val firstFrame = FirstFrameGate(this)
        setContent {
            // Follows the phone's dark theme setting. Status and navigation bars take the background colour,
            // with light icons in dark and dark icons in light.
            val dark = isSystemInDarkTheme()
            DisposableEffect(dark) {
                val bars = if (dark) SystemBarStyle.dark(DARK_BAR) else SystemBarStyle.light(LIGHT_BAR, DARK_BAR)
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
                onDispose { }
            }
            // Read before the settings: once this is true, the settings read below are the saved ones.
            val loaded by c.settings.loaded.collectAsStateWithLifecycle()
            val settings by c.settings.settings.collectAsStateWithLifecycle()
            SideEffect { if (loaded) firstFrame.ready = true }
            // "Hide in screenshots": FLAG_SECURE (also blanks the Recents preview). Always on, whatever the
            // switch says, while the lock screen or a PIN screen shows.
            val secure = !loaded || settings.hideScreenshots || lockCovering(c.vault)
            // "Hide in recent apps": a blank Recents card; screenshots still work. Always with the app lock on,
            // so Recents never shows what's behind the lock.
            val blankRecents = settings.hideRecents || c.vault.enabled
            // No overlays over the lock and PIN screens, and their content hidden from accessibility misuse.
            GuardWindow(lockCovering(c.vault), sensitive = true)
            LaunchedEffect(secure, blankRecents) {
                if (secure) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                setRecentsScreenshotEnabled(!blankRecents)
            }
            SumCtrlTheme(dark) {
                // Nothing of the app is shown while the app lock is locked.
                AppLockGate { AppRoot() }
            }
        }
    }
}

/** The Ctrl / Signal backgrounds behind the status and navigation bars: dark #08080A, light #F5F5F1. */
private val DARK_BAR = android.graphics.Color.rgb(8, 8, 10)
private val LIGHT_BAR = android.graphics.Color.rgb(245, 245, 241)

/**
 * Holds back the window's first frame until the saved settings have been applied to what is drawn, so the
 * app doesn't flash its default settings first. The system's start screen stays up for those few
 * milliseconds; after [MAX_WAIT_MS] the frame is drawn regardless.
 */
internal class FirstFrameGate(activity: ComponentActivity) {
    /** Set once a composition has run with the saved settings. */
    @Volatile
    var ready = false

    init {
        val content: View = activity.findViewById(android.R.id.content)
        val started = SystemClock.uptimeMillis()
        content.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (!ready && SystemClock.uptimeMillis() - started < MAX_WAIT_MS) return false
                content.viewTreeObserver.removeOnPreDrawListener(this)
                return true
            }
        })
    }

    private companion object {
        const val MAX_WAIT_MS = 500L
    }
}
