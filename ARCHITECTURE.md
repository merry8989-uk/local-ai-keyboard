# Local AI Keyboard — Architecture

An offline, privacy-first Android keyboard (IME) with a fully on-device LLM for
AI writing, grammar/spelling correction, and English + Hinglish input.

Target: Android 10+ (API 29+), 6 GB+ RAM, arm64-v8a.

---

## 1. Goals and non-goals

**Goals**
- A working soft keyboard (InputMethodService) that can be set as the system IME.
- 100% offline. No network permission. No telemetry. Text never leaves the device.
- Local spell-check and lightweight grammar correction as you type.
- Local LLM actions: fix text, rephrase, continue, compose email / essay / message.
- First-class **Hinglish** (Romanised Hindi) support alongside English.

**Non-goals (v1)**
- Full Indic script (Devanagari) input — planned for v2 via transliteration.
- Cloud fallback. If the model is not downloaded, AI actions are disabled, not degraded to a server.
- Cross-platform (iOS) sharing.

---

## 2. High-level architecture

```
┌──────────────────────────────────────────────────────────────┐
│                         Android IME                          │
│                                                              │
│  InputMethodService ──► KeyboardView (keys + gestures)        │
│        │                    │                                 │
│        │                    ▼                                 │
│        │              KeyboardLayout / KeyDefs                 │
│        ▼                                                      │
│  InputConnection ◄─── EditorBridge (commit/compose text)      │
│        ▲                                                      │
│        │                                                      │
│  ┌─────┴───────────┐   ┌──────────────┐   ┌────────────────┐ │
│  │  SuggestionBar  │   │  SpellEngine │   │  AiController  │ │
│  │ (candidates +   │◄──│ (SymSpell +  │   │ (actions,      │ │
│  │  AI action chips)│  │  grammar)    │   │  streaming)    │ │
│  └─────────────────┘   └──────┬───────┘   └───────┬────────┘ │
│                               │                   │          │
│                        ┌──────▼──────┐     ┌──────▼───────┐  │
│                        │  Dictionaries│    │  LlmEngine   │  │
│                        │  (EN, Hinglish)│   │  (interface) │  │
│                        └─────────────┘     └──────┬───────┘  │
│                                                   │          │
│                                     ┌─────────────▼────────┐ │
│                                     │ MediaPipe LLM Engine │ │
│                                     │ (Gemma, .task file)  │ │
│                                     └──────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

Everything runs in-process on the phone. The only optional network use is the
**one-time model download** in Settings (user-initiated), after which the app
works with no connectivity.

---

## 3. Module / package breakdown

Single Gradle `:app` module for v1 (simple to build), organised by package so it
can be split into Gradle modules later.

| Package | Responsibility |
|---|---|
| `ime` | `InputMethodService`, keyboard view, layouts, suggestion bar, editor bridge |
| `spell` | SymSpell, dictionary loading, edit-distance ranking, grammar rules |
| `lingua` | Hinglish lexicon, phonetic matching, (v2) Devanagari transliteration |
| `ai` | `LlmEngine` abstraction, MediaPipe backend, prompt templates, action controller |
| `settings` | Settings screen, model manager (download / verify / delete) |
| `util` | Text helpers, threading, logging |

---

## 4. On-device LLM

**Chosen backend: Google MediaPipe LLM Inference API.**
It ships as an Android AAR, uses the GPU delegate where available, and takes a
single `.task` / `.litertlm` model bundle — the least-friction path to a working
offline LLM on Android.

**Model ladder** (verified reachable, ungated, and in the `.task` format the
runtime reads, as of this writing):

| Model | File | Size | Notes |
|---|---|---|---|
| Qwen2.5 1.5B Instruct | `.task` (q8, ekv1280) | ~1.5 GB | **Default.** Ungated, instruction-tuned. |
| Qwen2.5 0.5B Instruct | `.task` (q8, ekv1280) | ~0.5 GB | Fastest option. |

Gemma 3 1B is still a good target, but its Hugging Face copy is **gated** — it
needs an access token and licence acceptance, so it is not the zero-config
default. URLs live in `settings/ModelManager.kt`.

`LlmEngine` is an interface so the backend is swappable:

```kotlin
interface LlmEngine {
    suspend fun load(modelPath: String)
    fun generate(prompt: String, maxTokens: Int = 256): Flow<String>  // streaming
    fun close()
    val isReady: Boolean
}
```

Two implementations:
- `MediaPipeLlmEngine` — the default, real implementation.
- `LlamaCppLlmEngine` — stub showing the GGUF/JNI alternative (for users who
  want Q4 quantised GGUF models and no Google runtime dependency).

**Latency budget.** A 1B int4 model on a modern mid-range SoC streams roughly
15–30 tokens/s. AI actions therefore run as *background* jobs with a streaming
result panel, never blocking the keyboard. The keyboard remains fully usable
while the model generates.

**Threading.** All model work runs on a dedicated single-thread dispatcher
(`Dispatchers.AI`) — the MediaPipe graph is not thread-safe.

---

## 5. Spell-check and grammar engine

**Spell:** SymSpell (symmetric delete) with a frequency dictionary.
- Build once at first launch from a bundled word-frequency list (top ~50k English
  words) plus the Hinglish lexicon; cache the compiled index to app storage.
- Lookup is O(1) per delete-variant — sub-millisecond for typical words.
- Max edit distance 2, prefix length 7 (standard SymSpell tuning).

**Grammar (lightweight, on-device, no LLM):** a rule pass over the current
sentence for high-frequency errors:
- sentence-initial capitalisation, lone `i` → `I`
- double spaces, space before punctuation
- `a` / `an` agreement
- repeated words ("the the")
- common Hinglish-English confusions (e.g. `muje` → `mujhe`)

Deeper rewrites are the LLM's job (the "Fix" action), not the rule engine's.

---

## 6. Hinglish support

Romanised Hindi is not covered by English dictionaries, so it gets its own layer:

- `HinglishLexicon` — curated common words (`kya`, `hai`, `nahi`, `bhai`,
  `yaar`, `accha`, `theek`, `matlab`, `kaise`, `kyun`, …) with frequencies.
- `PhoneticMatcher` — maps English-keyboard digraphs to Hindi sounds
  (`ph`→`फ`, `kh`→`ख`, `aa`→`आ`) so `kaise ho` and `kese ho` both resolve.
- Suggestion ranking blends English and Hinglish candidates by frequency and
  recency; the user's chosen language mode (EN / HI / Auto) biases the mix.
- **v2 (transliteration):** optional live conversion of the romanised buffer to
  Devanagari using the same phonetic map, shown as a candidate in the bar.

---

## 7. IME data flow (typing → suggestion → AI)

1. User taps a key. `KeyboardView` emits a key event.
2. `EditorBridge` commits simple characters directly; multi-char candidates go
   through `setComposingText` so the underline shows.
3. On every buffer change, `SpellEngine.suggest(word)` runs on a worker thread and
   posts ranked candidates to `SuggestionBar`.
4. Tapping the ✦ chip opens `AiController` actions (Fix, Rephrase, Continue,
   Email, Essay). `AiController` builds a prompt from `PromptTemplates`, feeds
   `LlmEngine`, and streams tokens into an inline result card.
5. Accepting a result replaces the selection via `EditorBridge`, respecting the
   user's undo expectation (a single `InputConnection` commit).

---

## 8. AI actions and prompt templates

| Action | Input | Output |
|---|---|---|
| Fix | current selection / sentence | spelling + grammar corrected, meaning preserved |
| Rephrase | selection | same meaning, different phrasing (tone selectable) |
| Continue | text before cursor | a natural continuation |
| Email | bullet notes | a polished email (subject + body) |
| Essay | topic / outline | structured multi-paragraph essay |
| Message | intent | short chat message, Hinglish-aware |

Templates live in `PromptTemplates.kt` as typed builders, kept short so the 1B
model stays on task. Example:

```
Fix:       "Correct the spelling and grammar of the text below. Keep the meaning
            and language (English or Hinglish). Return only the corrected text.\n\n{text}"
