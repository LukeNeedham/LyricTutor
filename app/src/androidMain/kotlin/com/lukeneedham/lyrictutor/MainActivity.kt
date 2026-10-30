package com.lukeneedham.lyrictutor

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lukeneedham.lyrictutor.ui.Root

class MainActivity : ComponentActivity() {

    /** Text another app shared to this one (Spotify's Share sheet). */
    private var sharedText by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Content is drawn behind the status and navigation bars: the pages keep their text clear of them with the window insets
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Only on a fresh start: a recreated activity (rotation) still has the old intent, which was already handled
        if (savedInstanceState == null) sharedText = intent.sharedText()
        setContent { Root(sharedText = sharedText) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharedText = intent.sharedText()
    }

    private fun Intent.sharedText(): String? =
        if (action == Intent.ACTION_SEND) getStringExtra(Intent.EXTRA_TEXT) else null
}
