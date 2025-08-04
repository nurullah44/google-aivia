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

package com.google.ai.edge.gallery.ui.llmchat

import android.graphics.Bitmap
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.bundleOf
import com.google.ai.edge.gallery.firebaseAnalytics
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.ui.common.chat.ChatInputType
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageAudioClip
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageImage
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageText
import com.google.ai.edge.gallery.ui.common.chat.ChatView
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Navigation destination data */
object LlmChatDestination {
  val route = "LlmChatRoute"
}

object LlmAskImageDestination {
  val route = "LlmAskImageRoute"
}

object LlmAskAudioDestination {
  val route = "LlmAskAudioRoute"
}

@Composable
fun LlmChatScreen(
  modelManagerViewModel: ModelManagerViewModel,
  navigateUp: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: LlmChatViewModel,
  customTitle: String? = null,
  overrideTask: Task? = null,
  hasLessonData: Boolean = false,
) {
  ChatViewWrapper(
    viewModel = viewModel,
    modelManagerViewModel = modelManagerViewModel,
    navigateUp = navigateUp,
    modifier = modifier,
    customTitle = customTitle,
    overrideTask = overrideTask,
    hasLessonData = hasLessonData,
  )
}

@Composable
fun LlmAskImageScreen(
  modelManagerViewModel: ModelManagerViewModel,
  navigateUp: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: LlmAskImageViewModel,
  customTitle: String? = null,
  overrideTask: Task? = null,
  hasPatientData: Boolean = false,
  hasCropData: Boolean = false,
) {
  ChatViewWrapper(
    viewModel = viewModel,
    modelManagerViewModel = modelManagerViewModel,
    navigateUp = navigateUp,
    modifier = modifier,
    customTitle = customTitle,
    overrideTask = overrideTask,
    chatInputType = ChatInputType.TEXT,
    hasPatientData = hasPatientData,
    hasCropData = hasCropData,
  )
}

@Composable
fun LlmAskAudioScreen(
  modelManagerViewModel: ModelManagerViewModel,
  navigateUp: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: LlmAskAudioViewModel,
) {
  ChatViewWrapper(
    viewModel = viewModel,
    modelManagerViewModel = modelManagerViewModel,
    navigateUp = navigateUp,
    modifier = modifier,
  )
}

