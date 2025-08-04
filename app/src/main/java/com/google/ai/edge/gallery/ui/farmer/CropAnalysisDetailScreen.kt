/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.edge.gallery.ui.farmer

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Agriculture
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.google.ai.edge.gallery.data.CropAnalysisRecord
import com.google.ai.edge.gallery.data.CropAnalysisRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

private const val TAG = "CropAnalysisDetail"

object CropAnalysisDetailDestination {
  const val route = "crop_analysis_detail"
  const val recordIdArg = "recordId"
  val routeWithArgs = "$route/{$recordIdArg}"
}

/**
 * UI State for Crop Analysis Detail Screen
 */
data class CropAnalysisDetailUiState(
  val isLoading: Boolean = true,
  val analysisRecord: CropAnalysisRecord? = null,
  val error: String? = null
)

@HiltViewModel
class CropAnalysisDetailViewModel @Inject constructor(
  private val cropAnalysisRepository: CropAnalysisRepository,
  savedStateHandle: SavedStateHandle
) : ViewModel() {

  private val recordId: String = checkNotNull(
    savedStateHandle[CropAnalysisDetailDestination.recordIdArg]
  )

  private val _uiState = MutableStateFlow(CropAnalysisDetailUiState())
  val uiState: StateFlow<CropAnalysisDetailUiState> = _uiState.asStateFlow()

  init {
    Log.d(TAG, "Loading analysis record: $recordId")
    loadAnalysisRecord()
  }

  /**
   * Load analysis record from repository
   */
  fun loadAnalysisRecord() {
    viewModelScope.launch {
      try {
        cropAnalysisRepository.getAllAnalysisRecords().collect { records ->
          val record = records.find { it.id == recordId }
          if (record != null) {
            Log.d(TAG, "Analysis record loaded successfully")
            _uiState.value = _uiState.value.copy(
              isLoading = false,
              analysisRecord = record,
              error = null
            )
          } else {
            Log.w(TAG, "Analysis record not found: $recordId")
            _uiState.value = _uiState.value.copy(
              isLoading = false,
              error = "Analysis record not found"
            )
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to load analysis record", e)
        _uiState.value = _uiState.value.copy(
          isLoading = false,
          error = e.message ?: "Unknown error occurred"
        )
      }
    }
  }

  /**
   * Clear error message
   */
  fun clearError() {
    _uiState.value = _uiState.value.copy(error = null)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropAnalysisDetailScreen(
  viewModel: CropAnalysisDetailViewModel = hiltViewModel(),
  navigateUp: () -> Unit
) {
  val uiState by viewModel.uiState.collectAsState()

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Analysis Details") },
        navigationIcon = {
          IconButton(onClick = navigateUp) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
          }
        }
      )
    }
  ) { paddingValues ->
    when {
      uiState.isLoading -> {
        LoadingContent(
          modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
        )
      }
      uiState.error != null -> {
        ErrorContent(
          error = uiState.error!!,
          modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
        )
      }
      uiState.analysisRecord != null -> {
        AnalysisDetailContent(
          analysisRecord = uiState.analysisRecord!!,
          modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
        )
      }
    }
  }

  // Handle errors
  uiState.error?.let { error ->
    LaunchedEffect(error) {
      Log.e(TAG, "Error displayed to user: $error")
    }
  }
}

@Composable
private fun LoadingContent(
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier,
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      CircularProgressIndicator()
      Text(
        text = "Loading analysis details...",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.outline
      )
    }
  }
}

@Composable
private fun ErrorContent(
  error: String,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier,
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Text(
        text = "Error loading analysis",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.error
      )
      Text(
        text = error,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.outline,
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
private fun AnalysisDetailContent(
  analysisRecord: CropAnalysisRecord,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Crop Information Card
    CropInfoCard(analysisRecord = analysisRecord)

    // Crop Image Card
    CropImageCard(analysisRecord = analysisRecord)

    // Analysis Results Card
    AnalysisResultsCard(analysisRecord = analysisRecord)
  }
}

@Composable
private fun CropInfoCard(
  analysisRecord: CropAnalysisRecord
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          Icons.Outlined.Agriculture,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary
        )
        Text(
          text = "Crop Information",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Medium
        )
      }

      Text(
        text = analysisRecord.getCropDisplayName(),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
      )

      // Crop details in a grid layout
      Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Climate:",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Medium
            )
            Text(
              text = analysisRecord.cropInfo.climate,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Spacer(modifier = Modifier.width(16.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Soil Type:",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Medium
            )
            Text(
              text = analysisRecord.cropInfo.soilType,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          Icons.Outlined.CalendarToday,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.outline,
          modifier = Modifier.size(16.dp)
        )
        Text(
          text = analysisRecord.getFormattedDate(),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.outline
        )
      }
    }
  }
}

@Composable
private fun CropImageCard(
  analysisRecord: CropAnalysisRecord
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          Icons.Outlined.Image,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary
        )
        Text(
          text = "Crop Image",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Medium
        )
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(200.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
      ) {
        if (analysisRecord.imagePath != null) {
          AsyncImage(
            model = analysisRecord.imagePath, // Use the path directly, Coil can handle both File and URI
            contentDescription = "Crop Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            onError = { 
              Log.w(TAG, "Failed to load image: ${analysisRecord.imagePath}")
            }
          )
        } else {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              Icons.Outlined.Image,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.outline,
              modifier = Modifier.size(48.dp)
            )
            Text(
              text = "No image available",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.outline
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AnalysisResultsCard(
  analysisRecord: CropAnalysisRecord
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Text(
        text = "Analysis Results",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Medium
      )

      Text(
        text = analysisRecord.analysis.analysisText,
        style = MaterialTheme.typography.bodyMedium,
        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
      )

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Model: ${analysisRecord.analysis.modelUsed}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.outline
        )
        Text(
          text = analysisRecord.getFormattedDate(),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.outline
        )
      }
    }
  }
}
