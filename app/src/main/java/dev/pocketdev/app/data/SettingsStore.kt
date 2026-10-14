package dev.pocketdev.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.pocketdev.app.core.ProviderKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "pocketdev_settings")

data class AppSettings(
    val providerKind: ProviderKind = ProviderKind.OLLAMA,
    val providerBaseUrl: String = ProviderKind.OLLAMA.defaultUrl,
)

class SettingsStore(private val context: Context) {
    private val kindKey = stringPreferencesKey("provider_kind")
    private val urlKey = stringPreferencesKey("provider_base_url")

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        val kind = p[kindKey]?.let { runCatching { ProviderKind.valueOf(it) }.getOrNull() } ?: ProviderKind.OLLAMA
        AppSettings(kind, p[urlKey] ?: kind.defaultUrl)
    }

    suspend fun saveProvider(kind: ProviderKind, baseUrl: String) {
        context.dataStore.edit {
            it[kindKey] = kind.name
            it[urlKey] = baseUrl.trim()
        }
    }
}
