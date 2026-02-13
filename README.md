# Quezic - Android Music Library App

A modern Android music library app built with Kotlin and Jetpack Compose. Quezic allows you to search, download, and organize music from multiple sources including YouTube and SoundCloud, with smart playlist recommendations powered by content analysis.

**IMPORTANT** In order for youtube search and download to work, press the cog wheel at the top right and authenticate locally with youtube.

## Features

### Music Search & Discovery
- **Multi-source search**: Search across YouTube, SoundCloud, and Bandcamp simultaneously
- **Source filtering**: Filter results by platform using filter chips
- **Real-time search**: Debounced search with instant results
- **Recent searches**: Quick access to your search history

### Music Download
- **Background downloads**: Download songs using WorkManager for reliable background processing
- **Progress notifications**: Track download progress in the notification shade
- **Quality selection**: Choose audio quality before downloading
- **Queue management**: Pause, resume, and cancel downloads

### Library Management
- **Songs tab**: View all downloaded and added songs
- **Artists tab**: Browse music by artist with artwork
- **Albums tab**: Organize music by album
- **Playlists tab**: Create and manage custom playlists

### Playlist Features
- **Create playlists**: Name and describe your playlists
- **Edit playlists**: Rename, update description, reorder songs
- **Smart suggestions**: Get AI-powered song recommendations based on playlist content
- **Add from search**: Add songs directly from search results

### Music Player
- **Full-screen player**: Beautiful player UI with album art and controls
- **Mini player**: Persistent mini player for quick access
- **Queue management**: View and reorder upcoming songs
- **Playback controls**: Play, pause, skip, shuffle, repeat
- **Background playback**: Continue listening with the app in background
- **Media controls**: System notification and lock screen controls

### Smart Recommendations
The recommendation engine analyzes your playlist content to suggest similar songs:
- Extracts top artists from your playlist
- Identifies common keywords and genres
- Searches for related content across sources
- Scores and ranks results by similarity

## Tech Stack

| Component | Library |
|-----------|---------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| Dependency Injection | Hilt |
| Database | Room |
| Networking | Retrofit + OkHttp |
| Audio Playback | Media3 (ExoPlayer) |
| Image Loading | Coil |
| Background Tasks | WorkManager |
| Async | Kotlin Coroutines + Flow |

## Project Structure

```
app/src/main/java/com/quezic/
├── data/
│   ├── local/          # Room database, entities, DAOs
│   ├── remote/         # Music extractor service
│   └── repository/     # Repository implementations
├── di/                 # Hilt dependency injection modules
├── domain/
│   ├── model/          # Domain models (Song, Playlist, etc.)
│   ├── recommendation/ # Smart recommendation engine
│   └── repository/     # Repository interfaces
├── download/           # Download manager and workers
├── player/             # ExoPlayer service and controller
└── ui/
    ├── components/     # Reusable UI components
    ├── navigation/     # Navigation graph
    ├── screens/        # Screen composables
    ├── theme/          # Material 3 theme
    └── viewmodel/      # ViewModels
```

## Setup

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 34

### Build
1. Clone the repository
2. Open in Android Studio
3. Sync Gradle dependencies
4. Run on device or emulator (API 26+)

```bash
./gradlew assembleDebug
```

## Architecture

The app follows Clean Architecture with MVVM:

```
UI Layer (Compose) 
    ↓ 
ViewModel Layer 
    ↓ 
Domain Layer (Use Cases) 
    ↓ 
Data Layer (Repositories) 
    ↓ 
Data Sources (Room, Network)
```

### Key Design Decisions

1. **Offline-first**: All music data is stored locally in Room database
2. **Background playback**: Media3 service handles playback independently
3. **Reactive UI**: Flows and StateFlow for reactive data updates
4. **Modular DI**: Hilt modules for clean dependency management

## Music Sources

The app uses **NewPipe Extractor** to extract audio from multiple platforms - no API keys required:

- **YouTube**: Music videos, official audio, lyrics videos, live performances
- **SoundCloud**: Tracks, remixes, emerging artists, DJ mixes

### How NewPipe Extractor Works

NewPipe Extractor is the same library used by the popular [NewPipe](https://newpipe.net/) Android app. It:
- Parses web pages to extract video/audio information
- Requires no API keys or authentication
- Extracts direct audio stream URLs for playback
- Supports search, related videos, and channel browsing

The extractor is initialized on app startup and handles all music discovery and streaming.

## Permissions

The app requires the following permissions:
- `INTERNET` - Network access for search and streaming
- `READ_MEDIA_AUDIO` - Access downloaded music files
- `FOREGROUND_SERVICE` - Background playback and downloads
- `POST_NOTIFICATIONS` - Download and playback notifications
- `WAKE_LOCK` - Keep device awake during playback
- `BLUETOOTH_CONNECT` - Bluetooth audio device support

## Contributing

Contributions are welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## License

This project is for educational purposes. Please respect copyright laws when downloading and using music content.

## Acknowledgments

- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Media3/ExoPlayer](https://developer.android.com/guide/topics/media/exoplayer)
- [Material Design 3](https://m3.material.io/)
- [NewPipe](https://newpipe.net/) - Inspiration for multi-source approach
