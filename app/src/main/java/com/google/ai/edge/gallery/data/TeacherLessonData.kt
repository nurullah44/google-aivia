package com.google.ai.edge.gallery.data

import kotlinx.serialization.Serializable

/**
 * Teacher lesson data for lesson planning (equivalent to PatientInfo)
 */
@Serializable
data class LessonInfo(
    val subject: String,
    val grade: String,
    val topic: String,
    val duration: String,
    val additionalNotes: String = ""
)

/**
 * Lesson plan result from AI
 */
@Serializable
data class LessonPlan(
    val planText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val modelUsed: String = ""
)

/**
 * Complete lesson record combining lesson info and AI-generated plan
 */
@Serializable
data class LessonRecord(
    val id: String, // Unique identifier
    val lessonInfo: LessonInfo,
    val lessonPlan: LessonPlan,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Get display name for lesson
     */
    fun getLessonDisplayName(): String {
        return "${lessonInfo.subject} - ${lessonInfo.topic}"
    }
    
    /**
     * Get formatted date string
     */
    fun getFormattedDate(): String {
        val formatter = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        return formatter.format(java.util.Date(createdAt))
    }
    
    /**
     * Get grade and duration info
     */
    fun getGradeDurationInfo(): String {
        return "Grade ${lessonInfo.grade} • ${lessonInfo.duration}"
    }
}