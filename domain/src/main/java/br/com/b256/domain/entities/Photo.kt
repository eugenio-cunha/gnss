package br.com.b256.domain.entities

import kotlin.time.Instant

/**
 * Uma fotografia georreferenciada já capturada e publicada no armazenamento público do
 * dispositivo (ver [br.com.b256.domain.interfaces.PhotoRepository]).
 *
 * O app apenas rastreia a referência ([uri]) — o arquivo em si vive na galeria e pode ser
 * removido pelo usuário por fora; nesse caso a entrada é descartada da lista.
 *
 * @property uri URI público (`content://…`) da imagem; identifica a foto de forma única.
 * @property name Nome de exibição do arquivo (ex.: `GNSS_20260910_143022.jpg`).
 * @property date Momento da captura.
 */
data class Photo(
    val uri: String,
    val name: String,
    val date: Instant,
)
