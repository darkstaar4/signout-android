/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.root

sealed interface PreferencesRootEvents {
    data object OnVersionInfoClick : PreferencesRootEvents
    data object OnOpenCognitoProfile : PreferencesRootEvents
    data object ClearNavigationEvent : PreferencesRootEvents
}
