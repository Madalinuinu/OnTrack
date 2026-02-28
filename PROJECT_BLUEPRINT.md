# OnTrack Daily — Project Blueprint

> **App name:** OnTrack Daily  
> **Purpose:** Local-only habit and system tracking with gamification (streaks).  
> **Last blueprint update:** 2025-02-07

---

## 1. Tech Stack

| Layer | Technology |
|-------|------------|
| **Platform** | Android |
| **Language** | Kotlin |
| **UI** | Jetpack Compose |
| **Local database** | Room |
| **Preferences** | DataStore (Preferences) |

- **Room:** Systems, habits, daily check-ins, streaks.
- **DataStore:** User name (from onboarding), app preferences, onboarding completion flag.

---

## 2. Authentication

- **No login.** All data is stored locally on the device.
- No accounts, no cloud sync, no server.

---

## 3. App Flow

### 3.1 Onboarding (first launch)

1. **Screen 1:** “Welcome to OnTrack Daily” (welcome / intro).
2. **Screen 2:** Input **Name** (e.g. text field).
3. **Save:** Persist name via **DataStore**.
4. Mark onboarding as completed (e.g. DataStore flag) so user is not shown onboarding again.
5. Navigate to **Home**.

### 3.2 Home

- **Title / greeting:** “Welcome back [Name]”.
- **Content:** List of current **Systems**.
- **Action:** Button to **create new System**.
- Tapping a System opens **Tracking** (daily checklist) for that system.

### 3.3 System creation

- **Name:** User gives the system a name.
- **Duration (optional):** e.g. “30 days”, “90 days”, or none.
- **Goal:** User defines the goal of the system (text/description).
- **Habits:** User adds one or more habits. For each habit:
  - **Frequency:** one of:
    - **Daily**
    - **Weekly** (e.g. “once per week”)
    - **X times per week** (e.g. “3 times per week”)
- Save system (and habits) to **Room**.

### 3.4 Tracking (daily checklist)

- **Entry:** User selects a **System** from Home.
- **View:** **Daily checklist** for that system:
  - Shows habits that are due today based on their frequency.
  - User can **check** (complete) or uncheck items.
- **Persistence:** Check state and completion dates stored in **Room** (e.g. check-ins / completions table).

### 3.5 Gamification (streaks)

- **Duolingo-style streak:** A streak increases when **all** daily tasks for that day (for the selected system or globally—clarify in implementation) are done.
- **Rules:**
  - If all required daily tasks are completed → **increase streak** (e.g. +1).
  - If not all are completed (or day is missed) → **reset streak** to 0 (or defined rule).
- **Storage:** Current streak (and optionally history) stored in **Room** and/or derived from check-in data.
- **UI:** Display current streak (e.g. “🔥 7 day streak”) where relevant (e.g. Home or Tracking screen).

---

## 4. Data Model (high level)

- **User preferences (DataStore):** `userName`, `onboardingCompleted`.
- **Room entities (implemented):**
  - **SystemEntity** (`systems`): id (PK), name, goal, duration (nullable Int), startDate (Long).
  - **HabitEntity** (`habits`): id (PK), systemId (FK), title, frequencyType (DAILY | WEEKLY | SPECIFIC_DAYS), targetCount (e.g. 3 for "3×/week").
  - **HabitLogEntity** (`habit_logs`): id (PK), habitId (FK), date (Long, epoch day), isCompleted. Unique on (habitId, date).
  - **Streak:** currentStreak (Int) and lastStreakDate (Long, epoch day) in DataStore. Streak day = all DAILY habits completed for that day.

---

## 5. Completed Features / Progress

*Update this section whenever a major feature is completed so context is never lost.*

