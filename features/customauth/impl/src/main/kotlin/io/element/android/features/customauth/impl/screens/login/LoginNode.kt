/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.customauth.impl.screens.login

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.core.plugin.Plugin
import com.bumble.appyx.core.plugin.plugins
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.element.android.anvilannotations.ContributesNode
import io.element.android.libraries.di.AppScope

@ContributesNode(AppScope::class)
class LoginNode
    @AssistedInject
    constructor(
        @Assisted buildContext: BuildContext,
        @Assisted plugins: List<Plugin>,
        private val presenter: LoginPresenter,
    ) : Node(
            buildContext = buildContext,
            plugins = plugins,
        ) {
        interface Callback : Plugin {
            fun onLoginSuccess()

            fun onNavigateToRegister()

            fun onNavigateToCredentials()

            fun onNavigateToPending()

            fun onReportProblem()
        }

        @Composable
        override fun View(modifier: Modifier) {
            val state = presenter.present()

            // Handle successful login
            if (state.isLoginSuccessful) {
                plugins<Callback>().forEach { it.onLoginSuccess() }
            }

            LoginScreen(
                state = state,
                onNavigateToRegister = {
                    plugins<Callback>().forEach { it.onNavigateToRegister() }
                },
                modifier = modifier,
            )
        }
    }
