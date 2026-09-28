package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.local.ContactEntity
import com.example.util.DocumentCategory
import com.example.util.FileHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

// -----------------------------------------------------------------------------
// Data Models & Parsers for Real-time Polls
// -----------------------------------------------------------------------------

data class PollOptionData(
    val id: String,
    val text: String,
    val voterUids: List<String> = emptyList(),
    val voterNames: List<String> = emptyList(),
    val voterAvatars: List<String> = emptyList()
)

data class PollVoterInfo(
    val uid: String,
    val name: String,
    val avatar: String = ""
)

data class PollData(
    val title: String,
    val question: String = "",
    val allowMultiple: Boolean,
    val options: List<PollOptionData>,
    val latestVoters: List<PollVoterInfo> = emptyList()
)

object PollParser {
    fun parse(rawText: String): PollData {
        if (rawText.contains("[POLL_DATA|")) {
            try {
                val jsonStr = rawText.substringAfter("[POLL_DATA|").substringBeforeLast("]")
                val json = JSONObject(jsonStr)
                val title = json.optString("title", "Live Poll")
                val question = json.optString("question", json.optString("topic", ""))
                val allowMultiple = json.optBoolean("allowMultiple", false)
                val optionsArray = json.optJSONArray("options") ?: JSONArray()
                val options = mutableListOf<PollOptionData>()
                for (i in 0 until optionsArray.length()) {
                    val optObj = optionsArray.getJSONObject(i)
                    val id = optObj.optString("id", "opt_$i")
                    val text = optObj.optString("text", "Option ${i + 1}")
                    val voterUidsArr = optObj.optJSONArray("voterUids") ?: JSONArray()
                    val voterUids = mutableListOf<String>()
                    for (j in 0 until voterUidsArr.length()) voterUids.add(voterUidsArr.getString(j))

                    val voterNamesArr = optObj.optJSONArray("voterNames") ?: JSONArray()
                    val voterNames = mutableListOf<String>()
                    for (j in 0 until voterNamesArr.length()) voterNames.add(voterNamesArr.getString(j))

                    val voterAvatarsArr = optObj.optJSONArray("voterAvatars") ?: JSONArray()
                    val voterAvatars = mutableListOf<String>()
                    for (j in 0 until voterAvatarsArr.length()) voterAvatars.add(voterAvatarsArr.getString(j))

                    options.add(PollOptionData(id, text, voterUids, voterNames, voterAvatars))
                }

                val latestArr = json.optJSONArray("latestVoters") ?: JSONArray()
                val latestVoters = mutableListOf<PollVoterInfo>()
                for (i in 0 until latestArr.length()) {
                    val vObj = latestArr.getJSONObject(i)
                    latestVoters.add(
                        PollVoterInfo(
                            uid = vObj.optString("uid"),
                            name = vObj.optString("name"),
                            avatar = vObj.optString("avatar", "")
                        )
                    )
                }
                return PollData(title, question, allowMultiple, options, latestVoters)
            } catch (e: Exception) {
                android.util.Log.e("PollParser", "Error parsing json poll: ${e.message}")
            }
        }

        // Fallback for simple raw string poll
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val title = lines.firstOrNull()?.replace("📊 POLL:", "")?.replace("📊", "")?.trim() ?: "Poll Question"
        val parsedOptions = lines.drop(1).mapIndexed { idx, line ->
            val clean = line.replace(Regex("^[0-9️⃣1️⃣2️⃣3️⃣4️⃣5️⃣\\-*•]+\\s*"), "")
                .replace(Regex("\\s*\\(\\d+\\s*votes\\)"), "")
                .trim()
            PollOptionData(
                id = "opt_$idx",
                text = if (clean.isNotBlank()) clean else "Option ${idx + 1}"
            )
        }
        return PollData(
            title = title,
            question = "",
            allowMultiple = false,
            options = if (parsedOptions.isNotEmpty()) parsedOptions else listOf(
                PollOptionData("opt_0", "Option 1"),
                PollOptionData("opt_1", "Option 2")
            ),
            latestVoters = emptyList()
        )
    }

