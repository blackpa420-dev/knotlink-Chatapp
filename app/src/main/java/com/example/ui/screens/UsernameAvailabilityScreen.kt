package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserIdentityEntity
import com.example.data.supabase.SupabaseService
import com.example.ui.components.GlassPanel
import com.example.ui.theme.BitSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AvailabilityStatus {
    IDLE,
    CHECKING,
    AVAILABLE,
    TAKEN,
    INVALID
}

@Composable
fun UsernameAvailabilityScreen(
    user: UserIdentityEntity?,
    isNightMode: Boolean,
    onBack: () -> Unit,
    onApplyUsername: ((newUsername: String) -> Unit)? = null
) {
    val lockedUsername = user?.username?.trim().orEmpty()
    if (lockedUsername.isNotBlank()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = if (isNightMode) Color(0xFF07080B) else Color(0xFFF1F5F9)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Username locked",
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Username is permanently locked",
                    color = if (isNightMode) Color.White else Color(0xFF0F172A),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = lockedUsername,
                    color = Color(0xFF2563EB),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Your username cannot be changed after it is assigned.",
                    color = if (isNightMode) Color(0xFF9CA3AF) else Color(0xFF64748B),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Back",
                    color = Color(0xFF2563EB),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF2563EB).copy(alpha = 0.12f))
                        .clickable(onClick = onBack)
                        .padding(horizontal = 28.dp, vertical = 12.dp)
                )
            }
        }
        return
    }

    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var inputQuery by remember {
        val initial = user?.username?.removePrefix("@")?.removeSuffix(".link")?.removeSuffix(".bit")?.removeSuffix(".chat") ?: ""
        mutableStateOf(initial)
    }

    var status by remember { mutableStateOf(AvailabilityStatus.IDLE) }
    var statusMessage by remember { mutableStateOf("") }
    var currentTakenUserDisplay by remember { mutableStateOf<String?>(null) }
    var checkJob by remember { mutableStateOf<Job?>(null) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }

    fun generateSuggestions(base: String): List<String> {
        val clean = base.lowercase().filter { it.isLetterOrDigit() || it == '_' }
        if (clean.isBlank()) return emptyList()
        val randomSuffix = (10..99).random()
        return listOf(
            "${clean}_$randomSuffix",
            "${clean}_link",
            "the_$clean",
            "${clean}_official",
            "${clean}_knot"
        )
    }

    fun performCheck(text: String) {
        val clean = text.trim().removePrefix("@").lowercase().filter { it.isLetterOrDigit() || it == '_' }
        inputQuery = clean

        if (clean.isBlank()) {
            status = AvailabilityStatus.IDLE
            statusMessage = "Enter a username to check if it's available across the KnotLink network."
            suggestions = emptyList()
            currentTakenUserDisplay = null
            checkJob?.cancel()
            return
        }

        if (clean.length < 3) {
            status = AvailabilityStatus.INVALID
            statusMessage = "Username must be at least 3 characters long."
            suggestions = emptyList()
            currentTakenUserDisplay = null
            checkJob?.cancel()
            return
        }

        if (clean.length > 24) {
            status = AvailabilityStatus.INVALID
            statusMessage = "Username cannot exceed 24 characters."
            suggestions = emptyList()
            currentTakenUserDisplay = null
            checkJob?.cancel()
            return
        }

        status = AvailabilityStatus.CHECKING
        statusMessage = "Checking live database availability..."
        currentTakenUserDisplay = null
        checkJob?.cancel()

        checkJob = scope.launch {
            delay(350) // Debounce typing
            val fullHandle = if (clean.endsWith(".link")) clean else "$clean.link"
            val currentUid = if (!user?.supabaseUid.isNullOrBlank()) user?.supabaseUid ?: "" else user?.email ?: ""
            val myUsername = user?.username?.removePrefix("@")?.removeSuffix(".link")?.lowercase() ?: ""

            val isTakenRes = SupabaseService.isUsernameTaken(clean)
            if (isTakenRes.isSuccess) {
                val isTaken = isTakenRes.getOrDefault(false)
                if (isTaken) {
                    status = AvailabilityStatus.TAKEN
                    val prof = SupabaseService.getProfileByUsername(fullHandle).getOrNull()
                        ?: SupabaseService.getProfileByUsername(clean).getOrNull()
                    val isMine = clean == myUsername || (prof != null && (prof.id == currentUid || (prof.email.isNotBlank() && prof.email.equals(user?.email, ignoreCase = true))))
                    val holderName = if (isMine) "you (Your current username)" else (prof?.fullName?.ifBlank { prof.username } ?: "Another user")
                    currentTakenUserDisplay = holderName
                    statusMessage = if (isMine) {
                        "@$clean.link is already registered to you (Your current username)."
                    } else {
                        "@$clean.link is already registered to $holderName."
                    }
                    suggestions = generateSuggestions(clean)
                } else {
                    status = AvailabilityStatus.AVAILABLE
                    statusMessage = "Congratulations! @$clean.link is available to claim."
                    suggestions = emptyList()
                }
            } else {
                // Never show AVAILABLE when the remote availability check failed.
                status = AvailabilityStatus.INVALID
                statusMessage = "Could not verify username availability. Please check your connection and try again."
                currentTakenUserDisplay = null
                suggestions = emptyList()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (inputQuery.isNotBlank()) {
            performCheck(inputQuery)
        }
    }

    val animBgColor by animateColorAsState(
        targetValue = if (isNightMode) Color(0xFF07080B) else Color(0xFFF1F5F9),
        animationSpec = tween(400),
        label = "bg_anim"
    )

    val animTextColor by animateColorAsState(
        targetValue = if (isNightMode) Color(0xFFF3F4F6) else Color(0xFF1E293B),
        animationSpec = tween(400),
        label = "text_anim"
    )

    val animSubTextColor by animateColorAsState(
        targetValue = if (isNightMode) Color(0xFF9CA3AF) else Color(0xFF64748B),
        animationSpec = tween(400),
        label = "subtext_anim"
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = animBgColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = animTextColor
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Username Availability",
                        color = animTextColor,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Real-Time KnotLink Database Verification",
                        color = animSubTextColor,
                        fontSize = 11.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2563EB).copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "LIVE",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Input Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "Check Username Handle",
                    color = animTextColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                val borderHighlight = when (status) {
                    AvailabilityStatus.AVAILABLE -> Color(0xFF22C55E)
                    AvailabilityStatus.TAKEN -> Color(0xFFEF4444)
                    AvailabilityStatus.INVALID -> Color(0xFFF59E0B)
                    AvailabilityStatus.CHECKING -> Color(0xFF38BDF8)
                    AvailabilityStatus.IDLE -> if (isNightMode) Color(0xFF2A2D3D) else Color(0xFFCBD5E1)
                }

                GlassPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, borderHighlight, RoundedCornerShape(20.dp)),
                    cornerRadius = 20.dp,
                    backgroundColor = if (isNightMode) Color(0xFF0F111A) else Color.White,
                    borderColor = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    when (status) {
                                        AvailabilityStatus.AVAILABLE -> Color(0xFF22C55E).copy(alpha = 0.15f)
                                        AvailabilityStatus.TAKEN -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                        else -> Color(0xFF2563EB).copy(alpha = 0.15f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Handle",
                                tint = when (status) {
                                    AvailabilityStatus.AVAILABLE -> Color(0xFF22C55E)
                                    AvailabilityStatus.TAKEN -> Color(0xFFEF4444)
                                    else -> Color(0xFF2563EB)
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            if (inputQuery.isEmpty()) {
                                Text(
                                    text = "Enter username handle (e.g. john)",
                                    color = animSubTextColor.copy(alpha = 0.6f),
                                    fontSize = 16.sp
                                )
                            }

                            BasicTextField(
                                value = inputQuery,
                                onValueChange = { performCheck(it) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(
                                    color = animTextColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                cursorBrush = SolidColor(BitSecondary),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
                            )
                        }

                        if (inputQuery.isNotBlank()) {
                            Text(
                                text = ".link",
                                color = Color(0xFF2563EB),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            IconButton(
                                onClick = {
                                    inputQuery = ""
                                    performCheck("")
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = animSubTextColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status Display Card
                AnimatedVisibility(
                    visible = status != AvailabilityStatus.IDLE,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    when (status) {
                        AvailabilityStatus.CHECKING -> {
                            GlassPanel(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 16.dp,
                                backgroundColor = if (isNightMode) Color(0xFF0F172A) else Color(0xFFF0F9FF),
                                borderColor = Color(0xFF38BDF8).copy(alpha = 0.4f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF38BDF8),
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        text = statusMessage,
                                        color = animTextColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        AvailabilityStatus.AVAILABLE -> {
                            GlassPanel(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 16.dp,
                                backgroundColor = if (isNightMode) Color(0xFF062E1C) else Color(0xFFECFDF5),
                                borderColor = Color(0xFF22C55E).copy(alpha = 0.5f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Available",
                                            tint = Color(0xFF22C55E),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "AVAILABLE",
                                            color = Color(0xFF22C55E),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "@${inputQuery}.link is available.",
                                        color = animTextColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        AvailabilityStatus.TAKEN -> {
                            GlassPanel(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 16.dp,
                                backgroundColor = if (isNightMode) Color(0xFF330E0E) else Color(0xFFFEF2F2),
                                borderColor = Color(0xFFEF4444).copy(alpha = 0.5f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Not Available",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "NOT AVAILABLE",
                                            color = Color(0xFFEF4444),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = statusMessage,
                                        color = animTextColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        AvailabilityStatus.INVALID -> {
                            GlassPanel(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 16.dp,
                                backgroundColor = if (isNightMode) Color(0xFF2A1C0E) else Color(0xFFFFFBEB),
                                borderColor = Color(0xFFF59E0B).copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Invalid",
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = statusMessage,
                                        color = animTextColor,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        else -> {}
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Information Card
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp,
                    backgroundColor = if (isNightMode) Color(0xFF0F1018) else Color(0xFFF8FAFC),
                    borderColor = if (isNightMode) Color(0xFF1E2130) else Color(0xFFE2E8F0)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Rules",
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "KnotLink Handle Guidelines",
                                color = animTextColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val rules = listOf(
                            "Handles are globally unique identifiers across KnotLink.",
                            "Lengths between 3 to 24 characters are supported.",
                            "Allowed characters: lowercase letters (a-z), digits (0-9), and underscores (_).",
                            "Others can discover and message you directly using your @handle.link."
                        )

                        rules.forEach { rule ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "•",
                                    color = Color(0xFF2563EB),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = rule,
                                    color = animSubTextColor,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
