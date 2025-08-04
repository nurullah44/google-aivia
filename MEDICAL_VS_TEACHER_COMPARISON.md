# MEDICAL vs TEACHER CRITICAL BEHAVIOR DIFFERENCES

## NAVIGATION DESTINATION DIFFERENCE - KRİTİK FARK!

### MEDICAL NAVIGATION:
```kotlin
navigateToAnalysis = { modelName ->
    // Navigate to LLM chat screen with patient data and medical context
    navController.navigate("${LlmAskImageDestination.route}/${modelName}?title=Medical Image Analysis&context=medical&patientData=true")
}
```

### TEACHER NAVIGATION:
```kotlin
navigateToPlanning = { modelName ->
    // Navigate to LLM chat screen with lesson data and teacher context
    navController.navigate("${LlmChatDestination.route}/${modelName}?title=Lesson Plan Generator&context=teacher&lessonData=true")
}
```

## KRİTİK FARK:
- **MEDICAL:** `LlmAskImageDestination` kullanıyor (IMAGE CHAT SCREEN)
- **TEACHER:** `LlmChatDestination` kullanıyor (TEXT CHAT SCREEN)

Bu iki destination FARKLI SCREEN'LER! Bu farklı davranışa neden oluyor!

### LlmAskImageDestination Features:
- Image upload capability
- Image analysis with text
- Visual medical analysis

### LlmChatDestination Features:  
- Text-only conversation
- No image handling
- Pure chat interface

## DİĞER KRİTİK FARKLAR:

### 1. TASK PARAMETER NAMES:
- Medical: `patientData=true`
- Teacher: `lessonData=true`

### 2. CONTEXT NAMES:
- Medical: `context=medical`  
- Teacher: `context=teacher`

### 3. DESTINATION ROUTES:
- Medical: Uses IMAGE-based destination
- Teacher: Uses TEXT-based destination

## NAVIGATION GRAPH'TA HANDLING:

### Medical Context Handling:
```kotlin
composable(
    route = "${LlmAskImageDestination.route}/{modelName}?title={title}&context={context}&patientData={patientData}",
    // IMAGE CHAT SCREEN WITH IMAGE HANDLING
)
```

### Teacher Context Handling:
```kotlin
composable(
    route = "${LlmChatDestination.route}/{modelName}?title={title}&context={context}&lessonData={lessonData}",
    // TEXT CHAT SCREEN WITHOUT IMAGE HANDLING
)
```

## SONUÇ:
Medical ve Teacher FARKLI CHAT SCREEN'LERİ kullanıyor:
- Medical → LlmAskImageScreen (Image + Text)
- Teacher → LlmChatScreen (Text Only)

Bu farklı davranışların temel nedeni!

## HOMESCREEN'DE KRİTİK FARK!

### DOCTOR MODE (ÇALIŞIYOR):
```kotlin
if (showDoctorMode) {
  ProfessionalTaskDialog(
    title = "Doctor",
    subtitle = "Healthcare AI for clinical professionals",
    tasks = listOf(
      "Medical Image Analysis" to { 
        navigateToTaskScreen(uiState.tasks.find { it.type.id == "healthcare_image" }!!)
      },
      "Clinical Documentation" to { /* TODO */ },
      "Medical Reference" to { /* TODO */ }
    ),
    onDismiss = { showDoctorMode = false }
  )
}
```

### TEACHER MODE (TODO!):
```kotlin
if (showTeacherMode) {
  ProfessionalTaskDialog(
    title = "Teacher", 
    subtitle = "Educational AI for learning environments",
    tasks = listOf(
      "Lesson Plans" to { /* TODO */ },  // BU TODO!
      "Assessments" to { /* TODO */ },
      "Explanations" to { /* TODO */ }
    ),
    onDismiss = { showTeacherMode = false }
  )
}
```

## ASIL SORUN:
Teacher'ın "Lesson Plans" task'ı TODO durumunda! Gerçek navigate fonksiyonu yok!

**OLMASI GEREKEN:**
```kotlin
"Lesson Plans" to { 
  navigateToTaskScreen(uiState.tasks.find { it.type.id == "teacher_lesson" }!!)
},
```

**MEVCUT:** `{ /* TODO */ }`

Bu yüzden Teacher mode çalışmıyor! Navigation eksik!
