package com.bitwarden.authenticator.data.authenticator.repository.di

import com.bitwarden.authenticator.data.authenticator.datasource.disk.AuthenticatorDiskSource
import com.bitwarden.authenticator.data.authenticator.manager.FileManager
import com.bitwarden.authenticator.data.authenticator.manager.TotpCodeManager
import com.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepository
import com.bitwarden.authenticator.data.authenticator.repository.AuthenticatorRepositoryImpl
import com.bitwarden.authenticator.data.platform.manager.DispatcherManager
import com.bitwarden.authenticator.data.platform.manager.imports.ImportManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides repositories in the authenticator package.
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthenticatorRepositoryModule {

    @Provides
    @Singleton
    fun provideAuthenticatorRepository(
        authenticatorDiskSource: AuthenticatorDiskSource,
        dispatcherManager: DispatcherManager,
        totpCodeManager: TotpCodeManager,
        fileManager: FileManager,
        importManager: ImportManager,
    ): AuthenticatorRepository = AuthenticatorRepositoryImpl(
        authenticatorDiskSource,
        totpCodeManager,
        fileManager,
        importManager,
        dispatcherManager,
    )
}
