/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.customauth.impl.screens.login

data class LoginState(
    val username: String,
    val password: String,
    val isLoading: Boolean,
    val errorMessage: String?,
    val canSubmit: Boolean,
    val isLoginSuccessful: Boolean,
    val eventSink: (LoginEvents) -> Unit,
)

sealed interface LoginEvents {
    data class SetUsername(val username: String) : LoginEvents

    data class SetPassword(val password: String) : LoginEvents

    data object Submit : LoginEvents

    data object ClearError : LoginEvents

    data class ForgotPassword(val username: String) : LoginEvents

    data class ResetPassword(
        val tempPassword: String,
        val newPassword: String,
        val confirmPassword: String,
    ) : LoginEvents
}
