/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.customauth.impl.screens.register

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
class RegisterNode
    @AssistedInject
    constructor(
        @Assisted buildContext: BuildContext,
        @Assisted plugins: List<Plugin>,
        private val presenter: RegisterPresenter,
    ) : Node(buildContext, plugins = plugins) {
        interface Callback : Plugin {
            fun onNavigateToLogin()

            fun onNavigateToVerification(username: String, email: String)
        }

        @Composable
        override fun View(modifier: Modifier) {
            val state = presenter.present()

            // Handle successful registration
            if (state.isRegistrationSuccessful && state.registrationUsername != null) {
                plugins<Callback>().forEach { 
                    it.onNavigateToVerification(state.registrationUsername, state.email) 
                }
            }

            RegisterScreen(
                state = state,
                onNavigateToLogin = {
                    plugins<Callback>().forEach { it.onNavigateToLogin() }
                },
                onNavigateToCredentials = {
                    // This is no longer used - we go to verification instead
                },
                modifier = modifier,
            )
        }
    } 