    fun serialize(pollData: PollData): String {
        val json = JSONObject()
        json.put("title", pollData.title)
        json.put("question", pollData.question)
        json.put("allowMultiple", pollData.allowMultiple)

        val optionsArr = JSONArray()
        for (opt in pollData.options) {
            val optObj = JSONObject()
            optObj.put("id", opt.id)
            optObj.put("text", opt.text)

            val uidsArr = JSONArray()
            opt.voterUids.forEach { uidsArr.put(it) }
            optObj.put("voterUids", uidsArr)

            val namesArr = JSONArray()
            opt.voterNames.forEach { namesArr.put(it) }
            optObj.put("voterNames", namesArr)

            val avatarsArr = JSONArray()
            opt.voterAvatars.forEach { avatarsArr.put(it) }
            optObj.put("voterAvatars", avatarsArr)

            optionsArr.put(optObj)
        }
        json.put("options", optionsArr)

        val latestArr = JSONArray()
        for (v in pollData.latestVoters) {
            val vObj = JSONObject()
            vObj.put("uid", v.uid)
            vObj.put("name", v.name)
            vObj.put("avatar", v.avatar)
            latestArr.put(vObj)
        }
        json.put("latestVoters", latestArr)

        return "📊 [POLL_DATA|$json]"
    }
}

// -----------------------------------------------------------------------------
// 1. Document / File Message Bubble (Modern Clean Cards, WhatsApp & Telegram style)
// -----------------------------------------------------------------------------

data class ParsedDocument(
    val fileName: String,
    val fileSizeFormatted: String,
    val mimeType: String,
    val downloadUrl: String,
    val extension: String
)

object DocumentMessageParser {
    fun parse(text: String): ParsedDocument {
        if (text.contains("[DOCUMENT_FILE|")) {
            val content = text.substringAfter("[DOCUMENT_FILE|").substringBefore("]")
            val parts = content.split("|")
            val fileName = parts.getOrNull(0)?.trim()?.ifBlank { "Document" } ?: "Document"
            val fileSize = parts.getOrNull(1)?.trim()?.ifBlank { "Unknown size" } ?: "Unknown size"
            val mimeType = parts.getOrNull(2)?.trim()?.ifBlank { "" } ?: ""
            val downloadUrl = parts.getOrNull(3)?.trim()?.ifBlank { "" } ?: ""
            val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
            return ParsedDocument(fileName, fileSize, mimeType, downloadUrl, ext)
        }

        // Fallback for legacy 📄 [Document: name] (url)
        val fileName = if (text.contains("[Document:")) {
            text.substringAfter("[Document:").substringBefore("]").trim()
        } else {
            text.replace("📄", "").substringBefore("(").trim().ifBlank { "Document" }
        }
        val downloadUrl = if (text.contains("(") && text.contains(")")) {
            text.substringAfter("(").substringBefore(")").trim()
        } else ""
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return ParsedDocument(fileName, "Document", "", downloadUrl, ext)
    }
}

