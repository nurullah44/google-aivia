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

private const val TAG = "CropAnalysisRepository"
private const val CROP_ANALYSIS_DATASTORE = "crop_analysis_datastore"
private val Context.cropAnalysisDataStore: DataStore<Preferences> by preferencesDataStore(name = CROP_ANALYSIS_DATASTORE)

@Singleton
class CropAnalysisRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Current analysis data for passing to chat screen
    private var _currentCropData: CropInfo? = null
    private var _currentImageUri: Uri? = null
    
    // In-memory storage for analysis records
    private val _analysisRecords = MutableStateFlow<List<CropAnalysisRecord>>(emptyList())

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
                    val savedRecordsJson = context.cropAnalysisDataStore.data
                        .map { preferences -> preferences[ANALYSIS_RECORDS_KEY] ?: "[]" }
                        .first()
                    
                    val savedRecords = json.decodeFromString<List<CropAnalysisRecord>>(savedRecordsJson)
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
            context.cropAnalysisDataStore.edit { preferences ->
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
            // Create crop images directory in internal storage
            val cropImagesDir = File(context.filesDir, "crop_images")
            if (!cropImagesDir.exists()) {
                cropImagesDir.mkdirs()
            }

            // Generate unique filename
            val fileName = "crop_image_${UUID.randomUUID()}.jpg"
            val destinationFile = File(cropImagesDir, fileName)

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
        cropInfo: CropInfo,
        imagePath: String?,
        analysis: CropAnalysis
    ): String {
        val recordId = UUID.randomUUID().toString()
        val record = CropAnalysisRecord(
            id = recordId,
            cropInfo = cropInfo,
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
    fun getAllAnalysisRecords(): Flow<List<CropAnalysisRecord>> {
        return _analysisRecords.asStateFlow()
    }

    /**
     * Search analysis records by crop name
     */
    fun searchAnalysisRecords(query: String): Flow<List<CropAnalysisRecord>> {
        return _analysisRecords.asStateFlow()
    }

    /**
     * Get analysis record by ID
     */
    suspend fun getAnalysisRecordById(id: String): CropAnalysisRecord? {
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
     * Set current crop data for analysis (temporary storage for navigation)
     */
    fun setCurrentCropData(cropInfo: CropInfo, imageUri: Uri) {
        _currentCropData = cropInfo
        _currentImageUri = imageUri
    }

    /**
     * Get current crop data without clearing it
     */
    fun getCurrentCropData(): Pair<CropInfo, Uri>? {
        val crop = _currentCropData
        val image = _currentImageUri
        
        return if (crop != null && image != null) {
            Pair(crop, image)
        } else {
            null
        }
    }

    /**
     * Clear current crop data
     */
    fun clearCurrentCropData() {
        _currentCropData = null
        _currentImageUri = null
    }

    /**
     * Format crop data for agricultural analysis prompt
     */
    fun formatCropDataForAnalysis(cropInfo: CropInfo): String {
        return """
Crop Information:
- Crop Type: ${cropInfo.cropType}
- Location: ${cropInfo.location}
- Climate: ${cropInfo.climate}
- Soil Type: ${cropInfo.soilType}

Please analyze the provided crop image in the context of this agricultural information and provide a comprehensive agricultural assessment including disease detection, pest control, and growth recommendations.
        """.trimIndent()
    }

    /**
     * Save analysis result from chat
     */
    suspend fun saveAnalysis(analysisText: String, modelName: String) {
        val currentCrop = _currentCropData
        val currentImage = _currentImageUri
        if (currentCrop != null) {
            // Copy image to permanent storage if available
            val permanentImagePath = currentImage?.let { uri ->
                copyImageToInternalStorage(uri)
            }
            
            val analysis = CropAnalysis(
                analysisText = analysisText,
                timestamp = System.currentTimeMillis(),
                modelUsed = modelName
            )
            
            saveAnalysisRecord(
                cropInfo = currentCrop,
                imagePath = permanentImagePath, // Use permanent path instead of URI
                analysis = analysis
            )
            
            // Clear data after successful save
            _currentCropData = null
            _currentImageUri = null
        }
    }

    /**
     * Save analysis record with image URI (converts URI to permanent path)
     */
    suspend fun saveAnalysisRecordWithImageUri(
        cropInfo: CropInfo,
        imageUri: Uri?,
        analysis: CropAnalysis
    ): String {
        // Copy image to permanent storage if available
        val permanentImagePath = imageUri?.let { uri ->
            copyImageToInternalStorage(uri)
        }
        
        return saveAnalysisRecord(
            cropInfo = cropInfo,
            imagePath = permanentImagePath,
            analysis = analysis
        )
    }
}
