package com.fredrickemmanuel.studyzen.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import com.fredrickemmanuel.studyzen.core.time.todayEpochDay
import com.fredrickemmanuel.studyzen.domain.ReminderPlanner
import com.fredrickemmanuel.studyzen.data.GuestMode
import com.fredrickemmanuel.studyzen.data.ai.AiSettingsStore
import com.fredrickemmanuel.studyzen.data.ai.LocalAiSettings
import com.fredrickemmanuel.studyzen.feature.ai.AiSettingsScreen
import com.fredrickemmanuel.studyzen.feature.media.LocalMediaPlatform
import com.fredrickemmanuel.studyzen.feature.scanner.ScannerScreen
import com.fredrickemmanuel.studyzen.feature.media.MediaPlatform
import com.fredrickemmanuel.studyzen.feature.media.NoMediaPlatform
import com.fredrickemmanuel.studyzen.feature.reminders.LocalReminderPlatform
import com.fredrickemmanuel.studyzen.feature.reminders.NoReminderPlatform
import com.fredrickemmanuel.studyzen.feature.reminders.NotificationPermissionScreen
import com.fredrickemmanuel.studyzen.feature.reminders.NotificationSettingsScreen
import com.fredrickemmanuel.studyzen.feature.reminders.ReminderPlatform
import com.fredrickemmanuel.studyzen.feature.reminders.RemindersScreen
import androidx.compose.runtime.rememberCoroutineScope
import com.fredrickemmanuel.studyzen.data.remote.SyncApi
import com.fredrickemmanuel.studyzen.data.sync.SyncEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.designsystem.components.EmptyState
import com.fredrickemmanuel.studyzen.core.designsystem.components.ScreenScaffold
import com.fredrickemmanuel.studyzen.data.LocalStudyStore
import com.fredrickemmanuel.studyzen.data.rememberStudyStore
import com.fredrickemmanuel.studyzen.feature.account.AccountRepository
import com.fredrickemmanuel.studyzen.feature.account.EditProfileScreen
import com.fredrickemmanuel.studyzen.feature.account.FakeAccountRepository
import com.fredrickemmanuel.studyzen.feature.account.LocalAccountRepository
import com.fredrickemmanuel.studyzen.feature.account.RemoteAccountRepository
import com.fredrickemmanuel.studyzen.feature.account.SecurityScreen
import com.fredrickemmanuel.studyzen.feature.auth.data.AuthRepository
import com.fredrickemmanuel.studyzen.feature.auth.data.CodePurpose
import com.fredrickemmanuel.studyzen.feature.auth.data.FakeAuthRepository
import com.fredrickemmanuel.studyzen.feature.auth.data.RemoteAuthRepository
import com.fredrickemmanuel.studyzen.data.remote.ApiConfig
import com.fredrickemmanuel.studyzen.feature.auth.google.FakeGoogleSignInClient
import com.fredrickemmanuel.studyzen.feature.auth.google.GoogleSignInClient
import com.fredrickemmanuel.studyzen.feature.auth.login.LoginScreen
import com.fredrickemmanuel.studyzen.feature.auth.reset.ForgotPasswordScreen
import com.fredrickemmanuel.studyzen.feature.auth.reset.ResetPasswordScreen
import com.fredrickemmanuel.studyzen.feature.auth.signup.SignUpScreen
import com.fredrickemmanuel.studyzen.feature.auth.verify.VerifyCodeScreen
import com.fredrickemmanuel.studyzen.feature.main.MainScreen
import com.fredrickemmanuel.studyzen.feature.notes.NoteEditorScreen
import com.fredrickemmanuel.studyzen.feature.notes.NotesScreen
import com.fredrickemmanuel.studyzen.feature.tools.calculator.CalculatorScreen
import com.fredrickemmanuel.studyzen.feature.tools.dictionary.DictionaryScreen
import com.fredrickemmanuel.studyzen.feature.plan.PlanBuilderScreen
import com.fredrickemmanuel.studyzen.feature.plan.SessionEditorScreen
import com.fredrickemmanuel.studyzen.feature.onboarding.OnboardingScreen
import com.fredrickemmanuel.studyzen.feature.study.StudyDetailScreen
import com.fredrickemmanuel.studyzen.feature.session.SessionSummaryScreen
import com.fredrickemmanuel.studyzen.feature.session.StartStudyingScreen
import com.fredrickemmanuel.studyzen.feature.session.StudySessionScreen
import com.fredrickemmanuel.studyzen.feature.study.LearningPathScreen
import com.fredrickemmanuel.studyzen.feature.study.LessonScreen
import com.fredrickemmanuel.studyzen.feature.study.QuizResultScreen
import com.fredrickemmanuel.studyzen.feature.study.QuizScreen
import com.fredrickemmanuel.studyzen.feature.study.QuizSetupScreen
import com.fredrickemmanuel.studyzen.feature.study.ResearchScreen
import com.fredrickemmanuel.studyzen.feature.study.ToolHistoryScreen
import com.fredrickemmanuel.studyzen.feature.study.StudyEditorScreen
import com.fredrickemmanuel.studyzen.feature.study.StudyWizardScreen
import com.fredrickemmanuel.studyzen.feature.study.StudyLibraryScreen

