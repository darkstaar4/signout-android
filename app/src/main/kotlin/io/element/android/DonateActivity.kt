/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import io.element.android.x.R
import timber.log.Timber

class DonateActivity : AppCompatActivity() {

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, DonateActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donate)
        
        setupViews()
    }

    private fun setupViews() {
        // Back button
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            onBackPressed()
        }

        // Monthly donation button
        findViewById<Button>(R.id.btn_donate_monthly).setOnClickListener {
            onMonthlyDonationClicked()
        }

        // Yearly donation button
        findViewById<Button>(R.id.btn_donate_yearly).setOnClickListener {
            onYearlyDonationClicked()
        }
    }

    private fun onMonthlyDonationClicked() {
        Timber.d("Monthly donation clicked: $1.25/month")
        // TODO: Implement payment flow for monthly donation
        // For now, just log the action
    }

    private fun onYearlyDonationClicked() {
        Timber.d("Yearly donation clicked: $12/year")
        // TODO: Implement payment flow for yearly donation
        // For now, just log the action
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        // Add smooth transition animation
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
} 