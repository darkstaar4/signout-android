/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.root

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.core.plugin.Plugin
import com.bumble.appyx.core.plugin.plugins
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.element.android.anvilannotations.ContributesNode
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.logout.api.direct.DirectLogoutEvents
import io.element.android.features.logout.api.direct.DirectLogoutView
import io.element.android.libraries.androidutils.browser.openUrlInChromeCustomTab
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.matrix.api.user.MatrixUser

@ContributesNode(SessionScope::class)
class PreferencesRootNode @AssistedInject constructor(
    @Assisted buildContext: BuildContext,
    @Assisted plugins: List<Plugin>,
    private val presenter: PreferencesRootPresenter,
    private val directLogoutView: DirectLogoutView,
) : Node(buildContext, plugins = plugins) {
    interface Callback : Plugin {
        fun onOpenBugReport()
        fun onSecureBackupClick()
        fun onOpenAnalytics()
        fun onOpenAbout()
        fun onOpenDonate()
        fun onOpenDeveloperSettings()
        fun onOpenNotificationSettings()
        fun onOpenLockScreenSettings()
        fun onOpenAdvancedSettings()
        fun onOpenUserProfile(matrixUser: MatrixUser)
        fun onOpenCognitoProfile()
        fun onOpenBlockedUsers()
        fun onSignOutClick()
        fun onOpenAccountDeactivation()
    }

    private fun onOpenBugReport() {
        plugins<Callback>().forEach { it.onOpenBugReport() }
    }

    private fun onOpenContactUrl(activity: Activity, isDark: Boolean) {
        activity.openUrlInChromeCustomTab(
            null,
            darkTheme = isDark,
            url = "https://www.getsignout.com/contact"
        )
    }

    private fun onSecureBackupClick() {
        plugins<Callback>().forEach { it.onSecureBackupClick() }
    }

    private fun onOpenDeveloperSettings() {
        plugins<Callback>().forEach { it.onOpenDeveloperSettings() }
    }

    private fun onOpenAdvancedSettings() {
        plugins<Callback>().forEach { it.onOpenAdvancedSettings() }
    }

    private fun onOpenAnalytics() {
        plugins<Callback>().forEach { it.onOpenAnalytics() }
    }

    private fun onOpenAbout() {
        plugins<Callback>().forEach { it.onOpenAbout() }
    }

    private fun onOpenDonate() {
        plugins<Callback>().forEach { it.onOpenDonate() }
    }

    private fun onManageAccountClick(
        activity: Activity,
        url: String?,
        isDark: Boolean,
    ) {
        url?.let {
            activity.openUrlInChromeCustomTab(
                null,
                darkTheme = isDark,
                url = it
            )
        }
    }

    private fun onOpenNotificationSettings() {
        plugins<Callback>().forEach { it.onOpenNotificationSettings() }
    }

    private fun onOpenLockScreenSettings() {
        plugins<Callback>().forEach { it.onOpenLockScreenSettings() }
    }

    private fun onOpenUserProfile(matrixUser: MatrixUser) {
        plugins<Callback>().forEach { it.onOpenUserProfile(matrixUser) }
    }

    private fun onOpenCognitoProfile() {
        plugins<Callback>().forEach { it.onOpenCognitoProfile() }
    }

    private fun onOpenBlockedUsers() {
        plugins<Callback>().forEach { it.onOpenBlockedUsers() }
    }

    private fun onSignOutClick() {
        plugins<Callback>().forEach { it.onSignOutClick() }
    }

    private fun onOpenAccountDeactivation() {
        plugins<Callback>().forEach { it.onOpenAccountDeactivation() }
    }

    @Composable
    override fun View(modifier: Modifier) {
        val state = presenter.present()
        val activity = requireNotNull(LocalActivity.current)
        val isDark = ElementTheme.isLightTheme.not()
        
        // Handle navigation events from presenter
        when (val event = state.pendingNavigationEvent) {
            is PreferencesRootEvents.OnOpenCognitoProfile -> {
                onOpenCognitoProfile()
                // Clear the event after handling
                state.eventSink(PreferencesRootEvents.ClearNavigationEvent)
            }
            else -> { /* No navigation event */ }
        }
        
        PreferencesRootView(
            state = state,
            modifier = modifier,
            onBackClick = this::navigateUp,
            onOpenRageShake = { onOpenContactUrl(activity, isDark) },
            onOpenAnalytics = this::onOpenAnalytics,
            onOpenAbout = this::onOpenAbout,
            onOpenDonate = this::onOpenDonate,
            onSecureBackupClick = this::onSecureBackupClick,
            onOpenDeveloperSettings = this::onOpenDeveloperSettings,
            onOpenAdvancedSettings = this::onOpenAdvancedSettings,
            onManageAccountClick = { onManageAccountClick(activity, it, isDark) },
            onOpenNotificationSettings = this::onOpenNotificationSettings,
            onOpenLockScreenSettings = this::onOpenLockScreenSettings,
            onOpenUserProfile = this::onOpenUserProfile,
            onOpenCognitoProfile = this::onOpenCognitoProfile,
            onOpenBlockedUsers = this::onOpenBlockedUsers,
            onSignOutClick = {
                if (state.directLogoutState.canDoDirectSignOut) {
                    state.directLogoutState.eventSink(DirectLogoutEvents.Logout(ignoreSdkError = false))
                } else {
                    onSignOutClick()
                }
            },
            onDeactivateClick = this::onOpenAccountDeactivation
        )

        directLogoutView.Render(state = state.directLogoutState)
    }
}
