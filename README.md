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
large. The app downloads them on request into app-private storage, via the
system downloader, from URLs in `settings/ModelManager.kt`.

The default is **Qwen2.5 1.5B Instruct** (`~1.5 GB`, ungated) with a smaller
**Qwen2.5 0.5B Instruct** (`~0.5 GB`) option. Both are `.task` bundles. Gemma is
supported by the runtime but its public copy is gated, so it needs a token — it
is not the default.

## Signing (updating without uninstalling)

Android only lets an APK update an app if it is signed with the **same key** as
the installed version. CI runners otherwise generate a *random* debug key per
run, which forces an uninstall before every update.

This repo ships a fixed **debug** keystore at `app/debug.p12`
(alias `androiddebugkey`, password `android` — the standard Android debug
convention). Every build signs with it, so updates install straight over the
previous APK, with no uninstall and no setup.

> `app/debug.p12` is a **debug** key: it is public by design and must never be
> used for a release build or the Play Store. For a real release, supply your own
> key via the `KEYSTORE_FILE` / `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD`
> secrets — the build uses it automatically when present.

### One-time note

The first APK you installed was signed with a random CI debug key, so moving onto
this fixed key needs **one final uninstall**. After that, updates are seamless.

## License

TBD (add a `LICENSE` file before publishing).
