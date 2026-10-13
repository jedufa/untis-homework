package com.untis.homework

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.min

data class ThemeProfile(
  val key: String,
  val label: String,
  val background: String,
  val primary: String,
  val secondary: String,
  val late: String,
  val done: String,
  val darkBackground: String,
)

data class ChildAppearance(
  val theme: String = "ocean",
  val bgColor: String = "",
) {
  fun themeProfile(): ThemeProfile = themeProfiles.firstOrNull { it.key == theme } ?: themeProfiles.first()
  fun backgroundHex(darkTheme: Boolean = false): String {
    val customBackground = normalizeHexColor(bgColor)
    return if (!darkTheme && customBackground != null) customBackground else if (darkTheme) themeProfile().darkBackground else themeProfile().background
  }
  fun accentHex(darkTheme: Boolean = false): String = if (darkTheme) themeProfile().accentForDark() else themeProfile().primary
}

private val themeProfiles = listOf(
  ThemeProfile("ocean", "Ocean", "#edf2f5", "#0d747a", "#169590", "#af3649", "#4c7555", "#0f2024"),
  ThemeProfile("berry", "Berry", "#f4eef4", "#8d3d76", "#c05d8e", "#b2394a", "#497358", "#281923"),
  ThemeProfile("forest", "Forest", "#eef3ef", "#327052", "#66a06b", "#a84a3d", "#376d48", "#14241a"),
  ThemeProfile("graphite", "Graphite", "#eef0f2", "#3f5666", "#6c8493", "#a74451", "#4f7057", "#171d22"),
  ThemeProfile("sunrise", "Sunrise", "#f7f1ea", "#b25742", "#e09a53", "#a33c4a", "#4f7a59", "#281d17"),
  ThemeProfile("mint", "Mint", "#edf6f1", "#187b70", "#5bbf9d", "#aa4050", "#3f7b59", "#10231f"),
  ThemeProfile("paper", "Paper", "#f4f2ec", "#6d5a46", "#a48b65", "#a4404c", "#526f53", "#221f1a"),
  ThemeProfile("night", "Night", "#e9edf3", "#394b7a", "#7572b7", "#a63d5b", "#3f7463", "#111827"),
)

private val colorSwatches = listOf("#ffcc00", "#66a06b", "#5bbf9d", "#8d3d76", "#e09a53", "#3f5666", "#ffffff")

internal fun normalizeHexColor(value: String?): String? {
  val text = value?.trim().orEmpty()
  if (!Regex("^#[0-9a-fA-F]{6}$").matches(text)) return null
  return text.lowercase(Locale.ROOT)
}

internal fun colorFromHex(value: String?): Color? {
  val normalized = normalizeHexColor(value) ?: return null
  return runCatching { Color(android.graphics.Color.parseColor(normalized)) }.getOrNull()
}

internal fun softenedColor(value: String?): Color? = colorFromHex(value)?.copy(alpha = 0.24f)

internal fun ChildAppearance.toJsonString(): String {
  return JSONObject()
    .put("theme", theme)
    .put("bgColor", bgColor)
    .toString()
}

internal fun childAppearanceFromJson(rawJson: String?): ChildAppearance {
  val raw = rawJson ?: return ChildAppearance()
  val obj = runCatching { JSONObject(raw) }.getOrNull() ?: return ChildAppearance()
  val theme = obj.optString("theme", "ocean").takeIf { value -> themeProfiles.any { it.key == value } } ?: "ocean"
  return ChildAppearance(
    theme = theme,
    bgColor = normalizeHexColor(obj.optString("bgColor")).orEmpty(),
  )
}

internal fun subjectStylesToJson(styles: Map<String, Map<String, String>>): String {
  val root = JSONObject()
  styles.toSortedMap().forEach { (childKey, childStyles) ->
    val child = JSONObject()
    childStyles.toSortedMap().forEach { (subjectKey, color) ->
      normalizeHexColor(color)?.let { child.put(subjectKey, it) }
    }
    if (child.length() > 0) root.put(childKey, child)
  }
  return root.toString()
}

internal fun subjectStylesFromJson(rawJson: String?): Map<String, Map<String, String>> {
  val raw = rawJson ?: return emptyMap()
  val root = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyMap()
  return root.keys().asSequence().mapNotNull { childKey ->
    val child = root.optJSONObject(childKey) ?: return@mapNotNull null
    val styles = child.keys().asSequence().mapNotNull { subjectKey ->
      normalizeHexColor(child.optString(subjectKey))?.let { subjectKey to it }
    }.toMap()
    if (styles.isEmpty()) null else childKey to styles
  }.toMap()
}

