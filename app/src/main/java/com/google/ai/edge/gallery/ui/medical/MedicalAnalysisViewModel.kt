package com.google.ai.edge.gallery.ui.medical

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.gallery.data.AnalysisRecord
import com.google.ai.edge.gallery.data.HistorySource
import com.google.ai.edge.gallery.data.MedicalAnalysis
import com.google.ai.edge.gallery.data.MedicalAnalysisRepository
import com.google.ai.edge.gallery.data.PatientInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Analysis type enum
 */
enum class AnalysisType {
    SHORT, DETAILED
}

/**
 * UI State for Medical Analysis Screen
 */
data class MedicalAnalysisUiState(
    val isLoading: Boolean = false,
    val showPatientForm: Boolean = false,
    val analysisRecords: List<AnalysisRecord> = emptyList(),
    val filteredRecords: List<AnalysisRecord> = emptyList(),
    val searchQuery: String = "",
    val error: String? = null,
    
    // Patient form state
    val patientFirstName: String = "",
    val patientLastName: String = "",
    val patientHistory: String = "",
    val historySource: HistorySource = HistorySource.MANUAL,
    val selectedImageUri: Uri? = null,
    val isFormValid: Boolean = false,
    val selectedAnalysisType: AnalysisType = AnalysisType.SHORT,
    
    // Analysis state
    val isAnalyzing: Boolean = false,
    val currentAnalysis: String? = null,
    val showAnalysisResult: Boolean = false
)

@HiltViewModel
class MedicalAnalysisViewModel @Inject constructor(
    private val medicalAnalysisRepository: MedicalAnalysisRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MedicalAnalysisUiState())
    val uiState: StateFlow<MedicalAnalysisUiState> = _uiState.asStateFlow()

    init {
        loadAnalysisRecords()
    }

    /**
     * Load all analysis records from repository
     */
    private fun loadAnalysisRecords() {
        viewModelScope.launch {
            medicalAnalysisRepository.getAllAnalysisRecords().collect { records ->
                _uiState.value = _uiState.value.copy(
                    analysisRecords = records,
                    filteredRecords = filterRecords(records, _uiState.value.searchQuery)
                )
            }
        }
    }

    /**
     * Filter records based on search query
     */
    private fun filterRecords(records: List<AnalysisRecord>, query: String): List<AnalysisRecord> {
        return if (query.isBlank()) {
            records
        } else {
            records.filter { record ->
                record.getPatientDisplayName().contains(query, ignoreCase = true)
            }
        }
    }

    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredRecords = filterRecords(_uiState.value.analysisRecords, query)
        )
    }

    /**
     * Show patient form dialog
     */
    fun showPatientForm() {
        _uiState.value = _uiState.value.copy(showPatientForm = true)
    }

    /**
     * Hide patient form dialog
     */
    fun hidePatientForm() {
        _uiState.value = _uiState.value.copy(
            showPatientForm = false,
            patientFirstName = "",
            patientLastName = "",
            patientHistory = "",
            historySource = HistorySource.MANUAL,
            selectedImageUri = null,
            isFormValid = false
        )
    }

    /**
     * Update patient first name
     */
    fun updatePatientFirstName(firstName: String) {
        val newState = _uiState.value.copy(patientFirstName = firstName)
        _uiState.value = newState.copy(isFormValid = validateForm(newState))
    }

    /**
     * Update patient last name
     */
    fun updatePatientLastName(lastName: String) {
        val newState = _uiState.value.copy(patientLastName = lastName)
        _uiState.value = newState.copy(isFormValid = validateForm(newState))
    }

    /**
     * Update patient history
     */
    fun updatePatientHistory(history: String, source: HistorySource = HistorySource.MANUAL) {
        val newState = _uiState.value.copy(patientHistory = history, historySource = source)
        _uiState.value = newState.copy(isFormValid = validateForm(newState))
    }

    /**
     * Update selected image
     */
    fun updateSelectedImage(imageUri: Uri?) {
        val newState = _uiState.value.copy(selectedImageUri = imageUri)
        _uiState.value = newState.copy(isFormValid = validateForm(newState))
    }

    /**
     * Update selected analysis type
     */
    fun updateAnalysisType(analysisType: AnalysisType) {
        _uiState.value = _uiState.value.copy(selectedAnalysisType = analysisType)
    }

    /**
     * Validate patient form
     */
    private fun validateForm(): Boolean {
        return validateForm(_uiState.value)
    }
    
    /**
     * Validate patient form with given state
     */
    private fun validateForm(state: MedicalAnalysisUiState): Boolean {
        return state.patientFirstName.isNotBlank() &&
                state.patientLastName.isNotBlank() &&
                state.patientHistory.isNotBlank() &&
                state.selectedImageUri != null
    }

    /**
     * Start medical analysis process
     * This will navigate to chat screen for actual AI analysis
     */
    fun startAnalysis(): PatientAnalysisData? {
        val state = _uiState.value
        if (!state.isFormValid) return null

        val patientInfo = PatientInfo(
            firstName = state.patientFirstName,
            lastName = state.patientLastName,
            history = state.patientHistory,
            historySource = state.historySource
        )
        
        // Store patient data in repository for chat screen to access
        medicalAnalysisRepository.setCurrentPatientData(patientInfo, state.selectedImageUri!!)

        return PatientAnalysisData(
            patientInfo = patientInfo,
            imageUri = state.selectedImageUri!!
        )
    }

    /**
     * Save analysis result after AI processing
     */
    fun saveAnalysisResult(
        patientData: PatientAnalysisData,
        analysisText: String,
        modelUsed: String
    ) {
        viewModelScope.launch {
            try {
                val analysis = MedicalAnalysis(
                    analysisText = analysisText,
                    timestamp = System.currentTimeMillis(),
                    modelUsed = modelUsed
                )
                
                // Use the new function that handles image URI to permanent path conversion
                medicalAnalysisRepository.saveAnalysisRecordWithImageUri(
                    patientInfo = patientData.patientInfo,
                    imageUri = patientData.imageUri,
                    analysis = analysis
                )
                
                // Clear form after successful save
                hidePatientForm()
                
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
     * Delete analysis record
     */
    fun deleteAnalysisRecord(recordId: String) {
        viewModelScope.launch {
            try {
                medicalAnalysisRepository.deleteAnalysisRecord(recordId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    /**
     * Show analysis details (for now just show a simple message)
     */
    fun showAnalysisDetails(record: AnalysisRecord) {
        // For now, we can show the analysis in a simple way
        // In a real app, this might navigate to a detail screen
        _uiState.value = _uiState.value.copy(
            currentAnalysis = record.analysis.analysisText,
            showAnalysisResult = true
        )
    }

    /**
     * Hide analysis details
     */
    fun hideAnalysisDetails() {
        _uiState.value = _uiState.value.copy(
            currentAnalysis = null,
            showAnalysisResult = false
        )
    }
}

/**
 * Data class to pass patient data between screens
 */
data class PatientAnalysisData(
    val patientInfo: PatientInfo,
    val imageUri: Uri
)
