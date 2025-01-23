import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.AwtWindow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FileDataPart
import kotlinx.coroutines.launch
import java.awt.Dimension
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter
import java.text.SimpleDateFormat
import java.util.*

@Composable
@Preview
fun App() {
    val coroutineScope = rememberCoroutineScope()

    var calendar = Calendar.getInstance()
    val formatter = SimpleDateFormat("MM-yyyy")

    val importUrlPart = "/api/v1/charge-card/import/"
    val fleetUrlPart = "/api/v1/fleet/import/"

    var host by remember { mutableStateOf("http://localhost:8080") }
    var period by remember { mutableStateOf(formatter.format(calendar.time)) }
    var apiKey by remember { mutableStateOf("password1234") }

    var isFileChooserOpen by remember { mutableStateOf(false) }
    var chosenFilePath by remember { mutableStateOf("") }

    var importText by remember { mutableStateOf("Import!") }
    var isImportEnabled by remember { mutableStateOf(false) }

    var outputText by remember { mutableStateOf("") }

    if (isFileChooserOpen) {
        FileDialog(
            onCloseRequest = {
                isFileChooserOpen = false
                chosenFilePath = it ?: ""

                if (chosenFilePath.isNotEmpty()) {
                    val importFile = isImportFile(chosenFilePath)
                    if (importFile != null) {
                        outputText = "The file is a charge card import card, press import to import the file"
                        isImportEnabled = true
                    } else {
                        outputText = "The selected file is not an import file (missing the correct headers)"
                    }
                } else {
                    outputText = "No file selected"
                }
            }
        )
    }

    MaterialTheme {

        Column(modifier = Modifier.padding(20.dp)) {
            Row() {
                OutlinedTextField(
                    value = host,
                    onValueChange = {
                        host = it.trim()
                    },
                    label = { Text("Host") },
                    modifier = Modifier
                        .weight(4F)
                        .padding(0.dp, 0.dp, 10.dp, 0.dp),
                )

                Button(
                    onClick = {
                        calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 1)
                        period = formatter.format(calendar.time)
                    },
                    modifier = Modifier.weight(0.25F, true).align(Alignment.CenterVertically),
                ) {
                    Text("↓")
                }

                OutlinedTextField(
                    value = period,
                    onValueChange = {

                    },
                    label = { Text("Period") },
                    modifier = Modifier.weight(1F).padding(10.dp, 0.dp, 10.dp, 0.dp),
                )

                Button(
                    onClick = {
                        calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1)
                        period = formatter.format(calendar.time)
                    },
                    modifier = Modifier.weight(0.25F, true).align(Alignment.CenterVertically),
                ) {
                    Text("↑")
                }
            }

            Spacer(Modifier.requiredHeight(10.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = {
                    apiKey = it.trim()
                },
                label = { Text("API Key of partner") },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.requiredHeight(10.dp))

            Row() {
                Button(onClick = {
                    isFileChooserOpen = true
                }) {
                    Text("Select file")
                }

                Spacer(Modifier.requiredWidth(5.dp))

                Button(onClick = {
                    importText = "Importing"
                    val importFile = isImportFile(chosenFilePath)
                    if (importFile != null) {
                        coroutineScope.launch {
                            uploadFile(
                                importFile,
                                host.trimEnd('/') + importUrlPart + period,
                                apiKey,
                                { body: String, statusCode: Int -> outputText = body })
                        }
                    } else {
                        outputText = "Import file not found, was it removed?"
                    }
                    importText = "Import!"
                    isImportEnabled = false
                }, enabled = isImportEnabled) {
                    Text(importText)
                }
            }

            Spacer(Modifier.requiredHeight(10.dp))

            Text(text = outputText, modifier = Modifier.fillMaxWidth().fillMaxHeight())
        }

    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Rebate Uploader") {
        window.minimumSize = Dimension(1200, 800)
        App()
    }
}

fun checkFiles(): List<File> {
    val root = System.getProperty("user.dir")
    val files = File(root).walk().filter { it.extension == "csv" }.toList().sortedByDescending { it.lastModified() }
    return files
}

fun isImportFile(path: String): File? {
    val file = File(path)
    val firstLine = file.useLines { it.firstOrNull() }

    if (
        firstLine != null &&
        firstLine.lowercase().contains("country") &&
        firstLine.lowercase().contains("rfid number")
    ) {
        return file
    }

    return null
}

suspend fun uploadFile(file: File, url: String, apiKey: String, handler: (String, Int) -> Unit) {
    Fuel.upload(url)
        .add { FileDataPart(File(file.path), name = "file", filename = file.name) }
        .header(mapOf("Authorization" to "Bearer $apiKey"))
        .responseString() { _, response, _ -> handler(String(response.data), response.statusCode) }
}

@Composable
private fun FileDialog(
    parent: Frame? = null,
    onCloseRequest: (result: String?) -> Unit,
) = AwtWindow(
    create = {
        object : FileDialog(parent, "Choose a file", LOAD) {
            override fun setVisible(value: Boolean) {
                super.setVisible(value)
                super.setMultipleMode(false)
                super.setFilenameFilter(FilenameFilter { _, name -> name.endsWith(".csv") })
                if (value) {
                    onCloseRequest(directory + file)
                }
            }
        }
    },
    dispose = FileDialog::dispose
)