package br.com.b256.domain.interfaces

import br.com.b256.domain.entities.GpsLocation
import br.com.b256.domain.entities.Photo
import br.com.b256.domain.entities.TitleBlock
import kotlinx.coroutines.flow.Flow

/**
 * Contrato para produzir, rastrear e gravar fotografias georreferenciadas.
 *
 * A implementação (em `:data`) desenha o [TitleBlock] sobre a imagem recém-capturada pela câmera,
 * grava os metadados de GPS no EXIF e persiste o resultado no armazenamento público de imagens do
 * dispositivo — de modo que a foto fique visível na galeria e possa ser compartilhada ou usada por
 * outros aplicativos. Cada foto é registrada localmente para alimentar a lista de pré-visualização.
 */
interface PhotoRepository {
    /**
     * Fluxo com as fotos já capturadas, da mais recente para a mais antiga. Entradas cujo arquivo
     * não é mais encontrado no dispositivo são descartadas automaticamente.
     */
    val photos: Flow<List<Photo>>

    /**
     * Compõe e salva a fotografia georreferenciada e a registra na lista de pré-visualização.
     *
     * @param sourceUri URI (como string) da imagem de origem, recém-capturada pela câmera.
     * @param titleBlock Bloco de dados a ser desenhado no canto inferior direito da imagem.
     * @param location Posição atual, usada para gravar os metadados de GPS no EXIF.
     * @return URI (como string) público da imagem gravada, pronto para compartilhamento.
     */
    suspend fun capture(
        sourceUri: String,
        titleBlock: TitleBlock,
        location: GpsLocation,
    ): String

    /**
     * Remove a foto identificada por [uri] da lista de pré-visualização. O arquivo em si não é
     * apagado do dispositivo.
     */
    suspend fun remove(uri: String)
}
