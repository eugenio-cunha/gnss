package br.com.b256.data.database.mapper

import br.com.b256.data.database.entities.PhotoEntity
import br.com.b256.domain.entities.Photo
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Instant

class PhotoMapperTest {
    private val entity = PhotoEntity(
        uri = "content://media/external/images/media/1",
        displayName = "GNSS_1.jpg",
        date = Instant.fromEpochMilliseconds(1_000),
    )
    private val domain = Photo(
        uri = "content://media/external/images/media/1",
        name = "GNSS_1.jpg",
        date = Instant.fromEpochMilliseconds(1_000),
    )

    @Test
    fun `asDomain converte PhotoEntity para Photo`() {
        assertEquals(domain, entity.asDomain())
    }

    @Test
    fun `asEntity converte Photo para PhotoEntity`() {
        assertEquals(entity, domain.asEntity())
    }

    @Test
    fun `listas sao convertidas elemento a elemento`() {
        assertEquals(listOf(domain), listOf(entity).asDomain())
    }
}
