# newsApp

A news application built with Jetpack Compose and Material 3 that displays top headlines from NewsAPI.

## Setup

### API Key Configuration

This app requires a NewsAPI key to fetch news articles. Follow these steps:

1. Get your API key from [NewsAPI](https://newsapi.org/)
2. Open or create the `local.properties` file in the project root directory
3. Add the following line:
   ```
   NEWS_API_KEY=your_api_key_here
   ```
4. Replace `your_api_key_here` with your actual NewsAPI key

**Note:** The `local.properties` file is already in `.gitignore` and will not be committed to version control.

## Architecture

The app follows a layered architecture:
- **UI Layer**: Jetpack Compose screens and components
- **Presentation Layer**: ViewModels and UI state/events
- **Data Layer**: Repository, API service (Retrofit), and DTOs

## Features

- Browse news articles by category (general, technology, business, sports, health, science, entertainment)
- View article details with images, author, and publication date
- Open articles in browser
- Loading and error states