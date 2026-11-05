# Nucleo

An offline Android game for ages 13+. You grow a universe from bare hydrogen to a planet that could host life, and on the way you learn real stellar physics, chemistry, orbital mechanics and galaxy classification through five linked labs:

| Tab | Lab | What you do |
|---|---|---|
| Forge | Star Forge | Gather hydrogen from the nebula, form stars, trigger supernovae, merge remnants |
| Spectrum | Spectrum Lab | Identify elements from their spectral lines (a deduction puzzle) to unlock them and earn RP |
| Reaction | Reaction Lab | Combine identified elements into molecules, fuel and planet materials |
| Missions | Gravity Slingshot | Build planets, then fly a probe using gravity assists |
| Telescope | Galaxy Sorter | Classify galaxies; correct answers boost the Forge |

No accounts, no ads, no purchases, no internet permission. Progress is saved only on the phone.

Built with Kotlin, Jetpack Compose (Material 3), Navigation Compose and kotlinx.serialization.

---

## Open it in Android Studio

**The easiest way:** in Android Studio choose **File → Open** and pick this folder (the one containing `settings.gradle.kts`). Wait for Gradle sync, choose a phone or emulator, and press **Run ▶**.

Requirements: a recent Android Studio, JDK 17 or newer, Android SDK 36. If Android Studio offers to update library versions in `gradle/libs.versions.toml`, accept the suggestions.

**If you prefer to paste files into a new project:**

