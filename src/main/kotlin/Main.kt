import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.AwtWindow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.fasterxml.jackson.databind.ObjectMapper
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

    val importUrlPart = "/api/v1/charge-cards/import/"
    val fleetUrlPart = "/api/v1/fleet/import/"

    var host by remember { mutableStateOf("https://fastned-rebate-service.acc.lightbase.nl") }
    var period by remember { mutableStateOf(formatter.format(calendar.time)) }
    var apiKey by remember { mutableStateOf("") }

    var isFileChooserOpen by remember { mutableStateOf(false) }
    var chosenFilePath by remember { mutableStateOf("") }

    var importText by remember { mutableStateOf("Import!") }
    var isImportEnabled by remember { mutableStateOf(false) }

    var outputText by remember { mutableStateOf(buildAnnotatedString { append("") }) }

    if (isFileChooserOpen) {
        FileDialog(
            onCloseRequest = {
                isFileChooserOpen = false
                chosenFilePath = it ?: ""

                if (chosenFilePath.isNotEmpty()) {
                    val importFile = isImportFile(chosenFilePath)
                    if (importFile != null) {
                        outputText =
                            buildAnnotatedString { append("The file is a charge card import card, press import to import the file") }
                        isImportEnabled = true
                    } else {
                        outputText =
                            buildAnnotatedString { append("The selected file is not an import file (missing the correct headers)") }
                    }
                } else {
                    outputText = buildAnnotatedString { append("No file selected") }
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
                        calendar.add(Calendar.MONTH, -1)
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
                        calendar.add(Calendar.MONTH, 1)
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
                    isImportEnabled = false
                    importText = "Importing..."

                    val importFile = isImportFile(chosenFilePath)
                    if (importFile != null) {
                        outputText = buildAnnotatedString { append("Importing, please wait!") }
                        coroutineScope.launch {
                            uploadFile(
                                importFile,
                                host.trimEnd('/') + importUrlPart + period,
                                apiKey,
                                { body: String, statusCode: Int ->
                                    outputText =
                                        convertResultBody(body, statusCode)
                                })
                            importText = "Import!"
                        }
                    } else {
                        outputText = buildAnnotatedString { append("Import file not found, was it removed?") }
                    }
                }, enabled = isImportEnabled) {
                    Text(importText)
                }
            }

            Spacer(Modifier.requiredHeight(10.dp))

            SelectableText(text = outputText, modifier = Modifier.fillMaxWidth().fillMaxHeight())
        }

    }
}

@Suppress("DEPRECATION")
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Rebate Uploader",
        icon = BitmapPainter(useResource("fastned-48.png", ::loadImageBitmap)),
    ) {
        window.minimumSize = Dimension(1200, 800)
        App()
    }
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

@Composable
private fun SelectableText(text: AnnotatedString, modifier: Modifier = Modifier.fillMaxWidth()) {
    SelectionContainer {
        Text(text = text, modifier = modifier)
    }
}

fun convertResultBody(text: String, statusCode: Int): AnnotatedString {
    val pretty = prettyPrintJsonUsingDefaultPrettyPrinter(text)
    if (statusCode == 202 && text.contains("reportUrl")) {
        val regex = Regex("https?://[^\"]+")
        val match = regex.find(pretty)
        if (match != null) {
            val myStyle = SpanStyle(
                color = Color(0xff64B5F6),
                textDecoration = TextDecoration.Underline
            )
            val preText = "The url for the report won't be usable until validation is complete!\n\n"
            val url = match.value
            val annotatedString = buildAnnotatedString {
                append(preText)
                append(pretty.substring(0, match.range.first))
                withLink(LinkAnnotation.Url(url = url)) {
                    withStyle(style = myStyle) {
                        append(url)
                    }
                }
                append(pretty.substring(match.range.last + 1))
            }
            return annotatedString
        }

    }

    return buildAnnotatedString {
        append(pretty)
    }
}

fun prettyPrintJsonUsingDefaultPrettyPrinter(uglyJsonString: String): String {
    val objectMapper = ObjectMapper()
    val jsonObject = objectMapper.readValue(uglyJsonString, Any::class.java)
    return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
}
