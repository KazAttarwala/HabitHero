# HabitHero 🦸‍♂️

HabitHero is an Android application designed to help users build and maintain good habits through daily tracking, streaks, and personalized AI-powered insights and recommendations.

## Features

### Core Functionality
- **Habit Tracking**: Create, track, and manage daily habits with customizable frequency
- **Progress Tracking**: Visual indicators show completion progress for each habit
- **Streak Building**: Maintain streaks of consecutive days to build consistency
- **Daily Recap**: End-of-day notifications summarizing completed and incomplete habits

### AI-Powered Insights
- **Habit Analysis**: Get AI-powered insights about your habit performance
- **Personalized Recommendations**: Receive tailored suggestions to improve consistency
- **Motivational Quotes**: Daily motivational quotes to keep you inspired

### Data Visualization
- **Weekly Progress Charts**: Visualize your habit completion rates with interactive charts
- **Completion Rate Statistics**: Track your overall success rate for each habit
- **Historical Data**: View past performance to identify patterns

### Gamification
- **Visual Rewards**: Enjoy confetti animations when completing habits
- **Achievement Tracking**: Build and maintain habit streaks

## Technologies Used

### Architecture
- MVVM (Model-View-ViewModel) architecture for clean separation of concerns
- Repository pattern for data access
- LiveData for reactive UI updates

### Backend & Storage
- **Firebase Authentication**: Secure user authentication
- **Firebase Firestore**: Cloud-based NoSQL database for habit data storage
- **WorkManager**: Background processing for notifications and data syncing

### UI Components
- Material Design components
- Navigation Component for screen navigation
- RecyclerView with custom adapters for list displays
- MPAndroidChart for data visualization

### API Integration
- **Anthropic Claude API**: Powers the AI-based insights and recommendations
- **Retrofit**: For API communication
- **Moshi**: JSON parsing

## Getting Started

### Prerequisites
- Android Studio Arctic Fox (2020.3.1) or newer
- JDK 11
- Android SDK 35 or higher
- Google Services JSON file for Firebase integration

### Installation
1. Clone the repository
   ```
   git clone https://github.com/yourusername/habithero.git
   ```

2. Open the project in Android Studio

3. Create a Firebase project and add your Android app to it
   - Download the `google-services.json` file
   - Place it in the app directory

4. Get an API key from Anthropic for Claude API access
   - Add your api key in local.properties file

5. Build and run the project

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/example/habithero/
│   │   │   ├── api/             # API service implementations
│   │   │   ├── config/          # Configuration classes
│   │   │   ├── model/           # Data models
│   │   │   ├── repository/      # Data repositories
│   │   │   ├── ui/
│   │   │   │   ├── adapters/    # RecyclerView adapters
│   │   │   │   ├── fragments/   # UI fragments
│   │   │   │   └── viewmodels/  # ViewModels
│   │   │   ├── utils/           # Utility classes
│   │   │   └── workers/         # Background workers
│   │   ├── res/                 # Resources (layouts, drawables, etc.)
│   │   └── AndroidManifest.xml
└── build.gradle.kts             # App-level build configuration
```

## Key Components

### Models
- **Habit**: Represents a habit with properties like title, description, frequency, progress, streak, etc.
- **HabitEntry**: Tracks individual habit completion entries
- **HabitAnalysis**: Contains AI-generated analysis and recommendations

### ViewModels
- **HomeViewModel**: Manages habit display and updates on the main screen
- **InsightsViewModel**: Handles data visualization and analytics
- **HabitEditViewModel**: Manages habit creation and editing

### Workers
- **MidnightResetWorker**: Resets daily habit progress at midnight
- **DailyRecapWorker**: Sends end-of-day notification with habit summary

## Firebase Setup

1. Create a new Firebase project
2. Add an Android app to your Firebase project
   - Use package name: `com.example.habithero`
3. Enable Authentication and Firestore in the Firebase console
4. Set up Firestore security rules to protect user data

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- [Firebase](https://firebase.google.com/) for backend services
- [Anthropic](https://www.anthropic.com/) for Claude AI API
- [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) for chart visualization
- [Konfetti](https://github.com/DanielMartinus/Konfetti) for confetti animations 