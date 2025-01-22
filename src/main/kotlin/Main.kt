import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FileDataPart
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
@Preview
fun App() {
    val coroutineScope = rememberCoroutineScope()

    val time = Calendar.getInstance().time
    val formatter = SimpleDateFormat("MM-yyyy")
    val current = formatter.format(time)

    var url by remember { mutableStateOf("http://localhost:8080/api/v1/charge-card/import/${current}") }
    var apiKey by remember { mutableStateOf("password1234") }

    var importText by remember { mutableStateOf("Import!") }
    var output by remember { mutableStateOf("") }
    var importEnabled by remember { mutableStateOf(false) }

    MaterialTheme {

        Column(modifier = Modifier.padding(20.dp)) {
            OutlinedTextField(
                value = url,
                onValueChange = {
                    url = it.trim()
                },
                label = { Text("Url") },
                modifier = Modifier.fillMaxWidth(),
            )

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
                    val files = checkFiles()
                    val importFile = determineImportFile(files)
                    if (importFile != null) {
                        output = "Found this file: " + importFile.name
                        importEnabled = true
                    } else {
                        output = "No import found"
                        importEnabled = false
                    }
                }) {
                    Text("Check")
                }

                Spacer(Modifier.requiredWidth(5.dp))

                Button(onClick = {
                    importText = "Importing"
                    val importFile = determineImportFile(checkFiles())
                    if (importFile != null) {
                        coroutineScope.launch {
                            uploadFile(importFile, url, apiKey, { body: String, statusCode: Int -> output = body })
                        }
                    } else {
                        output = "Import file not found"
                    }
                    importEnabled = false
                }, enabled = importEnabled) {
                    Text(importText)
//
//
//                    url.httpUpload()
//                        .add(FileDataPart(file, name = "file"))
//                        .response { request, response, result ->
//                            println(response)
//                        }.get()
                }
            }

            Spacer(Modifier.requiredHeight(10.dp))

            Text(text = output, modifier = Modifier.fillMaxWidth().fillMaxHeight())
        }

    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Rebate Uploader") {
        App()
    }
}

fun checkFiles(): List<File> {
    val root = System.getProperty("user.dir")
    val files = File(root).walk().filter { it.extension == "csv" }.toList().sortedByDescending { it.lastModified() }
    return files
}

fun determineImportFile(files: List<File>): File? {
    for (file in files) {
        val firstLine = file.useLines { it.firstOrNull() }

        println(firstLine?.lowercase()?.contains("country"))
        println(firstLine?.lowercase()?.contains("rfid number"))
        if (
            firstLine != null &&
            firstLine.lowercase().contains("country") &&
            firstLine.lowercase().contains("rfid number")
        ) {
            return file
        }
    }

    return null
}

suspend fun uploadFile(file: File, url: String, apiKey: String, handler: (String, Int) -> Unit) {
    Fuel.upload(url)
        .add { FileDataPart(File(file.path), name = "file", filename = file.name) }
        .header(mapOf("Authorization" to "Bearer $apiKey"))
        .responseString() { _, response, _ -> handler(String(response.data), response.statusCode) }
}