class MainActivity : ComponentActivity() {
  private val viewModel by viewModels<UntisViewModel> {
    object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return UntisViewModel(application) as T
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    setContent {
      val state by viewModel.state.collectAsState()
      UntisTheme(appearance = state.activeAppearance()) {
        UntisApp(viewModel, state)
      }
    }
  }
}

data class HomeworkPayload(
  val fetchedAt: String,
  val windowStart: String,
  val windowEnd: String,
  val children: List<ChildHomeworks>,
)

data class ChildHomeworks(
  val key: String,
  val displayName: String,
  val homeworks: List<HomeworkItem>,
)

data class HomeworkItem(
  val id: String,
  val subject: String,
  val subjectLongName: String,
  val teacher: String,
  val postedDate: String,
  val dueDate: String,
  val dueDateLabel: String,
  val text: String,
  val status: HomeworkStatus,
  val isPastDue: Boolean,
  val isArchived: Boolean,
  val attachments: List<String>,
)

private data class SubjectStyleTarget(
  val childKey: String,
  val subjectKey: String,
  val label: String,
)

enum class HomeworkStatus {
  Open,
  Completed,
  NotDone,
}

enum class HomeworkFilter {
  Open,
  Archive,
  All,
}

data class UntisUiState(
  val payload: HomeworkPayload? = null,
  val activeChildKey: String = "",
  val filter: HomeworkFilter = HomeworkFilter.Open,
  val loading: Boolean = false,
  val savingIds: Set<String> = emptySet(),
  val appearances: Map<String, ChildAppearance> = emptyMap(),
  val subjectStyles: Map<String, Map<String, String>> = emptyMap(),
  val error: String? = null,
  val lastLoadedFromCache: Boolean = false,
)

class UntisViewModel(application: Application) : AndroidViewModel(application) {
  private val prefs = application.getSharedPreferences("homework-offline-demo", Context.MODE_PRIVATE)
  private val fixture = application.assets.open("homework.json").bufferedReader().use { it.readText() }
  private var raw = prefs.getString("homework", null) ?: fixture
  private val _state = MutableStateFlow(UntisUiState(
    payload = HomeworkJsonCodec.parse(raw), activeChildKey = prefs.getString("active", "child1") ?: "child1",
    appearances = listOf("child1", "child2").associateWith { childAppearanceFromJson(prefs.getString("appearance_$it", null)) },
    subjectStyles = subjectStylesFromJson(prefs.getString("styles", null)),
  ))
  val state: StateFlow<UntisUiState> = _state
  fun refresh() { _state.update { it.copy(payload = HomeworkJsonCodec.parse(raw), error = null) } }
  fun setActiveChild(key: String) { prefs.edit().putString("active", key).apply(); _state.update { it.copy(activeChildKey = key) } }
  fun setFilter(filter: HomeworkFilter) { _state.update { it.copy(filter = filter) } }
  fun setHomeworkCompleted(childKey: String, homework: HomeworkItem, completed: Boolean) {
    raw = HomeworkJsonCodec.updateHomeworkStatus(raw, childKey, homework.id, if (completed) HomeworkStatus.Completed else HomeworkStatus.Open)
    prefs.edit().putString("homework", raw).apply(); refresh()
  }
  fun saveAppearance(childKey: String, appearance: ChildAppearance) {
    prefs.edit().putString("appearance_$childKey", appearance.toJsonString()).apply()
    _state.update { it.copy(appearances = it.appearances + (childKey to appearance)) }
  }
  fun setSubjectColor(childKey: String, subjectKey: String, color: String) {
    val value = normalizeHexColor(color) ?: return
    val next = _state.value.subjectStyles + (childKey to (_state.value.subjectStyles[childKey].orEmpty() + (subjectKey to value)))
    prefs.edit().putString("styles", subjectStylesToJson(next)).apply(); _state.update { it.copy(subjectStyles = next) }
  }
  fun resetSubjectColor(childKey: String, subjectKey: String) {
    val next = _state.value.subjectStyles + (childKey to (_state.value.subjectStyles[childKey].orEmpty() - subjectKey))
    prefs.edit().putString("styles", subjectStylesToJson(next)).apply(); _state.update { it.copy(subjectStyles = next) }
  }
}

