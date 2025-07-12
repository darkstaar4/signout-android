/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.timeline.postprocessor

import io.element.android.libraries.matrix.api.timeline.MatrixTimelineItem
import io.element.android.libraries.matrix.api.timeline.item.event.MembershipChange
import io.element.android.libraries.matrix.api.timeline.item.event.RoomMembershipContent
import timber.log.Timber

/**
 * This timeline post-processor filters out MembershipChange.NONE events (i.e., "made no changes" notifications)
 * from the timeline to prevent them from being displayed or counted.
 */
class MembershipChangeFilterPostProcessor {
    fun process(items: List<MatrixTimelineItem>): List<MatrixTimelineItem> {
        return items.filter { item ->
            if (item is MatrixTimelineItem.Event) {
                val content = item.event.content as? RoomMembershipContent
                if (content?.change == MembershipChange.NONE) {
                    Timber.v("Filtering out MembershipChange.NONE event from timeline: ${item.event.eventId}")
                    false
                } else {
                    true
                }
            } else {
                true
            }
        }
    }
} 