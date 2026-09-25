# 👑 Ludo King (Android)

A feature-rich, high-performance **Ludo King** game built natively for Android. Featuring authentic board gameplay rules, smart Bot AI, offline 2-to-4 player multiplayer, team match mode, dynamic sound architecture with custom audio effects, and automated GitHub Actions APK builds.

---

## 🌟 Key Features

- 🎲 **Authentic Ludo Mechanics:** 100% true to official Ludo King rules with genuine independent dice rolls, extra turns, and smooth animations.
- ⚡ **Instant Turn Transitions:** Ultra-responsive turn switching with zero lag between players.
- 🤖 **Smart Bot AI:** Strategic single-player mode with intelligent token prioritization and safe-spot seeking.
- 👥 **Multiplayer Game Modes:**
  - **Pass N Play:** 2 to 4 players on a single device.
  - **Vs Computer:** Play against challenging Bot AI.
  - **Team Up:** 2v2 classic team matches.
  - **Quick Ludo:** Fast-paced single-token victory mode.
- 🎵 **Dual-Channel Audio System:** Seamless concurrent playback for official sound effects and custom audio triggers.
- 📱 **Adaptive UI:** Fullscreen immersive portrait layout with rich animations and responsive dice controllers.
- 🚀 **Automated CI/CD:** Push-to-release workflow via GitHub Actions that automatically builds and publishes `LudoKing.apk`.

---

## 🔊 Audio System & Sound Triggers

### 1. Selected Custom Audio Effects

| Audio File | Game Trigger Event | Description |
|---|---|---|
| `token_kill.mp3` | **Token Capture / Kill** | Plays when a player's token captures an enemy token. |
| `home_path_entry.mp3` | **Home Path Entry** | Plays when a token enters the colored home stretch. |
| `game_winner.mp3` | **Game Victory** | Plays when a player wins the match. |

### 2. Official Ludo King Sounds

- 🎲 **Dice Roll:** `diceroll.mp3` / `dice.mp3`
- 🚶 **Step / Movement:** `step.mp3` / `move.mp3`
- ⭐ **Safe Spot (Star):** `safe.mp3`
- 🏠 **Home Reach (Center):** `panta.mp3`
- 🔘 **Button & UI Click:** `click.mp3`
- 🏁 **Game Start Countdown:** `gamestartsound.mp3`
- 🎶 **Background Theme:** `music.mp3`
- 🎉 **Victory Fanfare:** `congratulations.mp3`

---

## 📜 Game Rules

1. **Token Release:** Roll a **6** to bring a token out of the yard into active play.
2. **Extra Turns:**
   - Rolling a **6** grants an immediate extra roll.
   - Capturing an enemy token grants an extra roll.
   - Getting a token into the home triangle grants an extra roll.
3. **Three 6s Penalty:** Rolling three consecutive **6**s automatically cancels the turn and passes the dice to the next player.
4. **Safe Spots (Stars):** Tokens resting on star-marked squares are completely safe and cannot be captured.
5. **Winning:** The first player to bring all required tokens into the home center wins the game.

---

## 🛠️ Tech Stack & Requirements

- **Platform:** Android
- **Language:** Java 8 / 17
- **Minimum SDK:** API 21 (Android 5.0 Lollipop)
- **Target / Compile SDK:** API 33 (Android 13)
- **Android Gradle Plugin (AGP):** 8.2.2
- **Gradle Version:** 8.5
- **Dependencies:** AndroidX AppCompat, ConstraintLayout, Material Components, Glide 4.15.1

---

## 📦 How to Build & Install

### 1. Automated GitHub Release (Recommended)
Every commit pushed to the `main` or `master` branch triggers the GitHub Actions workflow to build and publish the latest APK directly under **GitHub Releases** (`v1.0.0-latest`).

### 2. Local Build via Terminal
```bash
# Clone the repository
git clone https://github.com/teamexpertbs/kingludo.git

# Navigate into project directory
cd kingludo

# Build the Debug APK
./gradlew assembleDebug
```
The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 👨‍💻 Developer & Credits

- **Developer:** Team Expert BS
- **Project:** Ludo King Android
- **Repository:** [teamexpertbs/kingludo](https://github.com/teamexpertbs/kingludo)

---

## 📄 License

This project is maintained and distributed for personal and educational use. All rights reserved by the developer.
