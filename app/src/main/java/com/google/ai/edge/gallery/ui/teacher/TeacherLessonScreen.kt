package com.google.ai.edge.gallery.ui.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.ai.edge.gallery.data.LessonRecord

object TeacherLessonDestination {
    const val route = "teacher_lesson"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherLessonScreen(
  viewModel: TeacherLessonViewModel = hiltViewModel(),
  navigateUp: () -> Unit,
  navigateToPlanning: (String) -> Unit,
  navigateToDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lesson Plan Generator") },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showLessonForm() },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Create New Lesson Plan",
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
                label = { Text("Search lesson plans...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true
            )

            // Lesson Plans History Grid
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
                            Icons.Default.School,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = if (uiState.searchQuery.isBlank()) {
                                "No lesson plans yet.\nTap + to create your first lesson plan."
                            } else {
                                "No lesson plans found matching \"${uiState.searchQuery}\""
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
                        LessonRecordCard(
                            record = record,
                            onClick = { 
                                // Navigate to detail screen
                                navigateToDetail(record.id)
                            },
                            onDelete = {
                                viewModel.deleteLessonRecord(record.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // Lesson Form Dialog
    if (uiState.showLessonForm) {
        LessonFormDialog(
            uiState = uiState,
            onDismiss = { viewModel.hideLessonForm() },
            onSubjectChange = viewModel::updateSubject,
            onGradeChange = viewModel::updateGrade,
            onTopicChange = viewModel::updateTopic,
            onDurationChange = viewModel::updateDuration,
            onAdditionalNotesChange = viewModel::updateAdditionalNotes,
            onCreatePlan = {
                viewModel.startLessonPlanning()?.let { lessonData ->
                    viewModel.hideLessonForm()
                    // Navigate to Gemma 3n chat with teacher context and lesson data
                    navigateToPlanning("Gemma-3n-E2B-it-int4")
                }
            }
        )
    }

    // Lesson Plan Details Dialog
    if (uiState.showLessonResult) {
        uiState.currentLessonPlan?.let { planText ->
            LessonDetailsDialog(
                planText = planText,
                onDismiss = { viewModel.hideLessonDetails() }
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
private fun LessonDetailsDialog(
    planText: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lesson Plan Details") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = planText,
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
private fun LessonRecordCard(
    record: LessonRecord,
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
                // Subject Icon
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Outlined.MenuBook,
                            contentDescription = "Lesson Plan",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = record.lessonInfo.subject,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Lesson Topic
                Text(
                    text = record.lessonInfo.topic,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Grade and Duration
                Text(
                    text = record.getGradeDurationInfo(),
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
private fun LessonFormDialog(
    uiState: TeacherLessonUiState,
    onDismiss: () -> Unit,
    onSubjectChange: (String) -> Unit,
    onGradeChange: (String) -> Unit,
    onTopicChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onAdditionalNotesChange: (String) -> Unit,
    onCreatePlan: () -> Unit
) {
    val gradeOptions = listOf(
        "K", "1st", "2nd", "3rd", "4th", "5th", "6th", 
        "7th", "8th", "9th", "10th", "11th", "12th", "College"
    )
    
    val durationOptions = listOf(
        "30 minutes", "45 minutes", "60 minutes", "90 minutes", "2 hours"
    )
    
    var showGradeDropdown by remember { mutableStateOf(false) }
    var showDurationDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Lesson Plan") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Subject Field
                OutlinedTextField(
                    value = uiState.subject,
                    onValueChange = onSubjectChange,
                    label = { Text("Subject") },
                    placeholder = { Text("e.g., Math, Science, English") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Grade Dropdown
                ExposedDropdownMenuBox(
                    expanded = showGradeDropdown,
                    onExpandedChange = { showGradeDropdown = !showGradeDropdown }
                ) {
                    OutlinedTextField(
                        value = uiState.grade,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Grade Level") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showGradeDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showGradeDropdown,
                        onDismissRequest = { showGradeDropdown = false }
                    ) {
                        gradeOptions.forEach { grade ->
                            DropdownMenuItem(
                                text = { Text(grade) },
                                onClick = {
                                    onGradeChange(grade)
                                    showGradeDropdown = false
                                }
                            )
                        }
                    }
                }

                // Topic Field
                OutlinedTextField(
                    value = uiState.topic,
                    onValueChange = onTopicChange,
                    label = { Text("Topic") },
                    placeholder = { Text("e.g., Fractions, Photosynthesis") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Duration Dropdown
                ExposedDropdownMenuBox(
                    expanded = showDurationDropdown,
                    onExpandedChange = { showDurationDropdown = !showDurationDropdown }
                ) {
                    OutlinedTextField(
                        value = uiState.duration,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Duration") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDurationDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showDurationDropdown,
                        onDismissRequest = { showDurationDropdown = false }
                    ) {
                        durationOptions.forEach { duration ->
                            DropdownMenuItem(
                                text = { Text(duration) },
                                onClick = {
                                    onDurationChange(duration)
                                    showDurationDropdown = false
                                }
                            )
                        }
                    }
                }

                // Additional Notes Field
                OutlinedTextField(
                    value = uiState.additionalNotes,
                    onValueChange = onAdditionalNotesChange,
                    label = { Text("Additional Notes (Optional)") },
                    placeholder = { Text("Special requirements, accommodations, etc.") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onCreatePlan,
                enabled = uiState.isFormValid
            ) {
                Text("Create Lesson Plan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}