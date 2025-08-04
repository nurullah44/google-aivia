package com.google.ai.edge.gallery.ui.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.gallery.data.LessonInfo
import com.google.ai.edge.gallery.data.LessonPlan
import com.google.ai.edge.gallery.data.LessonRecord
import com.google.ai.edge.gallery.data.TeacherLessonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Teacher Lesson Screen
 */
data class TeacherLessonUiState(
    val isLoading: Boolean = false,
    val showLessonForm: Boolean = false,
    val lessonRecords: List<LessonRecord> = emptyList(),
    val filteredRecords: List<LessonRecord> = emptyList(),
    val searchQuery: String = "",
    val error: String? = null,
    
    // Lesson form state
    val subject: String = "",
    val grade: String = "",
    val topic: String = "",
    val duration: String = "",
    val additionalNotes: String = "",
    val isFormValid: Boolean = false,
    
    // Lesson planning state
    val isPlanning: Boolean = false,
    val currentLessonPlan: String? = null,
    val showLessonResult: Boolean = false
)

@HiltViewModel
class TeacherLessonViewModel @Inject constructor(
    private val teacherLessonRepository: TeacherLessonRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherLessonUiState())
    val uiState: StateFlow<TeacherLessonUiState> = _uiState.asStateFlow()

    init {
        loadLessonRecords()
    }

    /**
     * Load all lesson records from repository
     */
    private fun loadLessonRecords() {
        viewModelScope.launch {
            teacherLessonRepository.getAllLessonRecords().collect { records ->
                _uiState.value = _uiState.value.copy(
                    lessonRecords = records,
                    filteredRecords = filterRecords(records, _uiState.value.searchQuery)
                )
            }
        }
    }

    /**
     * Filter records based on search query
     */
    private fun filterRecords(records: List<LessonRecord>, query: String): List<LessonRecord> {
        return if (query.isBlank()) {
            records
        } else {
            records.filter { record ->
                record.getLessonDisplayName().contains(query, ignoreCase = true) ||
                record.lessonInfo.subject.contains(query, ignoreCase = true) ||
                record.lessonInfo.topic.contains(query, ignoreCase = true)
            }
        }
    }

    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredRecords = filterRecords(_uiState.value.lessonRecords, query)
        )
    }

    /**
     * Show lesson form dialog
     */
    fun showLessonForm() {
        _uiState.value = _uiState.value.copy(showLessonForm = true)
    }

    /**
     * Hide lesson form dialog
     */
    fun hideLessonForm() {
        _uiState.value = _uiState.value.copy(
            showLessonForm = false,
            subject = "",
            grade = "",
            topic = "",
            duration = "",
            additionalNotes = "",
            isFormValid = false
        )
    }

    /**
     * Update subject
     */
    fun updateSubject(subject: String) {
        val newState = _uiState.value.copy(subject = subject)
        _uiState.value = newState.copy(isFormValid = validateForm(newState))
    }

    /**
     * Update grade
     */
    fun updateGrade(grade: String) {
        val newState = _uiState.value.copy(grade = grade)
        _uiState.value = newState.copy(isFormValid = validateForm(newState))
    }

    /**
     * Update topic
     */
    fun updateTopic(topic: String) {
        val newState = _uiState.value.copy(topic = topic)
        _uiState.value = newState.copy(isFormValid = validateForm(newState))
    }

    /**
     * Update duration
     */
    fun updateDuration(duration: String) {
        val newState = _uiState.value.copy(duration = duration)
        _uiState.value = newState.copy(isFormValid = validateForm(newState))
    }


    /**
     * Update additional notes
     */
    fun updateAdditionalNotes(notes: String) {
        val newState = _uiState.value.copy(additionalNotes = notes)
        _uiState.value = newState.copy(isFormValid = validateForm(newState))
    }

    /**
     * Validate lesson form
     */
    private fun validateForm(): Boolean {
        return validateForm(_uiState.value)
    }
    
    /**
     * Validate lesson form with given state
     */
    private fun validateForm(state: TeacherLessonUiState): Boolean {
        return state.subject.isNotBlank() &&
                state.grade.isNotBlank() &&
                state.topic.isNotBlank() &&
                state.duration.isNotBlank()
    }

    /**
     * Start lesson planning process
     * This will navigate to chat screen for actual AI lesson planning
     */
    fun startLessonPlanning(): LessonPlanningData? {
        val state = _uiState.value
        if (!state.isFormValid) return null

        val lessonInfo = LessonInfo(
            subject = state.subject,
            grade = state.grade,
            topic = state.topic,
            duration = state.duration,
            additionalNotes = state.additionalNotes
        )
        
        // Store lesson data in repository for chat screen to access
        teacherLessonRepository.setCurrentLessonData(lessonInfo)

        return LessonPlanningData(
            lessonInfo = lessonInfo
        )
    }

    /**
     * Save lesson plan result after AI processing
     */
    fun saveLessonPlanResult(
        lessonData: LessonPlanningData,
        planText: String,
        modelUsed: String
    ) {
        viewModelScope.launch {
            try {
                val lessonPlan = LessonPlan(
                    planText = planText,
                    timestamp = System.currentTimeMillis(),
                    modelUsed = modelUsed
                )
                
                teacherLessonRepository.saveLessonRecord(
                    lessonInfo = lessonData.lessonInfo,
                    lessonPlan = lessonPlan
                )
                
                // Clear form after successful save
                hideLessonForm()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Delete lesson record
     */
    fun deleteLessonRecord(recordId: String) {
        viewModelScope.launch {
            try {
                teacherLessonRepository.deleteLessonRecord(recordId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    /**
     * Show lesson plan details (for now just show a simple message)
     */
    fun showLessonDetails(record: LessonRecord) {
        // For now, we can show the lesson plan in a simple way
        // In a real app, this might navigate to a detail screen
        _uiState.value = _uiState.value.copy(
            currentLessonPlan = record.lessonPlan.planText,
            showLessonResult = true
        )
    }

    /**
     * Hide lesson plan details
     */
    fun hideLessonDetails() {
        _uiState.value = _uiState.value.copy(
            currentLessonPlan = null,
            showLessonResult = false
        )
    }
}

/**
 * Data class to pass lesson data between screens
 */
data class LessonPlanningData(
    val lessonInfo: LessonInfo
)