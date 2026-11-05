package uz.nucleo.data

import kotlinx.serialization.json.Json
import uz.nucleo.game.GameState
import uz.nucleo.game.Settings
import java.io.File

/**
 * Saves the game as one JSON file on the device (no accounts, no internet).
 *
 * - `save.json` holds the whole [GameState].
 * - `settings.json` holds [Settings], so "Reset progress" keeps them.
 *
 * Files are written to a temporary file first and then renamed, so a crash mid-write can
 * never leave a half-written save. Android Auto Backup copies these files (see res/xml).
 */
class SaveStore(private val directory: File) {

    private val json = Json {
        ignoreUnknownKeys = true   // older apps can read newer saves
        encodeDefaults = true
        prettyPrint = false
    }

    private val saveFile get() = File(directory, "save.json")
    private val settingsFile get() = File(directory, "settings.json")

    fun loadGame(): GameState? = readText(saveFile)?.let(::decodeGame)

    fun saveGame(state: GameState) = writeText(saveFile, encodeGame(state))

    fun loadSettings(): Settings =
        readText(settingsFile)?.let { runCatching { json.decodeFromString<Settings>(it) }.getOrNull() } ?: Settings()

    fun saveSettings(settings: Settings) = writeText(settingsFile, json.encodeToString(Settings.serializer(), settings))

    fun deleteGame() {
        saveFile.delete()
    }

    /** The text written by "Export save file". */
    fun encodeGame(state: GameState): String = json.encodeToString(GameState.serializer(), state)

    /** Reads an exported save. Returns null if the text isn't a save or comes from a newer schema. */
    fun decodeGame(text: String): GameState? {
        val state = runCatching { json.decodeFromString(GameState.serializer(), text) }.getOrNull() ?: return null
        return if (state.schemaVersion <= GameState.CURRENT_SCHEMA) state else null
    }

    private fun readText(file: File): String? =
        if (file.exists()) runCatching { file.readText() }.getOrNull() else null

    private fun writeText(file: File, text: String) {
        directory.mkdirs()
        val tmp = File(directory, file.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }
}