private fun HomeworkPayload.withHomework(
  childKey: String,
  homeworkId: String,
  update: (HomeworkItem) -> HomeworkItem,
): HomeworkPayload {
  return copy(
    children = children.map { child ->
      if (child.key != childKey) return@map child
      child.copy(
        homeworks = child.homeworks.map { homework ->
          if (homework.id == homeworkId) update(homework) else homework
        }
      )
    }
  )
}

private fun UntisUiState.activeChild(): ChildHomeworks? {
  return payload?.children?.firstOrNull { it.key == activeChildKey }
    ?: payload?.children?.firstOrNull()
}

private fun UntisUiState.activeAppearance(): ChildAppearance {
  val childKey = activeChild()?.key ?: activeChildKey
  return appearances[childKey] ?: ChildAppearance()
}

object HomeworkJsonCodec {
  fun parse(rawJson: String): HomeworkPayload {
    val root = JSONObject(rawJson)
    val window = root.optJSONObject("window") ?: JSONObject()
    val childrenObject = root.optJSONObject("children") ?: JSONObject()
    val childKeys = childrenObject.keys().asSequence().toList().sorted()
    return HomeworkPayload(
      fetchedAt = root.optString("fetchedAt"),
      windowStart = window.optString("start"),
      windowEnd = window.optString("end"),
      children = childKeys.map { key ->
        val child = childrenObject.optJSONObject(key) ?: JSONObject()
        val homeworksArray = child.optJSONArray("homeworks")
        ChildHomeworks(
          key = key,
          displayName = child.optString("displayName", key),
          homeworks = (0 until (homeworksArray?.length() ?: 0)).mapNotNull { index ->
            val item = homeworksArray?.optJSONObject(index) ?: return@mapNotNull null
            HomeworkItem(
              id = item.opt("id")?.toString().orEmpty(),
              subject = item.optString("subject"),
              subjectLongName = item.optString("subjectLongName"),
              teacher = item.optString("teacher"),
              postedDate = item.optString("postedDate"),
              dueDate = item.optString("dueDate"),
              dueDateLabel = item.optString("dueDateLabel"),
              text = item.optString("text"),
              status = parseStatus(item.optString("status")),
              isPastDue = item.optBoolean("isPastDue", false),
              isArchived = item.optBoolean("isArchived", false),
              attachments = item.optJSONArray("attachments")?.let { attachments ->
                (0 until attachments.length()).map { attachments.optString(it) }.filter { it.isNotBlank() }
              } ?: emptyList(),
            )
          }
        )
      },
    )
  }

  fun updateHomeworkStatus(
    rawJson: String,
    childKey: String,
    homeworkId: String,
    status: HomeworkStatus,
  ): String {
    val root = runCatching { JSONObject(rawJson) }.getOrElse { return rawJson }
    val homeworks = root.optJSONObject("children")
      ?.optJSONObject(childKey)
      ?.optJSONArray("homeworks")
      ?: return rawJson
    for (index in 0 until homeworks.length()) {
      val item = homeworks.optJSONObject(index) ?: continue
      if (item.opt("id")?.toString() != homeworkId) continue
      val statusValue = when (status) {
        HomeworkStatus.Open -> "open"
        HomeworkStatus.Completed -> "completed"
        HomeworkStatus.NotDone -> "not_done"
      }
      item
        .put("status", statusValue)
        .put("isArchived", status != HomeworkStatus.Open)
      return root.toString()
    }
    return rawJson
  }

  private fun parseStatus(value: String): HomeworkStatus {
    return when (value) {
      "completed" -> HomeworkStatus.Completed
      "not_done" -> HomeworkStatus.NotDone
      else -> HomeworkStatus.Open
    }
  }
}

private val untisLightColorScheme = lightColorScheme(
  primary = Color(0xFF385F45),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFDDE8DC),
  onPrimaryContainer = Color(0xFF173322),
  secondary = Color(0xFFD2761C),
  onSecondary = Color.White,
  surface = Color(0xFFFFFBF4),
  surfaceVariant = Color(0xFFE7DED4),
  background = Color(0xFFF5F1EA),
  error = Color(0xFF9B3644),
)

