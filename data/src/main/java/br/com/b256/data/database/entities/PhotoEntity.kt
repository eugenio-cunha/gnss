package br.com.b256.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Instant

/**
 * Modelo de linha da tabela `photo` (formato de banco), convertido para a entidade de domínio
 * [br.com.b256.domain.entities.Photo] pelo mapper em `database/mapper/PhotoMapper.kt`.
 *
 * Rastreia apenas a referência da foto — o arquivo em si vive no armazenamento público
 * (MediaStore). A [uri] pública é a chave primária.
 */
@Entity(
    tableName = "photo",
)
internal data class PhotoEntity(
    @PrimaryKey
    @ColumnInfo(name = "uri")
    val uri: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "date")
    val date: Instant,
)