| Feature | Status | Notes |
|---------|--------|--------|
| Project setup (Compose, Room, DataStore) | ✅ Done | DataStore Preferences + manual DI (no Hilt). ViewModel + lifecycle-compose. |
| Onboarding (Welcome + Name + DataStore) | ✅ Done | OnboardingScreen: welcome text, name TextField, Start button. Saves to DataStore, sets `isFirstLaunch` false, navigates to Home. |
| Home (Welcome back [Name], system list) | ✅ Done | HomeScreen: header "Welcome back [Name]", list of Systems as Cards (name, goal, Continue). Empty state: icon + message + "Create New System". Continue → TrackerScreen/{systemId}. HomeViewModel loads systems from Room. |
| Create new System (name, duration, goal, habits) | ✅ Done | CreateSystemScreen with name, goal, duration (optional), Habits list + Add Habit (ModalBottomSheet: title, frequency Daily/Weekly/X per week + slider 1–7). CreateSystemViewModel saves to Room and navigates back. |
| Room schema (System, Habit, Check-in, Streak) | ✅ Done | SystemEntity, HabitEntity, HabitLogEntity. FrequencyType enum (DAILY, WEEKLY, SPECIFIC_DAYS). AppDatabase + SystemDao, HabitDao, HabitLogDao. Insert system with habits: call systemDao.insertSystem() then habitDao.insertHabits() (in app/repository, optionally in DB transaction). |
| Daily checklist (Tracking screen) | ✅ Done | TrackerScreen: current date, list of checkboxes (DAILY always; WEEKLY if not done this week; SPECIFIC_DAYS with "X/Y done this week"). Toggle inserts/updates HabitLog for today; strikethrough when checked. TrackerViewModel combines habits + today/week logs. |
| Streak logic and display | ✅ Done | StreakManager: isDayComplete (all DAILY habits across systems), refreshStreak (yesterday missed → reset; today complete → increment). currentStreak + lastStreakDate in DataStore. Fire icon + count on HomeScreen (top right) and TrackerScreen (TopAppBar actions). |

**Last major feature completed:** Streak (StreakManager, DataStore, refresh on launch/Home + after toggle, fire badge on Home + Tracker)  
**Blueprint last updated:** 2025-02-07

---

## 5.1 Completed Features (Summary)

A single list of everything built so far, for quick reference.

- **App shell:** Single Activity, Compose, OnTrackTheme (Material 3, dynamic color on API 31+), enableEdgeToEdge.
- **Dependencies:** DataStore Preferences, Room + KSP, Navigation Compose, Lifecycle ViewModel/Runtime Compose, Material Icons Extended.
- **DI:** Manual (OnTrackApplication provides UserPreferences, AppDatabase, StreakManager; ViewModel factories per screen).
- **Onboarding:** One screen: “Welcome to OnTrack daily”, name TextField, Start button. Saves name + sets `isFirstLaunch` false in DataStore; then navigates to Home.
- **Home:** “Welcome back [Name]” + streak badge (top right). List of systems as Cards (name, goal, Continue). Empty state: folder icon + “No systems yet” + “Create New System”. Button “Create New System” at bottom when list is non-empty. Navigation: home → create_system, home → tracker/{systemId}.
- **Create System:** Form: system name, goal, duration (optional). Habits section: list of habit cards (title + frequency), “Add Habit” (ModalBottomSheet). Add Habit: title, frequency (Daily / Weekly / X per week), slider 1–7 for X. CreateSystemViewModel saves system + habits to Room and navigates back.
- **Tracker:** Route `tracker/{systemId}`. Top bar: title = system name, back, streak badge. Date header (e.g. “Saturday, Feb 7”). List of checkboxes: DAILY always; WEEKLY only if not done this week; SPECIFIC_DAYS with “X/Y done this week”. Toggle updates HabitLog for today; strikethrough when checked. TrackerViewModel combines habits + today/week logs.
- **Data layer:** Room: SystemEntity, HabitEntity, HabitLogEntity, FrequencyType (DAILY, WEEKLY, SPECIFIC_DAYS). SystemDao (insert, getAllSystems, getSystemById). HabitDao (insertHabits, getHabitsForSystem). HabitLogDao (getHabitLogsForDateRange, getLog, insert, updateCompletion, toggleHabitCompletion). DataStore: userName, isFirstLaunch, currentStreak, lastStreakDate, setStreak.
- **Streak:** StreakManager: isDayComplete(epochDay) = all DAILY habits across all systems completed that day; refreshStreak() on launch (Home init) and after each habit toggle (reset if yesterday missed, increment if today just completed). Streak badge (fire icon + count) on Home and Tracker.