private val untisDarkColorScheme = darkColorScheme(
  primary = Color(0xFF8FD3C8),
  onPrimary = Color(0xFF06343A),
  primaryContainer = Color(0xFF184D54),
  onPrimaryContainer = Color(0xFFD8FAF6),
  secondary = Color(0xFFE4B57A),
  onSecondary = Color(0xFF422705),
  tertiary = Color(0xFF9AC7A4),
  onTertiary = Color(0xFF11351D),
  surface = Color(0xFF171D22),
  surfaceVariant = Color(0xFF263039),
  background = Color(0xFF0F171B),
  error = Color(0xFFFFB3BE),
  onSurface = Color(0xFFE7EEF2),
  onSurfaceVariant = Color(0xFFBBC7CE),
)

internal fun untisColorScheme(appearance: ChildAppearance, darkTheme: Boolean): ColorScheme {
  val profile = appearance.themeProfile()
  return if (darkTheme) {
    darkColorScheme(
      primary = colorFromHex(appearance.accentHex(darkTheme = true)) ?: untisDarkColorScheme.primary,
      onPrimary = Color(0xFF061F24),
      primaryContainer = colorFromHex(profile.primary)?.copy(alpha = 0.42f) ?: untisDarkColorScheme.primaryContainer,
      onPrimaryContainer = Color(0xFFE7FAF8),
      secondary = colorFromHex(profile.secondary) ?: untisDarkColorScheme.secondary,
      onSecondary = Color(0xFF1F1608),
      tertiary = colorFromHex(profile.done) ?: untisDarkColorScheme.tertiary,
      onTertiary = Color(0xFF071F11),
      surface = untisDarkColorScheme.surface,
      surfaceVariant = untisDarkColorScheme.surfaceVariant,
      background = colorFromHex(appearance.backgroundHex(darkTheme = true)) ?: untisDarkColorScheme.background,
      error = colorFromHex(profile.late) ?: untisDarkColorScheme.error,
      onSurface = untisDarkColorScheme.onSurface,
      onSurfaceVariant = untisDarkColorScheme.onSurfaceVariant,
    )
  } else {
    lightColorScheme(
      primary = colorFromHex(appearance.accentHex()) ?: untisLightColorScheme.primary,
      onPrimary = Color.White,
      primaryContainer = softenedColor(profile.primary) ?: untisLightColorScheme.primaryContainer,
      onPrimaryContainer = colorFromHex(profile.primary) ?: untisLightColorScheme.onPrimaryContainer,
      secondary = colorFromHex(profile.secondary) ?: untisLightColorScheme.secondary,
      onSecondary = Color.White,
      tertiary = colorFromHex(profile.done) ?: Color(0xFF4C7555),
      onTertiary = Color.White,
      surface = Color(0xFFFFFBF4),
      surfaceVariant = Color(0xFFE7DED4),
      background = colorFromHex(appearance.backgroundHex()) ?: untisLightColorScheme.background,
      error = colorFromHex(profile.late) ?: untisLightColorScheme.error,
    )
  }
}

private fun ThemeProfile.accentForDark(): String = when (key) {
  "ocean" -> "#6bd6d0"
  "berry" -> "#f0a7cb"
  "forest" -> "#94d99b"
  "graphite" -> "#a9c0cf"
  "sunrise" -> "#f0b477"
  "mint" -> "#8ee3c2"
  "paper" -> "#d6bf96"
  "night" -> "#b8b4ff"
  else -> primary
}

