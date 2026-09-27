package dev.rk.systemapps.files.data.file

import dev.rk.systemapps.core.common.coroutines.DispatcherProvider
import dev.rk.systemapps.core.common.result.Outcome
import dev.rk.systemapps.core.common.result.outcomeOf
import dev.rk.systemapps.files.domain.model.DirectoryStats
import dev.rk.systemapps.files.domain.model.FileDetails
import dev.rk.systemapps.files.domain.model.FileNode
import dev.rk.systemapps.files.domain.model.ListingOptions
import dev.rk.systemapps.files.domain.repository.FileRepository
import java.io.FileNotFoundException
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class FileRepositoryImpl(
    private val dataSource: LocalFileDataSource,
    private val dispatchers: DispatcherProvider,
) : FileRepository {

    override fun list(
        directoryId: String,
        options: ListingOptions,
    ): Flow<Outcome<List<FileNode>>> = flow {
        emit(outcomeOf { FileNodeSorter.apply(dataSource.listDirectory(directoryId), options) })
    }.flowOn(dispatchers.io)

    override suspend fun stat(id: String): Outcome<FileNode> = withContext(dispatchers.io) {
        outcomeOf {
            dataSource.stat(id) ?: throw FileNotFoundException("Bulunamadı: $id")
        }
    }

    override suspend fun exists(id: String): Boolean = withContext(dispatchers.io) {
        dataSource.exists(id)
    }

    override suspend fun details(id: String): Outcome<FileDetails> = withContext(dispatchers.io) {
        outcomeOf { dataSource.details(id) }
    }

    override suspend fun directoryStats(id: String): Outcome<DirectoryStats> =
        withContext(dispatchers.io) { outcomeOf { dataSource.directoryStats(id) } }

    override suspend fun rename(id: String, newName: String): Outcome<FileNode> =
        withContext(dispatchers.io) { outcomeOf { dataSource.rename(id, newName) } }

    override suspend fun createDirectory(parentId: String, name: String): Outcome<FileNode> =
        withContext(dispatchers.io) { outcomeOf { dataSource.createDirectory(parentId, name) } }

    override suspend fun createFile(parentId: String, name: String): Outcome<FileNode> =
        withContext(dispatchers.io) { outcomeOf { dataSource.createFile(parentId, name) } }

    override fun search(rootId: String, query: String): Flow<FileNode> =
        dataSource.search(rootId, query, Locale.getDefault()).flowOn(dispatchers.io)
}
