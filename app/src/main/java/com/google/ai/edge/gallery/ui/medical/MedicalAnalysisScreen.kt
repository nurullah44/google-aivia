package com.google.ai.edge.gallery.ui.medical

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
// import coil.compose.AsyncImage
import com.google.ai.edge.gallery.data.AnalysisRecord
import com.google.ai.edge.gallery.data.HistorySource
// import java.io.File

object MedicalAnalysisDestination {
    const val route = "medical_analysis"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalAnalysisScreen(
    viewModel: MedicalAnalysisViewModel = hiltViewModel(),
    navigateUp: () -> Unit,
    navigateToAnalysis: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // File picker for text files
    val textFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { fileUri ->
            try {
                val inputStream = context.contentResolver.openInputStream(fileUri)
                val text = inputStream?.bufferedReader()?.readText() ?: ""
                viewModel.updatePatientHistory(text, HistorySource.FILE)
            } catch (e: Exception) {
                // Handle file read error
            }
        }
    }

    // Image picker
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.updateSelectedImage(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Medical Image Analysis") },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showPatientForm() },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add New Analysis",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                label = { Text("Search patients...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true
            )

            // Analysis History Grid
            if (uiState.filteredRecords.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = if (uiState.searchQuery.isBlank()) {
                                "No medical analyses yet.\nTap + to create your first analysis."
                            } else {
                                "No patients found matching \"${uiState.searchQuery}\""
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.filteredRecords) { record ->
                        AnalysisRecordCard(
                            record = record,
                            onClick = { /* TODO: Navigate to detail */ }
                        )
                    }
                }
            }
        }
    }

    // Patient Form Dialog
    if (uiState.showPatientForm) {
        PatientFormDialog(
            uiState = uiState,
            onDismiss = { viewModel.hidePatientForm() },
            onFirstNameChange = viewModel::updatePatientFirstName,
            onLastNameChange = viewModel::updatePatientLastName,
            onHistoryChange = { history -> viewModel.updatePatientHistory(history) },
            onSelectTextFile = { textFileLauncher.launch("text/*") },
            onSelectImage = { imageLauncher.launch("image/*") },
            onAnalyze = {
                viewModel.startAnalysis()?.let { patientData ->
                    viewModel.hidePatientForm()
                    // Navigate to Gemma 3n chat with medical context and patient data
                    navigateToAnalysis("Gemma-3n-E2B-it-int4")
                }
            }
        )
    }

    // Error Snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show snackbar and clear error
            viewModel.clearError()
        }
    }
}

@Composable
private fun AnalysisRecordCard(
    record: AnalysisRecord,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Patient Image Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Image,
                    contentDescription = "Medical Image",
                    tint = MaterialTheme.colorScheme.outline
                )
            }

            // Patient Name
            Text(
                text = record.getPatientDisplayName(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Date
            Text(
                text = record.getFormattedDate(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PatientFormDialog(
    uiState: MedicalAnalysisUiState,
    onDismiss: () -> Unit,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onHistoryChange: (String) -> Unit,
    onSelectTextFile: () -> Unit,
    onSelectImage: () -> Unit,
    onAnalyze: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Medical Analysis") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Patient Name Fields
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.patientFirstName,
                        onValueChange = onFirstNameChange,
                        label = { Text("First Name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uiState.patientLastName,
                        onValueChange = onLastNameChange,
                        label = { Text("Last Name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Patient History Section
                Text(
                    text = "Patient History",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )

                OutlinedTextField(
                    value = uiState.patientHistory,
                    onValueChange = onHistoryChange,
                    label = { Text("Medical history, symptoms, notes...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5
                )

                // File Upload Option
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSelectTextFile,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Description, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Upload Text")
                    }
                }

                if (uiState.historySource == HistorySource.FILE) {
                    Text(
                        text = "✓ History loaded from file",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Image Selection
                Text(
                    text = "Medical Image",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSelectImage,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Select Image")
                    }
                }

                if (uiState.selectedImageUri != null) {
                    Text(
                        text = "✓ Image selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAnalyze,
                enabled = uiState.isFormValid
            ) {
                Text("Analyze")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
