# 🌍 TripLens - Travel Experience Sharing Platform

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)
[![API](https://img.shields.io/badge/API-33%2B-brightgreen.svg)](https://android-arsenal.com/api?level=33)

A modern Android travel social app built with **MVVM architecture**, enabling users to share and discover travel experiences enriched with real-time weather and country information from external APIs.

---

## 📱 Features

### Core Functionality
- 🔐 **User Authentication** - Firebase Auth with email/password + "Stay Logged In"
- 📸 **Multi-Image Posts** - Upload up to 5 images per post with camera/gallery support
- 🌤️ **Real-Time Weather** - OpenWeather API integration (temperature, conditions, wind, humidity)
- 🏳️ **Country Intelligence** - RestCountries API (capital, population, currency, languages, flag)
- 💬 **Real-Time Comments** - Firestore sub-collection based commenting system
- 👤 **Profile Management** - Edit display name and profile picture
- ✏️ **Post Editing** - Update text and images, delete posts with confirmation
- 📶 **Offline Support** - Room database caching for offline browsing
- ⚡ **Background API Sync** - Posts save instantly, data enrichment happens in background

---

## 🚀 Quick Start

### Prerequisites
- **Android Studio** Hedgehog or newer
- **JDK** 11+
- **Minimum SDK** 33 (Android 13)

### 1. Clone the Repository
```bash
git clone https://github.com/yourusername/TripLens.git
cd TripLens
```

### 2. Configure API Keys

#### OpenWeather API (Required)
1. Get a free API key: [openweathermap.org/api](https://openweathermap.org/api)
2. Copy the template file:
   ```bash
   cp local.properties.template local.properties
   ```
3. Edit `local.properties` and add your key:
   ```properties
   OPENWEATHER_API_KEY=your_api_key_here
   ```

#### Firebase Configuration (Required)
1. Create a Firebase project: [console.firebase.google.com](https://console.firebase.google.com)
2. Add Android app with package name: `com.colman.triplens`
3. Download `google-services.json` and place in `app/` directory
4. Enable **Firebase Authentication** (Email/Password provider)
5. Enable **Cloud Firestore** (start in test mode for development)

### 3. Build & Run
```bash
./gradlew assembleDebug
# OR: Open in Android Studio and click Run ▶️
```

---

## 🏗️ Architecture

### MVVM Pattern
```
┌─────────────┐
│   Fragment  │ ← View Layer (UI)
└──────┬──────┘
       │ observes LiveData
┌──────▼──────┐
│  ViewModel  │ ← Presentation Logic
└──────┬──────┘
       │ calls
┌──────▼──────┐
│ Repository  │ ← Data Layer
└──────┬──────┘
       │
  ┌────┴────┬────────┬────────┐
  │         │        │        │
┌─▼──┐  ┌──▼──┐  ┌──▼───┐  ┌─▼────┐
│Room│  │Fire-│  │Retro-│  │Cloud-│
│    │  │store│  │ fit  │  │inary │
└────┘  └─────┘  └──────┘  └──────┘
Local   Remote   External  Image
Cache   Storage  APIs      Upload
```

### Key Components

#### Data Layer
- **Room Database** - Offline caching
- **Firebase Firestore** - Cloud storage for posts and comments
- **Retrofit** - REST API client for weather and country data
- **Cloudinary** - Image hosting and CDN

#### Presentation Layer
- **ViewModels** - Business logic
- **LiveData** - Reactive data streams
- **Repository Pattern** - Data abstraction

#### UI Layer
- **Fragments** - All screens (Login, Feed, PostDetail, Profile)
- **ViewBinding** - Type-safe view access
- **Navigation Component** - Single-activity navigation with SafeArgs
- **Material Design 3** - Modern UI components

---

## 📦 Tech Stack

### Core
- **Kotlin** 2.0.21
- **Android Gradle Plugin** 8.8.0
- **Minimum SDK** 33, **Target SDK** 36

### Architecture
- **Navigation Component** 2.8.5 (SafeArgs)
- **Room** 2.7+ (Local database with LiveData)
- **ViewModel + LiveData** (MVVM pattern)

### Networking
- **Retrofit** 2.11.0 (REST API client)
- **Gson** 2.11.0 (JSON parsing)

### Firebase
- **Firebase Auth** (User authentication)
- **Cloud Firestore** (NoSQL cloud database)

### Image Handling
- **Cloudinary Android SDK** 3.0.2 (Image upload)
- **Picasso** 2.8 (Image loading and caching)

### UI
- **Material Design 3** 1.13+
- **ViewPager2** (Image gallery)
- **RecyclerView** with DiffUtil

---

## 📂 Project Structure

```
app/src/main/java/com/colman/triplens/
├── auth/                    # Authentication logic
├── base/                    # App entry points
├── data/
│   ├── local/              # Room database
│   ├── model/              # Data models
│   ├── remote/             # API services
│   ├── repo/               # Repository pattern
│   ├── models/             # Cloudinary
│   └── util/               # Utilities
├── ui/
│   ├── auth/               # Login & Register
│   ├── feed/               # Main feed
│   ├── post/               # Post creation & detail
│   ├── profile/            # User profile
│   └── common/             # Shared UI components
└── util/                   # App utilities
```

---

**Built with ❤️ for travelers worldwide** 🌍✈️


