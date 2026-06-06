# LearnArm — CLAUDE.md

Persian → Armenian language-learning app. **Android client** (modular Compose + Room) + **FastAPI backend** (TTS, STT, pronunciation scoring). Content is backend-driven and structured in reusable lessons.

---

## Project Goals (V1)

| Component | Status | Notes |
|-----------|--------|-------|
| **Alphabet** | ✅ Done | 39 letters + ligatures; tap → audio playback; quiz (10 Qs, 4-choice) |
| **Phrases (Greetings)** | ✅ Done | 50 Eastern Armenian phrases across 4 tabs; tap → TTS |
| **Practice (Pronunciation)** | 🟡 Pending E2E | Record voice → Whisper + Levenshtein score → Persian feedback (عالی/خوب/نزدیک/دوباره) |
| **Lessons (Linear backend-driven)** | 🟡 In progress | 5 V1 lessons (alphabet pt1–3, greetings, taxi); 112 steps total; versioned JSON from `/content/lessons` |
| **SRS + Streak** | ⬜ Not started | Lightweight spaced-rep; daily-streak counter |
| **Short Stories** | ⬜ Not started | Armenian text reading + vocab lookup |
| **Vocab Match Game** | ⬜ Not started | Drag Persian ↔ Armenian word pairs; timer optional |

---

## Tech Stack

### Android Client
- **Kotlin** + Jetpack Compose
- **Room** (database) + **DataStore** (lightweight kv)
- **Hilt** (DI)
- **OkHttp** (HTTP)
- **MediaPlayer** (audio playback)
- **MediaRecorder** (voice record, AAC m4a @ 16 kHz)

### Backend
- **FastAPI** (Python 3.11+)
- **Piper TTS** (hy_AM-gor-medium Armenian voice)
- **faster-whisper** (small model, int8, CPU)
- **Levenshtein** (pronunciation scoring)
- Docker + docker-compose + Cloudflare tunnel

### Database (Room, on-device)
- `LetterEntity` — alphabet (39 rows)
- `PhraseEntity` — greetings + taxi (57 rows)
- `LessonEntity` — lesson metadata (title, description, index)
- `LessonStepEntity` — lesson steps (card type, content id, order)
- `ReviewEntity` — SRS tracking (due_date, ease, streak) [Phase 7]

---

## Lesson & Content Architecture

**Principle:** Lessons are linear (Duolingo-style), backend-versioned, step-by-step.

Each lesson is a **sequence of steps**:
1. **Letter card** — show Armenian letter + transliteration + Persian name; tap → audio
2. **Phrase card** — show Armenian phrase + transliteration + Persian translation; tap → audio
3. **Quiz** — multiple-choice (4 options, 1 correct)
4. **Practice** — record voice; backend scores pronunciation

**V1 Lessons** (in `/content/lessons` JSON):
- `lesson_1` — Alphabet Pt1 (letters ա–ծ, ~15 cards + 1 quiz)
- `lesson_2` — Alphabet Pt2 (letters կ–ջ, ~15 cards + 1 quiz)
- `lesson_3` — Alphabet Pt3 (letters ռ–ևույ, ~9 letters + 1 quiz)
- `lesson_4` — Greetings (12 phrases across greeting/courtesy/question, 15 cards + 2 quizzes)
- `lesson_5` — Taxi (contextual phrases: destination, fare, thanks; 10 cards + 1 practice)

Total: **5 lessons, 112 steps, 7 quizzes + 1 practice session.**

---

## Backend API Contract

### `GET /health`
```json
{ "status": "ok", "models": { "tts": "piper:hy_AM-gor-medium", "stt": "faster-whisper:small:int8" } }
```

### `GET /content/version`
```json
{ "version": 1, "updated_at": "2026-06-06T00:00:00Z" }
```

### `GET /content/lessons`
```json
{
  "version": 1,
  "lessons": [
    {
      "id": "lesson_1",
      "title": "الفبا - قسمت ۱",
      "description": "حروف ա تا ծ",
      "index": 0,
      "steps": [
        {
          "id": "step_1_1",
          "type": "letter",
          "content": { "id": 1, "name": "այբ" },
          "order": 0
        },
        {
          "id": "step_1_2",
          "type": "quiz",
          "content": { "letter_ids": [1, 2, 3, 4, 5, ...], "count": 10 },
          "order": 14
        }
      ]
    }
  ]
}
```

### `POST /tts`
Body: `{ "text": "բարև", "voice": "hy_AM-gor-medium" }`  
Returns: `audio/mpeg` MP3 bytes