@Composable
fun UntisTheme(appearance: ChildAppearance = ChildAppearance(), content: @Composable () -> Unit) {
  val darkTheme = isSystemInDarkTheme()
  val colorScheme = untisColorScheme(appearance, darkTheme)
  MaterialTheme(
    colorScheme = colorScheme,
    typography = MaterialTheme.typography,
    content = content,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UntisApp(viewModel: UntisViewModel, state: UntisUiState) {
  var settingsOpen by remember { mutableStateOf(false) }
  var subjectStyleTarget by remember { mutableStateOf<SubjectStyleTarget?>(null) }
  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .safeDrawingPadding(),
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("Untis Homework", maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
              text = "Offline demo · fictional homework",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        },
        actions = {
          IconButton(onClick = viewModel::refresh, enabled = !state.loading) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
          }
          IconButton(onClick = { settingsOpen = true }) {
            Icon(Icons.Default.Settings, contentDescription = "Settings")
          }
        },
      )
    },
  ) { padding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(padding),
    ) {
      HomeworkScreen(
        state = state,
        onChildSelected = viewModel::setActiveChild,
        onFilterSelected = viewModel::setFilter,
        onRefresh = viewModel::refresh,
        onToggleComplete = viewModel::setHomeworkCompleted,
        onCustomizeSubject = { target -> subjectStyleTarget = target },
      )
      if (state.loading) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
      }
    }
  }

  if (settingsOpen) {
    AlertDialog(onDismissRequest = { settingsOpen = false }, title = { Text("Make it yours") },
      text = { Column { state.activeChild()?.let { child ->
        AppearanceSettings(child, state.activeAppearance(), viewModel::saveAppearance)
        SubjectColorSettings(child, state.subjectStyles[child.key].orEmpty(), viewModel::setSubjectColor, viewModel::resetSubjectColor)
      } } }, confirmButton = { TextButton(onClick = { settingsOpen = false }) { Text("Close") } })
  }

  subjectStyleTarget?.let { target ->
    SubjectStyleDialog(
      target = target,
      savedColor = state.subjectStyles[target.childKey]?.get(target.subjectKey),
      onDismiss = { subjectStyleTarget = null },
      onSetSubjectColor = viewModel::setSubjectColor,
      onResetSubjectColor = viewModel::resetSubjectColor,
    )
  }
}

@Composable
private fun HomeworkScreen(
  state: UntisUiState,
  onChildSelected: (String) -> Unit,
  onFilterSelected: (HomeworkFilter) -> Unit,
  onRefresh: () -> Unit,
  onToggleComplete: (String, HomeworkItem, Boolean) -> Unit,
  onCustomizeSubject: (SubjectStyleTarget) -> Unit,
) {
  val payload = state.payload
  if (payload == null) {
    EmptyState(
      error = state.error,
      onRefresh = onRefresh,
    )
    return
  }

  val activeChild = payload.children.firstOrNull { it.key == state.activeChildKey }
    ?: payload.children.firstOrNull()
  if (activeChild == null) {
    EmptyState(
      error = state.error ?: "No children were returned by the Untis API.",
      onRefresh = onRefresh,
    )
    return
  }
  val activeSubjectStyles = state.subjectStyles[activeChild.key].orEmpty()

  Column(modifier = Modifier.fillMaxSize()) {
    if (payload.children.size > 1) {
      TabRow(selectedTabIndex = payload.children.indexOf(activeChild).coerceAtLeast(0)) {
        payload.children.forEach { child ->
          Tab(
            selected = child.key == activeChild.key,
            onClick = { onChildSelected(child.key) },
            text = {
              Text(
                text = child.displayName.shortChildName(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            },
          )
        }
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      item {
        SummaryCard(
          child = activeChild,
          payload = payload,
          state = state,
          onFilterSelected = onFilterSelected,
        )
      }

      state.error?.let { error ->
        item {
          InlineError(error = error, cached = state.lastLoadedFromCache)
        }
      }

      val rows = activeChild.homeworks.filtered(state.filter)
      if (rows.isEmpty()) {
        item {
          EmptyListCard(filter = state.filter)
        }
      } else {
        items(rows, key = { "${activeChild.key}:${it.id}" }) { homework ->
          HomeworkCard(
            childKey = activeChild.key,
            homework = homework,
            subjectColor = colorFromHex(activeSubjectStyles[homework.subjectKey()]),
            saving = "${activeChild.key}:${homework.id}" in state.savingIds,
            onToggleComplete = onToggleComplete,
            onCustomizeSubject = onCustomizeSubject,
          )
        }
      }
    }
  }
}

@Composable
private fun SummaryCard(
  child: ChildHomeworks,
  payload: HomeworkPayload,
  state: UntisUiState,
  onFilterSelected: (HomeworkFilter) -> Unit,
) {
  val open = child.homeworks.count { it.status == HomeworkStatus.Open }
  val complete = child.homeworks.count { it.status == HomeworkStatus.Completed }
  val missed = child.homeworks.count { it.status == HomeworkStatus.NotDone }
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(8.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = child.displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "Window ${payload.windowStart.ifBlank { "unknown" }} to ${payload.windowEnd.ifBlank { "unknown" }}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Icon(
          imageVector = Icons.Default.School,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        StatPill(label = "Open", value = open, color = MaterialTheme.colorScheme.primary)
        StatPill(label = "Done", value = complete, color = MaterialTheme.colorScheme.tertiary)
        StatPill(label = "Missed", value = missed, color = MaterialTheme.colorScheme.error)
      }
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(
          selected = state.filter == HomeworkFilter.Open,
          onClick = { onFilterSelected(HomeworkFilter.Open) },
          label = { Text("Open") },
        )
        FilterChip(
          selected = state.filter == HomeworkFilter.Archive,
          onClick = { onFilterSelected(HomeworkFilter.Archive) },
          label = { Text("Archive") },
        )
        FilterChip(
          selected = state.filter == HomeworkFilter.All,
          onClick = { onFilterSelected(HomeworkFilter.All) },
          label = { Text("All") },
        )
      }
    }
  }
}

