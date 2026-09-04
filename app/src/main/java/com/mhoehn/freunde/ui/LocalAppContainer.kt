package com.mhoehn.freunde.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.mhoehn.freunde.di.AppContainer

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("Kein AppContainer bereitgestellt - fehlt CompositionLocalProvider in MainActivity?")
}
