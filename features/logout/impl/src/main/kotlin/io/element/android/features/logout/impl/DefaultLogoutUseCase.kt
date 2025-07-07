/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.logout.impl

import com.squareup.anvil.annotations.ContributesBinding
import io.element.android.features.logout.api.LogoutUseCase
import io.element.android.features.customauth.impl.auth.CognitoAuthService
import io.element.android.libraries.di.AppScope
import io.element.android.libraries.matrix.api.MatrixClientProvider
import io.element.android.libraries.matrix.api.auth.MatrixAuthenticationService
import timber.log.Timber
import javax.inject.Inject

@ContributesBinding(AppScope::class)
class DefaultLogoutUseCase @Inject constructor(
    private val authenticationService: MatrixAuthenticationService,
    private val matrixClientProvider: MatrixClientProvider,
    private val cognitoAuthService: CognitoAuthService,
) : LogoutUseCase {
    override suspend fun logout(ignoreSdkError: Boolean) {
        try {
            // First, logout from Matrix
            val currentSession = authenticationService.getLatestSessionId()
            if (currentSession != null) {
                Timber.d("Logging out from Matrix session: $currentSession")
                matrixClientProvider.getOrRestore(currentSession)
                    .getOrThrow()
                    .logout(userInitiated = true, ignoreSdkError = true)
                Timber.d("Matrix logout successful")
            } else {
                Timber.w("No Matrix session to sign out from")
            }
            
            // Then, logout from Cognito
            Timber.d("Logging out from Cognito")
            cognitoAuthService.signOut()
            Timber.d("Cognito logout successful")
            
        } catch (exception: Exception) {
            Timber.e(exception, "Error during logout process")
            if (!ignoreSdkError) {
                throw exception
            }
        }
    }
}
