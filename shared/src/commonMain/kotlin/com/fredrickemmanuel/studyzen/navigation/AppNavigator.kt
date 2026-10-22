package com.fredrickemmanuel.studyzen.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf

/** A minimal back stack. The top of the stack is the screen being shown. */
@Stable
class AppNavigator(initial: List<Screen>) {
    val backStack = mutableStateListOf<Screen>().apply {
        addAll(initial.ifEmpty { listOf(Screen.Onboarding) })
    }

    val current: Screen get() = backStack.last()
    val canGoBack: Boolean get() = backStack.size > 1

    /** Open [screen] on top of the current one. */
    fun navigate(screen: Screen) {
        backStack.add(screen)
    }

    /** Swap the current screen for [screen] (back will skip the replaced one). */
    fun replaceTop(screen: Screen) {
        backStack.removeAt(backStack.lastIndex)
        backStack.add(screen)
    }

    /** Clear the stack so [screen] becomes the only entry (e.g. after log in). */
    fun replaceAll(screen: Screen) {
        backStack.clear()
        backStack.add(screen)
    }

    /**
     * Go back to the closest earlier [Screen.Main]; if the stack has none, behave like [back].
     * Used after finishing a flow (e.g. saving a plan) to return to the tabs.
     */
    fun backToMain() {
        val index = backStack.indexOfLast { it == Screen.Main }
        if (index < 0) {
            back()
            return
        }
        while (backStack.lastIndex > index) backStack.removeAt(backStack.lastIndex)
    }

    fun back() {
        if (canGoBack) backStack.removeAt(backStack.lastIndex)
    }
}

private val AppNavigatorSaver = listSaver<AppNavigator, String>(
    save = { navigator -> navigator.backStack.map { it.encode() } },
    restore = { saved -> AppNavigator(saved.map { Screen.decode(it) }) },
)

@Composable
fun rememberAppNavigator(start: Screen = Screen.Onboarding): AppNavigator =
    rememberSaveable(saver = AppNavigatorSaver) { AppNavigator(listOf(start)) }

/** Lets any screen navigate without passing callbacks through every level. */
val LocalAppNavigator = staticCompositionLocalOf<AppNavigator> { error("AppNavigator not provided") }