1. Create a new project with the **Empty Activity** template. Name: `Nucleo`. Package name: `com.nucleo`. Minimum SDK: API 26.
2. Replace `gradle/libs.versions.toml`, `app/build.gradle.kts` and `app/src/main/AndroidManifest.xml` with the ones from this repo.
3. Copy everything in `app/src/main/java/com/nucleo/` into the same folder of your project (delete the template's `MainActivity.kt` and `ui/theme` first).
4. Copy `app/src/main/res/` (fonts, icon, themes, backup rules) and `app/src/test/`.
5. Sync Gradle and run.

## Publish to Google Play

1. **Build → Generate Signed App Bundle** → Android App Bundle → create a keystore (keep it safe, you need it for every update).
2. Upload the `.aab` in the Play Console.
3. Target audience: 13–15, 16–17 and 18+ only. Data safety: "No data collected", "No data shared". Ads: none.
4. Privacy policy: Play needs a URL even for an app that collects nothing. Host [`docs/privacy-policy.md`](docs/privacy-policy.md) (for example on GitHub Pages) and add your contact email.

---

## How the code is organized

The rule: **all game rules are pure Kotlin** in `game/` (no Android imports), so they are easy to read and unit-test. Screens only draw the state and call the ViewModel.

```
app/src/main/java/com/nucleo/
├── MainActivity.kt        The only Activity: splash, game clock start/stop, share sheet, save-file picker
├── NucleoApp.kt           Navigation, bottom bar, snackbars, supernova overlay, "while you were away"
├── AppNavigator.kt        Every route and tab in one place (screens call nav.openCodex(), etc.)
├── GameViewModel.kt       Holds the state, runs the clock, connects screens to the engines, saves
├── Platform.kt            Small interface for Android-only actions (share, export, import)
│
├── content/               Game data (what exists), no logic
│   ├── Elements.kt        All 118 elements: mass, category, discovery year, cosmic origin, spectral lines
│   ├── Chemistry.kt       Molecules, the 20 Reaction Lab recipes, buildable planets
│   ├── Stars.kt           Star types, their phases and the cosmic events (supernova, kilonova…)
│   ├── Upgrades.kt        The 14 upgrades and their cost formula
│   ├── Missions.kt        The 10 Gravity Slingshot missions
│   ├── Galaxies.kt        Galaxy classes and the 230 galaxy records on 11 telescope plates
│   └── Sources.kt         Credits, licenses and the "how the game bends reality" list
│
├── game/                  Game rules (pure Kotlin, unit-tested)
│   ├── GameState.kt       Everything the player has, in one serializable object
│   ├── Progression.kt     Metallicity, eras, stages, unlocks, bonus numbers
│   ├── ForgeEngine.kt     Star output (closed-form, so offline catch-up is instant), deaths, mergers
│   ├── SpectrumEngine.kt  Clue scoring, RP, shuffle bag, smart repeats
│   ├── ReactionEngine.kt  Recipe lookup, crafting, planet building
│   ├── OrbitSim.kt        Gravity physics (velocity Verlet, fixed 1/120 s steps)
│   ├── GalaxyEngine.kt    Observing nights, answers, bonuses
│   ├── Economy.kt         Upgrade shop
│   └── Achievements.kt    Badges
│
├── data/SaveStore.kt      Saves GameState as one JSON file; export/import use the same JSON
│
└── ui/
    ├── theme/             Colors, fonts (Space Grotesk + Inter), text styles, theme
    ├── components/        Shared pieces: buttons, chips, cards, sheets, and Canvas art
    ├── forge/             Forge screen, star picker, star detail, inventory, event overlay
    ├── spectrum/          Spectrum home, round screen, result sheet, how-to-play
    ├── reaction/          Reaction Lab and recipe book
    ├── missions/          Missions map, hangar, mission play, mission result
    ├── telescope/         Galaxy Sorter and universe map
    ├── codex/             Codex tabs and element card
    └── more/              Intro, upgrades, settings, achievements, home system, ending
```

### Changing numbers (balancing)

Every tuning number lives in `content/` (star rates, costs, recipe amounts, mission par) or at the top of an engine object (for example `SpectrumEngine.BASE_RP`). Change it there; nothing else needs to change. Run the tests afterwards.

### Tests

```bash
./gradlew test
```

26 JVM tests cover the Forge simulation (closed form vs. step-by-step), first-supernova pacing, Spectrum scoring, the shuffle bag and repeats, recipes, orbit physics (energy conservation, determinism), upgrade costs, and an automatic solver that proves every mission can be cleared at par fuel.

---

## What's in this version, and what's left from the spec

Everything in the 5 tabs and the top bar works: splash and intro, Forge (nebula, 3 star types, 8 slots, star detail, supernova/kilonova/Type Ia, remnants, offline progress, starburst), Spectrum Lab (Signal, Classic, Hard and Blitz modes for the Elements and Molecules pools), Reaction Lab (20 recipes, batches, recipe book), Gravity Slingshot (hangar, 10 missions with real orbit physics), Galaxy Sorter (observing nights, 3-class and 10-class modes, bonuses, universe map), Codex, Upgrades, Settings, Achievements, Home System and the ending.

Simplified on purpose, to keep the code small and easy to follow:

- **One JSON save file instead of Room.** The whole `GameState` is saved to `files/save.json` every 30 s and when the app goes to the background. Export/import write and read the same JSON through the system file picker. Auto Backup includes it.
- **English text is written directly in the screens**, not in `strings.xml`. Before adding Uzbek or Russian, move the texts to `res/values/strings.xml` and use `stringResource(...)`.
- **Content is Kotlin code instead of JSON assets**, so you get autocomplete and compile-time checks.

Not included yet (planned for later versions in the spec):

- **Real galaxy images.** The Galaxy Sorter draws an illustration for each galaxy from its class, and labels it as an illustration. To use the real Galaxy10 DECaLS images, put the WebP files in `assets/images/galaxies/`, fill `imageFile` and `credit` on each record in `content/Galaxies.kt`, and load them in `GalaxyImage` (the credit line is already shown under the image).
- **Spectral lines** in `Elements.kt` are approximate. Check them against the NIST Atomic Spectra Database before release. Discoverer names are not included.
- Named stars, Messier and constellation pools, and mission sectors 2–3 (shown as "v1.1").
- Sound effects, music and optional notifications (the settings rows are there but disabled).
- The Python content pipeline from the spec (`tools/`).

## Licenses

Code: yours. Fonts: SIL Open Font License (see `licenses/`). Data adapted from CC BY-SA sources keeps that license; see [LICENSE-DATA.md](LICENSE-DATA.md).
