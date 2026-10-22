package com.fredrickemmanuel.studyzen.feature.onboarding

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenLogo
import com.fredrickemmanuel.studyzen.core.designsystem.components.drawDoubleChevronUp
import com.fredrickemmanuel.studyzen.core.designsystem.theme.StudyZenGradients
import com.fredrickemmanuel.studyzen.core.designsystem.theme.StudyZenPalette
import com.fredrickemmanuel.studyzen.core.designsystem.theme.StudyZenTheme
import org.jetbrains.compose.resources.painterResource
import studyzen.shared.generated.resources.Res
import studyzen.shared.generated.resources.mascot_hero

/**
 * The single onboarding screen, reproducing the first screen of the design reference:
 * pale lavender gradient, logo top-left, the mascot composition, a bold two-line headline,
 * short supporting copy and a purple swipe-up handle at the bottom.
 *
 * Swiping up on the handle (or tapping it) calls [onContinue].
 */
@Composable
fun OnboardingScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = StudyZenTheme.spacing

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StudyZenGradients.Screen),
    ) {
        // Soft translucent orbs, as in the reference
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = StudyZenPalette.Lavender.copy(alpha = 0.16f),
                radius = 30.dp.toPx(),
                center = Offset(size.width * 0.20f, size.height * 0.28f),
            )
            drawCircle(
                color = StudyZenPalette.Lavender.copy(alpha = 0.22f),
                radius = 20.dp.toPx(),
                center = Offset(size.width * 0.93f, size.height * 0.34f),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.35f),
                radius = 120.dp.toPx(),
                center = Offset(size.width * 0.5f, size.height * 0.36f),
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            StudyZenLogo(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = spacing.xl, top = spacing.lg),
                markSize = 34.dp,
                textStyle = MaterialTheme.typography.titleLarge,
            )

            // The mascot takes whatever height is left, so it scales on any phone size.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = spacing.xxl, vertical = spacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.mascot_hero),
                    contentDescription = "StudyZen learning mascot with a book, graduation cap, lightbulb and checklist",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Text(
                text = "Daily Focus\nBetter Learning",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.xxl),
            )
            Spacer(Modifier.height(spacing.md))
            Text(
                text = "Learn better, focus stronger, and grow every day. " +
                    "Build strong study habits for lasting success.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.xxxl),
            )

            Spacer(Modifier.height(spacing.xl))

            SwipeUpHandle(
                onActivate = onContinue,
                modifier = Modifier.navigationBarsPadding(),
            )
        }
    }
}

/**
 * The purple hill with a double chevron at the bottom of the screen. Swipe up on it, or tap it,
 * to continue. The chevron bobs gently to hint that it can be swiped.
 */
@Composable
private fun SwipeUpHandle(
    onActivate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnActivate by rememberUpdatedState(onActivate)

    val transition = rememberInfiniteTransition(label = "handle-bob")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "handle-bob-offset",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
            .pointerInput(Unit) {
                var dragTotal = 0f
                detectVerticalDragGestures(
                    onDragStart = { dragTotal = 0f },
                    onDragEnd = { if (dragTotal < -48f) currentOnActivate() },
                    onVerticalDrag = { _, delta -> dragTotal += delta },
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = "Continue",
                role = Role.Button,
            ) { currentOnActivate() },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val centerX = w / 2f
            val top = h * 0.20f
            val halfBase = w * 0.34f

            // Faint lavender band across the bottom
            drawRect(
                color = StudyZenPalette.Lavender.copy(alpha = 0.22f),
                topLeft = Offset(0f, h * 0.72f),
                size = androidx.compose.ui.geometry.Size(w, h * 0.28f),
            )

            // The hill
            val hill = Path().apply {
                moveTo(centerX - halfBase, h)
                cubicTo(centerX - halfBase * 0.45f, h, centerX - halfBase * 0.55f, top, centerX, top)
                cubicTo(centerX + halfBase * 0.55f, top, centerX + halfBase * 0.45f, h, centerX + halfBase, h)
                close()
            }
            drawPath(
                path = hill,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF8B73EC), Color(0xFF6A4FD8)),
                    startY = top,
                    endY = h,
                ),
            )

            drawDoubleChevronUp(
                center = Offset(centerX, top + h * 0.26f + bob.dp.toPx()),
                width = 20.dp.toPx(),
                tint = Color.White,
            )
        }
    }
}
