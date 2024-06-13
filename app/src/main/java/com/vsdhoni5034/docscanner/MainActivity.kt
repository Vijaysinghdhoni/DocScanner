package com.vsdhoni5034.docscanner

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.vsdhoni5034.docscanner.ui.theme.DocScannerTheme
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    private lateinit var scannerLauncher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val options = GmsDocumentScannerOptions.Builder()
            .setScannerMode(SCANNER_MODE_FULL)
            .setGalleryImportAllowed(true)
            .setPageLimit(10)
            .setResultFormats(RESULT_FORMAT_PDF)
            .build()

        val scanner = GmsDocumentScanning.getClient(options)

        enableEdgeToEdge()
        setContent {
            DocScannerTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(title = {
                            Text(
                                text = "Doc-Scanner App",
                                style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            )
                        })
                    },
                    floatingActionButton = {
                        ExtendedFloatingActionButton(
                            onClick = {
                                scanner.getStartScanIntent(this@MainActivity)
                                    .addOnSuccessListener {
                                        scannerLauncher.launch(
                                            IntentSenderRequest.Builder(it).build()
                                        )
                                    }.addOnFailureListener {
                                        Toast.makeText(
                                            applicationContext,
                                            it.message,
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.camera_ic),
                                    null
                                )
                            },
                            text = { Text(text = "Scan", fontWeight = FontWeight.Bold) },
                        )
                    }
                ) { innerPadding ->
                    val pdfs = remember {
                        mutableStateListOf<GmsDocumentScanningResult.Pdf>()
                    }
                    val isEmpty = remember { derivedStateOf { pdfs.isEmpty() } }

                    scannerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartIntentSenderForResult(),
                        onResult = { result ->
                            if (result.resultCode == RESULT_OK) {
                                val scanningResult =
                                    GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                                scanningResult?.pdf?.let { pdf ->
                                    pdfs.add(pdf)
                                    val fos = FileOutputStream(File(filesDir, "Scan.pdf"))
                                    contentResolver.openInputStream(pdf.uri)?.copyTo(fos)
                                }
                            }
                        }
                    )

                    Box(modifier = Modifier.padding(innerPadding)) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp)
                        ) {


                            items(pdfs) { pdfss ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                        .padding(10.dp),
                                    onClick = {
                                        val pdfUri = pdfss.uri
                                        val pdfFileUri = FileProvider.getUriForFile(
                                            this@MainActivity,
                                            this@MainActivity.packageName + ".provider",
                                            pdfUri.toFile()
                                        )
                                        val browserIntent = Intent(Intent.ACTION_VIEW, pdfFileUri)
                                        browserIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        this@MainActivity.startActivity(browserIntent)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.pdf_ic),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxHeight()
                                        )
                                        Text(text = pdfss.uri.lastPathSegment ?: "unknown")
                                    }
                                }
                            }
                        }
                        if (isEmpty.value) {
                            NoDocs(modifier = Modifier.align(Alignment.Center))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoDocs(modifier: Modifier = Modifier) {
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.RawRes(
            R.raw.nodoc
        )
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(7.dp)
    ) {

        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier
                .testTag("Lottie view")
                .size(300.dp)
        )

    }

}

//kotlin 2.0
//ksp
//day night theming
