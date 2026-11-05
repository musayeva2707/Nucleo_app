package uz.nucleo

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Things only the Android activity can do (share sheet, file picker).
 * MainActivity provides the real implementation; screens call it through [LocalPlatform].
 */
interface PlatformActions {
    /** Opens Android's share sheet with plain text (ACTION_SEND). The game itself never goes online. */
    fun shareText(text: String)

    /** Lets the player pick where to save an exported save file (Storage Access Framework). */
    fun exportSave()

    /** Lets the player pick a save file to import (Storage Access Framework). */
    fun importSave()
}

val LocalPlatform = staticCompositionLocalOf<PlatformActions> {
    object : PlatformActions {
        override fun shareText(text: String) = Unit
        override fun exportSave() = Unit
        override fun importSave() = Unit
    }
}
