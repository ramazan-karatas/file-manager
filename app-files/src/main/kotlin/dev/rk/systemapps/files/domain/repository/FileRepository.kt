package dev.rk.systemapps.files.domain.repository

import dev.rk.systemapps.core.common.result.Outcome
import dev.rk.systemapps.files.domain.model.DirectoryStats
import dev.rk.systemapps.files.domain.model.FileDetails
import dev.rk.systemapps.files.domain.model.FileNode
import dev.rk.systemapps.files.domain.model.ListingOptions
import kotlinx.coroutines.flow.Flow

/**
 * Repository exception fırlatmaz, [Outcome] döner (CLAUDE.md "Kod standartları").
 */
interface FileRepository {

    /**
     * Klasör içeriğini [options] uyarınca süzüp sıralayarak yayar.
     *
     * Akış olmasının sebebi, ileride klasör izleme (FileObserver) eklendiğinde
     * arayüzün değişmek zorunda kalmaması; şu an tek değer yayıp tamamlanır.
     */
    fun list(directoryId: String, options: ListingOptions): Flow<Outcome<List<FileNode>>>

    suspend fun stat(id: String): Outcome<FileNode>

    suspend fun exists(id: String): Boolean

    /** Özellikler diyaloğu için hızlı okunabilen bilgiler; klasör boyutu dâhil değildir. */
    suspend fun details(id: String): Outcome<FileDetails>

    /** Klasör ağacını dolaşır; pahalıdır, ayrı çağrılır ki UI bekletmeden spinner gösterebilsin. */
    suspend fun directoryStats(id: String): Outcome<DirectoryStats>

    suspend fun rename(id: String, newName: String): Outcome<FileNode>

    suspend fun createDirectory(parentId: String, name: String): Outcome<FileNode>

    suspend fun createFile(parentId: String, name: String): Outcome<FileNode>

    /** Alt klasörler dâhil arama; sonuçlar bulundukça yayılır, akış iptal edilebilir. */
    fun search(rootId: String, query: String): Flow<FileNode>
}
