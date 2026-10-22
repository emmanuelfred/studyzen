# Publishing downloads (APK, MSI, DMG, DEB and the web demo)

## 1. Put the project on GitHub
Create a public repo named `studyzen`, push the code. Replace `<your-user>` in `README.md` with your GitHub username (Find and Replace in the file).

## 2. Turn on the web demo (one time)
GitHub repo -> Settings -> Pages -> Source: **GitHub Actions**. Every push to `main` then rebuilds the site at `https://<your-user>.github.io/studyzen/`. Check the "Web demo" run under the Actions tab.

## 3. Publish the installers
```bash
git tag v1.0.0
git push origin v1.0.0
```
The "Release" workflow builds the Android APK, Windows MSI, macOS DMG and Linux DEB and attaches them to a new release. The README download links point at the newest release, using exactly these names: `StudyZen-android.apk`, `StudyZen-windows.msi`, `StudyZen-macos.dmg`, `StudyZen-linux.deb`.

## Building by hand instead
| Output | Command | File appears in |
|---|---|---|
| APK | `./gradlew :androidApp:assembleDebug` | `androidApp/build/outputs/apk/debug/` |
| MSI (on Windows) | `gradlew.bat :desktopApp:packageMsi` | `desktopApp/build/compose/binaries/main/msi/` |
| DMG (on macOS) | `./gradlew :desktopApp:packageDmg` | `.../main/dmg/` |
| DEB (on Linux) | `./gradlew :desktopApp:packageDeb` | `.../main/deb/` |
| Web files | `./gradlew :webApp:wasmJsBrowserDistribution` | `webApp/build/dist/wasmJs/productionExecutable/` |

Then GitHub -> Releases -> Draft a new release -> drag the files in, and keep the names above.

## Notes
- The APK is debug-signed so anyone can install it. A Play Store release would need your own signing key (never commit a keystore).
- The MSI must be built on Windows, the DMG on macOS (a free GitHub runner does this for you).
- Not run by me: these workflows and tasks are untested, so check the Actions tab after your first push and send me any red step.
