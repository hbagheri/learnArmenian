# LearnArm — Architecture & Roadmap (Phases 7–10)

---

## Phase 7: SRS + Daily Streak

### Database Schema Addition

```kotlin
@Entity(tableName = "reviews")
data class ReviewEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val item_type: String,        // "letter" | "phrase"
  val item_id: Long,
  val due_date: Long,           // epoch millis
  val ease_factor: Float = 2.5f,
  val interval: Int = 0,        // days until next review
  val streak_count: Int = 0,    // consecutive correct
  val total_correct: Int = 0,
  val total_reviews: Int = 0,
  val last_reviewed: Long = 0,  // epoch millis
  val created_at: Long,
)

@Entity(tableName = "streak_stats")
data class StreakStatsEntity(
  @PrimaryKey val id: Int = 0,          // singleton
  val current_streak: Int = 0,
  val longest_streak: Int = 0,
  val last_review_date: Long = 0,       // date, not datetime
  val total_reviews_today: Int = 0,
)
```

### Client-Side (Android)

**New DAO methods:**
```kotlin
// ReviewDao
suspend fun getReviewsDue(now: Long): List<ReviewEntity>
suspend fun upsertReview(entity: ReviewEntity)
suspend fun getStats(): StreakStatsEntity?

// StreakStatsDao
suspend fun upsertStats(entity: StreakStatsEntity)
```

**ViewModel: `ReviewsViewModel`**
- Tracks user response (correct/incorrect)
- Updates `ReviewEntity.ease_factor` via **SM-2 algorithm** (popular SRS):
  - Correct: `ease = max(1.3, ease + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02)))`
  - Incorrect: `ease = max(1.3, ease - 0.2)`, reset `streak_count = 0`
- Increments `StreakStatsEntity.current_streak` if no reviews missed today
- Screen: Shows "🔥 5-day streak" badge on home; "ادامه‌ی مرور" button if reviews due

**HomeScreen Update**
- If `reviews due > 0`: Show card "مرور امروز (N)" with due count
- Tap → `ReviewsScreen` (simplified quiz-like flow)

### Backend (No changes needed for V1)
SRS logic is fully client-side. No server state.

---

## Phase 8: Lessons UI & Runner

### Architecture: Lesson Runner (Step Sequencing)

**Problem solved:** Currently, lessons exist in Room (`LessonEntity` + `LessonStepEntity`) but no UI sequences them.

**Solution:** Two new screens:

#### **LessonsListScreen**
- LinearLayout of lesson cards
- Each card shows:
  - Lesson title + description
  - Progress bar (steps completed / total)
  - Status badge: "شروع کنید" (not started) | "ادامه دهید" (in progress) | "تکمیل شد" (done)
  - Locked indicator (gray if prerequisite incomplete)
- Tap → `LessonRunnerScreen`

#### **LessonRunnerScreen**
- **State machine:**
  1. Load lesson steps from DB
  2. Show step 0 (letter/phrase card, quiz, or practice)
  3. User completes step → validate (e.g., quiz must pass before advancing)
  4. Show step N+1
  5. At end: "درس تکمیل شد" screen + back button

- **Step rendering** — reuse existing components:
  ```kotlin
  when (step.type) {
    "letter" -> LetterCard(letter)
    "phrase" -> PhraseCard(phrase)
    "quiz" -> QuizScreen(questions)
    "practice" -> PracticeScreen(phrase)
  }
  ```

**ViewModel: `LessonRunnerViewModel`**
```kotlin
data class RunnerState(
  val lesson: LessonEntity,
  val steps: List<LessonStepEntity>,
  val currentStepIndex: Int = 0,
  val completedStepIds: Set<String> = emptySet(),
  val isLessonComplete: Boolean = false,
)
```

### Database Schema Addition
```kotlin
@Entity(tableName = "lesson_progress")
data class LessonProgressEntity(
  @PrimaryKey val lesson_id: String,
  val completed_step_ids: String = "", // JSON array or comma-separated
  val completed_at: Long? = null,
  val last_accessed: Long,
)
```

### HomeScreen Update
- Primary card: "ادامه‌ی درس" linking to `LessonsListScreen`
- Original buttons (alphabet, phrases, practice) now secondary

---

## Phase 9: Short Stories (Reading Practice)

### Content Model

**Story file format** (JSON in backend `/content/stories`):
```json
{
  "id": "story_1",
  "title": "The Little Girl",
  "title_hy": "Փոքր աղջիկ",
  "difficulty": "beginner",
  "duration_mins": 5,
  "text": "Մի տեղ կար մի փոքր աղջիկ...",
  "paragraphs": [
    {
      "id": "p1",
      "text": "Մի տեղ կար մի փոքր աղջիկ:",
      "words": [
        { "hy": "Մի", "fa": "یکی" },
        { "hy": "տեղ", "fa": "جا" },
        ...
      ]
    }
  ]
}
```

### Android UI: **StoriesListScreen + StoryReaderScreen**

**StoriesListScreen**
- Horizontal scroll grid of story cards
- Each card: title, duration, difficulty badge

**StoryReaderScreen**
- Full story text in readable font (Armenian-friendly: Noto Sans Armenian or fallback)
- **Tap word** → Persian translation pop-up (no animation; instant)
- Optional: **Audio icon** (speaker) near title → narrate full story (via `/tts`)
- "Next story" button at bottom

### Database Schema Addition
```kotlin
@Entity(tableName = "stories")
data class StoryEntity(
  @PrimaryKey val id: String,
  val title: String,
  val title_hy: String,
  val difficulty: String,
  val duration_mins: Int,
  val full_text: String,
  val fetched_at: Long,
  val version: Int,
)

@Entity(tableName = "story_words")
data class StoryWordEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val story_id: String,
  val word_hy: String,
  val word_fa: String,
)

@Entity(tableName = "story_progress")
data class StoryProgressEntity(
  @PrimaryKey val story_id: String,
  val last_read_at: Long,
  val completed_at: Long? = null,
)
```

