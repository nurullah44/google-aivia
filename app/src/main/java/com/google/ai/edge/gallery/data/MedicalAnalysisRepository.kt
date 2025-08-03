package com.google.ai.edge.gallery.data

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicalAnalysisRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Current analysis data for passing to chat screen
    private var _currentPatientData: PatientInfo? = null
    private var _currentImageUri: Uri? = null
    private companion object {
        private val Context.medicalDataStore: DataStore<Preferences> by preferencesDataStore(name = "medical_analysis")
        private val ANALYSIS_RECORDS_KEY = stringPreferencesKey("analysis_records")
    }

    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
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
        
        val currentRecords = getAllAnalysisRecords().first().toMutableList()
        currentRecords.add(0, record) // Add to beginning (newest first)
        
        context.medicalDataStore.edit { preferences ->
            preferences[ANALYSIS_RECORDS_KEY] = json.encodeToString(currentRecords)
        }
        
        return recordId
    }

    /**
     * Get all analysis records as Flow
     */
    fun getAllAnalysisRecords(): Flow<List<AnalysisRecord>> {
        return context.medicalDataStore.data.map { preferences ->
            val recordsJson = preferences[ANALYSIS_RECORDS_KEY] ?: "[]"
            try {
                json.decodeFromString<List<AnalysisRecord>>(recordsJson)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    /**
     * Search analysis records by patient name
     */
    fun searchAnalysisRecords(query: String): Flow<List<AnalysisRecord>> {
        return getAllAnalysisRecords().map { records ->
            if (query.isBlank()) {
                records
            } else {
                records.filter { record ->
                    record.getPatientDisplayName().contains(query, ignoreCase = true)
                }
            }
        }
    }

    /**
     * Get analysis record by ID
     */
    suspend fun getAnalysisRecordById(id: String): AnalysisRecord? {
        return getAllAnalysisRecords().first().find { it.id == id }
    }

    /**
     * Delete analysis record by ID
     */
    suspend fun deleteAnalysisRecord(id: String) {
        val currentRecords = getAllAnalysisRecords().first().toMutableList()
        currentRecords.removeAll { it.id == id }
        
        context.medicalDataStore.edit { preferences ->
            preferences[ANALYSIS_RECORDS_KEY] = json.encodeToString(currentRecords)
        }
    }

    /**
     * Clear all analysis records
     */
    suspend fun clearAllRecords() {
        context.medicalDataStore.edit { preferences ->
            preferences[ANALYSIS_RECORDS_KEY] = "[]"
        }
    }

    /**
     * Set current patient data for analysis (temporary storage for navigation)
     */
    fun setCurrentPatientData(patientInfo: PatientInfo, imageUri: Uri) {
        _currentPatientData = patientInfo
        _currentImageUri = imageUri
    }

    /**
     * Get current patient data and clear it
     */
    fun getCurrentPatientData(): Pair<PatientInfo, Uri>? {
        val patient = _currentPatientData
        val image = _currentImageUri
        
        // Clear after retrieval
        _currentPatientData = null
        _currentImageUri = null
        
        return if (patient != null && image != null) {
            Pair(patient, image)
        } else {
            null
        }
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
}