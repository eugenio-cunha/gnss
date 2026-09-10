package br.com.b256.data.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import br.com.b256.data.database.dao.PhotoDao
import br.com.b256.data.database.dao.TelemetryDao
import br.com.b256.data.database.entities.PhotoEntity
import br.com.b256.data.database.entities.TelemetryEntity
import br.com.b256.data.database.util.InstantConverter

/**
 * Banco Room do `:data`. Instanciado como singleton em `data/di/DatabaseModule.kt`.
 *
 * Ao adicionar uma nova [androidx.room.Entity]: inclua a classe em `entities`, exponha o
 * respectivo `@Dao` aqui (como [telemetryDao]) e **incremente `version`**, adicionando uma
 * `AutoMigration` em `autoMigrations` (ou uma `Migration` manual) — o schema exportado
 * (`exportSchema = true`) fica em `data/schemas` e é o que valida migrações em tempo de build.
 */
@Database(
    version = 2,
    entities = [TelemetryEntity::class, PhotoEntity::class],
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
    ],
    exportSchema = true,
)
@TypeConverters(
    InstantConverter::class,
)
internal abstract class RoomDatabase() : RoomDatabase() {
    abstract fun telemetryDao(): TelemetryDao

    abstract fun photoDao(): PhotoDao
}
