/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.user.editprofile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.core.plugin.Plugin
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.element.android.anvilannotations.ContributesNode
import io.element.android.libraries.di.SessionScope

@ContributesNode(SessionScope::class)
class CognitoProfileEditNode @AssistedInject constructor(
    @Assisted buildContext: BuildContext,
    @Assisted plugins: List<Plugin>,
    private val presenter: CognitoProfileEditPresenter,
) : Node(buildContext, plugins = plugins) {

    @Composable
    override fun View(modifier: Modifier) {
        android.util.Log.d("elementx", "CognitoProfileEditNode: View() called")
        val state = presenter.present()
        android.util.Log.d("elementx", "CognitoProfileEditNode: Presenter state obtained")
        CognitoProfileEditView(
            state = state,
            onBackClick = this::navigateUp,
            modifier = modifier,
        )
    }
} 