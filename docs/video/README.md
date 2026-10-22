# Demo video

The contest asks for an optional screencast of **3-5 minutes**.

## How to add it to the README

**Option A - YouTube (recommended).** Upload as *Unlisted* (or Public). Copy the link, open the root `README.md`, find the **Demo video** section and replace `https://youtu.be/REPLACE_WITH_YOUR_VIDEO_ID` with your link. Put a frame of the video at `docs/screenshots/video-thumbnail.png`.

**Option B - file in GitHub.** Edit `README.md` on github.com, drag the .mp4 (under 100 MB) into the editor and GitHub inserts a playable link. Do not commit big video files into the repo.

## Suggested script (about 4 minutes)

| Time | Show | Say |
|---|---|---|
| 0:00-0:20 | Logo/banner, then the app on the phone | "StudyZen: an AI study planner in Kotlin Multiplatform." |
| 0:20-1:30 | Create a Study Space with the wizard, open the learning path and a lesson | Plan, path, lessons |
| 1:30-2:30 | Lesson tools: Ask AI, Dictionary, Calculator, Notes; show history and "open previous or new" | One place, with history |
| 2:30-3:00 | Start studying screen, session timer, summary, quiz and progress | The study loop |
| 3:00-3:45 | **Same app on Desktop and Web** (and iOS if you ran it) | "One shared Kotlin codebase, about 94%" - show `scripts/shared-code-ratio.sh` output |
| 3:45-4:15 | Android-only extras: alarm reminder, camera scan | Platform APIs via small expect/actual classes |
| 4:15-4:30 | Guest mode, GitHub repo | How judges can run it |

Record the screen of each platform with OS tools (Xbox Game Bar on Windows `Win+G`, Android screen recorder, QuickTime on macOS).
