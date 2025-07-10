import extension.setupAnvil

/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

plugins {
    id("io.element.android-library")
}

android {
    namespace = "io.element.android.services.push.impl"
}

setupAnvil()

dependencies {
    implementation(libs.dagger)
    implementation(libs.androidx.corektx)
    implementation(projects.libraries.architecture)
    implementation(projects.libraries.core)
    implementation(projects.libraries.di)
    implementation(projects.libraries.matrix.api)
    implementation(projects.libraries.uiStrings)
    
    // Firebase Cloud Messaging
    api(platform(libs.google.firebase.bom))
    api("com.google.firebase:firebase-messaging-ktx") {
        exclude(group = "com.google.firebase", module = "firebase-core")
        exclude(group = "com.google.firebase", module = "firebase-analytics")
        exclude(group = "com.google.firebase", module = "firebase-measurement-connector")
    }
    
    // Testing
    testImplementation(libs.coroutines.test)
    testImplementation(libs.test.junit)
    testImplementation(libs.test.truth)
    testImplementation(libs.test.turbine)
    testImplementation(libs.test.robolectric)
    testImplementation(projects.tests.testutils)
} 