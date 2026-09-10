package br.com.b256.data.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.b256.data.database.RoomDatabase
import br.com.b256.data.database.entities.PhotoEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Instant

/**
 * Teste de DAO do Room sob Robolectric com banco em memória (mesma abordagem de [TelemetryDaoTest]).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PhotoDaoTest {
    private lateinit var database: RoomDatabase
    private lateinit var dao: PhotoDao

    @Before
    fun setUp() {
        database =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                RoomDatabase::class.java,
            ).build()
        dao = database.photoDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun photo(uri: String, epochMillis: Long) =
        PhotoEntity(
            uri = uri,
            displayName = uri.substringAfterLast('/'),
            date = Instant.fromEpochMilliseconds(epochMillis),
        )

    @Test
    fun `observeAll retorna as fotos da mais recente para a mais antiga`() =
        runTest {
            dao.upsert(photo("content://p/1", 1_000))
            dao.upsert(photo("content://p/3", 3_000))
            dao.upsert(photo("content://p/2", 2_000))

            assertEquals(
                listOf("content://p/3", "content://p/2", "content://p/1"),
                dao.observeAll().first().map { it.uri },
            )
        }

    @Test
    fun `upsert com a mesma uri substitui a entrada`() =
        runTest {
            dao.upsert(photo("content://p/1", 1_000))
            dao.upsert(photo("content://p/1", 5_000))

            val all = dao.observeAll().first()
            assertEquals(1, all.size)
            assertEquals(Instant.fromEpochMilliseconds(5_000), all.single().date)
        }

    @Test
    fun `deleteByUri remove apenas a entrada indicada`() =
        runTest {
            dao.upsert(photo("content://p/1", 1_000))
            dao.upsert(photo("content://p/2", 2_000))

            dao.deleteByUri("content://p/1")

            assertEquals(listOf("content://p/2"), dao.observeAll().first().map { it.uri })
        }
}
