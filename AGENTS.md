# Android App Development - Agent Guidelines

## Code Structure Rules
1. **File Limit**: Maximum 200 lines per Kotlin/Java file. (Note: It is acceptable if the file is slightly longer than 200 lines, or even if it is necessary to have many lines. Only split the file if having a large number of lines does not make sense.)
2. **Package Structure**:
   - `data/` - models, repositories, data sources
   - `domain/` - use cases, business logic
   - `presentation/` - UI, ViewModels, Composables
   - `di/` - dependency injection modules
   - `utils/` - helpers and extensions
3. **Single Responsibility**: One class = one purpose. Extract interfaces, models, and utils

## Documentation Rules
- Document all public functions with KDoc (what it does, params, returns)
- Explain complex business logic (why, not what)
- Comment non-obvious edge cases and workarounds
- Don't comment obvious code (e.g., `val name = "John"`)

## Debug APK Builds
- When asked to build a debug app/APK, build only the universal APK using `./gradlew :app:packageDebugUniversalApk` rather than assembling every ABI split.
- The universal debug APK is written to `app/build/outputs/apk_from_bundle/debug/app-debug-universal.apk`.
