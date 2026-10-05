package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AppFontFamily

@Composable
fun HeaderDoodleBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Soft pastel blue radial sparkle
        drawCircle(
            color = Color(0xFF2563EB).copy(alpha = 0.08f),
            radius = w * 0.4f,
            center = Offset(w * 0.5f, h * 0.4f)
        )

        val strokeW = 1.8.dp.toPx()
        val doodleColor = Color(0xFF2563EB).copy(alpha = 0.18f)

        // Top-left sparkle starburst
        val cx1 = w * 0.15f
        val cy1 = h * 0.22f
        val r1 = 12.dp.toPx()
        drawLine(doodleColor, Offset(cx1 - r1, cy1), Offset(cx1 + r1, cy1), strokeW)
        drawLine(doodleColor, Offset(cx1, cy1 - r1), Offset(cx1, cy1 + r1), strokeW)
        val diag = r1 * 0.6f
        drawLine(doodleColor, Offset(cx1 - diag, cy1 - diag), Offset(cx1 + diag, cy1 + diag), strokeW)
        drawLine(doodleColor, Offset(cx1 + diag, cy1 - diag), Offset(cx1 - diag, cy1 + diag), strokeW)

        // Top-right floating diamond dots
        drawCircle(color = doodleColor, radius = 4.dp.toPx(), center = Offset(w * 0.85f, h * 0.22f))
        drawCircle(color = doodleColor.copy(alpha = 0.12f), radius = 6.dp.toPx(), center = Offset(w * 0.82f, h * 0.35f))

        // Wavy vector doodle line on left
        val path = Path().apply {
            moveTo(w * 0.06f, h * 0.58f)
            quadraticTo(w * 0.12f, h * 0.52f, w * 0.18f, h * 0.58f)
            quadraticTo(w * 0.24f, h * 0.64f, w * 0.30f, h * 0.58f)
        }
        drawPath(
            path = path,
            color = doodleColor,
            style = Stroke(width = strokeW)
        )
    }
}

@Composable
fun WelcomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "badge_float")
    val floatOffsetY by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffsetY"
    )

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFDCEBFE), // Light soft pastel blue at top
                        Color(0xFFEFF6FF),
                        Color(0xFFF1F5F9)  // Clean white-grey bottom
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: App Branding Header with Unique Header Doodles
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                // Header Doodles Layer
                HeaderDoodleBackground(modifier = Modifier.matchParentSize())

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Centered KnotLink Top Bar Title (No small logo icon before text)
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "KnotLink",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = AppFontFamily,
                            color = Color(0xFF1E293B),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // 3D Floating KnotLink Logo Badge (Replacing Shield with KnotLink Logo)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(120.dp)
                            .offset(y = floatOffsetY.dp)
                    ) {
                        Canvas(modifier = Modifier.size(130.dp)) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF3B82F6).copy(alpha = 0.22f),
                                        Color.Transparent
                                    )
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF3B82F6),
                                            Color(0xFF2563EB),
                                            Color(0xFF1D4ED8)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = Color.White.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(28.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.appicon),
                                contentDescription = "KnotLink Logo",
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // "Welcome to KnotLink" in a Half-Rounded / Pill Badge Tag Container
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF2563EB).copy(alpha = 0.08f))
                            .border(
                                width = 1.dp,
                                color = Color(0xFF2563EB).copy(alpha = 0.25f),
                                shape = CircleShape
                            )
                            .padding(horizontal = 22.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Welcome to KnotLink",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Stacked UI Card Tile (Flat Clean White-Blue Mix Background with Doodles)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White,
                                Color(0xFFF8FAFC),
                                Color(0xFFEFF6FF)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White,
                                Color(0xFFDBEAFE)
                            )
                        ),
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    )
            ) {
                CardDoodleBackground(modifier = Modifier.matchParentSize())

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 28.dp, bottom = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Get Started",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Create a new account or log in with your existing account",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Primary CTA: Vibrant Solid Blue Capsule "Create Account"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF3B82F6),
                                        Color(0xFF2563EB)
                                    )
                                )
                            )
                            .clickable { onNavigateToRegister() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Create Account",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Secondary CTA: Light White-Blue Capsule "Log In"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(
                                width = 1.5.dp,
                                color = Color(0xFFCBD5E1),
                                shape = CircleShape
                            )
                            .clickable { onNavigateToLogin() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Log In",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Footer Link
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.clickable { onNavigateToForgotPassword() }
                    ) {
                        Text(
                            text = "Forgot password? ",
                            fontSize = 13.5.sp,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "Reset Password",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB)
                        )
                    }
                }
            }
        }
    }
}
