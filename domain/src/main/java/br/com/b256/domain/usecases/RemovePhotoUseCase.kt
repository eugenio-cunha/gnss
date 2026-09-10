package br.com.b256.domain.usecases

import br.com.b256.domain.interfaces.PhotoRepository
import javax.inject.Inject

/**
 * Caso de uso responsável por remover uma foto da lista de pré-visualização (sem apagar o arquivo
 * do dispositivo).
 *
 * @property repository Repositório de fotos da plataforma.
 */
class RemovePhotoUseCase @Inject constructor(
    private val repository: PhotoRepository,
) {
    /**
     * @param uri URI (como string) da foto a ser removida da lista.
     */
    suspend operator fun invoke(uri: String) = repository.remove(uri)
}
