/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.edge.gallery.data

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Mms
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.ai.edge.gallery.R

/** Type of task. */
enum class TaskType(val label: String, val id: String) {
  LLM_CHAT(label = "AI Chat", id = "llm_chat"),
  LLM_PROMPT_LAB(label = "Prompt Lab", id = "llm_prompt_lab"),
  LLM_ASK_IMAGE(label = "Ask Image", id = "llm_ask_image"),
  LLM_ASK_AUDIO(label = "Audio Scribe", id = "llm_ask_audio"),
  
  // Healthcare Professional Tasks
  HEALTHCARE_IMAGE_ANALYSIS(label = "Medical Image Analysis", id = "healthcare_image"),
  
  TEST_TASK_1(label = "Test task 1", id = "test_task_1"),
  TEST_TASK_2(label = "Test task 2", id = "test_task_2"),
}

/** Data class for a task listed in home screen. */
data class Task(
  /** Type of the task. */
  val type: TaskType,

  /** Icon to be shown in the task tile. */
  val icon: ImageVector? = null,

  /** Vector resource id for the icon. This precedes the icon if both are set. */
  val iconVectorResourceId: Int? = null,

  /** List of models for the task. */
  val models: MutableList<Model>,

  /** Description of the task. */
  val description: String,

  /** Documentation url for the task. */
  val docUrl: String = "",

  /** Source code url for the model-related functions. */
  val sourceCodeUrl: String = "",

  /** Placeholder text for the name of the agent shown above chat messages. */
  @StringRes val agentNameRes: Int = R.string.chat_generic_agent_name,

  /** Placeholder text for the text input field. */
  @StringRes val textInputPlaceHolderRes: Int = R.string.chat_textinput_placeholder,

  // The following fields are managed by the app. Don't need to set manually.
  var index: Int = -1,
  val updateTrigger: MutableState<Long> = mutableLongStateOf(0),
)

val TASK_LLM_CHAT =
  Task(
    type = TaskType.LLM_CHAT,
    icon = Icons.Outlined.Forum,
    models = mutableListOf(),
    description = "Chat with on-device large language models",
    docUrl = "https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android",
    sourceCodeUrl =
      "https://github.com/google-ai-edge/gallery/blob/main/Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/llmchat/LlmChatModelHelper.kt",
    textInputPlaceHolderRes = R.string.text_input_placeholder_llm_chat,
  )

val TASK_LLM_PROMPT_LAB =
  Task(
    type = TaskType.LLM_PROMPT_LAB,
    icon = Icons.Outlined.Widgets,
    models = mutableListOf(),
    description = "Single turn use cases with on-device large language models",
    docUrl = "https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android",
    sourceCodeUrl =
      "https://github.com/google-ai-edge/gallery/blob/main/Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/llmchat/LlmChatModelHelper.kt",
    textInputPlaceHolderRes = R.string.text_input_placeholder_llm_chat,
  )

val TASK_LLM_ASK_IMAGE =
  Task(
    type = TaskType.LLM_ASK_IMAGE,
    icon = Icons.Outlined.Mms,
    models = mutableListOf(),
    description = "Ask questions about images with on-device large language models",
    docUrl = "https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android",
    sourceCodeUrl =
      "https://github.com/google-ai-edge/gallery/blob/main/Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/llmchat/LlmChatModelHelper.kt",
    textInputPlaceHolderRes = R.string.text_input_placeholder_llm_chat,
  )

val TASK_LLM_ASK_AUDIO =
  Task(
    type = TaskType.LLM_ASK_AUDIO,
    icon = Icons.Outlined.Mic,
    models = mutableListOf(),
    // TODO(do not submit)
    description =
      "Instantly transcribe and/or translate audio clips using on-device large language models",
    docUrl = "https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android",
    sourceCodeUrl =
      "https://github.com/google-ai-edge/gallery/blob/main/Android/src/app/src/main/java/com/google/ai/edge/gallery/ui/llmchat/LlmChatModelHelper.kt",
    textInputPlaceHolderRes = R.string.text_input_placeholder_llm_chat,
  )

// Healthcare Professional Tasks
val TASK_HEALTHCARE_IMAGE_ANALYSIS =
  Task(
    type = TaskType.HEALTHCARE_IMAGE_ANALYSIS,
    icon = Icons.Outlined.MedicalServices,
    models = mutableListOf(),
    description = "Analyze medical images for clinical documentation and educational purposes. 100% private, HIPAA-compliant processing.",
    docUrl = "https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android",
    sourceCodeUrl = "",
    textInputPlaceHolderRes = R.string.text_input_placeholder_llm_chat,
    agentNameRes = R.string.chat_generic_agent_name,
  )



/** General AI tasks. */
val GENERAL_TASKS: List<Task> =
  listOf(TASK_LLM_ASK_IMAGE, TASK_LLM_ASK_AUDIO, TASK_LLM_PROMPT_LAB, TASK_LLM_CHAT)

/** Healthcare professional tasks. */
val HEALTHCARE_TASKS: List<Task> =
  listOf(TASK_HEALTHCARE_IMAGE_ANALYSIS)

/** All tasks (default to general). */
val TASKS: List<Task> = GENERAL_TASKS

/** Get tasks based on professional mode. */
fun getTasksForMode(isHealthcareMode: Boolean): List<Task> {
  return if (isHealthcareMode) HEALTHCARE_TASKS else GENERAL_TASKS
}

fun getModelByName(name: String): Model? {
  // Search in all tasks (both general and healthcare)
  val allTasks = GENERAL_TASKS + HEALTHCARE_TASKS
  for (task in allTasks) {
    for (model in task.models) {
      if (model.name == name) {
        return model
      }
    }
  }
  return null
}

fun processTasks() {
  // Process all tasks (both general and healthcare)
  val allTasks = GENERAL_TASKS + HEALTHCARE_TASKS
  for ((index, task) in allTasks.withIndex()) {
    task.index = index
    for (model in task.models) {
      model.preProcess()
    }
  }
}
