# Local AI Keyboard

An offline, privacy-first Android keyboard with a **local on-device LLM** for
AI writing, grammar & spelling correction, and English + **Hinglish** input.

No cloud. No accounts. No telemetry. Your keystrokes never leave the phone.

> Status: **runnable starter scaffold.** The keyboard, spell engine, Hinglish
> layer and the on-device LLM wiring are all implemented. See
> [ARCHITECTURE.md](ARCHITECTURE.md) for the full design and the milestone plan.

---

## Features

- **Real IME** — a working `InputMethodService` you can set as your system keyboard.
- **Local spell-check** — SymSpell + frequency dictionaries, ranked suggestions as you type.
- **Lightweight grammar pass** — capitalisation, `a/an`, doubled words, common typos.
- **Hinglish** — Romanised Hindi lexicon + phonetic matching (`kese` → `kaise`).
- **On-device AI** — Gemma-class model via the MediaPipe LLM Inference API:
  - **Fix** — spelling & grammar correction
  - **Rephrase** — same meaning, new phrasing
  - **Continue** — keep writing
  - **Email / Essay / Message** — compose from notes or a topic
- **Fully offline** — the model downloads once, on your command; after that, zero network.

## Requirements

- Android 10+ (API 29)
- arm64-v8a device with 6 GB+ RAM
- ~1 GB free storage for the default model

## Build

```bash
git clone https://github.com/merry8989-uk/local-ai-keyboard.git
cd local-ai-keyboard
# open in Android Studio (Hedgehog or newer) and Run, or:
./gradlew :app:assembleDebug
```

Then on the device: **Settings → System → Languages & input → On-screen keyboard →
Local AI Keyboard**, enable it and select it.

The first launch compiles the dictionary index (a few seconds). AI actions stay
disabled until you download a model in the app's Settings screen.

## Project layout

```
app/src/main/java/com/merry8989/localaikeyboard/
├── ime/         InputMethodService, keyboard view, layouts, suggestion bar
├── spell/       SymSpell, dictionaries, grammar rules
├── lingua/      Hinglish lexicon + phonetic matching
├── ai/          LlmEngine, MediaPipe backend, prompt templates, actions
├── settings/    Settings screen + model manager
└── util/        helpers
```

## Model weights

Model files (`.task` / `.litertlm` / `.gguf`) are **not** committed — they are
large and license-bound. The app downloads them on request into app-private
storage. See `settings/ModelManager.kt` for the URLs and checksums to configure.

## License

TBD (add a `LICENSE` file before publishing).
