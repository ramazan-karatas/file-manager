package dev.rk.systemapps.files.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.IOException
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.preferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "files_preferences",
)

/**
 * Uygulama tercihleri. F-1.4'te [BrowserPrefs] alanları da buraya eklenecek.
 */
class FilesPreferences(private val dataStore: DataStore<Preferences>) {

    /**
     * Kullanıcı tam erişim vermeyip "sınırlı modda devam et" dediyse true.
     * Bu bayrak olmadan onboarding her açılışta tekrar gösterilirdi.
     */
    val limitedModeAccepted: Flow<Boolean> = dataStore.data
        .catch { error ->
            // Bozuk tercih dosyası uygulamayı açılışta çökertmemeli.
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> preferences[Keys.LIMITED_MODE_ACCEPTED] ?: false }

    suspend fun setLimitedModeAccepted(accepted: Boolean) {
        dataStore.edit { preferences -> preferences[Keys.LIMITED_MODE_ACCEPTED] = accepted }
    }

    private object Keys {
        val LIMITED_MODE_ACCEPTED = booleanPreferencesKey("limited_mode_accepted")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {

    @Provides
    @Singleton
    fun provideFilesPreferences(@ApplicationContext context: Context): FilesPreferences =
        FilesPreferences(context.preferencesDataStore)
}
