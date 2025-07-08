/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.customauth.impl

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.core.plugin.Plugin
import com.bumble.appyx.core.plugin.plugins
import com.bumble.appyx.navmodel.backstack.BackStack
import com.bumble.appyx.navmodel.backstack.operation.push
import com.bumble.appyx.navmodel.backstack.operation.pop
import com.bumble.appyx.navmodel.backstack.operation.newRoot
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.element.android.anvilannotations.ContributesNode
import io.element.android.features.customauth.api.CustomAuthEntryPoint
import io.element.android.features.customauth.impl.screens.login.LoginNode
import io.element.android.features.customauth.impl.screens.register.RegisterNode
import io.element.android.features.customauth.impl.screens.verification.VerificationNode
import io.element.android.libraries.architecture.BackstackView
import io.element.android.libraries.architecture.BaseFlowNode
import io.element.android.libraries.architecture.createNode
import io.element.android.libraries.architecture.inputs
import io.element.android.libraries.di.AppScope
import kotlinx.parcelize.Parcelize

@ContributesNode(AppScope::class)
class CustomAuthFlowNode
    @AssistedInject
    constructor(
        @Assisted buildContext: BuildContext,
        @Assisted plugins: List<Plugin>,
    ) : BaseFlowNode<CustomAuthFlowNode.NavTarget>(
            backstack =
                BackStack(
                    initialElement = getInitialTarget(plugins),
                    savedStateMap = buildContext.savedStateMap,
                ),
            buildContext = buildContext,
            plugins = plugins,
        ) {
        private val params = inputs<CustomAuthEntryPoint.Params>()

        sealed interface NavTarget : Parcelable {
            @Parcelize
            data object Login : NavTarget

            @Parcelize
            data object Register : NavTarget

            @Parcelize
            data class Verification(val username: String, val email: String, val password: String) : NavTarget

            @Parcelize
            data object Credentials : NavTarget

            @Parcelize
            data object Pending : NavTarget

            @Parcelize
            data object Splash : NavTarget
        }

        override fun resolve(
            navTarget: NavTarget,
            buildContext: BuildContext,
        ): Node {
            return when (navTarget) {
                NavTarget.Login -> {
                    val callback =
                        object : LoginNode.Callback {
                            override fun onLoginSuccess() {
                                // Notify parent that authentication was successful
                                plugins<CustomAuthEntryPoint.Callback>().forEach {
                                    it.onAuthenticationSuccess()
                                }
                            }

                            override fun onNavigateToRegister() {
                                backstack.push(NavTarget.Register)
                            }

                            override fun onNavigateToCredentials() {
                                backstack.push(NavTarget.Credentials)
                            }

                            override fun onNavigateToPending() {
                                backstack.push(NavTarget.Pending)
                            }

                            override fun onReportProblem() {
                                plugins<CustomAuthEntryPoint.Callback>().forEach {
                                    it.onReportProblem()
                                }
                            }
                        }
                    createNode<LoginNode>(buildContext, plugins = listOf(callback))
                }
                NavTarget.Register -> {
                    val callback =
                        object : RegisterNode.Callback {
                            override fun onNavigateToLogin() {
                                backstack.pop()
                            }

                            override fun onNavigateToVerification(username: String, email: String, password: String) {
                                backstack.push(NavTarget.Verification(username, email, password))
                            }
                        }
                    createNode<RegisterNode>(buildContext, plugins = listOf(callback))
                }
                is NavTarget.Verification -> {
                    val callback =
                        object : VerificationNode.Callback {
                            override fun onNavigateToLogin() {
                                backstack.newRoot(NavTarget.Login)
                            }

                            override fun onVerificationSuccess() {
                                // Notify parent that authentication was successful
                                plugins<CustomAuthEntryPoint.Callback>().forEach {
                                    it.onAuthenticationSuccess()
                                }
                            }
                        }
                    createNode<VerificationNode>(
                        buildContext,
                        plugins = listOf(callback, VerificationNode.Inputs(
                            username = navTarget.username,
                            email = navTarget.email,
                            password = navTarget.password
                        ))
                    )
                }
                NavTarget.Credentials -> {
                    // Implement CredentialsNode
                    createNode<LoginNode>(buildContext) // Placeholder
                }
                NavTarget.Pending -> {
                    // Implement PendingNode
                    createNode<LoginNode>(buildContext) // Placeholder
                }
                NavTarget.Splash -> {
                    // Implement SplashNode
                    createNode<LoginNode>(buildContext) // Placeholder
                }
            }
        }

        @Composable
        override fun View(modifier: Modifier) {
            BackstackView()
        }

        companion object {
            private fun getInitialTarget(plugins: List<Plugin>): NavTarget {
                val params = plugins.filterIsInstance<CustomAuthEntryPoint.Params>().firstOrNull()
                return when (params?.initialScreen) {
                    CustomAuthEntryPoint.InitialScreen.Register -> NavTarget.Register
                    CustomAuthEntryPoint.InitialScreen.Splash -> NavTarget.Splash
                    else -> NavTarget.Login
                }
            }
        }
    }
