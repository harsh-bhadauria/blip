# Blip

> *Part task manager, part blob, entirely too invested in your productivity.*

Blip is a persistent overlay companion for Android. Instead of living inside an app you have to consciously open, Blip sits in the corner of your screen and surfaces tasks through periodic thought bubbles. The idea is simple: a task manager you can't accidentally ignore because it is always, quietly, right there.

No notifications. No redirects. Just a small blob that has opinions about your to-do list.

## Features

- **Floating overlay:** Blip lives above every other app as a foreground overlay. Always present, never in the way.
- **Thought bubbles:** Periodically floats one task your way. Prioritises tasks approaching their deadlines and spaces reminders intelligently so it doesn't become the thing you start ignoring too.
- **Urgency reactions:** Blip's shape and colour shift dynamically based on how many tasks you have and how close their deadlines are. The more behind you fall, the more distressed it looks. This is intentional.
- **Radial action menu:** Long-press and drag a task in the overlay to pull up a gesture-based radial menu for quick actions. No extra taps, no extra screens.
- **Quick-add:** Long-press Blip to instantly add a task with deadline chips, without leaving your current app.
- **Quiet hours:** Blip respects your focus and your sleep. Configure times where it keeps its thoughts to itself.
- **Control panel app:** A dedicated app for managing your full task list, tweaking behaviour, and configuring settings.


## Tech Stack

| | |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM |
| DI | Hilt |
| Storage | Room |
| Async | Coroutines & Flow |
| Companion rendering | AGSL RuntimeShader |

Blip is rendered using a custom AGSL shader with simplex noise, giving it an organic, fluid quality that reacts in real time to your task load. It is a live fluid simulation running in your phone's corner. Whether that is impressive or deeply unnecessary is a matter of perspective.


## Getting Started

### Prerequisites
- Android Studio (latest)
- Android SDK 33+

### Setup
```bash
git clone https://github.com/yourusername/blip.git
```

Open in Android Studio, let Gradle sync, and run on a physical device or emulator.

> **Note:** Blip requires the `SYSTEM_ALERT_WINDOW` permission to render the overlay. You will be prompted to grant this on first launch.

## Project Structure

```
ui.main       → Control panel app (task list, settings)
ui.overlay    → Foreground service, overlay rendering, bubble logic
data          → Room database, entities, DAOs
domain        → Repositories and models
```


## Contributing

Issues, PRs, and feature suggestions are welcome. If you build a skin — a character, a vibe, a theme — open a PR. Blip contains multitudes.

<h1></h1>
~ Made with ♥️ by Harsh
