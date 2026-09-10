package br.com.b256.domain.entities

/**
 * Bloco de dados textual ("title block") desenhado sobre uma fotografia (ver
 * [br.com.b256.domain.interfaces.PhotoRepository]).
 *
 * O conteúdo já chega aqui formatado e localizado pela camada de apresentação — a camada `:data`
 * apenas o desenha na imagem, sem conhecer a semântica de cada linha.
 *
 * @property heading Título do bloco, exibido em destaque no topo (ex.: "GNSS SKYPLOT").
 * @property lines Linhas de dados, uma por elemento, na ordem de exibição.
 */
data class TitleBlock(
    val heading: String,
    val lines: List<String>,
)
