import config.BuildTimeConfig
import extension.buildConfigFieldStr
import extension.setupAnvil

/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

plugins {
    id("io.element.android-compose-library")
    id("kotlin-parcelize")
}

android {
    namespace = "io.element.android.features.preferences.impl"
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigFieldStr(
            name = "URL_COPYRIGHT",
            value = BuildTimeConfig.URL_COPYRIGHT ?: "https://element.io/copyright",
        )
        buildConfigFieldStr(
            name = "URL_ACCEPTABLE_USE",
            value = BuildTimeConfig.URL_ACCEPTABLE_USE ?: "https://element.io/acceptable-use-policy-terms",
        )
        buildConfigFieldStr(
            name = "URL_PRIVACY",
            value = BuildTimeConfig.URL_PRIVACY ?: "https://element.io/privacy",
        )
    }
}

setupAnvil()

dependencies {
    implementation(projects.libraries.androidutils)
    implementation(projects.appconfig)
    implementation(projects.libraries.core)
    implementation(projects.libraries.architecture)
    implementation(projects.libraries.matrix.api)
    implementation(projects.libraries.designsystem)
    implementation(projects.libraries.featureflag.api)
    implementation(projects.libraries.featureflag.ui)
    implementation(projects.libraries.network)
    implementation(projects.libraries.pushstore.api)
    implementation(projects.libraries.indicator.api)
    implementation(projects.libraries.preferences.api)
    implementation(projects.libraries.troubleshoot.api)
    implementation(projects.libraries.testtags)
    implementation(projects.libraries.uiStrings)
    implementation(projects.libraries.matrixui)
    implementation(projects.libraries.mediapickers.api)
    implementation(projects.libraries.mediaupload.api)
    implementation(projects.libraries.permissions.api)
    implementation(projects.libraries.push.api)
    implementation(projects.libraries.pushproviders.api)
    implementation(projects.libraries.uiUtils)
    implementation(projects.libraries.fullscreenintent.api)
    implementation(projects.features.rageshake.api)
    implementation(projects.features.lockscreen.api)
    implementation(projects.features.analytics.api)
    implementation(projects.features.ftue.api)
    implementation(projects.features.licenses.api)
    implementation(projects.features.logout.api)
    implementation(projects.features.deactivation.api)
    implementation(projects.features.home.api)
    implementation(projects.features.invite.api)
    implementation(projects.features.customauth.impl)
    implementation(projects.services.analytics.api)
    implementation(projects.services.analytics.compose)
    implementation(projects.services.appnavstate.api)
    implementation(projects.services.toolbox.api)
    implementation(libs.datetime)
    implementation(libs.coil.compose)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.datastore.preferences)
    
    // AWS Cognito dependencies for user attribute access
    implementation("com.amazonaws:aws-android-sdk-cognitoidentityprovider:2.75.0")
    implementation("com.amazonaws:aws-android-sdk-core:2.75.0")
    
    api(projects.features.preferences.api)

    implementation(platform(libs.network.okhttp.bom))
    implementation(libs.network.okhttp)

    testImplementation(libs.test.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.molecule.runtime)
    testImplementation(libs.test.truth)
    testImplementation(libs.test.turbine)
    testImplementation(libs.test.mockk)
    testImplementation(libs.test.robolectric)
    testImplementation(projects.libraries.matrix.test)
    testImplementation(projects.libraries.featureflag.test)
    testImplementation(projects.libraries.mediapickers.test)
    testImplementation(projects.libraries.mediaupload.test)
    testImplementation(projects.libraries.permissions.test)
    testImplementation(projects.libraries.preferences.test)
    testImplementation(projects.libraries.push.test)
    testImplementation(projects.libraries.pushstore.test)
    testImplementation(projects.features.ftue.test)
    testImplementation(projects.features.invite.test)
    testImplementation(projects.features.rageshake.test)
    testImplementation(projects.features.logout.test)
    testImplementation(projects.libraries.indicator.test)
    testImplementation(projects.libraries.pushproviders.test)
    testImplementation(projects.services.analytics.test)
    testImplementation(projects.services.toolbox.test)
    testImplementation(projects.tests.testutils)
    testImplementation(libs.androidx.compose.ui.test.junit)
    testReleaseImplementation(libs.androidx.compose.ui.test.manifest)
}
