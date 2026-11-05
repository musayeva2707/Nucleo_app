package uz.nucleo

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import uz.nucleo.data.SaveStore

/**
 * The only Activity. It creates the ViewModel, keeps the game clock in step with the app's
 * visibility, and provides the few things only Android can do (share sheet, save-file picker).
 */
class MainActivity : ComponentActivity() {

    private val vm: GameViewModel by viewModels { GameViewModel.factory(SaveStore(filesDir)) }

    /** "Export save file": the player picks where the JSON file goes (Storage Access Framework). */
    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@registerForActivityResult
        val ok = runCatching {
            contentResolver.openOutputStream(uri)?.use { it.write(vm.exportSave().toByteArray()) }
        }.isSuccess
        toast(if (ok) "Save exported" else "Could not write the file")
    }

    /** "Import save file": reads a JSON save the player picks and replaces the current game. */
    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        val text = runCatching { contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } }.getOrNull()
        if (text == null || !vm.importSave(text)) toast("That file is not a Nucleo save")
    }

    private val platform = object : PlatformActions {
        override fun shareText(text: String) {
            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
            startActivity(Intent.createChooser(send, null))
        }

        override fun exportSave() = exportLauncher.launch("nucleo-save.json")

        override fun importSave() = importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val versionName = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull() ?: "1.0.0"
        setContent {
            CompositionLocalProvider(LocalPlatform provides platform) {
                NucleoApp(vm, versionName)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        vm.onAppForeground()
    }

    override fun onStop() {
        vm.onAppBackground()
        super.onStop()
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}
