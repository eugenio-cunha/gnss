package br.com.b256.domain.usecases

import br.com.b256.domain.interfaces.PhotoRepository
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RemovePhotoUseCaseTest {
    private val repository = mockk<PhotoRepository>()
    private val useCase = RemovePhotoUseCase(repository)

    @Test
    fun `invoke delega a remocao para o repository`() =
        runTest {
            coJustRun { repository.remove("content://media/1") }

            useCase("content://media/1")

            coVerify(exactly = 1) { repository.remove("content://media/1") }
        }
}