### `POST /pronunciation-score`
Multipart: `audio` (m4a) + `target` (Armenian text)  
Returns: `{ "score": 0.85, "recognized": "բարեվ", "feedback": "خوب" }`

---

## Android Modules

- **`:app`** — entry point, bootstraps app + seeders + sync
- **`:core:database`** — Room entities, DAOs, database
- **`:core:data`** — repositories, API clients, syncing, content versioning
- **`:core:audio`** — TTS audio player + caching
- **`:core:designsystem`** — theme, colors, typography
- **`:feature:home`** — home screen (3 buttons: alphabet, phrases, practice; "ادامه‌ی درس" overlay when lessons exist)
- **`:feature:quiz`** — quiz runner; reusable across lessons
- **`:feature:phrases`** — 4-tab phrase browser (greeting/courtesy/question/city)
- **`:feature:practice`** — pronunciation practice (record → score)
- **`:feature:lessons`** [TBD] — lesson list + lesson runner (sequences steps; reuses letter/phrase/quiz/practice UIs)

---

## File Locations (key)

- `app/src/main/assets/seed/alphabet.json` — 39 letters
- `app/src/main/assets/seed/phrases.json` — 57 phrases
- `core/database/src/main/java/com/learnarm/core/database/` — entities, DAOs
- `core/data/src/main/java/com/learnarm/core/data/` — repos, API clients, seeders
- `core/audio/src/main/java/com/learnarm/core/audio/` — TTS pipeline
- `feature/home/src/main/java/com/learnarm/feature/home/` — HomeScreen + HomeViewModel
- `feature/quiz/src/main/java/com/learnarm/feature/quiz/` — QuizScreen + QuizViewModel
- `feature/phrases/src/main/java/com/learnarm/feature/phrases/` — PhrasesScreen
- `feature/practice/src/main/java/com/learnarm/feature/practice/` — PracticeScreen + pronunciation logic

---

## Phased Rollout

### Phase 1–5 ✅ (Done)
- Alphabet (letter cards, audio, quiz)
- Phrases (browsable tabs, audio)
- Backend TTS + Whisper endpoints live

### Phase 6 (Pending device E2E test)
- Pronunciation practice screen fully wired

### Phase 7 ⬜
- **SRS + Streak**
  - `ReviewEntity` tracks `due_date`, `ease`, `streak_count`
  - Daily counter persists (DataStore)
  - Review cards appear in home or lesson flow

### Phase 8 ⬜
- **Lessons UI**
  - `LessonsListScreen` — linear list with current/locked indicators
  - `LessonRunnerScreen` — sequences steps; reuses existing UIs (letter, phrase, quiz, practice)
  - Home gets primary "ادامه‌ی درس" card

### Phase 9 ⬜
- **Short Stories** (Armenian reading + vocab lookup)
  - Curate or scrape from `azdak.org`, `armenianinstitute.org`
  - Simple markup: tap word → Persian translation + audio
  - Backend `/content/stories` endpoint

### Phase 10 ⬜
- **Vocab Match Game** (drag Persian ↔ Armenian pairs)
  - Timer optional
  - Pull word pairs from lessons + extras
  - Backend `/content/vocab-games` endpoint

---

## Development Workflow

```bash
cd /home/hassan/projects/personal/mobileApps/learnArmenian

# Build
./gradlew :app:assembleDebug

# Install on device
./gradlew :app:installDebug

# Tests (unit)
./gradlew test

# Backend (if working locally)
cd ~/projects/personal/localWebHosting/fastapi/learnArm
python -m uvicorn app.main:app --reload
```

### gradle.properties Overrides
```properties
learnarm.apiBaseUrl=https://learnarm.hbvsoft.ir          # prod
# learnarm.apiBaseUrl=http://localhost:50082             # local dev (with adb reverse)
```

---

## Conventions & Notes

- **Persian text** everywhere (UI, feedback, error messages)
- **Transliteration** (Latin) is optional on cards; mainly for learner clarity
- **Audio cache** is on-device (cache dir); no explicit cleanup needed
- **Lessons are immutable** during a run (no editing mid-way)
- **Content versioning** — client fetches `/content/version`, compares local `ContentVersionStore`, syncs if newer
- **No auth** — V1 is stateless; Cloudflare tunnel gates access

---

## Next Steps

1. **Phase 6 sign-off:** Device E2E test for pronunciation (record → score → feedback)
2. **Phase 7 design:** SRS algorithm + daily streak UI
3. **Phase 8:** Lesson runner screen architecture
4. **Phase 9–10:** Story + game content pipeline