Rephrase:  "Rewrite the text below in a {tone} tone. Keep the meaning. Return only
            the rewrite.\n\n{text}"
Email:     "Write a professional email. Subject line first, then the body, from
            these notes:\n\n{notes}"
```

---

## 9. Privacy

- No `INTERNET` permission in the manifest for the keyboard process.
- Model download is handled by a separate, explicit user action in Settings
  (download manager), not by the IME.
- No clipboard scraping, no logging of typed text, no analytics.
- The IME is configured `android:permission="android.permission.BIND_INPUT_METHOD"`
  so only the system can bind to it.

---

## 10. Milestones

- **M1 — Keyboard works.** IME service, QWERTY layout, shift/symbols, commit text.
  *Deliverable: installable, usable as a daily keyboard.*
- **M2 — Suggestions + spell.** SymSpell + dictionaries + suggestion bar.
- **M3 — Hinglish.** Lexicon, phonetic matching, language modes.
- **M4 — Local LLM.** ModelManager + MediaPipe engine + Fix/Rephrase actions.
- **M5 — Writing actions.** Email / Essay / Message, streaming result card.
- **M6 — Polish.** Themes, haptics, gestures (swipe to delete), settings export.
- **M7 — v2.** Devanagari transliteration, multi-language.

This repository currently contains **M1–M5 as a runnable starter scaffold**: the
keyboard, spell engine, Hinglish layer and the LLM wiring are all present and
compile against the declared dependencies. Model weights are downloaded at
runtime; they are never committed.
