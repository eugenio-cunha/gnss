package br.com.b256.domain.usecases

import br.com.b256.domain.entities.Photo
import br.com.b256.domain.interfaces.PhotoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Caso de uso responsável por observar a lista de fotos capturadas (mais recentes primeiro),
 * já filtrada para conter apenas as que ainda existem no dispositivo.
 *
 * @property repository Repositório de fotos da plataforma.
 */
class ObservePhotosUseCase @Inject constructor(
    private val repository: PhotoRepository,
) {
    /**
     * @return [Flow] com a lista de [Photo], da mais recente para a mais antiga.
     */
    operator fun invoke(): Flow<List<Photo>> = repository.photos
}