@Composable
private fun StatPill(label: String, value: Int, color: Color) {
  AssistChip(
    onClick = {},
    label = {
      Text("$label $value", maxLines = 1, overflow = TextOverflow.Ellipsis)
    },
    leadingIcon = {
      Box(
        modifier = Modifier
          .size(8.dp)
          .clip(CircleShape)
          .background(color),
      )
    },
  )
}

@Composable
private fun HomeworkCard(
  childKey: String,
  homework: HomeworkItem,
  subjectColor: Color?,
  saving: Boolean,
  onToggleComplete: (String, HomeworkItem, Boolean) -> Unit,
  onCustomizeSubject: (SubjectStyleTarget) -> Unit,
) {
  val scope = rememberCoroutineScope()
  val swipeOffset = remember(homework.id, homework.status) { Animatable(0f) }
  val density = LocalDensity.current
  val swipeThresholdPx = with(density) { 82.dp.toPx() }
  val maxSwipePx = with(density) { 110.dp.toPx() }
  val opacityDistancePx = with(density) { 300.dp.toPx() }
  val statusColor = when (homework.status) {
    HomeworkStatus.Completed -> MaterialTheme.colorScheme.tertiary
    HomeworkStatus.NotDone -> MaterialTheme.colorScheme.error
    HomeworkStatus.Open -> MaterialTheme.colorScheme.primary
  }
  Card(
    modifier = Modifier
      .graphicsLayer {
        translationX = swipeOffset.value
        alpha = 1f - min(abs(swipeOffset.value) / opacityDistancePx, 0.18f)
      }
      .pointerInput(homework.id, homework.status, saving) {
        if (saving) return@pointerInput
        detectHorizontalDragGestures(
          onHorizontalDrag = { change, dragAmount ->
            change.consume()
            scope.launch {
              swipeOffset.snapTo((swipeOffset.value + dragAmount).coerceIn(-maxSwipePx, maxSwipePx))
            }
          },
          onDragEnd = {
            when (swipeCompletionTarget(swipeOffset.value, homework.status, swipeThresholdPx)) {
              true -> onToggleComplete(childKey, homework, true)
              false -> onToggleComplete(childKey, homework, false)
              null -> Unit
            }
            scope.launch { swipeOffset.animateTo(0f, tween(durationMillis = 160)) }
          },
          onDragCancel = {
            scope.launch { swipeOffset.animateTo(0f, tween(durationMillis = 160)) }
          },
        )
      },
    colors = CardDefaults.cardColors(containerColor = subjectColor?.copy(alpha = 0.20f) ?: MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(8.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp),
      verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
      ) {
        val subjectKey = homework.subjectKey()
        SubjectBadge(
          subject = homework.subject.ifBlank { "?" },
          color = subjectColor,
          modifier = Modifier.pointerInput(childKey, subjectKey) {
            detectTapGestures(
              onLongPress = {
                onCustomizeSubject(
                  SubjectStyleTarget(
                    childKey = childKey,
                    subjectKey = subjectKey,
                    label = subjectTitle(homework),
                  )
                )
              },
            )
          },
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = subjectTitle(homework),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = dueText(homework),
            style = MaterialTheme.typography.bodySmall,
            color = if (homework.status == HomeworkStatus.NotDone) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        StatusBadge(homework.status, statusColor)
        TextButton(
          modifier = Modifier.height(30.dp),
          enabled = !saving && homework.status != HomeworkStatus.NotDone,
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
          onClick = {
            onToggleComplete(childKey, homework, homework.status != HomeworkStatus.Completed)
          },
        ) {
          Icon(
            imageVector = if (homework.status == HomeworkStatus.Completed) Icons.AutoMirrored.Filled.Undo else Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.size(4.dp))
          Text(
            text = if (homework.status == HomeworkStatus.Completed) "Reopen" else "Done",
            style = MaterialTheme.typography.labelSmall,
          )
        }
      }
      Text(
        text = homework.text.ifBlank { "No task text" },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      if (homework.teacher.isNotBlank() || homework.attachments.isNotEmpty()) {
        Text(
          text = buildList {
            if (homework.teacher.isNotBlank()) add("Teacher ${homework.teacher}")
            if (homework.attachments.isNotEmpty()) add("${homework.attachments.size} attachment(s)")
          }.joinToString("  |  "),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun SubjectBadge(subject: String, color: Color? = null, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier.size(34.dp),
    shape = RoundedCornerShape(8.dp),
    color = color?.copy(alpha = 0.34f) ?: MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onSurface,
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(
        text = subject.take(2).uppercase(Locale.getDefault()),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
private fun SubjectStyleDialog(
  target: SubjectStyleTarget,
  savedColor: String?,
  onDismiss: () -> Unit,
  onSetSubjectColor: (String, String, String) -> Unit,
  onResetSubjectColor: (String, String) -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Subject style") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          "Only for this child",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SubjectColorRow(
          childKey = target.childKey,
          subjectKey = target.subjectKey,
          label = target.label,
          savedColor = savedColor,
          onSetSubjectColor = onSetSubjectColor,
          onResetSubjectColor = onResetSubjectColor,
        )
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text("Close") }
    },
  )
}

@Composable
private fun StatusBadge(status: HomeworkStatus, color: Color) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Box(
      modifier = Modifier
        .size(8.dp)
        .clip(CircleShape)
        .background(color),
    )
    Text(
      text = when (status) {
        HomeworkStatus.Open -> "Open"
        HomeworkStatus.Completed -> "Done"
        HomeworkStatus.NotDone -> "Missed"
      },
      color = color,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
private fun EmptyState(error: String?, onRefresh: () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(18.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = Icons.Default.CloudOff,
      contentDescription = null,
      modifier = Modifier.size(42.dp),
      tint = MaterialTheme.colorScheme.primary,
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
      text = error ?: "No homework loaded yet.",
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(12.dp))
    Button(onClick = onRefresh) {
      Icon(Icons.Default.Refresh, contentDescription = null)
      Spacer(modifier = Modifier.size(8.dp))
      Text("Refresh")
    }
  }
}

@Composable
private fun EmptyListCard(filter: HomeworkFilter) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(8.dp),
  ) {
    Text(
      text = when (filter) {
        HomeworkFilter.Open -> "No open homework."
        HomeworkFilter.Archive -> "No archived homework."
        HomeworkFilter.All -> "No homework returned for this child."
      },
      modifier = Modifier.padding(14.dp),
      style = MaterialTheme.typography.bodyMedium,
    )
  }
}

@Composable
private fun InlineError(error: String, cached: Boolean) {
  Card(
    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFECEF)),
    shape = RoundedCornerShape(8.dp),
  ) {
    Text(
      text = if (cached) "Showing cached data. $error" else error,
      modifier = Modifier.padding(12.dp),
      color = MaterialTheme.colorScheme.error,
      style = MaterialTheme.typography.bodyMedium,
    )
  }
}

