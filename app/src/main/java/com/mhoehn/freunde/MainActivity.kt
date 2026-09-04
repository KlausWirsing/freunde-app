package com.mhoehn.freunde

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.mhoehn.freunde.ui.LocalAppContainer
import com.mhoehn.freunde.ui.navigation.FreundeNavGraph
import com.mhoehn.freunde.ui.theme.FreundeTheme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Ohne Berechtigung werden Erinnerungen einfach nicht angezeigt - kein weiteres Handling nötig. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val pendingPersonId = intent?.getStringExtra(EXTRA_PERSON_ID)
        val container = (application as FreundeApplication).container

        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                FreundeTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        FreundeNavGraph(pendingPersonId = pendingPersonId)
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_PERSON_ID = "extra_person_id"
    }
}