@Composable
fun DocumentMessageBubble(
    messageText: String,
    isUser: Boolean,
    isNightMode: Boolean,
    animTextColor: Color
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val parsed = remember(messageText) { DocumentMessageParser.parse(messageText) }

    var isDownloading by remember { mutableStateOf(false) }
    var isDownloaded by remember { mutableStateOf(false) }
    var localCachedFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(parsed.downloadUrl, parsed.fileName) {
        val cacheDir = File(context.cacheDir, "documents")
        val sanitized = parsed.fileName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val testFile = File(cacheDir, sanitized)
        if (testFile.exists() && testFile.length() > 0) {
            localCachedFile = testFile
            isDownloaded = true
        } else if (parsed.downloadUrl.startsWith("file://")) {
            val f = File(Uri.parse(parsed.downloadUrl).path ?: "")
            if (f.exists()) {
                localCachedFile = f
                isDownloaded = true
            }
        }
    }

    val category = remember(parsed.extension, parsed.mimeType) {
        FileHelper.getCategory(parsed.extension, parsed.mimeType)
    }

    // Modern styled file badge colors and icons
    val (gradientColors, badgeIcon, badgeLabel) = when (category) {
        DocumentCategory.EXCEL -> Triple(listOf(Color(0xFF0F766E), Color(0xFF10B981)), Icons.Default.TableChart, "XLSX")
        DocumentCategory.PDF -> Triple(listOf(Color(0xFFDC2626), Color(0xFFF43F5E)), Icons.Default.PictureAsPdf, "PDF")
        DocumentCategory.WORD -> Triple(listOf(Color(0xFF1D4ED8), Color(0xFF3B82F6)), Icons.Default.Description, "DOCX")
        DocumentCategory.POWERPOINT -> Triple(listOf(Color(0xFFC2410C), Color(0xFFF97316)), Icons.Default.Slideshow, "PPTX")
        DocumentCategory.ZIP -> Triple(listOf(Color(0xFFB45309), Color(0xFFF59E0B)), Icons.Default.FolderZip, "ZIP")
        DocumentCategory.CODE_TEXT -> Triple(listOf(Color(0xFF4338CA), Color(0xFF6366F1)), Icons.Default.Code, if (parsed.extension.isNotBlank()) parsed.extension.uppercase() else "TXT")
        DocumentCategory.AUDIO -> Triple(listOf(Color(0xFF7E22CE), Color(0xFFA855F7)), Icons.Default.Audiotrack, "AUDIO")
        DocumentCategory.VIDEO -> Triple(listOf(Color(0xFFBE123C), Color(0xFFFB7185)), Icons.Default.Movie, "VIDEO")
        DocumentCategory.APK -> Triple(listOf(Color(0xFF047857), Color(0xFF10B981)), Icons.Default.Android, "APK")
        DocumentCategory.IMAGE -> Triple(listOf(Color(0xFF0E7490), Color(0xFF06B6D4)), Icons.Default.GridView, "IMG")
        DocumentCategory.GENERIC -> Triple(listOf(Color(0xFF475569), Color(0xFF64748B)), Icons.Default.InsertDriveFile, if (parsed.extension.isNotBlank()) parsed.extension.uppercase() else "FILE")
    }

    val bubbleShape = if (isUser) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }

    val cardBg = if (isNightMode) Color(0xFF1E202B) else Color.White
    val textColor = if (isNightMode) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isNightMode) Color(0xFF9CA3AF) else Color(0xFF64748B)

    fun handleOpenOrDownload() {
        if (isDownloading) return
        scope.launch {
            try {
                if (localCachedFile != null && localCachedFile!!.exists()) {
                    FileHelper.openFile(context, localCachedFile!!, parsed.mimeType)
                } else {
                    isDownloading = true
                    Toast.makeText(context, "Opening ${parsed.fileName}...", Toast.LENGTH_SHORT).show()
                    val downloaded = FileHelper.downloadOrGetLocalFile(context, parsed.downloadUrl, parsed.fileName)
                    isDownloading = false
                    if (downloaded != null && downloaded.exists()) {
                        localCachedFile = downloaded
                        isDownloaded = true
                        FileHelper.openFile(context, downloaded, parsed.mimeType)
                    } else {
                        Toast.makeText(context, "Failed to download document", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                isDownloading = false
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Surface(
        shape = bubbleShape,
        color = cardBg,
        shadowElevation = 3.dp,
        border = BorderStroke(
            1.dp,
            if (isUser) Color(0xFF2563EB).copy(alpha = 0.35f)
            else if (isNightMode) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)
        ),
        modifier = Modifier
            .width(270.dp)
            .padding(vertical = 2.dp)
            .clickable { handleOpenOrDownload() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Modern File Type Gradient Badge & Icon
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = badgeLabel,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = badgeLabel.take(4),
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // File Name and Size Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = parsed.fileName,
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${parsed.fileSizeFormatted} • $badgeLabel",
                    color = subTextColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Modern Action Button (Download / Open / Spinner)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDownloaded) Color(0xFF10B981).copy(alpha = 0.15f)
                        else Color(0xFF2563EB).copy(alpha = 0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF2563EB)
                    )
                } else if (isDownloaded) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open File",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download File",
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 2. Real-time Interactive Poll Bubble (Title Top, Topic/Question below, Real Profiles)
// -----------------------------------------------------------------------------

@Composable
fun InteractivePollBubble(
    rawPollText: String,
    isUser: Boolean,
    isNightMode: Boolean,
    animTextColor: Color,
    currentUid: String,
    currentUserName: String,
    currentUserAvatar: String = "",
    onVote: (updatedPollText: String) -> Unit
) {
    val pollData = remember(rawPollText) { PollParser.parse(rawPollText) }
    var showVotersDialog by remember { mutableStateOf<PollOptionData?>(null) }

    val totalUniqueVoters = remember(pollData) {
        pollData.options.flatMap { it.voterUids }.distinct().size
    }
    val totalVotes = remember(pollData) {
        pollData.options.sumOf { it.voterUids.size }
    }

    val bubbleShape = if (isUser) {
        RoundedCornerShape(22.dp, 22.dp, 4.dp, 22.dp)
    } else {
        RoundedCornerShape(22.dp, 22.dp, 22.dp, 4.dp)
    }

    val cardBg = if (isNightMode) Color(0xFF181A24) else Color(0xFFF8FAFC)
    val textColor = if (isNightMode) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isNightMode) Color(0xFF9CA3AF) else Color(0xFF64748B)

    Surface(
        shape = bubbleShape,
        color = cardBg,
        shadowElevation = 5.dp,
        border = BorderStroke(
            1.5.dp,
            Brush.linearGradient(
                listOf(Color(0xFF2563EB).copy(alpha = 0.5f), Color(0xFF8B5CF6).copy(alpha = 0.5f))
            )
        ),
        modifier = Modifier
            .width(285.dp)
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Poll Icon Badge + Title (Top) + Multiple Choice Tag + Total Voters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF8B5CF6)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Poll",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = pollData.title,
                        color = Color(0xFF2563EB),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (pollData.allowMultiple) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF2563EB).copy(alpha = 0.12f),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = "MULTIPLE",
                            color = Color(0xFF2563EB),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Question / Topic Text (Displayed right below Title)
            val displayQuestion = if (pollData.question.isNotBlank()) pollData.question else pollData.title
            Text(
                text = displayQuestion,
                color = textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Poll Options List with Real-time Voting & Percentages
            pollData.options.forEachIndexed { index, option ->
                val count = option.voterUids.size
                val percentage = if (totalVotes > 0) (count.toFloat() / totalVotes.toFloat()) else 0f
                val animatedProgress by animateFloatAsState(
                    targetValue = percentage,
                    animationSpec = tween(durationMillis = 400),
                    label = "PollBar_$index"
                )
                val isSelected = option.voterUids.contains(currentUid)

                Surface(
                    onClick = {
                        // Handle real-time vote toggle
                        val isAlreadyVotedForThis = option.voterUids.contains(currentUid)
                        val updatedOptions = if (pollData.allowMultiple) {
                            // Multiple choice toggle
                            pollData.options.map { opt ->
                                if (opt.id == option.id) {
                                    val uids = opt.voterUids.toMutableList()
                                    val names = opt.voterNames.toMutableList()
                                    val avatars = opt.voterAvatars.toMutableList()
                                    if (isAlreadyVotedForThis) {
                                        val idx = uids.indexOf(currentUid)
                                        if (idx != -1) {
                                            uids.removeAt(idx)
                                            if (idx < names.size) names.removeAt(idx)
                                            if (idx < avatars.size) avatars.removeAt(idx)
                                        }
                                    } else {
                                        uids.add(currentUid)
                                        names.add(currentUserName)
                                        avatars.add(currentUserAvatar)
                                    }
                                    opt.copy(voterUids = uids, voterNames = names, voterAvatars = avatars)
                                } else opt
                            }
                        } else {
                            // Single choice toggle
                            pollData.options.map { opt ->
                                val uids = opt.voterUids.filter { it != currentUid }.toMutableList()
                                val names = opt.voterNames.toMutableList()
                                val avatars = opt.voterAvatars.toMutableList()
                                if (opt.voterUids.contains(currentUid)) {
                                    val idx = opt.voterUids.indexOf(currentUid)
                                    if (idx != -1 && idx < names.size) names.removeAt(idx)
                                    if (idx != -1 && idx < avatars.size) avatars.removeAt(idx)
                                }
                                if (opt.id == option.id && !isAlreadyVotedForThis) {
                                    uids.add(currentUid)
                                    names.add(currentUserName)
                                    avatars.add(currentUserAvatar)
                                }
                                opt.copy(voterUids = uids, voterNames = names, voterAvatars = avatars)
                            }
                        }

                        // Maintain latest voters (up to 10 in data, display max 5 in UI)
                        val allVotedUids = updatedOptions.flatMap { it.voterUids }.distinct()
                        val updatedLatest = pollData.latestVoters.filter { allVotedUids.contains(it.uid) }.toMutableList()
                        if (allVotedUids.contains(currentUid)) {
                            val existingIdx = updatedLatest.indexOfFirst { it.uid == currentUid }
                            if (existingIdx != -1) {
                                updatedLatest.removeAt(existingIdx)
                            }
                            updatedLatest.add(0, PollVoterInfo(currentUid, currentUserName, currentUserAvatar))
                        }
                        val newPoll = pollData.copy(options = updatedOptions, latestVoters = updatedLatest.take(10))
                        onVote(PollParser.serialize(newPoll))
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color(0xFF2563EB).copy(alpha = 0.12f) else if (isNightMode) Color(0xFF232533) else Color.White,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) Color(0xFF2563EB) else if (isNightMode) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Background Progress Fill
                        if (totalVotes > 0 && animatedProgress > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedProgress)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) Color(0xFF2563EB).copy(alpha = 0.25f)
                                        else Color(0xFF94A3B8).copy(alpha = 0.15f)
                                    )
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Checkbox for Multiple Choice, Radio for Single Choice
                                if (pollData.allowMultiple) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFF2563EB) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFF2563EB) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = option.text,
                                    color = textColor,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (count > 0) {
                                    Text(
                                        text = "$count (${(percentage * 100).toInt()}%)",
                                        color = if (isSelected) Color(0xFF2563EB) else subTextColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = { showVotersDialog = option },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "View voters",
                                            tint = subTextColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Voter Summary & Real-time Latest Voters Avatars (MAX 5)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (totalVotes == 0) "Tap an option to vote" else "$totalVotes total votes",
                    color = subTextColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                // Overlapping avatars of latest voters (up to max 5, real profile icons)
                val displayedVoters = pollData.latestVoters.take(5)
                if (displayedVoters.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy((-6).dp)
                    ) {
                        displayedVoters.forEach { voter ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF2563EB), Color(0xFF7C3AED))
                                        )
                                    )
                                    .border(1.5.dp, cardBg, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (voter.avatar.isNotBlank()) {
                                    AsyncImage(
                                        model = voter.avatar,
                                        contentDescription = voter.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = voter.name.take(1).uppercase(),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Voters Breakdown Dialog
    showVotersDialog?.let { opt ->
        Dialog(onDismissRequest = { showVotersDialog = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isNightMode) Color(0xFF1E1F2B) else Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Voters: ${opt.text}",
                            color = textColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { showVotersDialog = null },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = textColor)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (opt.voterNames.isEmpty()) {
                        Text(
                            text = "No votes recorded yet for this option.",
                            color = subTextColor,
                            fontSize = 13.sp
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(opt.voterNames.zip(opt.voterUids)) { (vName, vUid) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2563EB)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = vName.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (vUid == currentUid) "$vName (You)" else vName,
                                        color = textColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 3. Poll Creation Dialog (Title, Question/Topic, Clean Multiple Choices toggle)
// -----------------------------------------------------------------------------

@Composable
fun PollCreatorDialog(
    isNightMode: Boolean,
    onDismiss: () -> Unit,
    onCreatePoll: (pollSerializedString: String) -> Unit
) {
    var pollQuestion by remember { mutableStateOf("") }
    var allowMultipleChoices by remember { mutableStateOf(false) }
    val optionsList = remember { mutableStateListOf("", "") }

    // Focus requesters for auto-focusing newly added option fields
    val focusRequesters = remember { mutableMapOf<Int, FocusRequester>() }
    var focusTargetIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(focusTargetIndex) {
        focusTargetIndex?.let { idx ->
            delay(100)
            focusRequesters[idx]?.requestFocus()
            focusTargetIndex = null
        }
    }

    val bgColor = if (isNightMode) Color(0xFF1E1F2B) else Color.White
    val textColor = if (isNightMode) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isNightMode) Color(0xFF9CA3AF) else Color(0xFF64748B)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = bgColor,
            shadowElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Create Live Poll 📊",
                        color = textColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = textColor)
                    }
                }

                // Box 2: Question / Topic
                OutlinedTextField(
                    value = pollQuestion,
                    onValueChange = { pollQuestion = it },
                    label = { Text("Question / Topic") },
                    placeholder = { Text("e.g. Which location do you recommend?") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = if (isNightMode) Color.White.copy(alpha = 0.2f) else Color(0xFFCBD5E1)
                    )
                )

                // Multiple choices row (Clean, sleek toggle without cut-off text)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isNightMode) Color(0xFF272732) else Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { allowMultipleChoices = !allowMultipleChoices }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Multiple choices",
                            color = textColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Switch(
                            checked = allowMultipleChoices,
                            onCheckedChange = { allowMultipleChoices = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF2563EB)
                            )
                        )
                    }
                }

                Text(
                    text = "Poll Choices",
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // Dynamic Options List
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    optionsList.forEachIndexed { index, optionVal ->
                        val requester = focusRequesters.getOrPut(index) { FocusRequester() }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = optionVal,
                                onValueChange = { newValue -> optionsList[index] = newValue },
                                label = { Text("Option ${index + 1}") },
                                placeholder = { Text("Choice ${index + 1}") },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(requester),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true
                            )
                            if (optionsList.size > 2) {
                                IconButton(
                                    onClick = {
                                        optionsList.removeAt(index)
                                        focusRequesters.remove(index)
                                    },
                                    modifier = Modifier.padding(start = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove Option",
                                        tint = Color(0xFFEF4444)
                                    )
                                }
                            }
                        }
                    }
                }

                // Add Option Button (Auto-focuses the newly created option box!)
                OutlinedButton(
                    onClick = {
                        if (optionsList.size < 10) {
                            optionsList.add("")
                            focusTargetIndex = optionsList.lastIndex
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF2563EB))
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Option",
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add Option (+)",
                        color = Color(0xFF2563EB),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Submit Button
                Button(
                    onClick = {
                        val validOptions = optionsList.map { it.trim() }.filter { it.isNotBlank() }
                        val effectiveTitle = "Live Poll"
                        val effectiveQuestion = pollQuestion.trim()
                        if (validOptions.size >= 2) {
                            val pollOptions = validOptions.mapIndexed { idx, optText ->
                                PollOptionData("opt_$idx", optText)
                            }
                            val poll = PollData(
                                title = effectiveTitle,
                                question = effectiveQuestion,
                                allowMultiple = allowMultipleChoices,
                                options = pollOptions,
                                latestVoters = emptyList()
                            )
                            onCreatePoll(PollParser.serialize(poll))
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Create & Share Poll", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 4. Contact Picker Full-Page Screen (No Dummy Contacts, Permission Flow First)
// -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactPickerFullPageSheet(
    isNightMode: Boolean,
    onDismiss: () -> Unit,
    onSelectContact: (name: String, phone: String) -> Unit
) {
    val context = LocalContext.current
    val bgColor = if (isNightMode) Color(0xFF0D0E12) else Color(0xFFF8FAFC)
    val cardBg = if (isNightMode) Color(0xFF1E202B) else Color.White
    val textColor = if (isNightMode) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isNightMode) Color(0xFF9CA3AF) else Color(0xFF64748B)

    var searchQuery by remember { mutableStateOf("") }
    val realDeviceContacts = remember { mutableStateListOf<ContactEntity>() }
    var isLoadingContacts by remember { mutableStateOf(false) }

    var hasContactPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_CONTACTS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    fun loadContactsFromDevice() {
        isLoadingContacts = true
        realDeviceContacts.clear()
        try {
            val resolver = context.contentResolver
            val cursor = resolver.query(
                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                "${android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )
            cursor?.use { c ->
                val nameIdx = c.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = c.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                val seenNumbers = mutableSetOf<String>()
                val items = mutableListOf<ContactEntity>()
                while (c.moveToNext()) {
                    val name = if (nameIdx >= 0) c.getString(nameIdx) ?: "Contact" else "Contact"
                    val num = if (numIdx >= 0) c.getString(numIdx) ?: "" else ""
                    val cleanNum = num.replace("\\s+".toRegex(), "")
                    if (cleanNum.isNotEmpty() && !seenNumbers.contains(cleanNum)) {
                        seenNumbers.add(cleanNum)
                        items.add(
                            ContactEntity(
                                id = "dev_${seenNumbers.size}",
                                name = name,
                                statusText = num,
                                isOnline = true,
                                isFavorite = false,
                                categoryLetter = name.take(1).uppercase()
                            )
                        )
                    }
                }
                realDeviceContacts.addAll(items)
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactPicker", "Error querying contacts: ${e.message}")
        }
        isLoadingContacts = false
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasContactPermission = isGranted
        if (isGranted) {
            loadContactsFromDevice()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasContactPermission) {
            permissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
        } else {
            loadContactsFromDevice()
        }
    }

    val filteredContacts = remember(realDeviceContacts.size, searchQuery) {
        if (searchQuery.isBlank()) realDeviceContacts
        else realDeviceContacts.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.statusText.contains(searchQuery, ignoreCase = true)
        }
    }

    // Full-screen Dialog / Page
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = bgColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Share Contact",
                            color = textColor,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (hasContactPermission) "${filteredContacts.size} contacts on device" else "Contact permission needed",
                            color = subTextColor,
                            fontSize = 12.sp
                        )
                    }
                }

                if (!hasContactPermission) {
                    // Clean Permission Required State
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2563EB).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Permission Required",
                            color = textColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "To share a contact from your phone, BitChat needs permission to access your device's contacts.",
                            color = subTextColor,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                permissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Allow Contact Access", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Full Page Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by name or phone number...", color = subTextColor, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = subTextColor)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = subTextColor)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = if (isNightMode) Color.White.copy(alpha = 0.12f) else Color(0xFFCBD5E1)
                        )
                    )

                    if (isLoadingContacts) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF2563EB))
                        }
                    } else if (filteredContacts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = subTextColor,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (searchQuery.isBlank()) "No device contacts found" else "No contacts match \"$searchQuery\"",
                                    color = subTextColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        // Full Page Real Contacts List
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredContacts, key = { it.id + it.statusText }) { contact ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelectContact(contact.name, contact.statusText)
                                            onDismiss()
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    color = cardBg,
                                    shadowElevation = 1.dp,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isNightMode) Color.White.copy(alpha = 0.05f) else Color(0xFFE2E8F0)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Letter Avatar Circle with Gradient
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(Color(0xFF10B981), Color(0xFF059669))
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = contact.name.take(1).uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = contact.name,
                                                color = textColor,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = contact.statusText,
                                                color = subTextColor,
                                                fontSize = 13.sp
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF2563EB).copy(alpha = 0.12f))
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "Share",
                                                color = Color(0xFF2563EB),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistCreatorDialog(
    isNightMode: Boolean,
    onDismiss: () -> Unit,
    onCreateChecklist: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    val items = remember { mutableStateListOf("Task 1", "Task 2") }
    var newItemText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.92f),
        containerColor = if (isNightMode) Color(0xFF151821) else Color(0xFFF8FAFC),
        scrimColor = Color.Black.copy(alpha = 0.48f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .widthIn(max = 44.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(if (isNightMode) Color.White.copy(alpha = 0.18f) else Color(0xFFD1D9E3))
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Create Checklist",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isNightMode) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        "Build a shared list everyone can tick off",
                        fontSize = 12.sp,
                        color = if (isNightMode) Color(0xFF9CA3AF) else Color(0xFF64748B),
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close", tint = if (isNightMode) Color.White else Color(0xFF475569))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Checklist title") },
                placeholder = { Text("e.g. Groceries, Project tasks…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF06B6D4),
                    unfocusedBorderColor = if (isNightMode) Color.White.copy(alpha = 0.16f) else Color(0xFFD3DAE4),
                    focusedLabelColor = Color(0xFF06B6D4),
                    unfocusedLabelColor = if (isNightMode) Color(0xFF9CA3AF) else Color(0xFF64748B),
                    focusedContainerColor = if (isNightMode) Color(0xFF1B1F2A) else Color.White,
                    unfocusedContainerColor = if (isNightMode) Color(0xFF1B1F2A) else Color.White
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Items", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isNightMode) Color.White else Color(0xFF0F172A))
                Text(items.size.toString() + "/50", fontSize = 12.sp, color = if (isNightMode) Color(0xFF94A3B8) else Color(0xFF64748B))
            }

            Spacer(modifier = Modifier.height(8.dp))

            items.forEachIndexed { idx, item ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isNightMode) Color(0xFF1B1F2A) else Color.White,
                    border = BorderStroke(1.dp, if (isNightMode) Color.White.copy(alpha = 0.07f) else Color(0xFFE1E7EF)),
                    shadowElevation = if (isNightMode) 0.dp else 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(start = 13.dp, end = 5.dp, top = 5.dp, bottom = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .border(1.5.dp, Color(0xFF06B6D4), RoundedCornerShape(9.dp))
                                .background(if (isNightMode) Color(0xFF06B6D4).copy(alpha = 0.10f) else Color(0xFFECFEFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text((idx + 1).toString(), color = Color(0xFF06B6D4), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(item, color = if (isNightMode) Color.White else Color(0xFF0F172A), fontSize = 14.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { if (items.size > 1) items.removeAt(idx) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, "Remove", tint = Color(0xFFEF4444), modifier = Modifier.size(17.dp))
                        }
                    }
                }
            }

            if (items.size < 50) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newItemText,
                        onValueChange = { newItemText = it },
                        placeholder = { Text("Add another item…") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF06B6D4),
                            unfocusedBorderColor = if (isNightMode) Color.White.copy(alpha = 0.14f) else Color(0xFFD3DAE4),
                            focusedContainerColor = if (isNightMode) Color(0xFF1B1F2A) else Color.White,
                            unfocusedContainerColor = if (isNightMode) Color(0xFF1B1F2A) else Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val clean = newItemText.trim()
                            if (clean.isNotEmpty() && items.size < 50) {
                                items.add(clean)
                                newItemText = ""
                            }
                        },
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(Color(0xFF06B6D4))
                    ) {
                        Icon(Icons.Default.Add, "Add item", tint = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    if (title.isBlank()) {
                        Toast.makeText(context, "Please enter a checklist title", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (items.isEmpty()) {
                        Toast.makeText(context, "Add at least 1 item", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val itemsJsonArray = items.mapIndexed { i, txt ->
                        JSONObject().apply {
                            put("id", i.toString())
                            put("text", txt)
                            put("checked", false)
                            put("checkedBy", "")
                            put("avatar", "")
                        }
                    }
                    val payloadObject = JSONObject().apply {
                        put("title", title)
                        put("items", JSONArray(itemsJsonArray))
                    }
                    onCreateChecklist("[CHECKLIST_JSON|$payloadObject]")
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(17.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4))
            ) {
                Icon(Icons.Default.CheckBox, null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send Checklist", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun InteractiveChecklistBubble(
    rawChecklistText: String,
    isUser: Boolean,
    isNightMode: Boolean,
    animTextColor: Color,
    currentUid: String,
    currentUserName: String,
    currentUserAvatar: String,
    onToggleItem: (updatedJson: String) -> Unit
) {
    val jsonStr = if (rawChecklistText.contains("[CHECKLIST_JSON|")) {
        rawChecklistText.substringAfter("[CHECKLIST_JSON|").substringBeforeLast("]")
    } else {
        rawChecklistText
    }

    var title by remember(jsonStr) { mutableStateOf("Checklist") }
    val itemList = remember(jsonStr) { mutableStateListOf<ChecklistItemData>() }

    LaunchedEffect(jsonStr) {
        itemList.clear()
        try {
            val jsonObj = org.json.JSONObject(jsonStr)
            title = jsonObj.optString("title", "Checklist")
            val arr = jsonObj.optJSONArray("items")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    itemList.add(
                        ChecklistItemData(
                            id = obj.optString("id", i.toString()),
                            text = obj.optString("text", ""),
                            checked = obj.optBoolean("checked", false),
                            checkedBy = obj.optString("checkedBy", ""),
                            checkedByAvatar = obj.optString("avatar", "")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            title = "Checklist"
        }
    }

    val doneCount = itemList.count { it.checked }
    val totalCount = itemList.size
    val progress = if (totalCount > 0) doneCount.toFloat() / totalCount else 0f

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isUser) Color(0xFF0E7490) else if (isNightMode) Color(0xFF121B28) else Color(0xFFECFEFF),
        border = BorderStroke(1.dp, if (isUser) Color.Transparent else Color(0xFF06B6D4).copy(alpha = 0.35f)),
        modifier = Modifier.widthIn(min = 230.dp, max = 290.dp).padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.CheckBox,
                    contentDescription = null,
                    tint = if (isUser) Color.White else Color(0xFF06B6D4),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = if (isUser) Color.White else (if (isNightMode) Color.White else Color(0xFF0F172A)),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Progress bar & text
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(if (isUser) Color.White.copy(0.25f) else Color(0xFF06B6D4).copy(0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(if (isUser) Color.White else Color(0xFF06B6D4))
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$doneCount/$totalCount",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isUser) Color.White.copy(0.9f) else (if (isNightMode) Color(0xFF94A3B8) else Color(0xFF475569))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Checklist Items List
            itemList.forEachIndexed { index, item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val newChecked = !item.checked
                            itemList[index] = item.copy(
                                checked = newChecked,
                                checkedBy = if (newChecked) currentUserName else "",
                                checkedByAvatar = if (newChecked) currentUserAvatar else ""
                            )
                            try {
                                val jsonObj = org.json.JSONObject()
                                jsonObj.put("title", title)
                                val arr = org.json.JSONArray()
                                itemList.forEach { itm ->
                                    val obj = org.json.JSONObject()
                                    obj.put("id", itm.id)
                                    obj.put("text", itm.text)
                                    obj.put("checked", itm.checked)
                                    obj.put("checkedBy", itm.checkedBy)
                                    obj.put("avatar", itm.checkedByAvatar)
                                    arr.put(obj)
                                }
                                jsonObj.put("items", arr)
                                val updatedPayload = "[CHECKLIST_JSON|${jsonObj.toString()}]"
                                onToggleItem(updatedPayload)
                            } catch (_: Exception) {}
                        }
                        .padding(vertical = 5.dp, horizontal = 2.dp)
                ) {
                    Icon(
                        imageVector = if (item.checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = null,
                        tint = if (item.checked) (if (isUser) Color.White else Color(0xFF06B6D4)) else (if (isUser) Color.White.copy(0.6f) else Color(0xFF94A3B8)),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.text,
                        fontSize = 13.5.sp,
                        color = if (item.checked) {
                            if (isUser) Color.White.copy(0.7f) else (if (isNightMode) Color.White.copy(0.5f) else Color.Gray)
                        } else {
                            if (isUser) Color.White else (if (isNightMode) Color.White else Color(0xFF0F172A))
                        },
                        style = androidx.compose.ui.text.TextStyle(
                            textDecoration = if (item.checked) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    if (item.checked && item.checkedBy.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF06B6D4)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (item.checkedByAvatar.isNotBlank()) {
                                coil.compose.AsyncImage(
                                    model = item.checkedByAvatar,
                                    contentDescription = item.checkedBy,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = item.checkedBy.take(1).uppercase(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class ChecklistItemData(
    val id: String,
    val text: String,
    val checked: Boolean,
    val checkedBy: String = "",
    val checkedByAvatar: String = ""
)