@Composable
private fun AppearanceSettings(
  child: ChildHomeworks,
  appearance: ChildAppearance,
  onSaveAppearance: (String, ChildAppearance) -> Unit,
) {
  var background by remember(child.key, appearance.theme, appearance.bgColor) { mutableStateOf(appearance.backgroundHex()) }
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("Profile colors", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    Text(
      child.displayName,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    themeProfiles.chunked(2).forEach { rowProfiles ->
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        rowProfiles.forEach { profile ->
          FilterChip(
            selected = appearance.theme == profile.key,
            onClick = { onSaveAppearance(child.key, appearance.copy(theme = profile.key)) },
            label = { Text(profile.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = {
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .clip(CircleShape)
                  .background(colorFromHex(profile.primary) ?: MaterialTheme.colorScheme.primary),
              )
            },
          )
        }
      }
    }
    OutlinedTextField(
      value = background,
      onValueChange = { value ->
        background = value
        normalizeHexColor(value)?.let { onSaveAppearance(child.key, appearance.copy(bgColor = it)) }
      },
      singleLine = true,
      label = { Text("Background color") },
      leadingIcon = {
        Box(
          modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(colorFromHex(background) ?: MaterialTheme.colorScheme.background),
        )
      },
      modifier = Modifier.fillMaxWidth(),
    )
    TextButton(
      onClick = {
        background = appearance.themeProfile().background
        onSaveAppearance(child.key, appearance.copy(bgColor = ""))
      },
    ) {
      Text("Use profile background")
    }
  }
}

@Composable
private fun SubjectColorSettings(
  child: ChildHomeworks,
  styles: Map<String, String>,
  onSetSubjectColor: (String, String, String) -> Unit,
  onResetSubjectColor: (String, String) -> Unit,
) {
  val subjects = child.homeworks
    .map { it.subjectKey() to subjectTitle(it) }
    .distinctBy { it.first }
    .sortedBy { it.second }
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text("Subject colors", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    if (subjects.isEmpty()) {
      Text(
        "No subjects loaded yet.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    subjects.forEach { (subjectKey, label) ->
      SubjectColorRow(
        childKey = child.key,
        subjectKey = subjectKey,
        label = label,
        savedColor = styles[subjectKey],
        onSetSubjectColor = onSetSubjectColor,
        onResetSubjectColor = onResetSubjectColor,
      )
    }
  }
}

@Composable
private fun SubjectColorRow(
  childKey: String,
  subjectKey: String,
  label: String,
  savedColor: String?,
  onSetSubjectColor: (String, String, String) -> Unit,
  onResetSubjectColor: (String, String) -> Unit,
) {
  var customColor by remember(childKey, subjectKey, savedColor) { mutableStateOf(savedColor ?: "#ffffff") }
  val activeColor = colorFromHex(savedColor)
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      SubjectBadge(subjectKey, activeColor)
      Column(modifier = Modifier.weight(1f)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subjectKey, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      TextButton(onClick = { onResetSubjectColor(childKey, subjectKey) }) {
        Text("Reset")
      }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
      colorSwatches.forEach { color ->
        IconButton(
          modifier = Modifier.size(34.dp),
          onClick = {
            customColor = color
            onSetSubjectColor(childKey, subjectKey, color)
          },
        ) {
          Box(
            modifier = Modifier
              .size(if (savedColor == color) 24.dp else 20.dp)
              .clip(CircleShape)
              .background(colorFromHex(color) ?: MaterialTheme.colorScheme.primary),
          )
        }
      }
    }
    OutlinedTextField(
      value = customColor,
      onValueChange = { value ->
        customColor = value
        normalizeHexColor(value)?.let { onSetSubjectColor(childKey, subjectKey, it) }
      },
      singleLine = true,
      label = { Text("Custom color") },
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

private fun List<HomeworkItem>.filtered(filter: HomeworkFilter): List<HomeworkItem> {
  return when (filter) {
    HomeworkFilter.Open -> filter { it.status == HomeworkStatus.Open }
    HomeworkFilter.Archive -> filter { it.status != HomeworkStatus.Open }
    HomeworkFilter.All -> this
  }
}

private fun HomeworkItem.subjectKey(): String {
  return subject.trim().uppercase(Locale.getDefault()).replace(Regex("[0-9]"), "").ifBlank {
    subjectLongName.trim().take(16).uppercase(Locale.getDefault())
  }.ifBlank { "?" }
}

internal fun swipeCompletionTarget(
  deltaPx: Float,
  status: HomeworkStatus,
  thresholdPx: Float = 82f,
): Boolean? {
  if (deltaPx > thresholdPx && status != HomeworkStatus.Completed) return true
  if (deltaPx < -thresholdPx && status == HomeworkStatus.Completed) return false
  return null
}

private fun subjectTitle(homework: HomeworkItem): String {
  if (homework.subjectLongName.isBlank()) return homework.subject.ifBlank { "Subject" }
  return "${homework.subject} - ${homework.subjectLongName}"
}

private fun dueText(homework: HomeworkItem): String {
  val formatted = homework.dueDate.formatIsoDate().ifBlank { homework.dueDateLabel }
  return if (formatted.isBlank()) "No due date" else "Due $formatted"
}

private fun String.formatIsoDate(): String {
  if (isBlank()) return ""
  return runCatching {
    LocalDate.parse(this).format(DateTimeFormatter.ofPattern("EEE, dd.MM.", Locale.GERMANY))
  }.getOrDefault(this)
}

private fun String.shortChildName(): String {
  return trim().removeSuffix(" (fictional)")
}
