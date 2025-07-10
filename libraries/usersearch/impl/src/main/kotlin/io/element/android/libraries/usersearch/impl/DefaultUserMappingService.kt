/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.usersearch.impl

import android.content.Context
import android.content.SharedPreferences
import com.squareup.anvil.annotations.ContributesBinding
import io.element.android.libraries.di.ApplicationContext
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.usersearch.api.UserMapping
import io.element.android.libraries.usersearch.api.UserMappingService
import timber.log.Timber
import javax.inject.Inject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class CachedUserMapping(
    val matrixUserId: String,
    val matrixUsername: String,
    val cognitoUsername: String,
    val displayName: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val specialty: String?,
    val officeCity: String?,
    val avatarUrl: String?,
    val cachedAt: Long
)

@ContributesBinding(SessionScope::class)
class DefaultUserMappingService @Inject constructor(
    @ApplicationContext private val context: Context
) : UserMappingService {
    
    companion object {
        // Shared storage across all instances
        private val sharedUserMappings = ConcurrentHashMap<String, UserMapping>()
        private val isInitialized = AtomicBoolean(false)
        private var instanceCounter = 0
        
        // Cache settings
        private const val CACHE_EXPIRY_HOURS = 24
        private const val PREFS_NAME = "user_mapping_cache"
        private const val CACHE_KEY_PREFIX = "mapping_"
    }
    
    private val instanceId = ++instanceCounter
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    
    init {
        Timber.d("UserMappingService: [Instance $instanceId] Created instance")
        
        // Initialize shared storage synchronously on first instance
        if (isInitialized.compareAndSet(false, true)) {
            Timber.d("UserMappingService: [Instance $instanceId] Initializing shared storage synchronously")
            loadCachedMappings()
            Timber.d("UserMappingService: [Instance $instanceId] Initialized shared storage synchronously")
        }
    }
    
    override fun getUserMapping(matrixUsername: String): UserMapping? {
        val mapping = sharedUserMappings[matrixUsername]
        Timber.d("UserMappingService: [Instance $instanceId] Getting mapping for '$matrixUsername': $mapping (total mappings: ${sharedUserMappings.size})")
        return mapping
    }
    
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
    ) {
        val displayName = "$givenName $familyName"
        
        val userMapping = UserMapping(
            matrixUserId = matrixUserId,
            matrixUsername = matrixUsername,
            cognitoUsername = cognitoUsername,
            displayName = displayName,
            firstName = givenName,
            lastName = familyName,
            email = email,
            specialty = specialty,
            officeCity = officeCity,
            avatarUrl = avatarUrl
        )
        
        // Add to shared storage
        sharedUserMappings[matrixUsername] = userMapping
        
        // Cache persistently
        cacheUserMapping(matrixUsername, userMapping)
        
        Timber.d("UserMappingService: [Instance $instanceId] Added mapping for $matrixUsername: $displayName")
    }
    
    override fun searchUsers(query: String): List<UserMapping> {
        val results = sharedUserMappings.values.filter { userMapping ->
            userMapping.displayName.contains(query, ignoreCase = true) ||
            userMapping.firstName.contains(query, ignoreCase = true) ||
            userMapping.lastName.contains(query, ignoreCase = true) ||
            userMapping.matrixUsername.contains(query, ignoreCase = true) ||
            userMapping.email.contains(query, ignoreCase = true) ||
            userMapping.specialty?.contains(query, ignoreCase = true) == true ||
            userMapping.officeCity?.contains(query, ignoreCase = true) == true
        }.take(10) // Default limit of 10
        
        Timber.d("UserMappingService: [Instance $instanceId] Search for '$query' returned ${results.size} results")
        return results
    }
    
    override fun removeUser(matrixUsername: String) {
        sharedUserMappings.remove(matrixUsername)
        
        // Remove from cache
        sharedPreferences.edit()
            .remove("$CACHE_KEY_PREFIX$matrixUsername")
            .apply()
        
        Timber.d("UserMappingService: [Instance $instanceId] Removed mapping for $matrixUsername")
    }
    
    override fun clearAll() {
        Timber.d("UserMappingService: [Instance $instanceId] Clearing all mappings")
        sharedUserMappings.clear()
        
        // Clear persistent cache
        try {
            val editor = sharedPreferences.edit()
            val keys = sharedPreferences.all.keys.filter { it.startsWith("mapping_") }
            keys.forEach { key ->
                editor.remove(key)
            }
            editor.apply()
            Timber.d("UserMappingService: [Instance $instanceId] Cleared persistent cache")
        } catch (e: Exception) {
            Timber.e(e, "UserMappingService: [Instance $instanceId] Failed to clear persistent cache")
        }
    }
    
    override fun getCachedMappingsCount(): Int {
        return sharedUserMappings.size
    }
    
    private fun loadCachedMappings() {
        try {
            val currentTime = System.currentTimeMillis()
            val expiredKeys = mutableListOf<String>()
            var loadedCount = 0
            
            // Load all cached mappings
            sharedPreferences.all.forEach { (key, value) ->
                if (key.startsWith(CACHE_KEY_PREFIX) && value is String) {
                    try {
                        val cached = json.decodeFromString<CachedUserMapping>(value)
                        val ageHours = (currentTime - cached.cachedAt) / (1000 * 60 * 60)
                        
                        if (ageHours < CACHE_EXPIRY_HOURS) {
                            val userMapping = UserMapping(
                                matrixUserId = cached.matrixUserId,
                                matrixUsername = cached.matrixUsername,
                                cognitoUsername = cached.cognitoUsername,
                                displayName = cached.displayName,
                                firstName = cached.firstName,
                                lastName = cached.lastName,
                                email = cached.email,
                                specialty = cached.specialty,
                                officeCity = cached.officeCity,
                                avatarUrl = cached.avatarUrl
                            )
                            sharedUserMappings[cached.matrixUsername] = userMapping
                            loadedCount++
                        } else {
                            expiredKeys.add(key)
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "UserMappingService: [Instance $instanceId] Failed to decode cached mapping for key: $key")
                        expiredKeys.add(key)
                    }
                }
            }
            
            // Remove expired entries
            if (expiredKeys.isNotEmpty()) {
                val editor = sharedPreferences.edit()
                expiredKeys.forEach { editor.remove(it) }
                editor.apply()
            }
            
            Timber.d("UserMappingService: [Instance $instanceId] Loaded $loadedCount cached mappings, removed ${expiredKeys.size} expired entries")
        } catch (e: Exception) {
            Timber.e(e, "UserMappingService: [Instance $instanceId] Failed to load cached mappings")
        }
    }
    
    private fun cacheUserMapping(matrixUsername: String, userMapping: UserMapping) {
        try {
            val cached = CachedUserMapping(
                matrixUserId = userMapping.matrixUserId,
                matrixUsername = userMapping.matrixUsername,
                cognitoUsername = userMapping.cognitoUsername,
                displayName = userMapping.displayName,
                firstName = userMapping.firstName,
                lastName = userMapping.lastName,
                email = userMapping.email,
                specialty = userMapping.specialty,
                officeCity = userMapping.officeCity,
                avatarUrl = userMapping.avatarUrl,
                cachedAt = System.currentTimeMillis()
            )
            
            val jsonString = json.encodeToString(cached)
            sharedPreferences.edit()
                .putString("$CACHE_KEY_PREFIX$matrixUsername", jsonString)
                .apply()
            
            Timber.d("UserMappingService: [Instance $instanceId] Cached mapping for $matrixUsername persistently")
        } catch (e: Exception) {
            Timber.e(e, "UserMappingService: [Instance $instanceId] Failed to cache mapping for $matrixUsername")
        }
    }
}