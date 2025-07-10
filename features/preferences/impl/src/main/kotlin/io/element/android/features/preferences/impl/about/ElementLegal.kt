/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.about

import androidx.annotation.StringRes
import io.element.android.features.preferences.impl.R

private const val LEGAL_URL = "https://www.getsignout.com/legal"

sealed class ElementLegal(
    @StringRes val titleRes: Int,
    val url: String,
) {
    data object Legal : ElementLegal(R.string.action_legal, LEGAL_URL)
}

fun getAllLegals(): List<ElementLegal> {
    return listOf(
        ElementLegal.Legal,
    )
}
