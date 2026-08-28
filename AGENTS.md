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

## Android/Termux Environment
- When working in the user's Android/Termux environment, do not run local Gradle builds, lint, tests, APK packaging, or Android runtime verification.
- Use the GitHub Actions CI results for build, lint, and test verification in this environment.

## GitHub CI and Release Rules
- The `CI` workflow runs lint and unit tests for pushes to `main` and `beta`, for pull requests targeting either branch, and when manually dispatched.
- A push to `main` must never create a tag or GitHub release. It runs the `CI` workflow only.
- A push to `beta` must never create a tag or GitHub release. It runs the `CI` workflow only.
- Releases are manual-only and split into two channel-locked workflows:
  - `.github/workflows/release-beta.yml` publishes a `-beta.N` prerelease. Trigger only from the `beta` branch (for example, `gh workflow run release-beta.yml --ref beta`).
  - `.github/workflows/release-main.yml` publishes a stable release. Trigger only from the `main` branch (for example, `gh workflow run release-main.yml --ref main`).
- Each release workflow fails fast when invoked against the wrong branch.
- `Release Beta` and `Release Stable` must each continue running their own lint and unit-test gates before signing or publishing, even though the separate CI workflow exists.
- Stable GitHub releases must have `prerelease: false`; beta-channel releases must have `prerelease: true`.

## Changelog and Release Metadata
- Before a stable release, add a non-empty `## [x.y.z] - YYYY-MM-DD` section to `CHANGELOG.md` matching the version that the workflow will publish.
- Before pushing release-worthy changes to `beta`, keep the `## [Unreleased]` section non-empty; beta release notes are sourced from that section.
- The release workflow must fail before creating a tag when its expected changelog section is missing or empty.
- GitHub release notes are sourced from the matching changelog section, not generated from commit history.
- `release-metadata.json` must include the exact extracted Markdown in its `releaseNotes` field, alongside the canonical changelog URL.
