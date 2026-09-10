package br.com.b256.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import br.com.b256.data.database.entities.PhotoEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO da tabela `photo`. Diferente de [TelemetryDao], a listagem é exposta como [Flow] para a
 * lista de pré-visualização reagir a novas capturas e a remoções sem recarregar manualmente.
 */
@Dao
internal interface PhotoDao {
    @Query(value = "SELECT * FROM photo ORDER BY date DESC;")
    fun observeAll(): Flow<List<PhotoEntity>>

    @Upsert
    suspend fun upsert(entity: PhotoEntity)

    @Query(value = "DELETE FROM photo WHERE uri = :uri;")
    suspend fun deleteByUri(uri: String)
}
