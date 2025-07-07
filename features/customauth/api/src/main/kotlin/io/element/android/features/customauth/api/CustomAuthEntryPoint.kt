/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.customauth.api

import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.core.plugin.Plugin
import io.element.android.libraries.architecture.FeatureEntryPoint
import io.element.android.libraries.architecture.NodeInputs

interface CustomAuthEntryPoint : FeatureEntryPoint {
    data class Params(
        val initialScreen: InitialScreen = InitialScreen.Login,
    ) : NodeInputs

    enum class InitialScreen {
        Login,
        Register,
        Splash,
    }

    fun nodeBuilder(
        parentNode: Node,
        buildContext: BuildContext,
    ): NodeBuilder

    interface NodeBuilder {
        fun params(params: Params): NodeBuilder

        fun callback(callback: Callback): NodeBuilder

        fun build(): Node
    }

    interface Callback : Plugin {
        fun onAuthenticationSuccess()

        fun onReportProblem()
    }
}