@Composable
fun ChatViewWrapper(
  viewModel: LlmChatViewModelBase,
  modelManagerViewModel: ModelManagerViewModel,
  navigateUp: () -> Unit,
  modifier: Modifier = Modifier,
  customTitle: String? = null,
  overrideTask: Task? = null,
  chatInputType: ChatInputType = ChatInputType.TEXT,
  hasPatientData: Boolean = false,
  hasCropData: Boolean = false,
  hasLessonData: Boolean = false,
) {
  val context = LocalContext.current

  // Handle patient data injection for medical analysis
  LaunchedEffect(hasPatientData) {
    if (hasPatientData) {
      // Get medical repository through Hilt entry point
      val appContext = context.applicationContext as com.google.ai.edge.gallery.GalleryApplication
      val medicalRepository = appContext.medicalAnalysisRepository
      val patientData = medicalRepository.getCurrentPatientData()
      if (patientData != null) {
        val (patientInfo, imageUri) = patientData
        val selectedModel = modelManagerViewModel.uiState.value.selectedModel
        
        // Add analyzing message
        viewModel.addMessage(
          model = selectedModel,
          message = com.google.ai.edge.gallery.ui.common.chat.ChatMessageInfo(
            content = "🔬 Analyzing medical image for ${patientInfo.firstName} ${patientInfo.lastName}..."
          )
        )
        
        // Create patient context message
        val patientContext = medicalRepository.formatPatientDataForAnalysis(patientInfo)
        
        // Add patient info as user message
        viewModel.addMessage(
          model = selectedModel,
          message = ChatMessageText(
            content = patientContext,
            side = com.google.ai.edge.gallery.ui.common.chat.ChatSide.USER
          )
        )
        
        // Convert URI to Bitmap and add image message
        try {
          val inputStream = context.contentResolver.openInputStream(imageUri)
          val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
          inputStream?.close()
          
          if (bitmap != null) {
            val imageMessage = com.google.ai.edge.gallery.ui.common.chat.ChatMessageImage(
              bitmap = bitmap,
              imageBitMap = bitmap.asImageBitmap(),
              side = com.google.ai.edge.gallery.ui.common.chat.ChatSide.USER
            )
            viewModel.addMessage(
              model = selectedModel,
              message = imageMessage
            )
            
            // Trigger automatic analysis
            viewModel.generateResponse(
              model = selectedModel,
              input = patientContext,
              images = listOf(bitmap),
              onError = {
                viewModel.addMessage(
                  model = selectedModel,
                  message = com.google.ai.edge.gallery.ui.common.chat.ChatMessageWarning(
                    content = "Error occurred during medical analysis. Please try again."
                  )
                )
              }
            )
          }
        } catch (e: Exception) {
          viewModel.addMessage(
            model = selectedModel,
            message = com.google.ai.edge.gallery.ui.common.chat.ChatMessageWarning(
              content = "Error loading medical image. Please try again."
            )
          )
        }
      }
    }
  }

  // Handle crop data injection for agricultural analysis
  LaunchedEffect(hasCropData) {
    if (hasCropData) {
      // Get crop repository through Hilt entry point
      val appContext = context.applicationContext as com.google.ai.edge.gallery.GalleryApplication
      val cropRepository = appContext.cropAnalysisRepository
      val cropData = cropRepository.getCurrentCropData()
      android.util.Log.d("CropChatDebug", "hasCropData: $hasCropData, cropData: $cropData")
      if (cropData != null) {
        val (cropInfo, imageUri) = cropData
        val selectedModel = modelManagerViewModel.uiState.value.selectedModel
        android.util.Log.d("CropChatDebug", "Crop info found: ${cropInfo.cropType}, model: ${selectedModel.name}")
        
        // Add analyzing message
        viewModel.addMessage(
          model = selectedModel,
          message = com.google.ai.edge.gallery.ui.common.chat.ChatMessageInfo(
            content = "🌾 Analyzing crop image for ${cropInfo.cropType} in ${cropInfo.location}..."
          )
        )
        
        // Create crop context message
        val cropContext = cropRepository.formatCropDataForAnalysis(cropInfo)
        
        // Add crop info as user message
        viewModel.addMessage(
          model = selectedModel,
          message = ChatMessageText(
            content = cropContext,
            side = com.google.ai.edge.gallery.ui.common.chat.ChatSide.USER
          )
        )
        
        // Convert URI to Bitmap and add image message
        try {
          val inputStream = context.contentResolver.openInputStream(imageUri)
          val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
          inputStream?.close()
          
          if (bitmap != null) {
            val imageMessage = com.google.ai.edge.gallery.ui.common.chat.ChatMessageImage(
              bitmap = bitmap,
              imageBitMap = bitmap.asImageBitmap(),
              side = com.google.ai.edge.gallery.ui.common.chat.ChatSide.USER
            )
            viewModel.addMessage(
              model = selectedModel,
              message = imageMessage
            )
            
            // Trigger automatic analysis
            viewModel.generateResponse(
              model = selectedModel,
              input = cropContext,
              images = listOf(bitmap),
              onError = {
                viewModel.addMessage(
                  model = selectedModel,
                  message = com.google.ai.edge.gallery.ui.common.chat.ChatMessageWarning(
                    content = "Error occurred during crop analysis. Please try again."
                  )
                )
              }
            )
          }
        } catch (e: Exception) {
          android.util.Log.e("CropChatDebug", "Error loading crop image", e)
          viewModel.addMessage(
            model = selectedModel,
            message = com.google.ai.edge.gallery.ui.common.chat.ChatMessageWarning(
              content = "Error loading crop image. Please try again."
            )
          )
        }
      } else {
        android.util.Log.e("CropChatDebug", "Crop data is null!")
      }
    }
  }

  // Handle lesson data injection for teacher lesson planning
  LaunchedEffect(hasLessonData) {
    if (hasLessonData) {
      // Get teacher lesson repository through Hilt entry point
      val appContext = context.applicationContext as com.google.ai.edge.gallery.GalleryApplication
      val teacherRepository = appContext.teacherLessonRepository
      val lessonData = teacherRepository.getCurrentLessonData()
      if (lessonData != null) {
        val selectedModel = modelManagerViewModel.uiState.value.selectedModel
        
        // Add planning message
        viewModel.addMessage(
          model = selectedModel,
          message = com.google.ai.edge.gallery.ui.common.chat.ChatMessageInfo(
            content = "📚 Creating lesson plan for ${lessonData.subject} - ${lessonData.topic}..."
          )
        )
        
        // Create lesson context message
        val lessonContext = teacherRepository.formatLessonDataForPlanning(lessonData)
        
        // Add lesson info as user message
        viewModel.addMessage(
          model = selectedModel,
          message = ChatMessageText(
            content = lessonContext,
            side = com.google.ai.edge.gallery.ui.common.chat.ChatSide.USER
          )
        )
        
        // Trigger automatic lesson plan generation
        viewModel.generateResponse(
          model = selectedModel,
          input = lessonContext,
          images = emptyList(),
          onError = {
            viewModel.addMessage(
              model = selectedModel,
              message = com.google.ai.edge.gallery.ui.common.chat.ChatMessageWarning(
                content = "Error occurred during lesson plan generation. Please try again."
              )
            )
          }
        )
      }
    }
  }

  ChatView(
    task = overrideTask ?: viewModel.task,
    viewModel = viewModel,
    modelManagerViewModel = modelManagerViewModel,
    hasPatientData = hasPatientData,
    hasCropData = hasCropData,
    hasLessonData = hasLessonData,
    onSaveAnalysisClicked = { model, message ->
      if (hasPatientData && message is ChatMessageText) {
        // Get medical repository through Hilt entry point
        val appContext = context.applicationContext as com.google.ai.edge.gallery.GalleryApplication
        val medicalRepository = appContext.medicalAnalysisRepository
        
        // Launch coroutine to save analysis
        CoroutineScope(Dispatchers.IO).launch {
          medicalRepository.saveAnalysis(message.content, model.name)
          
          // Show confirmation message on main thread
          withContext(Dispatchers.Main) {
            viewModel.addMessage(
              model = model,
              message = com.google.ai.edge.gallery.ui.common.chat.ChatMessageInfo(
                content = "✅ Analysis saved successfully!"
              )
            )
          }
        }
      } else if (hasCropData && message is ChatMessageText) {
        // Get crop repository through Hilt entry point
        val appContext = context.applicationContext as com.google.ai.edge.gallery.GalleryApplication
        val cropRepository = appContext.cropAnalysisRepository
        
        // Launch coroutine to save analysis
        CoroutineScope(Dispatchers.IO).launch {
          cropRepository.saveAnalysis(message.content, model.name)
          
          // Show confirmation message on main thread
          withContext(Dispatchers.Main) {
            viewModel.addMessage(
              model = model,
              message = com.google.ai.edge.gallery.ui.common.chat.ChatMessageInfo(
                content = "✅ Crop analysis saved successfully!"
              )
            )
          }
        }
      } else if (hasLessonData && message is ChatMessageText) {
        // Get teacher lesson repository through Hilt entry point
        val appContext = context.applicationContext as com.google.ai.edge.gallery.GalleryApplication
        val teacherRepository = appContext.teacherLessonRepository
        
        // Launch coroutine to save lesson plan
        CoroutineScope(Dispatchers.IO).launch {
          teacherRepository.saveLessonPlan(message.content, model.name)
          
          // Show confirmation message on main thread
          withContext(Dispatchers.Main) {
            viewModel.addMessage(
              model = model,
              message = com.google.ai.edge.gallery.ui.common.chat.ChatMessageInfo(
                content = "✅ Lesson plan saved successfully!"
              )
            )
          }
        }
      }
    },
    onSendMessage = { model, messages ->
      for (message in messages) {
        viewModel.addMessage(model = model, message = message)
      }

      var text = ""
      val images: MutableList<Bitmap> = mutableListOf()
      val audioMessages: MutableList<ChatMessageAudioClip> = mutableListOf()
      var chatMessageText: ChatMessageText? = null
      for (message in messages) {
        if (message is ChatMessageText) {
          chatMessageText = message
          text = message.content
        } else if (message is ChatMessageImage) {
          images.add(message.bitmap)
        } else if (message is ChatMessageAudioClip) {
          audioMessages.add(message)
        }
      }
      if ((text.isNotEmpty() && chatMessageText != null) || audioMessages.isNotEmpty()) {
        // Prepend system prompt if one is set
        val systemPrompt = viewModel.getSystemPrompt(model)
        val finalInput = if (systemPrompt != null && text.isNotEmpty()) {
          "${systemPrompt.prompt}\n\nUser: $text"
        } else {
          text
        }
        
        modelManagerViewModel.addTextInputHistory(text)
        viewModel.generateResponse(
          model = model,
          input = finalInput,
          images = images,
          audioMessages = audioMessages,
          onError = {
            viewModel.handleError(
              context = context,
              model = model,
              modelManagerViewModel = modelManagerViewModel,
              triggeredMessage = chatMessageText,
            )
          },
        )

        firebaseAnalytics?.logEvent(
          "generate_action",
          bundleOf("capability_name" to viewModel.task.type.toString(), "model_id" to model.name),
        )
      }
    },
    onRunAgainClicked = { model, message ->
      if (message is ChatMessageText) {
        viewModel.runAgain(
          model = model,
          message = message,
          onError = {
            viewModel.handleError(
              context = context,
              model = model,
              modelManagerViewModel = modelManagerViewModel,
              triggeredMessage = message,
            )
          },
        )
      }
    },
    onBenchmarkClicked = { _, _, _, _ -> },
    onResetSessionClicked = { model -> viewModel.resetSession(model = model) },
    showStopButtonInInputWhenInProgress = true,
    onStopButtonClicked = { model -> viewModel.stopResponse(model = model) },
    navigateUp = navigateUp,
    modifier = modifier,
    customTitle = customTitle,
    chatInputType = chatInputType,
  )
}
