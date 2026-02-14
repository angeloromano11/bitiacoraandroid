# Bitácora Digital - Android

Native Android app built with Kotlin and Jetpack Compose.

## Requirements

- Android Studio Hedgehog (2023.1) or later
- Kotlin 1.9+
- Minimum SDK 26 (Android 8.0)
- Target SDK 34 (Android 14)
- Physical Android device recommended for camera/microphone testing

## Setup

1. Open the project in Android Studio
2. Sync Gradle dependencies
3. Build and run on emulator or device

## Architecture

MVVM with Service layer + Hilt dependency injection:

- **Screens** (Jetpack Compose): RecordingScreen, LibraryScreen, SessionDetailScreen, MemoryAssistantScreen, SettingsScreen, OnboardingScreen
- **ViewModels** (Hilt-injected): RecordingViewModel, LibraryViewModel, SessionDetailViewModel, MemoryAssistantViewModel, SettingsViewModel, OnboardingViewModel
- **Services**: CameraService (CameraX), SpeechService (SpeechRecognizer), AIService (Gemini API via Retrofit), MemoryService, MemorySearchService, MemoryAssistantService, StorageService, KeychainHelper (EncryptedSharedPreferences)
- **Data**: Room database with SessionDao + MemoryEntryDao, repositories

## Dependencies

| Library | Usage |
|---------|-------|
| Jetpack Compose + Material 3 | UI |
| Navigation Compose | Screen navigation |
| CameraX | Video recording |
| Room | SQLite database |
| Media3 (ExoPlayer) | Video playback |
| Retrofit + Gson | Gemini API networking |
| Hilt | Dependency injection |
| EncryptedSharedPreferences | Secure API key storage |
| DataStore | User preferences |

## Environment

Set your Gemini API key in the app's Settings screen, or configure it during onboarding.

## Documentation

- Full implementation plan: [docs/plan_android.md](../docs/plan_android.md)
- Feature inventory: [docs/FEATURES.md](../docs/FEATURES.md)
