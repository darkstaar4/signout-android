/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.home.impl.datasource

import io.element.android.libraries.dateformatter.api.DateFormatter
import io.element.android.libraries.dateformatter.test.FakeDateFormatter
import io.element.android.libraries.eventformatter.api.RoomLastMessageFormatter
import io.element.android.libraries.usersearch.api.CognitoUserIntegrationService
import io.element.android.libraries.usersearch.api.UserMapping
import io.element.android.libraries.usersearch.api.UserMappingService
import kotlinx.coroutines.test.TestScope

fun aRoomListRoomSummaryFactory(
    dateFormatter: DateFormatter = FakeDateFormatter { _, _, _ -> "Today" },
    roomLastMessageFormatter: RoomLastMessageFormatter = RoomLastMessageFormatter { _, _ -> "Hey" },
    userMappingService: UserMappingService = FakeUserMappingService(),
    cognitoUserIntegrationService: CognitoUserIntegrationService = FakeCognitoUserIntegrationService(),
    sessionCoroutineScope: TestScope = TestScope()
) = RoomListRoomSummaryFactory(
    dateFormatter = dateFormatter,
    roomLastMessageFormatter = roomLastMessageFormatter,
    userMappingService = userMappingService,
    cognitoUserIntegrationService = cognitoUserIntegrationService,
    sessionCoroutineScope = sessionCoroutineScope
)

class FakeUserMappingService : UserMappingService {
    override fun getUserMapping(matrixUsername: String): UserMapping? = null
    
    override fun addUserFromCognitoData(
        matrixUserId: String,
        matrixUsername: String,
        cognitoUsername: String,
        givenName: String,
        familyName: String,
        email: String,
        specialty: String?,
        officeCity: String?,
        avatarUrl: String?
    ) { }
    
    override fun searchUsers(query: String): List<UserMapping> = emptyList()
    
    override fun removeUser(matrixUsername: String) { }
    
    override fun clearAll() { }
    
    override fun getCachedMappingsCount(): Int = 0
}

class FakeCognitoUserIntegrationService : CognitoUserIntegrationService {
    override suspend fun populateCurrentUserMapping() { }
    
    override suspend fun discoverUserMapping(matrixUserId: String, matrixDisplayName: String?) { }
    
    override suspend fun extractMappingFromMatrixData(
        matrixUserId: String,
        matrixDisplayName: String?,
        matrixUsername: String
    ) { }
}
