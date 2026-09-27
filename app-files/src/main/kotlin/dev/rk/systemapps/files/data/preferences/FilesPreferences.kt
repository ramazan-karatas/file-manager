package dev.rk.systemapps.files.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.rk.systemapps.files.domain.model.BrowserPrefs
import dev.rk.systemapps.files.domain.model.SortBy
import java.io.IOException
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.preferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "files_preferences",
)

/**
 * Uygulama tercihleri. Arayüz olmasının sebebi ViewModel testlerinde sahte bir
 * uygulama verilebilmesi.
 */
interface FilesPreferences {

    /**
     * Kullanıcı tam erişim vermeyip "sınırlı modda devam et" dediyse true.
     * Bu bayrak olmadan onboarding her açılışta tekrar gösterilirdi.
     */
    val limitedModeAccepted: Flow<Boolean>

    val browserPrefs: Flow<BrowserPrefs>

    suspend fun setLimitedModeAccepted(accepted: Boolean)

    suspend fun setBrowserPrefs(prefs: BrowserPrefs)
}

class DataStoreFilesPreferences(
    private val dataStore: DataStore<Preferences>,
) : FilesPreferences {

    private val preferences: Flow<Preferences> = dataStore.data
        .catch { error ->
            // Bozuk tercih dosyası uygulamayı açılışta çökertmemeli.
            if (error is IOException) emit(emptyPreferences()) else throw error
        }

    override val limitedModeAccepted: Flow<Boolean> = preferences
        .map { it[Keys.LIMITED_MODE_ACCEPTED] ?: false }
        .distinctUntilChanged()

    override val browserPrefs: Flow<BrowserPrefs> = preferences
        .map { stored ->
            val defaults = BrowserPrefs()
            BrowserPrefs(
                sortBy = stored[Keys.SORT_BY]?.toSortBy() ?: defaults.sortBy,
                ascending = stored[Keys.ASCENDING] ?: defaults.ascending,
                gridMode = stored[Keys.GRID_MODE] ?: defaults.gridMode,
                showHidden = stored[Keys.SHOW_HIDDEN] ?: defaults.showHidden,
                foldersFirst = stored[Keys.FOLDERS_FIRST] ?: defaults.foldersFirst,
            )
        }
        .distinctUntilChanged()

    override suspend fun setLimitedModeAccepted(accepted: Boolean) {
        dataStore.edit { it[Keys.LIMITED_MODE_ACCEPTED] = accepted }
    }

    override suspend fun setBrowserPrefs(prefs: BrowserPrefs) {
        dataStore.edit { stored ->
            stored[Keys.SORT_BY] = prefs.sortBy.name
            stored[Keys.ASCENDING] = prefs.ascending
            stored[Keys.GRID_MODE] = prefs.gridMode
            stored[Keys.SHOW_HIDDEN] = prefs.showHidden
            stored[Keys.FOLDERS_FIRST] = prefs.foldersFirst
        }
    }

    /** Kaydedilmiş değer geçersizse (sürüm değişikliği, elle düzenleme) varsayılana düşülür. */
    private fun String.toSortBy(): SortBy? = SortBy.entries.firstOrNull { it.name == this }

    private object Keys {
        val LIMITED_MODE_ACCEPTED = booleanPreferencesKey("limited_mode_accepted")
        val SORT_BY = stringPreferencesKey("browser_sort_by")
        val ASCENDING = booleanPreferencesKey("browser_ascending")
        val GRID_MODE = booleanPreferencesKey("browser_grid_mode")
        val SHOW_HIDDEN = booleanPreferencesKey("browser_show_hidden")
        val FOLDERS_FIRST = booleanPreferencesKey("browser_folders_first")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {

    @Provides
    @Singleton
    fun provideFilesPreferences(@ApplicationContext context: Context): FilesPreferences =
        DataStoreFilesPreferences(context.preferencesDataStore)
}
