package br.com.b256.data.di

import br.com.b256.data.repositories.SettingsRepositoryImpl
import br.com.b256.data.services.photo.PhotoRepositoryImpl
import br.com.b256.domain.interfaces.PhotoRepository
import br.com.b256.domain.interfaces.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Módulo Hilt de referência para ligar contratos de `:domain` às implementações de `:data` via
 * `@Binds` (preferível a `@Provides` quando a implementação só precisa satisfazer a interface,
 * sem lógica de criação extra). Cada novo repository ganha uma função `@Binds` aqui.
 */
@Module
@InstallIn(SingletonComponent::class)
internal interface RepositoryModule {
    @Binds
    fun bindsTelemetryRepository(telemetryRepository: SettingsRepositoryImpl): SettingsRepository

    @Binds
    fun bindsPhotoRepository(photoRepository: PhotoRepositoryImpl): PhotoRepository
}
