# LearnArm

**Persian → Armenian language-learning Android app** with backend TTS/STT and spaced-repetition.

## Features

### V1 — Core Learning
- **Alphabet** (39 letters) — Tap cards for pronunciation, take quizzes
- **Phrases** (50+ greetings & taxi phrases) — Categorized, searchable, audio playback
- **Pronunciation Practice** — Record voice, backend scores with Levenshtein matching
- **Linear Lessons** — Backend-driven, 5 progressive lessons (112 steps total)

### Content
1. Alphabet Pt1–3 (13 + 13 + 13 letters, with quizzes)
2. Greetings (12 phrases, quizzes + practice)
3. Taxi (contextual phrases for urban mobility)

## Tech Stack

### Android Client
- **Kotlin** + Jetpack Compose (Material3)
- **Room** database + **DataStore** (key-value)
- **Hilt** dependency injection
- **OkHttp** (HTTP client)
- **MediaPlayer** (audio playback)
- **MediaRecorder** (voice recording, AAC 16 kHz)

### Backend (FastAPI)
- **Piper TTS** — Armenian voice (hy_AM-gor-medium)
- **faster-whisper** — Speech-to-text (small model, int8)
- **Levenshtein** — Pronunciation scoring
- **Docker** + Cloudflare tunnel for public access

### Database (Room)
```
LetterEntity       — 39 rows
PhraseEntity       — 57 rows
LessonEntity       — 5 lessons
LessonStepEntity   — 112 steps
ReviewEntity       — SRS tracking (Phase 7)
```

## Architecture

```
SvelteKit Frontend (Mobile)
        ↓
   Compose UI
        ↓
Room Database ← ← ← ← ← ← ← ← ← ← ← Lesson Syncer (ContentVersionStore)
        ↓                                   ↓
  Repositories ────────────────→ FastAPI Backend
  (Letter, Phrase, Lesson)        /health
                                  /content/version
                                  /content/lessons
                                  /tts
                                  /stt (Whisper)
                                  /pronunciation-score
```

## Modules

```
:app                    — Entry point, Hilt setup
:core:database          — Room entities, DAOs
:core:data              — Repositories, API clients, sync
:core:audio             — TTS player, caching
:core:designsystem      — Theme, colors, typography
:feature:home           — Home screen (letter grid)
:feature:quiz           — Multi-choice quizzes
:feature:phrases        — Phrase browser (4 categories)
:feature:practice       — Pronunciation recording & scoring
:feature:lessons        — Lesson list + lesson runner
:feature:reviews        — SRS review screen (Phase 7)
```

## Running Locally

### Prerequisites
- Android SDK (API 24+)
- Gradle 8.1+
- FastAPI backend running (see backend setup)

### Build & Install
```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

### Configuration
Edit `gradle.properties`:
```properties
# Production (Cloudflare tunnel)
learnarm.apiBaseUrl=https://learnarm.hbvsoft.ir

# Local dev (requires: adb reverse tcp:50082 tcp:50082)
# learnarm.apiBaseUrl=http://localhost:50082

# Emulator
# learnarm.apiBaseUrl=http://10.0.2.2:50082
```

## Roadmap

- [x] **Phase 1** — Alphabet scaffold + audio
- [x] **Phase 2** — Alphabet quiz
- [x] **Phase 3** — Phrases (4 categories, TTS)
- [x] **Phase 4** — Lesson model + backend versioning
- [x] **Phase 5** — Phrases screen (tab UI)
- [x] **Phase 6** — Pronunciation practice (Whisper + scoring)
- [x] **Phase 8** — Lesson runner (step sequencing, quizzes, practice)
- [ ] **Phase 7** — SRS + daily streak
- [ ] **Phase 9** — Admin backend (manage lessons)
- [ ] **Phase 10** — Short stories + vocab games

## Backend API Contract

### `GET /health`
```json
{
  "status": "ok",
  "models": {
    "tts": "piper:hy_AM-gor-medium",
    "stt": "faster-whisper:small:int8"
  }
}
```

### `GET /content/version`
```json
{ "version": 1, "updated_at": "2026-06-06T00:00:00Z" }
```

### `POST /tts`
```
Body: { "text": "բարև", "voice": "hy_AM-gor-medium" }
Returns: audio/mpeg (MP3 bytes)
```

### `POST /pronunciation-score`
```
Multipart: audio (m4a), target (Armenian text)
Returns: { "score": 0.85, "recognized": "բարեվ", "feedback": "خوب" }
```

## Notes

- **No authentication** — V1 is stateless (Cloudflare gate + IP restrictions recommended)
- **Audio caching** — Client caches MP3s at `cacheDir/letter_audio/<sha256>.mp3`
- **Content versioning** — Client polls `/content/version` and syncs if newer
- **Persian text** — All UI labels, feedback, errors in Persian for accessibility

## License

Personal project. All rights reserved.

---

Made with ❤️ for learning Armenian.
