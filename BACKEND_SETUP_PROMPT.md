# LearnArm Backend — Setup Request

## Context

A different Claude session and I are building **LearnArm**, a Persian→Armenian
Android language-learning app. The Android client is local-only (Room DB,
bundled content) but needs two backend capabilities that don't work well
on-device:

1. **Armenian TTS** — Android's `TextToSpeech` doesn't support Armenian on
   most devices. We need a server that converts Armenian text to MP3 so the
   client can play letter/word pronunciations.
2. **Pronunciation scoring** — user speaks an Armenian word; backend
   transcribes via Whisper and compares to the target, returning 0–100 % score.

V1 is stateless. No accounts, no auth — Cloudflare tunnel access is the gate.

## Your environment

The laptop already runs a `docker-compose.yml` stack with 2× WordPress + 1×
MariaDB, plus a Cloudflare tunnel that can route a new subdomain to a local
port. You may add services to the existing compose file or create a sibling
file — your call. **Do not touch the WordPress / MariaDB config.**

## What to build

A new service `learnarm-api` (FastAPI, Python 3.11+) exposing:

### `GET /health`

```json
{ "status": "ok", "models": { "tts": "...", "stt": "..." } }
```

### `POST /tts`

Body: `{ "text": "բարև", "voice": "hy-default" }`
Returns: `audio/mpeg` MP3 bytes.

- Use **Piper TTS** with an Armenian voice (search `rhasspy/piper-voices`
  on HuggingFace; `hy_AM-armtts2-medium` if available, otherwise any usable
  Armenian Piper voice — document the choice).
- Pre-download the voice model in the Dockerfile so first request is fast.
- Cache by SHA-256 of `(text, voice)` to a volume-mounted `audio_cache/`.

### `POST /stt`

multipart/form-data: `audio` file + optional `language=hy` form field.
Returns: `{ "text": "...", "language": "hy", "duration_ms": N }`.

- Use **faster-whisper**, model `small` (downgrade to `base` if RAM is tight).
- Force `language="hy"` when provided.

### `POST /pronunciation-score`

multipart/form-data: `audio` + `target` form field (Armenian text).
Returns:

```json
{
  "target": "բարև",
  "recognized": "բարեվ",
  "score": 0.83,
  "feedback": "نزدیک"
}
```

- Transcribe with Whisper, normalize both strings (lowercase, strip
  whitespace, keep only Armenian letters), compute Levenshtein distance,
  score = `1 - dist / max(len(target), len(recognized))`.
- Map score to Persian feedback bands:
  - `>= 0.95` → `"عالی"`
  - `>= 0.80` → `"خوب"`
  - `>= 0.60` → `"نزدیک"`
  - `<  0.60` → `"دوباره"`

## Tech defaults (change with reason if you must)

- FastAPI + uvicorn, single container
- faster-whisper on CPU (use GPU if the laptop has one)
- Piper TTS via Python binding or subprocess
- Volume `./learnarm-api/audio_cache:/app/audio_cache`
- **Enable CORS** for `*` (mobile app calls directly; Cloudflare gates access)
- Expose internal port 8000

## Integration

- Add `learnarm-api` to the existing compose network so any existing reverse
  proxy can reach it. If there's no reverse proxy, just expose 8000 and ask
  the user to point the Cloudflare tunnel at `localhost:8000`.
- Ask the user which subdomain they want (e.g. `learnarm-api.<their-domain>`).

## Out of scope for V1

- Auth / API keys (Cloudflare access is the gate)
- Rate limiting
- User accounts / database
- HTTPS at app level (Cloudflare handles it)
- Streaming responses

## Deliverables

When done, report:

1. The public HTTPS URL where the API is reachable.
2. A working `curl` example for each endpoint (paste real output).
3. Latency for `POST /tts` cold vs. cached, and `POST /stt` for a 3-second clip.
4. Container memory + disk footprint.
5. A one-line summary of any non-default decisions you made.

Be autonomous — make judgment calls and document them rather than asking the
user for every detail. The user is hosting this on their own laptop and just
wants it running.
