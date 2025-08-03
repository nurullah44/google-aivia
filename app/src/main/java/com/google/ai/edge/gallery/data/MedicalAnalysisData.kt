package com.google.ai.edge.gallery.data

import kotlinx.serialization.Serializable

/**
 * Patient information for medical analysis
 */
@Serializable
data class PatientInfo(
    val firstName: String,
    val lastName: String,
    val history: String, // Patient medical history/notes
    val historySource: HistorySource = HistorySource.MANUAL // How history was provided
)

/**
 * Source of patient history information
 */
@Serializable
enum class HistorySource {
    MANUAL, // Manually typed
    FILE    // Uploaded from text file
}

/**
 * Medical analysis result from AI
 */
@Serializable
data class MedicalAnalysis(
    val analysisText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val modelUsed: String = ""
)

/**
 * Complete analysis record combining patient info, image, and AI analysis
 */
@Serializable
data class AnalysisRecord(
    val id: String, // Unique identifier
    val patientInfo: PatientInfo,
    val imagePath: String?, // Path to stored image file
    val analysis: MedicalAnalysis,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Get display name for patient
     */
    fun getPatientDisplayName(): String {
        return "${patientInfo.firstName} ${patientInfo.lastName}"
    }
    
    /**
     * Get formatted date string
     */
    fun getFormattedDate(): String {
        val formatter = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        return formatter.format(java.util.Date(createdAt))
    }
}