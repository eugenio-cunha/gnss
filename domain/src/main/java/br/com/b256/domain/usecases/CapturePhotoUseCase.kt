package br.com.b256.domain.usecases

import br.com.b256.domain.entities.GpsLocation
import br.com.b256.domain.entities.TitleBlock
import br.com.b256.domain.interfaces.PhotoRepository
import javax.inject.Inject

/**
 * Caso de uso responsável por compor uma fotografia com o "title block" da posição atual e
 * gravá-la no armazenamento público do dispositivo (ver [PhotoRepository]).
 *
 * @property repository Repositório de fotos da plataforma.
 */
class CapturePhotoUseCase
    @Inject
    constructor(
        private val repository: PhotoRepository,
    ) {
        /**
         * Executa a composição e a gravação da foto.
         *
         * @param sourceUri URI (como string) da imagem recém-capturada pela câmera.
         * @param titleBlock Bloco de dados a ser desenhado sobre a imagem.
         * @param location Posição atual, usada para os metadados de GPS.
         * @return URI (como string) público da imagem gravada.
         */
        suspend operator fun invoke(
            sourceUri: String,
            titleBlock: TitleBlock,
            location: GpsLocation,
        ): String =
            repository.capture(
                sourceUri = sourceUri,
                titleBlock = titleBlock,
                location = location,
            )
    }
