import extension.setupAnvil

/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

plugins {
    id("io.element.android-library")
    id("kotlin-parcelize")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.element.android.libraries.usersearch.impl"
}

setupAnvil()

dependencies {
    implementation(projects.libraries.architecture)
    implementation(projects.libraries.core)
    implementation(projects.libraries.di)
    implementation(projects.libraries.matrix.api)
    implementation(projects.libraries.usersearch.api)
    implementation(projects.features.customauth.impl)
    implementation(libs.kotlinx.collections.immutable)
    
    // Network dependencies for backend API
    implementation(projects.libraries.network)
    implementation(platform(libs.network.retrofit.bom))
    implementation(libs.network.retrofit)
    implementation(libs.network.retrofit.converter.serialization)
    
    // JSON serialization
    implementation(libs.serialization.json)
    
    // OkHttp for direct API calls
    implementation(libs.network.okhttp)
    
    // Keep other dependencies
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("com.amazonaws:aws-android-sdk-cognitoidentityprovider:2.77.0")
    
    testImplementation(libs.test.junit)
    testImplementation(libs.test.truth)
    testImplementation(projects.libraries.matrix.test)
    testImplementation(projects.libraries.usersearch.test)
}
