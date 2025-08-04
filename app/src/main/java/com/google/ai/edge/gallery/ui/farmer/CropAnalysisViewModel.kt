package com.google.ai.edge.gallery.ui.farmer

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.gallery.data.CropAnalysis
import com.google.ai.edge.gallery.data.CropAnalysisRecord
import com.google.ai.edge.gallery.data.CropAnalysisRepository
import com.google.ai.edge.gallery.data.CropInfo
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
 * UI State for Crop Analysis Screen
 */
data class CropAnalysisUiState(
    val isLoading: Boolean = false,
    val showCropForm: Boolean = false,
    val analysisRecords: List<CropAnalysisRecord> = emptyList(),
    val filteredRecords: List<CropAnalysisRecord> = emptyList(),
    val searchQuery: String = "",
    val error: String? = null,
    
    // Crop form state
    val cropType: String = "",
    val location: String = "",
    val climate: String = "",
    val soilType: String = "",
    val selectedImageUri: Uri? = null,
    val isFormValid: Boolean = false,
    val selectedAnalysisType: AnalysisType = AnalysisType.SHORT,
    
    // Analysis state
    val isAnalyzing: Boolean = false,
    val currentAnalysis: String? = null,
    val showAnalysisResult: Boolean = false
)

@HiltViewModel
class CropAnalysisViewModel @Inject constructor(
    private val cropAnalysisRepository: CropAnalysisRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CropAnalysisUiState())
    val uiState: StateFlow<CropAnalysisUiState> = _uiState.asStateFlow()

    init {
        loadAnalysisRecords()
    }

    /**
     * Load all analysis records from repository
     */
    private fun loadAnalysisRecords() {
        viewModelScope.launch {
            cropAnalysisRepository.getAllAnalysisRecords().collect { records ->
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
    private fun filterRecords(records: List<CropAnalysisRecord>, query: String): List<CropAnalysisRecord> {
        return if (query.isBlank()) {
            records
        } else {
            records.filter { record ->
                record.getCropDisplayName().contains(query, ignoreCase = true)
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
     * Show crop form dialog
     */
    fun showCropForm() {
        _uiState.value = _uiState.value.copy(showCropForm = true)
    }

    /**
     * Hide crop form dialog
     */
    fun hideCropForm() {
        _uiState.value = _uiState.value.copy(
            showCropForm = false,
            cropType = "",
            location = "",
            climate = "",
            soilType = "",
            selectedImageUri = null,
            isFormValid = false
        )
    }

    /**
     * Update crop type
     */
    fun updateCropType(cropType: String) {
        val newState = _uiState.value.copy(cropType = cropType)
        _uiState.value = newState.copy(isFormValid = validateForm(newState))
    }

    /**
     * Update location
     */
    fun updateLocation(location: String) {
        val newState = _uiState.value.copy(location = location)
        _uiState.value = newState.copy(isFormValid = validateForm(newState))
    }

    /**
     * Update climate
     */
    fun updateClimate(climate: String) {
        val newState = _uiState.value.copy(climate = climate)
        _uiState.value = newState.copy(isFormValid = validateForm(newState))
    }

    /**
     * Update soil type
     */
    fun updateSoilType(soilType: String) {
        val newState = _uiState.value.copy(soilType = soilType)
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
     * Validate crop form
     */
    private fun validateForm(): Boolean {
        return validateForm(_uiState.value)
    }
    
    /**
     * Validate crop form with given state
     */
    private fun validateForm(state: CropAnalysisUiState): Boolean {
        return state.cropType.isNotBlank() &&
                state.location.isNotBlank() &&
                state.climate.isNotBlank() &&
                state.soilType.isNotBlank() &&
                state.selectedImageUri != null
    }

    /**
     * Start crop analysis process
     * This will navigate to chat screen for actual AI analysis
     */
    fun startAnalysis(): CropAnalysisData? {
        val state = _uiState.value
        if (!state.isFormValid) return null

        val cropInfo = CropInfo(
            cropType = state.cropType,
            location = state.location,
            climate = state.climate,
            soilType = state.soilType
        )
        
        // Store crop data in repository for chat screen to access
        cropAnalysisRepository.setCurrentCropData(cropInfo, state.selectedImageUri!!)

        return CropAnalysisData(
            cropInfo = cropInfo,
            imageUri = state.selectedImageUri!!
        )
    }

    /**
     * Save analysis result after AI processing
     */
    fun saveAnalysisResult(
        cropData: CropAnalysisData,
        analysisText: String,
        modelUsed: String
    ) {
        viewModelScope.launch {
            try {
                val analysis = CropAnalysis(
                    analysisText = analysisText,
                    timestamp = System.currentTimeMillis(),
                    modelUsed = modelUsed
                )
                
                // Use the new function that handles image URI to permanent path conversion
                cropAnalysisRepository.saveAnalysisRecordWithImageUri(
                    cropInfo = cropData.cropInfo,
                    imageUri = cropData.imageUri,
                    analysis = analysis
                )
                
                // Clear form after successful save
                hideCropForm()
                
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
                cropAnalysisRepository.deleteAnalysisRecord(recordId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    /**
     * Show analysis details (for now just show a simple message)
     */
    fun showAnalysisDetails(record: CropAnalysisRecord) {
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
 * Data class to pass crop data between screens
 */
data class CropAnalysisData(
    val cropInfo: CropInfo,
    val imageUri: Uri
)
