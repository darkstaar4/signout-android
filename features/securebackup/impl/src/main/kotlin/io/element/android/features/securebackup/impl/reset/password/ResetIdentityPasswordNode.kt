/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.securebackup.impl.reset.password

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.core.plugin.Plugin
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.element.android.anvilannotations.ContributesNode
import io.element.android.libraries.architecture.NodeInputs
import io.element.android.libraries.architecture.inputs
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.matrix.api.encryption.IdentityPasswordResetHandle

@ContributesNode(SessionScope::class)
class ResetIdentityPasswordNode @AssistedInject constructor(
    @Assisted buildContext: BuildContext,
    @Assisted plugins: List<Plugin>,
    private val presenterFactory: ResetIdentityPasswordPresenter.Factory,
) : Node(buildContext, plugins = plugins) {
    data class Inputs(val handle: IdentityPasswordResetHandle) : NodeInputs

    @Composable
    override fun View(modifier: Modifier) {
        val handle = inputs<Inputs>().handle
        val presenter = presenterFactory.create(handle)
        val state = presenter.present()
        ResetIdentityPasswordView(
            state = state,
            onBack = ::navigateUp
        )
    }
}
