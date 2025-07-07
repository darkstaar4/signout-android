/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.customauth.impl

import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.node.Node
import com.squareup.anvil.annotations.ContributesBinding
import io.element.android.features.customauth.api.CustomAuthEntryPoint
import io.element.android.libraries.architecture.createNode
import io.element.android.libraries.di.AppScope
import javax.inject.Inject

@ContributesBinding(AppScope::class)
class CustomAuthEntryPointImpl
    @Inject
    constructor() : CustomAuthEntryPoint {
        override fun nodeBuilder(
            parentNode: Node,
            buildContext: BuildContext,
        ): CustomAuthEntryPoint.NodeBuilder {
            return CustomAuthNodeBuilder(parentNode, buildContext)
        }
    }

class CustomAuthNodeBuilder(
    private val parentNode: Node,
    private val buildContext: BuildContext,
) : CustomAuthEntryPoint.NodeBuilder {
    private var params: CustomAuthEntryPoint.Params = CustomAuthEntryPoint.Params()
    private var callback: CustomAuthEntryPoint.Callback? = null

    override fun params(params: CustomAuthEntryPoint.Params): CustomAuthEntryPoint.NodeBuilder {
        this.params = params
        return this
    }

    override fun callback(callback: CustomAuthEntryPoint.Callback): CustomAuthEntryPoint.NodeBuilder {
        this.callback = callback
        return this
    }

    override fun build(): Node {
        return parentNode.createNode<CustomAuthFlowNode>(
            buildContext,
            plugins =
                listOfNotNull(
                    params,
                    callback,
                ),
        )
    }
}
