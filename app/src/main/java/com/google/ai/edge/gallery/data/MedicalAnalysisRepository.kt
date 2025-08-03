package com.google.ai.edge.gallery.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MedicalAnalysisRepository"
private const val MEDICAL_ANALYSIS_DATASTORE = "medical_analysis_datastore"
private val Context.medicalAnalysisDataStore: DataStore<Preferences> by preferencesDataStore(name = MEDICAL_ANALYSIS_DATASTORE)

@Singleton
class MedicalAnalysisRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Current analysis data for passing to chat screen
    private var _currentPatientData: PatientInfo? = null
    private var _currentImageUri: Uri? = null
    
    // In-memory storage for analysis records
    private val _analysisRecords = MutableStateFlow<List<AnalysisRecord>>(emptyList())

    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // DataStore key for analysis records
    private val ANALYSIS_RECORDS_KEY = stringPreferencesKey("analysis_records")

    // Repository scope for background operations
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        // Load saved records on initialization
        loadAnalysisRecordsFromStorage()
    }

    /**
     * Load analysis records from persistent storage
     */
    private fun loadAnalysisRecordsFromStorage() {
        try {
            // Use repository scope to load data asynchronously
            repositoryScope.launch {
                try {
                    val savedRecordsJson = context.medicalAnalysisDataStore.data
                        .map { preferences -> preferences[ANALYSIS_RECORDS_KEY] ?: "[]" }
                        .first()
                    
                    val savedRecords = json.decodeFromString<List<AnalysisRecord>>(savedRecordsJson)
                    _analysisRecords.value = savedRecords
                    Log.d(TAG, "Loaded ${savedRecords.size} analysis records from storage")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load analysis records from storage", e)
                    _analysisRecords.value = emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize analysis records loading", e)
        }
    }

    /**
     * Save analysis records to persistent storage
     */
    private suspend fun saveAnalysisRecordsToStorage() {
        try {
            val recordsJson = json.encodeToString(_analysisRecords.value)
            context.medicalAnalysisDataStore.edit { preferences ->
                preferences[ANALYSIS_RECORDS_KEY] = recordsJson
            }
            Log.d(TAG, "Saved ${_analysisRecords.value.size} analysis records to storage")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save analysis records to storage", e)
        }
    }

    /**
     * Copy image from URI to internal storage and return the permanent file path
     */
    private suspend fun copyImageToInternalStorage(imageUri: Uri): String? {
        return try {
            // Create medical images directory in internal storage
            val medicalImagesDir = File(context.filesDir, "medical_images")
            if (!medicalImagesDir.exists()) {
                medicalImagesDir.mkdirs()
            }

            // Generate unique filename
            val fileName = "medical_image_${UUID.randomUUID()}.jpg"
            val destinationFile = File(medicalImagesDir, fileName)

            // Copy image from URI to internal storage
            context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            Log.d(TAG, "Image copied to internal storage: ${destinationFile.absolutePath}")
            destinationFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy image to internal storage", e)
            null
        }
    }

    /**
     * Save a new analysis record
     */
    suspend fun saveAnalysisRecord(
        patientInfo: PatientInfo,
        imagePath: String?,
        analysis: MedicalAnalysis
    ): String {
        val recordId = UUID.randomUUID().toString()
        val record = AnalysisRecord(
            id = recordId,
            patientInfo = patientInfo,
            imagePath = imagePath,
            analysis = analysis
        )
        
        val currentRecords = _analysisRecords.value.toMutableList()
        currentRecords.add(0, record) // Add to beginning (newest first)
        _analysisRecords.value = currentRecords
        
        // Save to persistent storage
        saveAnalysisRecordsToStorage()
        
        return recordId
    }

    /**
     * Get all analysis records as Flow
     */
    fun getAllAnalysisRecords(): Flow<List<AnalysisRecord>> {
        return _analysisRecords.asStateFlow()
    }

    /**
     * Search analysis records by patient name
     */
    fun searchAnalysisRecords(query: String): Flow<List<AnalysisRecord>> {
        return _analysisRecords.asStateFlow()
    }

    /**
     * Get analysis record by ID
     */
    suspend fun getAnalysisRecordById(id: String): AnalysisRecord? {
        return _analysisRecords.value.find { it.id == id }
    }

    /**
     * Delete analysis record by ID
     */
    suspend fun deleteAnalysisRecord(id: String) {
        val currentRecords = _analysisRecords.value.toMutableList()
        currentRecords.removeAll { it.id == id }
        _analysisRecords.value = currentRecords
        
        // Save to persistent storage
        saveAnalysisRecordsToStorage()
    }

    /**
     * Clear all analysis records
     */
    suspend fun clearAllRecords() {
        _analysisRecords.value = emptyList()
        
        // Save to persistent storage
        saveAnalysisRecordsToStorage()
    }

    /**
     * Set current patient data for analysis (temporary storage for navigation)
     */
    fun setCurrentPatientData(patientInfo: PatientInfo, imageUri: Uri) {
        _currentPatientData = patientInfo
        _currentImageUri = imageUri
    }

    /**
     * Get current patient data without clearing it
     */
    fun getCurrentPatientData(): Pair<PatientInfo, Uri>? {
        val patient = _currentPatientData
        val image = _currentImageUri
        
        return if (patient != null && image != null) {
            Pair(patient, image)
        } else {
            null
        }
    }

    /**
     * Clear current patient data
     */
    fun clearCurrentPatientData() {
        _currentPatientData = null
        _currentImageUri = null
    }

    /**
     * Format patient data for medical analysis prompt
     */
    fun formatPatientDataForAnalysis(patientInfo: PatientInfo): String {
        return """
Patient Information:
- Name: ${patientInfo.firstName} ${patientInfo.lastName}
- Medical History: ${patientInfo.history}
- History Source: ${if (patientInfo.historySource == HistorySource.FILE) "Uploaded from file" else "Manually entered"}

Please analyze the provided medical image in the context of this patient's information and provide a comprehensive clinical assessment.
        """.trimIndent()
    }

    /**
     * Save analysis result from chat
     */
    suspend fun saveAnalysis(analysisText: String, modelName: String) {
        val currentPatient = _currentPatientData
        val currentImage = _currentImageUri
        if (currentPatient != null) {
            // Copy image to permanent storage if available
            val permanentImagePath = currentImage?.let { uri ->
                copyImageToInternalStorage(uri)
            }
            
            val analysis = MedicalAnalysis(
                analysisText = analysisText,
                timestamp = System.currentTimeMillis(),
                modelUsed = modelName
            )
            
            saveAnalysisRecord(
                patientInfo = currentPatient,
                imagePath = permanentImagePath, // Use permanent path instead of URI
                analysis = analysis
            )
            
            // Clear data after successful save
            _currentPatientData = null
            _currentImageUri = null
        }
    }

    /**
     * Save analysis record with image URI (converts URI to permanent path)
     */
    suspend fun saveAnalysisRecordWithImageUri(
        patientInfo: PatientInfo,
        imageUri: Uri?,
        analysis: MedicalAnalysis
    ): String {
        // Copy image to permanent storage if available
        val permanentImagePath = imageUri?.let { uri ->
            copyImageToInternalStorage(uri)
        }
        
        return saveAnalysisRecord(
            patientInfo = patientInfo,
            imagePath = permanentImagePath,
            analysis = analysis
        )
    }
}
