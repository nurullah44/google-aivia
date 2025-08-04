package com.google.ai.edge.gallery.ui.farmer

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Agriculture
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
import coil.compose.AsyncImage
import com.google.ai.edge.gallery.data.CropAnalysisRecord
import java.io.File

object CropAnalysisDestination {
    const val route = "crop_analysis"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropAnalysisScreen(
  viewModel: CropAnalysisViewModel = hiltViewModel(),
  navigateUp: () -> Unit,
  navigateToAnalysis: (String) -> Unit,
  navigateToDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Image picker
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.updateSelectedImage(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crop Analysis") },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCropForm() },
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
                label = { Text("Search crops...") },
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
                            Icons.Outlined.Agriculture,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = if (uiState.searchQuery.isBlank()) {
                                "No crop analyses yet.\nTap + to create your first analysis."
                            } else {
                                "No crops found matching \"${uiState.searchQuery}\""
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
                            onClick = { 
                                // Navigate to detail screen
                                navigateToDetail(record.id)
                            },
                            onDelete = {
                                viewModel.deleteAnalysisRecord(record.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // Crop Form Dialog
    if (uiState.showCropForm) {
        CropFormDialog(
            uiState = uiState,
            onDismiss = { viewModel.hideCropForm() },
            onCropTypeChange = viewModel::updateCropType,
            onLocationChange = viewModel::updateLocation,
            onClimateChange = viewModel::updateClimate,
            onSoilTypeChange = viewModel::updateSoilType,
            onSelectImage = { imageLauncher.launch("image/*") },
            onAnalyze = {
                viewModel.startAnalysis()?.let { cropData ->
                    viewModel.hideCropForm()
                    // Navigate to Gemma 3n chat with crop context and crop data
                    navigateToAnalysis("Gemma-3n-E2B-it-int4")
                }
            },
            onAnalysisTypeChange = viewModel::updateAnalysisType
        )
    }

    // Analysis Details Dialog
    if (uiState.showAnalysisResult) {
        uiState.currentAnalysis?.let { analysisText ->
            AnalysisDetailsDialog(
                analysisText = analysisText,
                onDismiss = { viewModel.hideAnalysisDetails() }
            )
        }
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
private fun AnalysisDetailsDialog(
    analysisText: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Analysis Details") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = analysisText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun AnalysisRecordCard(
    record: CropAnalysisRecord,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Crop Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (record.imagePath != null) {
                        AsyncImage(
                            model = record.imagePath, // Use path directly, Coil handles both File and URI
                            contentDescription = "Crop Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Outlined.Image,
                            contentDescription = "Crop Image",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Crop Name
                Text(
                    text = record.getCropDisplayName(),
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
            
            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CropFormDialog(
    uiState: CropAnalysisUiState,
    onDismiss: () -> Unit,
    onCropTypeChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onClimateChange: (String) -> Unit,
    onSoilTypeChange: (String) -> Unit,
    onSelectImage: () -> Unit,
    onAnalyze: () -> Unit,
    onAnalysisTypeChange: (AnalysisType) -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Crop Analysis") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Crop Information Fields
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.cropType,
                        onValueChange = onCropTypeChange,
                        label = { Text("Crop Type") },
                        placeholder = { Text("e.g., Wheat, Corn, Tomato") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uiState.location,
                        onValueChange = onLocationChange,
                        label = { Text("Location") },
                        placeholder = { Text("e.g., Field A, Greenhouse 1") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.climate,
                        onValueChange = onClimateChange,
                        label = { Text("Climate") },
                        placeholder = { Text("e.g., Humid, Dry, Moderate") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uiState.soilType,
                        onValueChange = onSoilTypeChange,
                        label = { Text("Soil Type") },
                        placeholder = { Text("e.g., Clay, Sandy, Loam") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Image Selection
                Text(
                    text = "Crop Image",
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

                // Analysis Type Selection
                Text(
                    text = "Analysis Type",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        onClick = { onAnalysisTypeChange(AnalysisType.SHORT) },
                        label = { Text("Short Analysis") },
                        selected = uiState.selectedAnalysisType == AnalysisType.SHORT,
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        onClick = { onAnalysisTypeChange(AnalysisType.DETAILED) },
                        label = { Text("Detailed Analysis") },
                        selected = uiState.selectedAnalysisType == AnalysisType.DETAILED,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = when (uiState.selectedAnalysisType) {
                        AnalysisType.SHORT -> "Quick overview with key findings"
                        AnalysisType.DETAILED -> "Comprehensive analysis with detailed explanations"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAnalyze,
                enabled = uiState.isFormValid
            ) {
                Text(
                    when (uiState.selectedAnalysisType) {
                        AnalysisType.SHORT -> "Quick Analysis"
                        AnalysisType.DETAILED -> "Detailed Analysis"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
