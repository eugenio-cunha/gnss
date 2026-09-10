package br.com.b256.domain.usecases

import br.com.b256.domain.entities.GpsLocation
import br.com.b256.domain.entities.TitleBlock
import br.com.b256.domain.entities.UTM
import br.com.b256.domain.entities.enums.Datum
import br.com.b256.domain.interfaces.PhotoRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Instant

class CapturePhotoUseCaseTest {
    private val repository = mockk<PhotoRepository>()
    private val useCase = CapturePhotoUseCase(repository)

    private val location =
        GpsLocation(
            latitude = -23.5,
            longitude = -46.6,
            altitude = 720.0,
            accuracy = 3.5f,
            speed = null,
            bearing = null,
            date = Instant.fromEpochMilliseconds(1_700_000_000_000),
            utm =
                UTM(
                    zone = "23K",
                    easting = "333000m E",
                    northing = "7400000m N",
                    centralMeridian = "-45.0",
                    datum = Datum.WGS84,
                ),
        )
    private val titleBlock = TitleBlock(heading = "GNSS SKYPLOT", lines = listOf("linha 1"))

    @Test
    fun `invoke delega para o repository e repassa o uri publico`() =
        runTest {
            coEvery {
                repository.capture(
                    sourceUri = "content://capture/1",
                    titleBlock = titleBlock,
                    location = location,
                )
            } returns "content://media/external/images/media/42"

            val result =
                useCase(
                    sourceUri = "content://capture/1",
                    titleBlock = titleBlock,
                    location = location,
                )

            assertEquals("content://media/external/images/media/42", result)
            coVerify(exactly = 1) {
                repository.capture(
                    sourceUri = "content://capture/1",
                    titleBlock = titleBlock,
                    location = location,
                )
            }
        }
}
