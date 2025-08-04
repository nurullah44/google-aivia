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

package com.google.ai.edge.gallery.ui.teacher

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
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.School
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.gallery.data.LessonRecord
import com.google.ai.edge.gallery.data.TeacherLessonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "TeacherLessonDetail"

object TeacherLessonDetailDestination {
  const val route = "teacher_lesson_detail"
  const val recordIdArg = "recordId"
  val routeWithArgs = "$route/{$recordIdArg}"
}

/**
 * UI State for Teacher Lesson Detail Screen
 */
data class TeacherLessonDetailUiState(
  val isLoading: Boolean = true,
  val lessonRecord: LessonRecord? = null,
  val error: String? = null
)

@HiltViewModel
class TeacherLessonDetailViewModel @Inject constructor(
  private val teacherLessonRepository: TeacherLessonRepository,
  savedStateHandle: SavedStateHandle
) : ViewModel() {

  private val recordId: String = checkNotNull(
    savedStateHandle[TeacherLessonDetailDestination.recordIdArg]
  )

  private val _uiState = MutableStateFlow(TeacherLessonDetailUiState())
  val uiState: StateFlow<TeacherLessonDetailUiState> = _uiState.asStateFlow()

  init {
    Log.d(TAG, "Loading lesson record: $recordId")
    loadLessonRecord()
  }

  /**
   * Load lesson record from repository
   */
  fun loadLessonRecord() {
    viewModelScope.launch {
      try {
        teacherLessonRepository.getAllLessonRecords().collect { records ->
          val record = records.find { it.id == recordId }
          if (record != null) {
            Log.d(TAG, "Lesson record loaded successfully")
            _uiState.value = _uiState.value.copy(
              isLoading = false,
              lessonRecord = record,
              error = null
            )
          } else {
            Log.w(TAG, "Lesson record not found: $recordId")
            _uiState.value = _uiState.value.copy(
              isLoading = false,
              error = "Lesson record not found"
            )
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to load lesson record", e)
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
fun TeacherLessonDetailScreen(
  viewModel: TeacherLessonDetailViewModel = hiltViewModel(),
  navigateUp: () -> Unit
) {
  val uiState by viewModel.uiState.collectAsState()

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Lesson Plan Details") },
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
      uiState.lessonRecord != null -> {
        LessonDetailContent(
          lessonRecord = uiState.lessonRecord!!,
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
        text = "Loading lesson plan details...",
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
        text = "Error loading lesson plan",
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
private fun LessonDetailContent(
  lessonRecord: LessonRecord,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Lesson Information Card
    LessonInfoCard(lessonRecord = lessonRecord)

    // Subject Overview Card
    SubjectOverviewCard(lessonRecord = lessonRecord)

    // Lesson Plan Results Card
    LessonPlanResultsCard(lessonRecord = lessonRecord)
  }
}

@Composable
private fun LessonInfoCard(
  lessonRecord: LessonRecord
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
          Icons.Outlined.School,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary
        )
        Text(
          text = "Lesson Information",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Medium
        )
      }

      Text(
        text = lessonRecord.getLessonDisplayName(),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column {
          Text(
            text = "Grade Level:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
          )
          Text(
            text = lessonRecord.lessonInfo.grade,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Column {
          Text(
            text = "Duration:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
          )
          Text(
            text = lessonRecord.lessonInfo.duration,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      if (lessonRecord.lessonInfo.additionalNotes.isNotBlank()) {
        Text(
          text = "Additional Notes:",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Medium
        )
        Text(
          text = lessonRecord.lessonInfo.additionalNotes,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
          text = lessonRecord.getFormattedDate(),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.outline
        )
      }
    }
  }
}

@Composable
private fun SubjectOverviewCard(
  lessonRecord: LessonRecord
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
          Icons.AutoMirrored.Outlined.MenuBook,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary
        )
        Text(
          text = "Subject Overview",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Medium
        )
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(120.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            Icons.AutoMirrored.Outlined.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
          )
          Text(
            text = lessonRecord.lessonInfo.subject,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = lessonRecord.lessonInfo.topic,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

@Composable
private fun LessonPlanResultsCard(
  lessonRecord: LessonRecord
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
        text = "Lesson Plan",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Medium
      )

      Text(
        text = lessonRecord.lessonPlan.planText,
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
          text = "Model: ${lessonRecord.lessonPlan.modelUsed}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.outline
        )
        Text(
          text = lessonRecord.getFormattedDate(),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.outline
        )
      }
    }
  }
}
