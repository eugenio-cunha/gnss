package br.com.b256.data.database.mapper

import br.com.b256.data.database.entities.PhotoEntity
import br.com.b256.domain.entities.Photo

/**
 * Conversão nos dois sentidos entre a entidade de banco ([PhotoEntity]) e a de domínio ([Photo]).
 * Mesma convenção usada em `database/mapper/TelemetryMapper.kt`.
 */
internal fun PhotoEntity.asDomain(): Photo =
    Photo(
        uri = uri,
        name = displayName,
        date = date,
    )

internal fun List<PhotoEntity>.asDomain(): List<Photo> = map { it.asDomain() }

internal fun Photo.asEntity(): PhotoEntity =
    PhotoEntity(
        uri = uri,
        displayName = name,
        date = date,
    )
