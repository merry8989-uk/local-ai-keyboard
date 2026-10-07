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

## Signing (updating without uninstalling)

Android only lets an APK update an app if it is signed with the **same key** as
the installed version. CI runners generate a *random* debug key on every run, so
without this step each build would need an uninstall first. Sign with one stable
key to fix that:

1. Create a keystore once, and keep it safe — losing it means you can never
   update the app again:

   ```bash
   keytool -genkeypair -v -keystore release.keystore \
     -alias localkey -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Base64-encode it and add four repository secrets
   (Settings → Secrets and variables → Actions):

   | Secret | Value |
   |---|---|
   | `KEYSTORE_BASE64` | output of `base64 -w0 release.keystore` |
   | `KEYSTORE_PASSWORD` | the store password you chose |
   | `KEY_ALIAS` | `localkey` |
   | `KEY_PASSWORD` | the key password you chose |

3. Push. The next APK is signed with your key, and from then on every build
   installs straight over the previous one — no uninstall.

For local builds, put the same four values in `gradle.properties` (or export
them as environment variables) and Gradle signs the debug build with the same key.

## License

TBD (add a `LICENSE` file before publishing).
