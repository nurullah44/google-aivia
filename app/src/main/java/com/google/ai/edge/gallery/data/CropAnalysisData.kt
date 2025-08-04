package com.google.ai.edge.gallery.data

import kotlinx.serialization.Serializable

/**
 * Crop information for agricultural analysis
 */
@Serializable
data class CropInfo(
    val cropType: String,
    val location: String,
    val climate: String,
    val soilType: String
)

/**
 * Crop analysis result from AI
 */
@Serializable
data class CropAnalysis(
    val analysisText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val modelUsed: String = ""
)

/**
 * Complete crop analysis record combining crop info, image, and AI analysis
 */
@Serializable
data class CropAnalysisRecord(
    val id: String, // Unique identifier
    val cropInfo: CropInfo,
    val imagePath: String?, // Path to stored image file
    val analysis: CropAnalysis,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Get display name for crop
     */
    fun getCropDisplayName(): String {
        return "${cropInfo.cropType} - ${cropInfo.location}"
    }
    
    /**
     * Get formatted date string
     */
    fun getFormattedDate(): String {
        val formatter = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        return formatter.format(java.util.Date(createdAt))
    }
}
