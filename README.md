# 🎓 CampusMind AI (Android Edition)

A multi-persona AI study companion and course assistant for university students, built natively with **Kotlin** and **Jetpack Compose (Material 3)**.

---

## 🎯 What CampusMind AI Does

University students balance coursework across multiple domains — debugging programming code, drafting essays, structuring revision calendars, and digging through dense lecture notes.

**CampusMind AI** consolidates these needs into a unified Android application with:
1. **Five Purpose-Built Personas** with unique system prompts and guidance styles.
2. **Deterministic Student Memory Layer** that auto-extracts your name, major, year, and institution.
3. **Document & Lecture Q&A (RAG)** with strict built-in privacy protection.
4. **State-of-the-Art Gemini Models** for high-speed academic problem solving.
5. **Modern Android UX** with light/dark theming, audio read-aloud (TTS), code syntax formatting, and chat export.

---

## ✨ Core Features

### 🎭 1. Five Specialized Academic Personas
- 🎓 **Campus Assistant (Default):** General academic questions, study methods, and university life.
- 🐍 **Python Tutor:** Focused coding help with complete, runnable code examples and syntax explanations.
- ✍️ **Writing Coach:** Academic writing refinement, clarity, thesis development, and argument structuring.
- 📊 **Study Planner:** Exam preparation schedules, time blocking, and revision strategies.
- 🌍 **Research Helper:** Academic source finding, literature review structures, and research methodology.

### 🧠 2. Automatic Student Memory Layer
- Automatically detects student facts from natural conversational flow:
  - **Name:** (e.g. *"My name is Aazan"*, *"I'm Alice"*)
  - **Major:** (e.g. *"studying Computer Science"*, *"my major is Bioengineering"*)
  - **Year:** (e.g. *"freshman"*, *"sophomore"*, *"3rd year"*)
  - **University:** (e.g. *"study at IIUI"*, *"attend Stanford University"*)
- Allows manual inspection, custom student notes (e.g., target GPA, research topics), and one-tap clearing.
- Injects remembered student context into every turn so responses remain personalized without asking repeatedly.

### 📄 3. Lecture Notes & Document Q&A (RAG)
- Students can load pre-packaged course lectures (e.g. *PF Lecture 18: Static and Automatic Variables*, *CS 201: Big-O Complexity*) or paste their own lecture materials.
- Retrieves relevant excerpts to ground answers strictly in course concepts.
- **Strict Privacy Protocol:** Document-grounded mode enforces that instructor names, emails, phone numbers, and author identities are never revealed.

### ⚙️ 4. Student Productivity Tools
- **Model Switcher:** Seamlessly toggle between Gemini 3.5 Flash (Best Overall), Gemini 3.1 Pro (Coding & STEM), and Gemini 3.1 Flash Lite (Fastest).
- **Text-to-Speech (TTS):** Audio read-aloud for study on the go.
- **Syntax Highlighting & Copy:** Clean code block rendering with individual copy buttons.
- **Theme Switching:** Dark mode (slate & orange) and Light mode.
- **Chat Persistence & Export:** Chats survive app restarts and can be shared or exported as JSON.
- **Live Statistics:** Session turns and word count chips.

---

## 🛠 Tech Stack

- **Platform:** Android (Min SDK 26, Target SDK 36)
- **Language:** Kotlin 2.2
- **UI Framework:** Jetpack Compose with Material Design 3 (M3)
- **Architecture:** MVVM (Model-View-ViewModel) + StateFlow coroutines
- **Networking:** OkHttpClient 4.12
- **Secrets Management:** Secrets Gradle Plugin (`.env` / `BuildConfig`)
- **Persistence:** SharedPreferences JSON storage

---

## 🚀 Getting Started

1. Configure your `GEMINI_API_KEY` in the Google AI Studio **Secrets panel** (injected into `.env`).
2. Build and launch the app in the streaming emulator or export as an APK.
