package dev.rk.systemapps.files.domain.repository

import dev.rk.systemapps.core.common.result.Outcome
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
}
