/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.customauth.impl.screens.verification

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.core.plugin.Plugin
import com.bumble.appyx.core.plugin.plugins
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.element.android.anvilannotations.ContributesNode
import io.element.android.libraries.architecture.NodeInputs
import io.element.android.libraries.architecture.inputs
import io.element.android.libraries.di.AppScope
import kotlinx.parcelize.Parcelize

@ContributesNode(AppScope::class)
class VerificationNode
    @AssistedInject
    constructor(
        @Assisted buildContext: BuildContext,
        @Assisted plugins: List<Plugin>,
        presenterFactory: VerificationPresenter.Factory,
    ) : Node(buildContext, plugins = plugins) {
        
        data class Inputs(
            val username: String,
            val email: String,
            val password: String
        ) : NodeInputs
        
        private val inputs = inputs<Inputs>()
        private val presenter = presenterFactory.create(
            VerificationPresenter.Params(
                username = inputs.username,
                email = inputs.email,
                password = inputs.password
            )
        )
        
        interface Callback : Plugin {
            fun onNavigateToLogin()
            fun onVerificationSuccess()
        }

        @Composable
        override fun View(modifier: Modifier) {
            val state = presenter.present()

            VerificationScreen(
                state = state,
                onNavigateToLogin = {
                    plugins<Callback>().forEach { it.onNavigateToLogin() }
                },
                onNavigateToCredentials = {
                    // When user clicks Continue, trigger authentication success
                    plugins<Callback>().forEach { it.onVerificationSuccess() }
                },
                modifier = modifier,
            )
        }
        
        interface Factory {
            fun create(params: VerificationPresenter.Params): VerificationPresenter
        }
    } 