package com.emergent.codeide.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("codeide_settings")

data class AppSettings(
    val showSoftKeyboard: Boolean = true,
    val fontSize: Float = 13f,
    val wordWrap: Boolean = false,
    val tabSize: Int = 4,
    val syntaxHighlighting: Boolean = true,
    val workspaceRoot: String = "", // optional custom root
)

class SettingsRepository(private val context: Context) {
    private val KEY_SHOW_KB = booleanPreferencesKey("show_soft_keyboard")
    private val KEY_FONT = floatPreferencesKey("font_size")
    private val KEY_WRAP = booleanPreferencesKey("word_wrap")
    private val KEY_TAB = floatPreferencesKey("tab_size")
    private val KEY_SYN = booleanPreferencesKey("syntax_on")
    private val KEY_ROOT = stringPreferencesKey("workspace_root")

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            showSoftKeyboard = p[KEY_SHOW_KB] ?: true,
            fontSize = p[KEY_FONT] ?: 13f,
            wordWrap = p[KEY_WRAP] ?: false,
            tabSize = (p[KEY_TAB] ?: 4f).toInt(),
            syntaxHighlighting = p[KEY_SYN] ?: true,
            workspaceRoot = p[KEY_ROOT] ?: "",
        )
    }

    suspend fun setShowSoftKeyboard(v: Boolean) =
        context.dataStore.edit { it[KEY_SHOW_KB] = v }.let { Unit }
    suspend fun setFontSize(v: Float) =
        context.dataStore.edit { it[KEY_FONT] = v }.let { Unit }
    suspend fun setWordWrap(v: Boolean) =
        context.dataStore.edit { it[KEY_WRAP] = v }.let { Unit }
    suspend fun setTabSize(v: Int) =
        context.dataStore.edit { it[KEY_TAB] = v.toFloat() }.let { Unit }
    suspend fun setSyntax(v: Boolean) =
        context.dataStore.edit { it[KEY_SYN] = v }.let { Unit }
    suspend fun setWorkspaceRoot(v: String) =
        context.dataStore.edit { it[KEY_ROOT] = v }.let { Unit }
}
