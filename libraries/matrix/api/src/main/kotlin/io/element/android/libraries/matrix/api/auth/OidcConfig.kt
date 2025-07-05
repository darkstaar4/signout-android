/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.api.auth

import io.element.android.libraries.matrix.api.BuildConfig

object OidcConfig {
    const val CLIENT_URI = BuildConfig.CLIENT_URI

    // Note: host must match with the host of CLIENT_URI
    const val LOGO_URI = BuildConfig.LOGO_URI

    // Note: host must match with the host of CLIENT_URI
    const val TOS_URI = BuildConfig.TOS_URI

    // Note: host must match with the host of CLIENT_URI
    const val POLICY_URI = BuildConfig.POLICY_URI

    // AWS Cognito Configuration for SignOut
    // TODO: Replace these with your actual Cognito settings
    private const val COGNITO_DOMAIN = "https://signout-auth.auth.us-east-1.amazoncognito.com"
    private const val COGNITO_CLIENT_ID = "signout-android-client"
    private const val COGNITO_USER_POOL_ID = "us-east-1_XXXXXXXXX"
    private const val COGNITO_ISSUER = "https://cognito-idp.us-east-1.amazonaws.com/$COGNITO_USER_POOL_ID"

    // Some homeservers/auth issuers don't support dynamic client registration, and have to be registered manually
    val STATIC_REGISTRATIONS = mapOf(
        "https://id.thirdroom.io/realms/thirdroom" to "elementx",
        // AWS Cognito for SignOut server
        COGNITO_DOMAIN to COGNITO_CLIENT_ID,
        COGNITO_ISSUER to COGNITO_CLIENT_ID,
    )
}
