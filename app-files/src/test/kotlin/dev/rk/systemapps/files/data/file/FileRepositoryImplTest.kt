package dev.rk.systemapps.files.data.file

import app.cash.turbine.test
import dev.rk.systemapps.core.common.result.Outcome
import dev.rk.systemapps.files.domain.MimeTypeResolver
import dev.rk.systemapps.files.domain.model.ListingOptions
import dev.rk.systemapps.files.domain.model.SortBy
import dev.rk.systemapps.files.util.TestDispatcherProvider
import dev.rk.systemapps.files.util.names
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FileRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val repository = FileRepositoryImpl(
        dataSource = LocalFileDataSource(MimeTypeResolver { null }),
        dispatchers = TestDispatcherProvider(),
    )

    @Test
    fun `list suzulmus ve siralanmis sonuc yayar`() = runTest {
        tempFolder.newFile("b.txt")
        tempFolder.newFile("a.txt")
        tempFolder.newFile(".gizli")
        tempFolder.newFolder("klasor")

        repository.list(tempFolder.root.absolutePath, ListingOptions()).test {
            val outcome = awaitItem()
            assertTrue(outcome is Outcome.Success)
            assertEquals(
                listOf("klasor", "a.txt", "b.txt"),
                (outcome as Outcome.Success).value.names(),
            )
            awaitComplete()
        }
    }

    @Test
    fun `list secenekleri sonuca yansir`() = runTest {
        tempFolder.newFile("kucuk.bin").writeBytes(ByteArray(1))
        tempFolder.newFile("buyuk.bin").writeBytes(ByteArray(100))

        val options = ListingOptions(sortBy = SortBy.SIZE, ascending = false)
        repository.list(tempFolder.root.absolutePath, options).test {
            val outcome = awaitItem() as Outcome.Success
            assertEquals(listOf("buyuk.bin", "kucuk.bin"), outcome.value.names())
            awaitComplete()
        }
    }

    @Test
    fun `olmayan klasor Failure olarak yayilir exception sizmaz`() = runTest {
        val missing = File(tempFolder.root, "yok").absolutePath

        repository.list(missing, ListingOptions()).test {
            assertTrue(awaitItem() is Outcome.Failure)
            awaitComplete()
        }
    }

    @Test
    fun `stat bulunan ogeyi Success olarak dondurur`() = runTest {
        val file = tempFolder.newFile("rapor.pdf")

        val outcome = repository.stat(file.absolutePath)

        assertTrue(outcome is Outcome.Success)
        assertEquals("rapor.pdf", (outcome as Outcome.Success).value.name)
    }

    @Test
    fun `stat bulunamazsa Failure dondurur`() = runTest {
        val outcome = repository.stat(File(tempFolder.root, "yok.txt").absolutePath)

        assertTrue(outcome is Outcome.Failure)
    }

    @Test
    fun `exists varligi bildirir`() = runTest {
        val file = tempFolder.newFile("var.txt")

        assertTrue(repository.exists(file.absolutePath))
        assertFalse(repository.exists(File(tempFolder.root, "yok.txt").absolutePath))
    }
}
