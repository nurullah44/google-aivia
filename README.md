# Google AIVIA

[![License](https://img.shields.io/badge/License-CC%20BY%204.0-blue.svg)](LICENSE)

**Enhanced On-Device AI Experience - Built on Google AI Edge Gallery**

Google AIVIA is an advanced Android application that transforms the original Google AI Edge Gallery into a comprehensive AI assistant platform. This enhanced version brings professional-grade AI capabilities directly to your device, running entirely offline once models are loaded.

## Overview

Google AIVIA extends the core functionality of Google AI Edge Gallery with specialized professional modes:

- **General AI Tasks**: Chat, image analysis, and prompt experimentation
- **Healthcare Mode**: Medical image analysis and patient information processing
- **Education Mode**: Lesson planning and educational content generation
- **Agriculture Mode**: Crop analysis and farming insights

## Core Features

**Local Processing**: All AI operations run on-device, ensuring privacy and offline functionality.

**Professional Modes**: 
- Healthcare professionals can analyze medical images and process patient data
- Teachers can generate lesson plans and educational content
- Farmers can analyze crop health and get agricultural recommendations

**Model Management**: Easy switching between different AI models with automatic download and management.

**Multi-Modal Support**: Text, image, and audio processing capabilities.

## Getting Started

1. **Download**: [Download APK](https://github.com/nurullah44/google-aivia/releases/download/v1.0.0/google-aivia.apk)
2. **Install**: Enable "Install from unknown sources" in Android settings
3. **Launch**: Open the app and select your preferred mode
4. **Download Models**: Choose and download AI models for offline use

## Architecture

Google AIVIA is built on the Google AI Edge framework with the following key components:

- **UI Layer**: Jetpack Compose for modern Android UI
- **Business Logic**: MVVM architecture with Hilt dependency injection
- **Data Layer**: Room database with DataStore for preferences
- **AI Engine**: MediaPipe LLM Inference API for on-device model execution
- **Model Management**: Dynamic model downloading from Hugging Face

The application follows Android best practices with clean architecture principles, ensuring maintainability and scalability.

## Technology Stack

- **Framework**: Google AI Edge
- **Runtime**: LiteRT for optimized model execution
- **UI**: Jetpack Compose
- **Architecture**: MVVM with Hilt
- **Models**: Hugging Face integration for model discovery

## Development

This project is an enhanced fork of the original Google AI Edge Gallery, focusing on professional use cases and improved user experience. The codebase has been restructured to support multiple professional domains while maintaining the core AI capabilities.

## License

Licensed under the Creative Commons Attribution 4.0 International License. See the [LICENSE](LICENSE) file for details.

## Links

- [Original Google AI Edge Gallery](https://github.com/google-ai-edge/gallery)
- [Google AI Edge Documentation](https://ai.google.dev/edge)
- [MediaPipe LLM Inference](https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference)
