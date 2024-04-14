package com.x8bit.bitwarden.authenticator.data.platform.repository

import com.x8bit.bitwarden.authenticator.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.authenticator.data.platform.manager.DispatcherManager
import com.x8bit.bitwarden.authenticator.ui.platform.feature.settings.appearance.model.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Primary implementation of [SettingsRepository].
 */
class SettingsRepositoryImpl(
    private val settingsDiskSource: SettingsDiskSource,
    dispatcherManager: DispatcherManager,
) : SettingsRepository {

    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)

    override var appTheme: AppTheme by settingsDiskSource::appTheme

    override var authenticatorAlertThresholdSeconds = settingsDiskSource.getAlertThresholdSeconds()

    override val appThemeStateFlow: StateFlow<AppTheme>
        get() = settingsDiskSource
            .appThemeFlow
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = settingsDiskSource.appTheme,
            )

    override val authenticatorAlertThresholdSecondsFlow: StateFlow<Int>
        get() = settingsDiskSource
            .getAlertThresholdSecondsFlow()
            .map { it }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = settingsDiskSource.getAlertThresholdSeconds(),
            )

    override var hasSeenWelcomeTutorial: Boolean
        get() = settingsDiskSource.hasSeenWelcomeTutorial
        set(value) {
            settingsDiskSource.hasSeenWelcomeTutorial = value
        }

    override val hasSeenWelcomeTutorialFlow: StateFlow<Boolean>
        get() = settingsDiskSource
            .hasSeenWelcomeTutorialFlow
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = hasSeenWelcomeTutorial,
            )
}