---

## 5.2 Edge Cases & Missing Features

Things that are **not** implemented yet; worth considering for future work.

| Gap | Current behavior | Suggestion |
|-----|------------------|------------|
| **Edit system** | No way to change name, goal, duration, or habits after creation. | Add “Edit” on system card or in Tracker; new screen or bottom sheet with pre-filled form; SystemDao/HabitDao update/delete + optional migration for habit logs. |
| **Delete system** | Systems cannot be removed. | Add long-press or overflow menu on system card → “Delete” with confirm; SystemDao `deleteSystem(id)` (or rely on FK CASCADE if habits/logs already cascade). |
| **Delete / edit habit** | Habits can only be added at creation; no remove or edit. | In Create System (or Edit System), allow removing a habit from the list; optionally HabitDao delete. For edit, pre-fill Add Habit sheet. |
| **Change user name** | Name is set once at onboarding. | Settings or profile screen to update name in DataStore. |
| **Empty name at Start** | User can tap Start with blank name. | Disable “Start” when name.isBlank() or show Snackbar; optionally allow and show “there” on Home. |
| **Create system with 0 habits** | Allowed. | Current behavior is fine; could add validation “Add at least one habit” if product wants to require it. |
| **Duration meaning** | Stored but not used (no “system ended” or reminders). | Either use duration for “days left” / end date in UI or remove from the form for now. |
| **Time zone / date change** | Streak uses device date; midnight rollover could be confusing. | Document that “day” = device calendar day; consider running refreshStreak when app comes to foreground if date might have changed. |
| **Tracker for deleted system** | If system were deletable, opening tracker/{id} could show missing system. | TrackerViewModel already shows loading; add handling when system == null after load (e.g. “System not found” + back). |

---

## 6. Implementation Notes

- Use **single Activity** with Compose navigation (e.g. `NavHost`).
- Use **ViewModel** + **StateFlow/State** for UI state; Repository layer for Room and DataStore.
- Consider **Hilt** or **Koin** for DI if the project grows.
- Streak calculation: define “day” (e.g. calendar day in device timezone) and run logic when opening Tracking or via a daily job/check.

---

## 7. Suggested UI Improvements

Three changes to make the app feel more modern and polished.

1. **Custom color palette & shape**  
   Replace the default purple Material 3 palette with a cohesive habit-app theme (e.g. soft teal/cyan primary, warm surface tints). In `Theme.kt`, set a custom `lightColorScheme` / `darkColorScheme` and pass `MaterialTheme.colorScheme` consistently. Add `Shape` to the theme: use `RoundedCornerShape(12.dp)` or `MaterialTheme.shapes.medium` for cards and buttons so cards, bottom sheets, and buttons share the same corner radius.

2. **Rounded corners and elevation**  
   Apply consistent rounding to cards and containers: e.g. `Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp))` on Home system cards and Create System habit chips. Use `OutlinedTextField` with `shape = RoundedCornerShape(12.dp)` (or via `TextFieldDefaults`) so inputs match. Add subtle elevation or a light border so cards and the streak badge feel layered.

3. **Light animations**  
   Add short, purposeful animations: **list items** — `animateItem()` or `Modifier.animateItem()` in LazyColumn for system cards and tracker rows so they animate in when the list loads; **checkbox** — keep default ripple and consider a quick scale/alpha when a habit is checked to reinforce feedback; **navigation** — use `AnimatedContent` or `EnterTransition`/`ExitTransition` in the NavHost so screen transitions (e.g. Home → Tracker, Create System → Home) slide or fade instead of cutting. Optionally animate the streak badge (e.g. scale when streak increases) for a Duolingo-like reward feel.

---

*When you complete a major feature, update **Section 5** and the “Last blueprint update” / “Last major feature completed” lines at the top and bottom of this file.*
