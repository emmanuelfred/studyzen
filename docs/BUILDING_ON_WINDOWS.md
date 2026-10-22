# Building every download from a Windows PC

Run these in a terminal at the project root (Android Studio -> Terminal, or PowerShell). Use JDK 17 or newer.

| You get | Command | File ends up in |
|---|---|---|
| Android APK | `.\gradlew.bat :androidApp:assembleDebug` | `androidApp\build\outputs\apk\debug\androidApp-debug.apk` |
| Windows installer (.msi) | `.\gradlew.bat :desktopApp:packageMsi` | `desktopApp\build\compose\binaries\main\msi\` |
| Windows app folder, no install (.exe inside) | `.\gradlew.bat :desktopApp:createDistributable` | `desktopApp\build\compose\binaries\main\app\StudyZen\` |
| Any-OS Java file (.jar) | `.\gradlew.bat :desktopApp:packageUberJarForCurrentOS` | `desktopApp\build\compose\jars\` |
| Web site files | `.\gradlew.bat :webApp:wasmJsBrowserDistribution` | `webApp\build\dist\wasmJs\productionExecutable\` |

## One-time requirement for the .msi
`packageMsi` needs the free **WiX Toolset 3.14** (https://github.com/wixtoolset/wix3/releases, install `wix314.exe`) and a restart of the terminal. Without it the task stops with a "Could not find WiX" message. The app folder and .jar need nothing extra.

## What a Windows PC cannot build
- **macOS .dmg** and **Linux .deb** must be built on those systems. The GitHub workflow in `.github/workflows/release.yml` does it for you when you push a tag (`git tag v1.0.0` then `git push origin v1.0.0`).
- **iOS** needs a Mac with Xcode.

## Zip the app folder for people who do not want an installer
```powershell
Compress-Archive desktopApp\build\compose\binaries\main\app\StudyZen StudyZen-windows-portable.zip
```

## Name the files before uploading to GitHub Releases
`StudyZen-android.apk`, `StudyZen-windows.msi`, `StudyZen-windows-portable.zip`. The README download links expect the first two names.

## Notes
- The debug APK is signed with a debug key, so it installs on any phone after "install unknown apps" is allowed.
- Windows may show "unknown publisher" (SmartScreen) because the installer is not code-signed. Click More info -> Run anyway. Mention this in the README.
- Before building: set `ApiConfig.baseUrlOverride` to `"https://studyzen-api.markpayhub.com"` and put your Desktop Google client in `DesktopGoogleConfig.kt`.
