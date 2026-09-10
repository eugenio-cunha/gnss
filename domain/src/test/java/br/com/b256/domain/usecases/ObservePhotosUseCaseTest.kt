package br.com.b256.domain.usecases

import app.cash.turbine.test
import br.com.b256.domain.entities.Photo
import br.com.b256.domain.interfaces.PhotoRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Instant

class ObservePhotosUseCaseTest {
    private val repository = mockk<PhotoRepository>()
    private val useCase = ObservePhotosUseCase(repository)

    @Test
    fun `invoke repassa a lista emitida pelo repository`() =
        runTest {
            val photos = listOf(
                Photo("content://media/2", "GNSS_2.jpg", Instant.fromEpochMilliseconds(2_000)),
                Photo("content://media/1", "GNSS_1.jpg", Instant.fromEpochMilliseconds(1_000)),
            )
            every { repository.photos } returns flowOf(photos)

            useCase().test {
                assertEquals(photos, awaitItem())
                awaitComplete()
            }
        }
}
