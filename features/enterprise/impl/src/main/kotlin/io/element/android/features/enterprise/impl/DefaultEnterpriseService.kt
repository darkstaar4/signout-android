/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.enterprise.impl

import com.squareup.anvil.annotations.ContributesBinding
import io.element.android.compound.tokens.generated.SemanticColors
import io.element.android.compound.tokens.generated.compoundColorsDark
import io.element.android.compound.tokens.generated.compoundColorsLight
import io.element.android.features.enterprise.api.EnterpriseService
import io.element.android.libraries.di.AppScope
import io.element.android.libraries.matrix.api.core.SessionId
import javax.inject.Inject

@ContributesBinding(AppScope::class)
class DefaultEnterpriseService @Inject constructor() : EnterpriseService {
    override val isEnterpriseBuild = true

    override suspend fun isEnterpriseUser(sessionId: SessionId) = true

    override fun defaultHomeserverList(): List<String> = listOf("https://signout.io")
    override suspend fun isAllowedToConnectToHomeserver(homeserverUrl: String) = homeserverUrl == "https://signout.io"

    override fun semanticColorsLight(): SemanticColors = compoundColorsLight

    override fun semanticColorsDark(): SemanticColors = compoundColorsDark

    override fun firebasePushGateway(): String? = null
    override fun unifiedPushDefaultPushGateway(): String? = null
}
