/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import io.element.android.features.logout.api.direct.DirectLogoutState
import io.element.android.features.preferences.impl.utils.ShowDeveloperSettingsProvider
import io.element.android.features.rageshake.api.RageshakeFeatureAvailability
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarDispatcher
import io.element.android.libraries.designsystem.utils.snackbar.collectSnackbarMessageAsState
import io.element.android.libraries.featureflag.api.FeatureFlagService
import io.element.android.libraries.featureflag.api.FeatureFlags
import io.element.android.libraries.indicator.api.IndicatorService
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.oidc.AccountManagementAction
import io.element.android.libraries.matrix.api.verification.SessionVerificationService
import io.element.android.services.analytics.api.AnalyticsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

class PreferencesRootPresenter @Inject constructor(
    private val matrixClient: MatrixClient,
    private val sessionVerificationService: SessionVerificationService,
    private val analyticsService: AnalyticsService,
    private val versionFormatter: VersionFormatter,
    private val snackbarDispatcher: SnackbarDispatcher,
    private val featureFlagService: FeatureFlagService,
    private val indicatorService: IndicatorService,
    private val directLogoutPresenter: Presenter<DirectLogoutState>,
    private val showDeveloperSettingsProvider: ShowDeveloperSettingsProvider,
    private val rageshakeFeatureAvailability: RageshakeFeatureAvailability,
) : Presenter<PreferencesRootState> {
    @Composable
    override fun present(): PreferencesRootState {
        val coroutineScope = rememberCoroutineScope()
        val matrixUser = matrixClient.userProfile.collectAsState()
        LaunchedEffect(Unit) {
            // Force a refresh of the profile
            matrixClient.getUserProfile()
        }

        val snackbarMessage by snackbarDispatcher.collectSnackbarMessageAsState()
        val hasAnalyticsProviders = remember { analyticsService.getAvailableAnalyticsProviders().isNotEmpty() }
        
        var pendingNavigationEvent: PreferencesRootEvents? by remember { mutableStateOf(null) }

        val showNotificationSettings = remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            showNotificationSettings.value = featureFlagService.isFeatureEnabled(FeatureFlags.NotificationSettings)
        }
        val showLockScreenSettings = remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            showLockScreenSettings.value = featureFlagService.isFeatureEnabled(FeatureFlags.PinUnlock)
        }

        // We should display the 'complete verification' option if the current session can be verified
        val canVerifyUserSession by sessionVerificationService.needsSessionVerification.collectAsState(false)

        val showSecureBackupIndicator by indicatorService.showSettingChatBackupIndicator()

        val accountManagementUrl: MutableState<String?> = remember {
            mutableStateOf(null)
        }
        val devicesManagementUrl: MutableState<String?> = remember {
            mutableStateOf(null)
        }
        var canDeactivateAccount by remember {
            mutableStateOf(false)
        }
        val canReportBug = remember { rageshakeFeatureAvailability.isAvailable() }
        LaunchedEffect(Unit) {
            canDeactivateAccount = false // Always disable deactivate account option
        }

        val showBlockedUsersItem by produceState(initialValue = false) {
            matrixClient.ignoredUsersFlow
                .onEach { value = it.isNotEmpty() }
                .launchIn(this)
        }

        val directLogoutState = directLogoutPresenter.present()

        LaunchedEffect(Unit) {
            initAccountManagementUrl(accountManagementUrl, devicesManagementUrl)
        }

        val showDeveloperSettings by showDeveloperSettingsProvider.showDeveloperSettings.collectAsState()

        fun handleEvent(event: PreferencesRootEvents) {
            when (event) {
                is PreferencesRootEvents.OnVersionInfoClick -> {
                    showDeveloperSettingsProvider.unlockDeveloperSettings(coroutineScope)
                }
                is PreferencesRootEvents.OnOpenCognitoProfile -> {
                    android.util.Log.d("elementx", "PreferencesRootPresenter: OnOpenCognitoProfile event received")
                    pendingNavigationEvent = event
                }
                is PreferencesRootEvents.ClearNavigationEvent -> {
                    pendingNavigationEvent = null
                }
            }
        }

        return PreferencesRootState(
            myUser = matrixUser.value,
            version = versionFormatter.get(),
            deviceId = matrixClient.deviceId,
            showSecureBackup = !canVerifyUserSession,
            showSecureBackupBadge = showSecureBackupIndicator,
            accountManagementUrl = accountManagementUrl.value,
            devicesManagementUrl = devicesManagementUrl.value,
            showAnalyticsSettings = false,
            canReportBug = canReportBug,
            showDeveloperSettings = showDeveloperSettings,
            canDeactivateAccount = canDeactivateAccount,
            showNotificationSettings = showNotificationSettings.value,
            showLockScreenSettings = showLockScreenSettings.value,
            showBlockedUsersItem = showBlockedUsersItem,
            directLogoutState = directLogoutState,
            snackbarMessage = snackbarMessage,
            pendingNavigationEvent = pendingNavigationEvent,
            eventSink = ::handleEvent,
        )
    }

    private fun CoroutineScope.initAccountManagementUrl(
        accountManagementUrl: MutableState<String?>,
        devicesManagementUrl: MutableState<String?>,
    ) = launch {
        accountManagementUrl.value = matrixClient.getAccountManagementUrl(AccountManagementAction.Profile).getOrNull()
        devicesManagementUrl.value = matrixClient.getAccountManagementUrl(AccountManagementAction.SessionsList).getOrNull()
    }
}
