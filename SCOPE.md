# LearnArm — Content & Feature Scope

**Goal:** Persian→Armenian learning app for beginners; complete **V1 curriculum** supporting **4 language skills** (reading, listening, writing, speaking).

---

## Content Size (V1 Final)

| Category | Words | Examples |
|----------|-------|----------|
| Alphabet | 39 | ա, բ, գ, ... (single letters) |
| **Vocabulary (organized in themes)** | 1000–1200 | See below |
| **Phrases** | 200–300 | Greetings, courtesy, questions, city |
| **Stories** | 10–15 | Short Armenian narratives (50–200 words each) |
| **Total unique content** | ~1400–1700 | Across all skills |

---

## Vocabulary Themes (1000–1200 words)

### Essential Categories
1. **Family & Relations** (50 words)
   - پدر/مادر, برادر/خواهر, شوهر/همسر, پسر/دختر, نوه, عموی, ...

2. **Body & Health** (80 words)
   - سر, چشم, دست, پا, دندان, بیماری, دستور العمل, ...

3. **Food & Drink** (100 words)
   - نان, آب, گوشت, سبزی, میوه, شیر, نوشیدنی, رستوران, ...

4. **House & Home** (100 words)
   - خانه, اتاق, تخت, میز, صندلی, آشپزخانه, حمام, ...

5. **Clothing** (60 words)
   - لباس, کفش, کلاه, جوراب, شلوار, پیراهن, دامن, ...

6. **Colors** (12 words)
   - سرخ, آبی, سبز, زرد, سیاه, سفید, نارنجی, بنفش, ...

7. **Numbers & Time** (50 words)
   - یک تا صد, روز, ماه, هفته, ساعت, دقیقه, امروز, فردا, ...

8. **Nature & Animals** (80 words)
   - درخت, گل, آب, ماهی, پرنده, سگ, گربه, اسب, ...

9. **City & Transport** (100 words)
   - خیابان, پل, ایستگاه, اتوبوس, ماشین, قطار, تاکسی, ...

10. **Work & School** (80 words)
    - معلم, دانش‌آموز, کتاب, قلم, دفتر, کلاس, امتحان, ...

11. **Actions & Verbs** (150 words)
    - رفتن, آمدن, خوردن, نوشیدن, خواب رفتن, بیدار شدن, نشستن, ...

12. **Adjectives & Descriptions** (100 words)
    - بزرگ, کوچک, قدیمی, نو, خوب, بد, سفت, نرم, ...

13. **Seasons & Weather** (40 words)
    - بهار, تابستان, پاییز, زمستان, باران, برف, آفتاب, ...

14. **Common Phrases & Expressions** (200 words)
    - Greetings (سلام, صبح بخیر, خدا حافظ)
    - Courtesy (لطفاً, متشکرم, معاف کنید)
    - Questions (چطوری؟ کجا؟ کی؟ چقدر؟)
    - Answers + small talk

15. **Miscellaneous** (100 words)
    - Money, health terms, technology basics

---

## 4 Language Skills (Implementation)

### 1. **Reading (خواندن)**
**User sees Armenian text + Persian translation; recognizes meaning.**

- **Vocabulary cards:** Armenian word + Persian + icon (each word has visual)
- **Phrases:** Full Armenian phrase + Persian translation
- **Stories:** Full Armenian story; tap word → Persian pop-up
- **Quiz types:** Multiple-choice word matching (Armenian → Persian options)

**UIs:** VocabCard, PhraseCard, StoryReaderScreen, QuizScreen

---

### 2. **Listening (شنیدن)**
**User hears Armenian audio; recognizes word/phrase.**

- **Vocabulary:** Tap word → audio plays (MP3 via `/tts`, cached)
- **Phrases:** Tap phrase → audio plays
- **Stories:** Tap story → narrate full text (optional TTS)
- **Quiz types:** Hear audio clip, pick correct Persian meaning from 4 options

**UIs:** Button with speaker icon; audio auto-plays on card if configured

---

### 3. **Writing (نوشتن)**
**User sees Persian + writes Armenian (spell it out; currently on Android keyboard).**

- **Spelling test:** Show Persian word, user types Armenian spelling
- **Dictation:** Hear Armenian audio, user types Armenian text
- **Quiz types:** Fill-in-the-blank Armenian sentences (cloze test)

**UIs:** TextInput with Armenian keyboard; validation by exact match or edit distance ≤ 1–2

---

### 4. **Speaking (صحبت کردن)**
**User records voice; backend scores vs. target pronunciation.**

- **Pronunciation practice:** Show Armenian word/phrase, user records voice
- **Backend scoring:** Whisper transcribe + Levenshtein → 0–100% score + Persian feedback (عالی/خوب/نزدیک/دوباره)
- **Confidence threshold:** Accept ≥ 80% score to advance

**UIs:** PracticeScreen (mic button + recording indicator + score card)

---

## Content Organization (Backend-Driven)

All content delivered as **versioned JSON packs** from FastAPI backend. Client caches in Room, syncs on version change.

### `/content/lessons` — Linear progression
```json
{
  "version": 1,
  "lessons": [
    {
      "id": "lesson_1",
      "title": "الفبا - قسمت ۱",
      "steps": [
        { "type": "letter", "content": { "id": 1, "name": "այբ", "icon": "..." } },
        { "type": "letter", "content": { "id": 2, "name": "բե", "icon": "..." } },
        ...
        { "type": "quiz", "content": { "count": 10, "type": "letter_sound" } }
      ]
    },
    ...
  ]
}
```

