@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package de.drick.compose.hotpreview

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.intl.Locale
import org.jetbrains.compose.resources.ComposeEnvironment
import org.jetbrains.compose.resources.DensityQualifier
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.LanguageQualifier
import org.jetbrains.compose.resources.LocalComposeEnvironment
import org.jetbrains.compose.resources.RegionQualifier
import org.jetbrains.compose.resources.ResourceEnvironment
import org.jetbrains.compose.resources.ThemeQualifier

@Composable
internal fun OverrideEnv(locale: String, content: @Composable () -> Unit) {
    val env = remember {
        customComposeEnvironment(Locale(locale))
    }
    CompositionLocalProvider(
        LocalComposeEnvironment provides env, content = content
    )
}

@OptIn(ExperimentalResourceApi::class, InternalResourceApi::class)
private fun customComposeEnvironment(composeLocale: Locale) = object : ComposeEnvironment {
    @Composable
    override fun rememberEnvironment(): ResourceEnvironment {
        val composeTheme = isSystemInDarkTheme()
        val composeDensity = LocalDensity.current

        //cache ResourceEnvironment unless compose environment is changed
        return remember(composeLocale, composeTheme, composeDensity) {
            ResourceEnvironment(
                LanguageQualifier(composeLocale.language),
                RegionQualifier(composeLocale.region),
                ThemeQualifier.selectByValue(composeTheme),
                DensityQualifier.selectByDensity(composeDensity.density)
            )
        }
    }
}