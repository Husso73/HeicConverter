package com.example.heicconverter.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.heicconverter.converter.BatchConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.example.heicconverter.R
import androidx.compose.ui.res.stringResource
// ── États ─────────────────────────────────────────────────────────────────────
sealed interface ConversionState {
    data object Idle                              : ConversionState
    data class  Converting(val done: Int, val total: Int) : ConversionState
    data class  Done(val count: Int)              : ConversionState
    data class  Error(val msg: String)            : ConversionState
}

// ── Ecran principal ───────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var images  by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var state   by remember { mutableStateOf<ConversionState>(ConversionState.Idle) }
    var showResultDialog by remember { mutableStateOf(false) }
    var lastResult       by remember { mutableStateOf<ConversionState.Done?>(null) }
    var lastError        by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { picked ->
        if (picked.isNotEmpty()) {
            val combined = (images + picked).distinctBy { it.toString() }
            if (combined.size > 20) {
                images = combined.take(20)
                Toast.makeText(context, context.getString(R.string.limit_reached), Toast.LENGTH_SHORT).show()
            } else {
                images = combined
            }
            state = ConversionState.Idle
        }
    }

    // ── Dialog résultat ───────────────────────────────────────────────────────
    if (showResultDialog) {
        AlertDialog(
            onDismissRequest = {
                showResultDialog = false
                state = ConversionState.Idle
            },
            shape = RoundedCornerShape(20.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            if (lastError == null) Color(0xFF16A34A).copy(alpha = 0.12f)
                            else Color(0xFFDC2626).copy(alpha = 0.12f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (lastError == null) Icons.Outlined.CheckCircle
                        else Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = if (lastError == null) Color(0xFF16A34A) else Color(0xFFDC2626),
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = if (lastError == null) stringResource(R.string.conversion_done)
                    else stringResource(R.string.error),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = lastError ?: stringResource(R.string.success_message, lastResult?.count ?: 0),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                if (lastError == null) {
                    val folder = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        "HEIC Converter"
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.Folder, null,
                                    modifier = Modifier.size(16.dp),
                                    tint     = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    folder.absolutePath,
                                    fontSize   = 11.sp,
                                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Button(
                            onClick = {
                                showResultDialog = false
                                state = ConversionState.Idle
                            },
                            shape  = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text(stringResource(R.string.ok))
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            showResultDialog = false
                            state = ConversionState.Idle
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.ok))
                    }
                }
            },
            dismissButton = { null }
        )
    }

// ── Scaffold ──────────────────────────────────────────────────────────────
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_outline_insert_photo_24),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF3B82F6))
                                .padding(4.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = stringResource(R.string.app_description),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (images.isNotEmpty() && state is ConversionState.Idle) {
                        TextButton(onClick = {
                            images = emptyList()
                            state  = ConversionState.Idle
                        }) {
                            Icon(Icons.Outlined.DeleteSweep, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(text = stringResource(R.string.clear_all), fontSize = 13.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // ── Bouton ajouter ────────────────────────────────────────────────
            OutlinedButton(
                onClick  = { picker.launch(arrayOf("image/heic", "image/heif")) },
                enabled  = state !is ConversionState.Converting && images.size < 20,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(14.dp),
                border   = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp)
            ) {
                Icon(Icons.Outlined.AddPhotoAlternate, null, Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    if (images.isEmpty())  stringResource(R.string.pick_images)
                    else stringResource(R.string.add_more),
                    fontWeight = FontWeight.SemiBold
                )
            }

            // ── Grille ────────────────────────────────────────────────────────
            if (images.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text=stringResource(R.string.photos_selected, images.size),
                        fontSize = 13.sp, fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                ImageGrid(
                    images   = images,
                    enabled  = state !is ConversionState.Converting,
                    onRemove = { uri ->
                        images = images.filter { it != uri }
                    }
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Barre de progression ──────────────────────────────────────────
            if (state is ConversionState.Converting) {
                val s = state as ConversionState.Converting
                val progress = if (s.total > 0) s.done.toFloat() / s.total else 0f

                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(16.dp),
                    colors    = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.converting),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                "${s.done} / ${s.total}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF2563EB)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color            = Color(0xFF2563EB),
                            trackColor       = Color(0xFF2563EB).copy(alpha = 0.15f),
                            strokeCap        = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${(progress * 100).toInt()} %",
                            fontSize = 12.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            // ── Bouton convertir ──────────────────────────────────────────────
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                        lastError = context.getString(R.string.android_version_error)
                        lastResult = null
                        showResultDialog = true
                        return@Button
                    }
                    state = ConversionState.Converting(0, images.size)
                    val toConvert = images.toList()
                    scope.launch {
                        try {
                            val results = withContext(Dispatchers.IO) {
                                BatchConverter.convertAll(context, toConvert) { progress ->
                                    // callback appelé après chaque image convertie
                                    state = ConversionState.Converting(progress, toConvert.size)
                                }
                            }
                            lastResult = ConversionState.Done(results.size)
                            lastError  = null
                        } catch (e: Exception) {
                            lastResult = null
                            lastError  = e.message ?: context.getString(R.string.unknown_error)
                        }
                        showResultDialog = true
                        state = ConversionState.Idle
                    }
                },
                enabled  = images.isNotEmpty() && state !is ConversionState.Converting,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = Color(0xFF2563EB),
                    disabledContainerColor = Color(0xFF2563EB).copy(alpha = 0.4f)
                )
            ) {
                Icon(Icons.Outlined.SwapHoriz, null, Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.convert_to_jpg), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Grille images ─────────────────────────────────────────────────────────────
@Composable
private fun ImageGrid(
    images: List<Uri>,
    enabled: Boolean,
    onRemove: (Uri) -> Unit
) {
    val cols       = 3
    val rows       = (images.size + cols - 1) / cols
    val cellSize   = 104.dp
    val gap        = 6.dp
    val gridHeight = (cellSize + gap) * rows

    LazyVerticalGrid(
        columns               = GridCells.Fixed(cols),
        modifier              = Modifier
            .fillMaxWidth()
            .height(gridHeight)
            .clip(RoundedCornerShape(14.dp)),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalArrangement   = Arrangement.spacedBy(gap),
        userScrollEnabled     = false
    ) {
        items(images, key = { it.toString() }) { uri ->
            Box(modifier = Modifier.size(cellSize)) {
                AsyncImage(
                    model              = uri,
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                if (enabled) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(0xCC0F172A))
                            .clickable { onRemove(uri) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Close, "Retirer",
                            tint = Color.White, modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

// 2. CRÉE CETTE FONCTION D'APERÇU JUSTE EN DESSOUS
// C'est grâce à ça que tu le verras dans l'onglet que tu pointes
@Preview(showBackground = true)
@Composable
fun AppLogoPreview() {
    // Tu peux même mettre un padding ici pour mieux le voir dans l'aperçu
    Box(modifier = Modifier.padding(16.dp)) {
        HomeScreen()
    }
}