package com.google.ai.edge.gallery.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TeacherLessonRepository"
private const val TEACHER_LESSON_DATASTORE = "teacher_lesson_datastore"
private val Context.teacherLessonDataStore: DataStore<Preferences> by preferencesDataStore(name = TEACHER_LESSON_DATASTORE)

@Singleton
class TeacherLessonRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Current lesson data for passing to chat screen
    private var currentLessonData: LessonInfo? = null
    
    // In-memory storage for lesson records
    private val _lessonRecords = MutableStateFlow<List<LessonRecord>>(emptyList())

    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // DataStore key for lesson records
    private val lessonRecordsKey = stringPreferencesKey("lesson_records")

    init {
        // Load saved records on initialization
        CoroutineScope(Dispatchers.IO).launch {
            loadLessonRecordsFromStorage()
        }
    }

    /**
     * Load lesson records from persistent storage
     */
    private suspend fun loadLessonRecordsFromStorage() {
        try {
            val recordsJson = context.teacherLessonDataStore.data.first()[lessonRecordsKey]
            if (!recordsJson.isNullOrEmpty()) {
                val records = json.decodeFromString<List<LessonRecord>>(recordsJson)
                _lessonRecords.value = records
                Log.d(TAG, "Loaded ${records.size} lesson records from storage")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load lesson records from storage", e)
            _lessonRecords.value = emptyList()
        }
    }

    /**
     * Save lesson records to persistent storage
     */
    private suspend fun saveLessonRecordsToStorage() {
        try {
            val recordsJson = json.encodeToString(_lessonRecords.value)
            context.teacherLessonDataStore.edit { preferences ->
                preferences[lessonRecordsKey] = recordsJson
            }
            Log.d(TAG, "Saved ${_lessonRecords.value.size} lesson records to storage")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save lesson records to storage", e)
        }
    }

    /**
     * Set current lesson data for planning (temporary storage for navigation)
     */
    fun setCurrentLessonData(lessonInfo: LessonInfo) {
        currentLessonData = lessonInfo
    }

    /**
     * Get current lesson data for planning
     */
    fun getCurrentLessonData(): LessonInfo? {
        return currentLessonData
    }

    /**
     * Clear current lesson data
     */
    fun clearCurrentLessonData() {
        currentLessonData = null
    }

    /**
     * Format lesson data for AI planning
     */
    fun formatLessonDataForPlanning(lessonInfo: LessonInfo): String {
        return buildString {
            appendLine("=== LESSON PLANNING REQUEST ===")
            appendLine("Subject: ${lessonInfo.subject}")
            appendLine("Grade Level: ${lessonInfo.grade}")
            appendLine("Topic: ${lessonInfo.topic}")
            appendLine("Duration: ${lessonInfo.duration}")
            if (lessonInfo.additionalNotes.isNotBlank()) {
                appendLine("Additional Notes: ${lessonInfo.additionalNotes}")
            }
            appendLine()
            appendLine("Please create a comprehensive lesson plan that includes:")
            appendLine("1. Learning objectives")
            appendLine("2. Materials needed")
            appendLine("3. Lesson structure with timing")
            appendLine("4. Activities and exercises")
            appendLine("5. Assessment methods")
            appendLine("6. Homework/extension activities")
            appendLine()
            appendLine("Make the lesson engaging and age-appropriate for grade ${lessonInfo.grade} students.")
        }
    }

    /**
     * Save generated lesson plan
     */
    suspend fun saveLessonPlan(planText: String, modelUsed: String) {
        currentLessonData?.let { lessonInfo ->
            val lessonRecord = LessonRecord(
                id = UUID.randomUUID().toString(),
                lessonInfo = lessonInfo,
                lessonPlan = LessonPlan(
                    planText = planText,
                    modelUsed = modelUsed
                )
            )
            
            val currentRecords = _lessonRecords.value.toMutableList()
            currentRecords.add(0, lessonRecord) // Add to beginning (newest first)
            _lessonRecords.value = currentRecords
            
            // Save to persistent storage
            saveLessonRecordsToStorage()
            
            // Clear current lesson data after saving
            currentLessonData = null
            
            Log.d(TAG, "Saved lesson plan for ${lessonInfo.subject} - ${lessonInfo.topic}")
        }
    }

    /**
     * Save a new lesson record
     */
    suspend fun saveLessonRecord(
        lessonInfo: LessonInfo,
        lessonPlan: LessonPlan
    ): String {
        val recordId = UUID.randomUUID().toString()
        val record = LessonRecord(
            id = recordId,
            lessonInfo = lessonInfo,
            lessonPlan = lessonPlan
        )
        
        val currentRecords = _lessonRecords.value.toMutableList()
        currentRecords.add(0, record) // Add to beginning (newest first)
        _lessonRecords.value = currentRecords
        
        // Save to persistent storage
        saveLessonRecordsToStorage()
        
        return recordId
    }

    /**
     * Get all lesson records as Flow
     */
    fun getAllLessonRecords(): Flow<List<LessonRecord>> {
        return _lessonRecords.asStateFlow()
    }

    /**
     * Search lesson records by subject or topic
     */
    fun searchLessonRecords(query: String): Flow<List<LessonRecord>> {
        return _lessonRecords.map { records ->
            if (query.isBlank()) {
                records
            } else {
                records.filter { record ->
                    record.lessonInfo.subject.contains(query, ignoreCase = true) ||
                    record.lessonInfo.topic.contains(query, ignoreCase = true) ||
                    record.lessonInfo.grade.contains(query, ignoreCase = true)
                }
            }
        }
    }

    /**
     * Get lesson record by ID
     */
    suspend fun getLessonRecordById(id: String): LessonRecord? {
        return _lessonRecords.value.find { it.id == id }
    }

    /**
     * Delete lesson record by ID
     */
    suspend fun deleteLessonRecord(id: String) {
        val currentRecords = _lessonRecords.value.toMutableList()
        currentRecords.removeAll { it.id == id }
        _lessonRecords.value = currentRecords
        
        // Save to persistent storage
        saveLessonRecordsToStorage()
    }

    /**
     * Clear all lesson records
     */
    suspend fun clearAllRecords() {
        _lessonRecords.value = emptyList()
        
        // Save to persistent storage
        saveLessonRecordsToStorage()
    }
}