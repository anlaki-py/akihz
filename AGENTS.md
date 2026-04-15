# Android App Development - Agent Guidelines

## Code Structure Rules
1. **File Limit**: Maximum 200 lines per Kotlin/Java file
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

## Mandatory
1. Never run `./gradlew build` to ensure it compiles or any similar commands, i will do that manually and hand you the logs and errors.
2. Send completion notification
