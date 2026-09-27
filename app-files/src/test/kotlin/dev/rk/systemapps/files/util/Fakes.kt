package dev.rk.systemapps.files.util

import dev.rk.systemapps.core.common.result.Outcome
import dev.rk.systemapps.files.data.preferences.FilesPreferences
import dev.rk.systemapps.files.domain.model.BrowserPrefs
import dev.rk.systemapps.files.domain.model.DirectoryStats
import dev.rk.systemapps.files.domain.model.FileDetails
import dev.rk.systemapps.files.domain.model.FileClipboard
import dev.rk.systemapps.files.domain.model.FileNode
import dev.rk.systemapps.files.domain.model.ListingOptions
import dev.rk.systemapps.files.domain.repository.FileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

class FakeFileRepository(nodes: List<FileNode> = emptyList()) : FileRepository {

    var result: Outcome<List<FileNode>> = Outcome.Success(nodes)

    /** Her `list` çağrısında kullanılan seçenekler — tercih değişimini doğrulamak için. */
    val requestedOptions = mutableListOf<ListingOptions>()

    override fun list(
        directoryId: String,
        options: ListingOptions,
    ): Flow<Outcome<List<FileNode>>> = flow {
        requestedOptions += options
        emit(result)
    }

    override suspend fun stat(id: String): Outcome<FileNode> = Outcome.Failure()

    override suspend fun exists(id: String): Boolean = false

    var detailsResult: Outcome<FileDetails> = Outcome.Failure()
    var statsResult: Outcome<DirectoryStats> = Outcome.Failure()
    var nameResult: Outcome<FileNode> = Outcome.Failure()

    /** Son çağrılan ad işlemi: ("rename"/"mkdir"/"touch", hedef, ad). */
    val nameCalls = mutableListOf<Triple<String, String, String>>()

    override suspend fun details(id: String): Outcome<FileDetails> = detailsResult

    override suspend fun directoryStats(id: String): Outcome<DirectoryStats> = statsResult

    override suspend fun rename(id: String, newName: String): Outcome<FileNode> {
        nameCalls += Triple("rename", id, newName)
        return nameResult
    }

    override suspend fun createDirectory(parentId: String, name: String): Outcome<FileNode> {
        nameCalls += Triple("mkdir", parentId, name)
        return nameResult
    }

    override suspend fun createFile(parentId: String, name: String): Outcome<FileNode> {
        nameCalls += Triple("touch", parentId, name)
        return nameResult
    }

    /** Arama sonuçları; sorgu adın içinde geçen düğümler döner. */
    var searchSource: List<FileNode> = emptyList()
    val searchQueries = mutableListOf<String>()

    override fun search(rootId: String, query: String): Flow<FileNode> = flow {
        searchQueries += query
        if (query.isBlank()) return@flow
        searchSource.filter { it.name.contains(query, ignoreCase = true) }
            .forEach { emit(it) }
    }
}

class FakeFilesPreferences(
    initialPrefs: BrowserPrefs = BrowserPrefs(),
    initialLimitedAccepted: Boolean = false,
) : FilesPreferences {

    private val prefs = MutableStateFlow(initialPrefs)
    private val limitedAccepted = MutableStateFlow(initialLimitedAccepted)
    private val clipboardState = MutableStateFlow<FileClipboard?>(null)

    override val limitedModeAccepted: Flow<Boolean> = limitedAccepted
    override val browserPrefs: Flow<BrowserPrefs> = prefs
    override val clipboard: Flow<FileClipboard?> = clipboardState

    val currentPrefs: BrowserPrefs get() = prefs.value
    val currentClipboard: FileClipboard? get() = clipboardState.value

    override suspend fun setClipboard(clipboard: FileClipboard?) {
        clipboardState.value = clipboard
    }

    override suspend fun setLimitedModeAccepted(accepted: Boolean) {
        limitedAccepted.value = accepted
    }

    override suspend fun setBrowserPrefs(prefs: BrowserPrefs) {
        this.prefs.value = prefs
    }
}