### `/content/vocabulary` — All 1000+ words (indexed by theme)
```json
{
  "version": 1,
  "themes": [
    {
      "id": "family",
      "title": "خانواده",
      "words": [
        {
          "hy": "հայր",
          "fa": "پدر",
          "icon": "👨",
          "transliteration": "hayr",
          "pronunciation": "hair"
        },
        ...
      ]
    },
    ...
  ]
}
```

### `/content/phrases` — 200+ grouped phrases
```json
{
  "version": 1,
  "categories": [
    {
      "id": "greeting",
      "title": "احوال‌پرسی",
      "phrases": [
        {
          "hy": "Բարև",
          "fa": "سلام",
          "transliteration": "barev",
          "context": "Casual hello"
        },
        ...
      ]
    }
  ]
}
```

### `/content/stories` — 10–15 short stories (300–1000 words each)
```json
{
  "version": 1,
  "stories": [
    {
      "id": "story_1",
      "title": "The Little Girl",
      "text": "Մի տեղ կար մի փոքր աղջիկ...",
      "paragraphs": [
        {
          "text": "...",
          "words": [
            { "hy": "Մի", "fa": "یکی" },
            { "hy": "տեղ", "fa": "جا" },
            ...
          ]
        }
      ]
    }
  ]
}
```

### `/content/vocab-games` — Word pair matching (by theme)
```json
{
  "version": 1,
  "games": [
    {
      "id": "game_family",
      "title": "خانواده",
      "pairs": [
        { "hy": "հայր", "fa": "پدر", "icon": "👨" },
        { "hy": "մայր", "fa": "مادر", "icon": "👩" },
        ...
      ]
    }
  ]
}
```

---

## Lesson Structure (V1 — 15 lessons, ~500 total steps)

### **Unit 1: Alphabet** (3 lessons, 15 cards each)
- Lesson 1: Letters ա–ծ (15 letters)
- Lesson 2: Letters կ–ջ (15 letters)
- Lesson 3: Letters ռ–ևույ (9 letters) + ligature և
- **Each lesson:** 15 letter cards + 1 quiz (10 questions) + optional pronunciation practice

### **Unit 2: Essential Vocabulary** (5 lessons, 200 words)
- Lesson 4: Family + body parts (50 words, 5 vocab cards + 1 quiz)
- Lesson 5: Food + house (60 words, 6 vocab cards + 1 quiz)
- Lesson 6: Clothing + nature (60 words, 6 vocab cards + 1 quiz)
- Lesson 7: Numbers + time (30 words, 3 vocab cards + 1 quiz)
- Lesson 8: Miscellaneous (stationery, money, etc.; 30 words)

### **Unit 3: Greetings & Phrases** (3 lessons, 100+ phrases)
- Lesson 9: Greetings & courtesy (30 phrases, tap → audio, 2 quizzes)
- Lesson 10: Questions & answers (35 phrases, 2 quizzes)
- Lesson 11: City survival (35 phrases: taxi, restaurant, directions; 2 quizzes + 1 practice)

### **Unit 4: Integrated Skills** (4 lessons, 100 words + 5 stories)
- Lesson 12: Story 1 (reading + vocab lookup; writing quiz; pronunciation)
- Lesson 13: Story 2 (same structure)
- Lesson 14: Story 3
- Lesson 15: Final review (vocabulary match game + pronunciation challenge)

**Total:** 15 lessons, ~500 steps, 4 language skills integrated at each level.

---

## Android Features (Phased)

### Phase 8b — Lesson Runner ✅
- LessonsListScreen (progress indicators)
- LessonRunnerScreen (sequences steps)

### Phase 9 — Vocabulary Browser
- VocabThemeListScreen (15 categories)
- VocabCardScreen (vocabulary card with icon + audio + Persian)
- Vocabulary quiz (word → meaning)

### Phase 10 — Writing Practice
- SpellingTestScreen (Persian → user types Armenian)
- ClozeTestScreen (fill-in-the-blank Armenian)
- ValidationLogic (exact match or edit distance ≤ 2)

### Phase 11 — Stories
- StoryListScreen
- StoryReaderScreen (tap word → Persian pop-up)
- Story quiz (comprehension + vocabulary)

### Phase 12 — Vocab Game
- VocabGameScreen (drag Armenian ↔ Persian pairs)
- Timer + scoring

---

## Content Authorship (Responsibility)

| Item | Who | Timeline |
|------|-----|----------|
| Vocabulary data (1000+ words, icons, transliterations) | **User** | Iterative (Google Sheets → JSON) |
| Phrases (200+) | **User** | Before phase 9 |
| Stories (10–15 x 300–1000 words) | **User** + curate from Armenian sources | Before phase 11 |
| Backend JSON serialization + validation | **AI assistant** | Each phase |
| Android UI + sync logic | **AI assistant** | Each phase |

---

## Estimated Effort

| Phase | Task | Days | Tokens |
|-------|------|------|--------|
| 8b | Lesson runner UI | 2–3 | ~800 |
| 9 | Vocab browser + quiz | 2 | ~600 |
| 10 | Writing practice + spelling | 1–2 | ~500 |
| 11 | Stories + reading UI | 2–3 | ~600 |
| 12 | Vocab game | 1–2 | ~400 |
| **Total** | | 8–12 days | ~2900 tokens |

---

## Next Action

1. **Prepare vocabulary list** (1000+ words, categorized + icons + transliterations)
   - Use Google Sheets or JSON file
   - Minimum: { "hy", "fa", "icon", "category" }
2. **Curate Armenian stories** (10–15 short texts, public domain or permissible sources)
3. **Start Phase 8b** (lesson runner UI scaffolding)