### Backend Endpoint: `GET /content/stories`
```json
{
  "version": 1,
  "stories": [
    { "id": "story_1", "title": "...", ... },
    ...
  ]
}
```

### Content Pipeline
1. **Source:** Armenian websites or public domain (azdak.org, armenianinstitute.org)
2. **Markup:** Manual or script-based:
   - Tokenize by word boundaries
   - Map each Armenian word to Persian translation (via dictionary API or manual)
3. **Storage:** Embed in JSON; client stores in Room

---

## Phase 10: Vocab Match Game

### Game Mechanics

**Game board:**
- Two columns: left = Armenian, right = Persian
- Each word as a draggable tile
- **Drag Armenian word → snap to matching Persian (or vice versa)**
- **Correct pair** → both tiles highlight green + disappear
- **Incorrect pair** → shake + reset
- **Timer** (optional, configurable): 60s or unlimited
- **Score:** pairs matched / total pairs

### Content Model
```json
{
  "id": "vocab_game_1",
  "title": "خانواده",
  "category": "family",
  "pairs": [
    { "hy": "հայր", "fa": "پدر" },
    { "hy": "մայր", "fa": "مادر" },
    ...
  ]
}
```

### Android UI: **VocabGameScreen**

```kotlin
data class GameState(
  val pairs: List<VocabPair>,
  val matched: Set<String> = emptySet(),
  val elapsed_seconds: Int = 0,
  val max_seconds: Int? = null,  // null = unlimited
  val dragging: String? = null,  // currently dragging tile ID
)
```

**Implementation:**
- Use Compose's drag-and-drop (gesture detection + offset animation)
- Left column: shuffle Armenian words
- Right column: shuffle Persian translations
- User drags left tile → snaps to right slot if correct, shakes if wrong

### Backend Endpoint: `GET /content/vocab-games`
```json
{
  "version": 1,
  "games": [
    {
      "id": "vocab_game_1",
      "title": "خانواده",
      "category": "family",
      "pairs": [...]
    }
  ]
}
```

### Content Source
- Extract word pairs from existing lessons (phrases + translations)
- Organize by category (family, food, actions, etc.)
- Add 2–3 extra pairs per game for variety

---

## Content Sync & Versioning Strategy

All content (lessons, stories, vocab games) is **versioned**:

1. **Backend:** Each endpoint returns `{ "version": N, "items": [...] }`
2. **Client:** `DataStore` tracks `content_version_lessons`, `content_version_stories`, etc.
3. **Sync logic** (in `:core:data/sync/`):
   ```kotlin
   suspend fun syncIfNeeded(contentType: String) {
     val localVersion = dataStore.getVersion(contentType)
     val remoteVersion = apiClient.getVersion(contentType)
     if (remoteVersion > localVersion) {
       val content = apiClient.fetchContent(contentType)
       db.insert(content)
       dataStore.setVersion(contentType, remoteVersion)
     }
   }
   ```
4. **Triggers:**
   - App startup (throttled to 1× per day)
   - Manual refresh button on each screen
   - Background sync (WorkManager, V2+ if needed)

---

## Token Efficiency: Content Pipeline Design

**Why backend-driven?**
- ✅ Content updates without app release
- ✅ Lessons/stories/games are **data**, not code → small JSON payloads
- ✅ Claude doesn't need to touch the app after initial scaffolding
- ❌ But: Need a separate backend process to curate / seed content

**Who populates `/content/lessons`, `/content/stories`, etc.?**
- **Option A:** Manual JSON files in backend; Python script to validate + serve
- **Option B:** Headless CMS (e.g., Strapi) — too heavy for V1
- **Recommendation:** Option A + simple CLI to validate

**How to avoid token explosion?**
1. Content is authored **once**, stored in JSON
2. Claude writes the backend endpoint logic (1× per phase)
3. Content seed is edited by user or a text tool (not Claude)
4. Claude only touches code, not content

---

## Summary: Scope & Effort

| Phase | Tasks | Effort | Token Cost |
|-------|-------|--------|-----------|
| 7 | SRS algorithm, review screen, streak badge | 1–2 days | ~500 tokens (code + logic) |
| 8 | Lesson list + runner screens; progress tracking | 2–3 days | ~800 tokens (UI + state mgmt) |
| 9 | Story UI + content pipeline | 2 days | ~600 tokens (UI); 200 content |
| 10 | Vocab game drag-and-drop + board logic | 1–2 days | ~400 tokens (UI + gesture) |

**Total (Phases 7–10):** ~6–8 days; ~2500 tokens (code only; content is user's responsibility).

---

## Backend Content Files (User Responsibility)

Once the Android UI is scaffolded, populate these JSON files in the FastAPI backend:

1. `data/lessons.json` — 5 V1 lessons (alphabet × 3, greetings, taxi)
2. `data/stories.json` — 3–5 short Armenian stories
3. `data/vocab_games.json` — 4–5 word-pair games

**Automation:** Simple Python script to validate structure, e.g.:
```bash
python backend/validate_content.py data/*.json
```

---

## Nice-to-haves (Post-V1)

- **Offline mode:** Lessons + stories cached locally (already happens with Room)
- **User accounts:** Track progress across devices (post-V1; needs backend DB + auth)
- **Leaderboard:** Daily/weekly streak top users (post-V1)
- **AI-generated stories:** Use Claude API to generate short stories on demand
- **Spaced-rep refinement:** Use SuperMemo-3 instead of SM-2
- **Voice recording review:** Playback user's recording vs. target pronunciation
