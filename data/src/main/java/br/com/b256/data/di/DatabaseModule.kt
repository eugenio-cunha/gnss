package br.com.b256.data.di

import android.content.Context
import androidx.room.Room
import br.com.b256.data.database.RoomDatabase
import br.com.b256.data.database.dao.PhotoDao
import br.com.b256.data.database.dao.TelemetryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.jvm.java

/** Provê a instância singleton do [RoomDatabase] (arquivo de banco `"gnss"`). */
@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun providesNiaDatabase(
        @ApplicationContext context: Context,
    ): RoomDatabase =
        Room.databaseBuilder(
            context,
            RoomDatabase::class.java,
            "gnss",
        ).build()
}

/**
 * Expõe cada `@Dao` do [RoomDatabase] como dependência injetável própria. Um novo `@Dao`
 * (ex.: ao criar uma nova `@Entity`) ganha uma função `@Provides` análoga a esta aqui.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object DaoModule {
    @Provides
    fun providesTelemetryDao(database: RoomDatabase): TelemetryDao = database.telemetryDao()

    @Provides
    fun providesPhotoDao(database: RoomDatabase): PhotoDao = database.photoDao()
}
