/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

import extension.setupAnvil

plugins {
    id("io.element.android-compose-library")
    id("kotlin-parcelize")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.element.android.features.customauth.impl"
}

setupAnvil()

dependencies {
    implementation(projects.anvilannotations)
    implementation(projects.appconfig)
    implementation(projects.features.customauth.api)
    implementation(projects.libraries.architecture)
    implementation(projects.libraries.androidutils)
    implementation(projects.libraries.core)
    implementation(projects.libraries.designsystem)
    implementation(projects.libraries.di)
    implementation(projects.libraries.matrix.api)
    implementation(projects.libraries.matrixui)
    implementation(projects.libraries.network)
    implementation(projects.libraries.preferences.api)
    implementation(projects.libraries.uiStrings)
    implementation(projects.services.analytics.api)
    implementation(projects.features.login.impl)

    // AWS Cognito dependencies
    implementation("com.amazonaws:aws-android-sdk-cognitoidentityprovider:2.75.0")
    implementation("com.amazonaws:aws-android-sdk-core:2.75.0")

    // Compose UI dependencies
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)

    // Image loading
    implementation(libs.coil.compose)

    // JSON serialization
    implementation(libs.serialization.json)

    // HTTP client
    implementation(libs.network.okhttp)

    // Coroutines
    implementation(libs.coroutines.core)

    // Testing
    testImplementation(libs.test.junit)
    testImplementation(libs.test.truth)
    testImplementation(libs.test.turbine)
    testImplementation(libs.test.robolectric)
    testImplementation(libs.test.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(projects.tests.testutils)
    testImplementation(libs.androidx.compose.ui.test.junit)
    testReleaseImplementation(libs.androidx.compose.ui.test.manifest)


}