/** Lets screens reach the sync engine (status, "Sync now", clearing data on log out). */
val LocalSyncEngine = staticCompositionLocalOf<SyncEngine?> { null }

/** Lets screens such as Profile reach the auth repository (log out). */
val LocalAuthRepository = staticCompositionLocalOf<AuthRepository?> { null }

/**
 * Decides which screen is visible and how screens lead to each other.
 *
 * Flow:
 *  Onboarding -> Login <-> SignUp -> VerifyCode(email) -> Main (tabs)
 *                            \-> ForgotPassword -> VerifyCode(reset) -> ResetPassword -> Login
 *
 * To connect the Node backend, replace [FakeAuthRepository] with a real [AuthRepository].
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    repository: AuthRepository = remember {
        if (ApiConfig.USE_FAKE_BACKEND) FakeAuthRepository() else RemoteAuthRepository()
    },
    accountRepository: AccountRepository = remember {
        if (ApiConfig.USE_FAKE_BACKEND) FakeAccountRepository() else RemoteAccountRepository()
    },
    googleClient: GoogleSignInClient = remember { FakeGoogleSignInClient() },
    reminderPlatform: ReminderPlatform = NoReminderPlatform,
    mediaPlatform: MediaPlatform = NoMediaPlatform,
) {
    val navigator = rememberAppNavigator(
        start = if (repository.currentUser != null || GuestMode.enabled) Screen.Main else Screen.Onboarding,
    )
    val store = rememberStudyStore()
    val aiSettings = remember { AiSettingsStore() }

    /** Shows the signed-in learner's real name and e-mail in the app. */
    fun syncProfile() {
        repository.currentUser?.let { user ->
            store.profile = store.profile.copy(name = user.name, email = user.email)
        }
    }
    LaunchedEffect(repository) { syncProfile() }

    val scope = rememberCoroutineScope()
    val syncEngine = remember(store, repository) {
        SyncEngine(store = store, transport = SyncApi(), signedInUserId = { repository.currentUser?.id })
    }

    // Keep the learner's data on the device whenever it changes.
    LaunchedEffect(store) {
        snapshotFlow { store.snapshot() }.collectLatest {
            delay(400)
            store.saveNow()
        }
    }
    // Back up to the server whenever something changes, and check for changes from other phones
    // every couple of minutes. Does nothing when logged out or offline: the app keeps working.
    LaunchedEffect(syncEngine) {
        launch {
            snapshotFlow { store.snapshot() }.collectLatest {
                delay(2_000)
                syncEngine.sync()
            }
        }
        while (true) {
            syncEngine.sync(force = true)
            delay(120_000)
        }
    }
    // Keep the phone's alarms in step with the sessions and notification settings.
    LaunchedEffect(store, reminderPlatform) {
        snapshotFlow { ReminderPlanner.plan(store.sessions.toList(), store.profile, todayEpochDay()) }
            .collect { reminderPlatform.sync(it) }
    }
    // A tapped reminder opens that session's timer once the learner is on the main screen.
    val pendingSession = DeepLinks.pendingSessionId
    LaunchedEffect(pendingSession, navigator.current) {
        if (pendingSession != null && navigator.current == Screen.Main) {
            DeepLinks.pendingSessionId = null
            if (store.session(pendingSession) != null) navigator.navigate(Screen.StudySession(pendingSession))
        }
    }
    val loggedIn = {
        GuestMode.enabled = false
        // drop sample data (or another account's leftovers) before showing this learner's app
        repository.currentUser?.let { syncEngine.prepareForLogin(it.id) }
        syncProfile()
        navigator.replaceAll(Screen.Main)
        scope.launch { syncEngine.sync(force = true) }
        Unit
    }
    var mainTab by rememberSaveable { mutableIntStateOf(0) }

    // System back button / back gesture (Android, iOS swipe-back, desktop and web where supported)
    BackHandler(enabled = navigator.canGoBack) { navigator.back() }

    CompositionLocalProvider(
        LocalAppNavigator provides navigator,
        LocalStudyStore provides store,
        LocalAuthRepository provides repository,
        LocalAccountRepository provides accountRepository,
        LocalSyncEngine provides syncEngine,
        LocalReminderPlatform provides reminderPlatform,
        LocalMediaPlatform provides mediaPlatform,
        LocalAiSettings provides aiSettings,
    ) {
        AnimatedContent(
            targetState = navigator.current,
            modifier = modifier,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
            label = "navigation",
        ) { screen ->
            when (screen) {
                Screen.Onboarding -> OnboardingScreen(onContinue = { navigator.navigate(Screen.Login) })

                Screen.Login -> LoginScreen(
                    repository = repository,
                    googleClient = googleClient,
                    onBack = navigator::back,
                    onLoggedIn = loggedIn,
                    onNeedsVerification = { email ->
                        navigator.navigate(Screen.VerifyCode(email, CodePurpose.VerifyEmail))
                    },
                    onForgotPassword = { navigator.navigate(Screen.ForgotPassword) },
                    onSignUp = { navigator.navigate(Screen.SignUp) },
                    onContinueAsGuest = {
                        // works fully on this device; a few example studies make the app easy to explore
                        GuestMode.enabled = true
                        if (store.studies.isEmpty() && store.sessions.isEmpty()) store.loadDemoData()
                        store.profile = store.profile.copy(name = "Guest learner")
                        navigator.replaceAll(Screen.Main)
                    },
                )

                Screen.SignUp -> SignUpScreen(
                    repository = repository,
                    googleClient = googleClient,
                    onBack = navigator::back,
                    onGoogleSignedIn = loggedIn,
                    onSignedUp = { email ->
                        store.profile = store.profile.copy(email = email)
                        navigator.navigate(Screen.VerifyCode(email, CodePurpose.VerifyEmail))
                    },
                    onLogin = navigator::back,
                )

                is Screen.VerifyCode -> VerifyCodeScreen(
                    email = screen.email,
                    purpose = screen.purpose,
                    repository = repository,
                    onBack = navigator::back,
                    onVerified = { code ->
                        when (screen.purpose) {
                            CodePurpose.VerifyEmail -> loggedIn()
                            CodePurpose.ResetPassword ->
                                navigator.replaceTop(Screen.ResetPassword(screen.email, code))
                        }
                    },
                )

                Screen.ForgotPassword -> ForgotPasswordScreen(
                    repository = repository,
                    onBack = navigator::back,
                    onCodeSent = { email ->
                        navigator.navigate(Screen.VerifyCode(email, CodePurpose.ResetPassword))
                    },
                )

                is Screen.ResetPassword -> ResetPasswordScreen(
                    email = screen.email,
                    code = screen.code,
                    repository = repository,
                    onBack = navigator::back,
                    onDone = { navigator.replaceAll(Screen.Login) },
                )

                Screen.Main -> MainScreen(selectedTab = mainTab, onTabChange = { mainTab = it })

                Screen.StudyLibrary -> StudyLibraryScreen()
                is Screen.StudyEditor -> if (screen.studyId == null) StudyWizardScreen() else StudyEditorScreen(screen.studyId)
                is Screen.StudyDetail -> StudyDetailScreen(screen.studyId)
                Screen.StartStudying -> StartStudyingScreen()
                is Screen.StudySession -> StudySessionScreen(screen.sessionId)
                is Screen.SessionSummary -> SessionSummaryScreen(screen.sessionId)
                is Screen.LearningPath -> LearningPathScreen(screen.studyId)
                is Screen.Lesson -> LessonScreen(screen.studyId, screen.lessonId)

                is Screen.PlanBuilder -> PlanBuilderScreen(screen.planId)
                is Screen.SessionEditor -> SessionEditorScreen(screen.sessionId, screen.epochDay)
                Screen.Notes -> NotesScreen()
                is Screen.NoteEditor -> NoteEditorScreen(screen.studyId, screen.noteId)
                is Screen.Calculator -> CalculatorScreen(screen.studyId, initialExpression = screen.expression)
                is Screen.Dictionary -> DictionaryScreen(screen.studyId, initialWord = screen.word)
                is Screen.Scanner -> ScannerScreen(screen.studyId, initialPrompt = null, scanId = screen.scanId, lessonId = screen.lessonId)
                is Screen.Assistant -> ToolHistoryScreen(screen.studyId, StudyTool.Chat, screen.lessonId)
                is Screen.ToolHistory -> ToolHistoryScreen(screen.studyId, screen.tool, screen.lessonId)
                is Screen.Research -> ResearchScreen(screen.studyId, screen.itemId)
                is Screen.QuizSetup -> QuizSetupScreen(screen.studyId, screen.lessonId)
                is Screen.Quiz -> QuizScreen(screen.studyId, screen.assessmentId)
                is Screen.QuizResult -> QuizResultScreen(screen.studyId, screen.assessmentId)
                is Screen.Solution -> ScannerScreen(screen.studyId, initialPrompt = screen.problem)
                Screen.AiSettings -> AiSettingsScreen()
                Screen.Reminders -> RemindersScreen()
                Screen.NotificationSettings -> NotificationSettingsScreen()
                Screen.NotificationPermission -> NotificationPermissionScreen()
                Screen.EditProfile -> EditProfileScreen()
                Screen.Security -> SecurityScreen()

                // Built in the next batches; each line below is replaced as its screen lands.
                else -> ComingSoon(screen.encode().substringBefore('|'))
            }
        }
    }
}

/** TEMPORARY stand-in for screens that are not built yet. */
@Composable
private fun ComingSoon(name: String) {
    val nav = LocalAppNavigator.current
    ScreenScaffold(title = "Coming soon", onBack = nav::back) {
        Spacer(Modifier.height(24.dp))
        EmptyState(
            title = "Not built yet",
            message = "The \"$name\" screen arrives in the next step.",
        )
    }
}
