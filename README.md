# newsApp

An Android news reader built with Kotlin and Jetpack Compose. Browse headlines by category, search articles, and bookmark favourites across devices with per-user cloud sync via AWS.

## Features

- **Browse by category** — general, technology, business, sports, health, science, entertainment
- **Search** — filter articles by title, author, or source
- **Article detail** — reads the original article in a full-screen WebView with a loading progress bar
- **Bookmarks** — save articles locally and sync them to the cloud per user account
- **Authentication** — sign up, confirm email, sign in, and sign out via AWS Cognito
- **Offline-first** — articles are cached in Room; the feed remains readable without network

## Setup

Clone the repo and open it in Android Studio. No API keys are required — the backend endpoints and AWS Amplify configuration are already bundled in the project.

Build and run:

```bash
./gradlew installDebug
```

**Requirements**: Android device or emulator running API 24 (Android 7.0) or higher.

## Tech Stack

| Concern | Library |
|---|---|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material 3 (BOM 2024.09.00) |
| Navigation | Navigation Compose 2.8.0 |
| Dependency Injection | Hilt 2.51.1 |
| Async | Kotlin Coroutines + Flow 1.8.1 |
| Networking | Retrofit 2.9.0 + OkHttp 4.12.0 |
| Local cache | Room 2.6.1 + Paging 3.3.2 |
| Images | Coil 2.7.0 |
| Auth | AWS Amplify Cognito 2.10.0 |
| Testing | JUnit 4 + MockK + Turbine |

## Architecture

The app uses **Clean Architecture + MVVM** with Unidirectional Data Flow (UDF).

```
ui/          — Jetpack Compose screens and components
presentation/ — ViewModels, UiState data classes, ScreenEvents interfaces
domain/      — Pure Kotlin models, repository interfaces, use cases
data/        — Retrofit DTOs, Room entities/DAOs, repository implementations, mappers
di/          — Hilt modules (Network, Database, Repository)
```

### Data flow

Room is the single source of truth. Network responses are upserted into Room; the UI reads exclusively from Room via Paging 3. Pulling to refresh or switching categories triggers a network fetch → Room write → automatic PagingSource invalidation → UI update.

Bookmark state is maintained in an in-memory `Set<String>` (`MutableStateFlow`) inside `NewsRepositoryImpl` to avoid JOIN queries on every paginated page, and reconciled with Room after each mutation. Bookmarks are also synced to a per-user DynamoDB table via an authenticated API.

### Auth flow

`AuthViewModel` drives sign-up → email confirmation → sign-in using AWS Amplify Cognito (SRP auth). After sign-in, remote bookmarks are fetched and merged into Room. On sign-out, local bookmarks are cleared. `CognitoAuthInterceptor` (OkHttp) attaches the Cognito ID token to all bookmark API requests.

If a user tries to bookmark an article while signed out, the app redirects to sign-in and retries the bookmark action after successful authentication (via `savedStateHandle` on the nav back stack).

## Commands

```bash
# Build
./gradlew build

# Run all unit tests
./gradlew testDebugUnitTest

# Run a single test class
./gradlew testDebugUnitTest --tests "com.example.news.presentation.home.HomeViewModelTest"

# Install on connected device/emulator
./gradlew installDebug
```
