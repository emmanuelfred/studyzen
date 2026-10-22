import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "com.fredrickemmanuel.studyzen.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "StudyZen"
            packageVersion = "1.0.0"
            description = "AI study planner"
            vendor = "Fredrick Emmanuel"
            // Java modules the packaged app needs that the build cannot detect on its own
            // (settings storage, sound, Google sign-in and HTTP).
            modules("java.prefs", "java.net.http", "jdk.unsupported")
            windows {
                menuGroup = "StudyZen"
                shortcut = true
                dirChooser = true
                perUserInstall = true
            }
        }
    }
}