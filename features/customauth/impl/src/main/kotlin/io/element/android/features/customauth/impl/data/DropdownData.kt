/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.customauth.impl.data

import io.element.android.features.customauth.impl.R

data class DropdownOption(
    val label: String,
    val value: String,
    val iconResId: Int? = null, // Optional drawable resource ID
)

data class CountryCode(
    val code: String,
    val name: String,
    val dialCode: String,
    val flag: String,
)

object DropdownData {
    val PROFESSIONAL_TITLES =
        listOf(
            DropdownOption("MD", "MD"),
            DropdownOption("DO", "DO"),
            DropdownOption("PA-C", "PA-C"),
            DropdownOption("NP", "NP"),
            DropdownOption("RN", "RN"),
            DropdownOption("PharmD", "PharmD"),
            DropdownOption("DDS", "DDS"),
            DropdownOption("DMD", "DMD"),
            DropdownOption("DPT", "DPT"),
            DropdownOption("OTR", "OTR"),
            DropdownOption("SLP", "SLP"),
            DropdownOption("Student", "Student"),
        )

    val COUNTRIES =
        listOf(
            DropdownOption("🇺🇸 United States", "USA"),
            DropdownOption("🇨🇦 Canada", "Canada"),
        )

    val US_STATES =
        listOf(
            // US States
            DropdownOption("Alabama", "AL", R.drawable.al),
            DropdownOption("Alaska", "AK", R.drawable.ak),
            DropdownOption("Arizona", "AZ", R.drawable.az),
            DropdownOption("Arkansas", "AR", R.drawable.ar),
            DropdownOption("California", "CA", R.drawable.ca),
            DropdownOption("Colorado", "CO", R.drawable.co),
            DropdownOption("Connecticut", "CT", R.drawable.ct),
            DropdownOption("Delaware", "DE", R.drawable.de),
            DropdownOption("Florida", "FL", R.drawable.fl),
            DropdownOption("Georgia", "GA", R.drawable.ga),
            DropdownOption("Hawaii", "HI", R.drawable.hi),
            DropdownOption("Idaho", "ID", R.drawable.id),
            DropdownOption("Illinois", "IL", R.drawable.il),
            DropdownOption("Indiana", "IN", R.drawable.`in`),
            DropdownOption("Iowa", "IA", R.drawable.ia),
            DropdownOption("Kansas", "KS", R.drawable.ks),
            DropdownOption("Kentucky", "KY", R.drawable.ky),
            DropdownOption("Louisiana", "LA", R.drawable.la),
            DropdownOption("Maine", "ME", R.drawable.me),
            DropdownOption("Maryland", "MD", R.drawable.md),
            DropdownOption("Massachusetts", "MA", R.drawable.ma),
            DropdownOption("Michigan", "MI", R.drawable.mi),
            DropdownOption("Minnesota", "MN", R.drawable.mn),
            DropdownOption("Mississippi", "MS", R.drawable.ms),
            DropdownOption("Missouri", "MO", R.drawable.mo),
            DropdownOption("Montana", "MT", R.drawable.mt),
            DropdownOption("Nebraska", "NE", R.drawable.ne),
            DropdownOption("Nevada", "NV", R.drawable.nv),
            DropdownOption("New Hampshire", "NH", R.drawable.nh),
            DropdownOption("New Jersey", "NJ", R.drawable.nj),
            DropdownOption("New Mexico", "NM", R.drawable.nm),
            DropdownOption("New York", "NY", R.drawable.ny),
            DropdownOption("North Carolina", "NC", R.drawable.nc),
            DropdownOption("North Dakota", "ND", R.drawable.nd),
            DropdownOption("Ohio", "OH", R.drawable.oh),
            DropdownOption("Oklahoma", "OK", R.drawable.ok),
            DropdownOption("Oregon", "OR", R.drawable.or),
            DropdownOption("Pennsylvania", "PA", R.drawable.pa),
            DropdownOption("Rhode Island", "RI", R.drawable.ri),
            DropdownOption("South Carolina", "SC", R.drawable.sc),
            DropdownOption("South Dakota", "SD", R.drawable.sd),
            DropdownOption("Tennessee", "TN", R.drawable.tn),
            DropdownOption("Texas", "TX", R.drawable.tx),
            DropdownOption("Utah", "UT", R.drawable.ut),
            DropdownOption("Vermont", "VT", R.drawable.vt),
            DropdownOption("Virginia", "VA", R.drawable.va),
            DropdownOption("Washington", "WA", R.drawable.wa),
            DropdownOption("West Virginia", "WV", R.drawable.wv),
            DropdownOption("Wisconsin", "WI", R.drawable.wi),
            DropdownOption("Wyoming", "WY", R.drawable.wy),
            // US Territories (keeping emojis for now since we don't have custom flags)
            DropdownOption("🏴󠁵󠁳󠁤󠁣󠁿 District of Columbia", "DC"),
            DropdownOption("🇵🇷 Puerto Rico", "PR"),
            DropdownOption("🇻🇮 U.S. Virgin Islands", "VI"),
            DropdownOption("🇬🇺 Guam", "GU"),
            DropdownOption("🇦🇸 American Samoa", "AS"),
            DropdownOption("🇲🇵 Northern Mariana Islands", "MP"),
        )

    val CANADIAN_PROVINCES =
        listOf(
            DropdownOption("🏴󠁣󠁡󠁡󠁢󠁿 Alberta", "AB"),
            DropdownOption("🏴󠁣󠁡󠁢󠁣󠁿 British Columbia", "BC"),
            DropdownOption("🏴󠁣󠁡󠁭󠁢󠁿 Manitoba", "MB"),
            DropdownOption("🏴󠁣󠁡󠁮󠁢󠁿 New Brunswick", "NB"),
            DropdownOption("🏴󠁣󠁡󠁮󠁬󠁿 Newfoundland and Labrador", "NL"),
            DropdownOption("🏴󠁣󠁡󠁮󠁴󠁿 Northwest Territories", "NT"),
            DropdownOption("🏴󠁣󠁡󠁮󠁳󠁿 Nova Scotia", "NS"),
            DropdownOption("🏴󠁣󠁡󠁮󠁵󠁿 Nunavut", "NU"),
            DropdownOption("🏴󠁣󠁡󠁯󠁮󠁿 Ontario", "ON"),
            DropdownOption("🏴󠁣󠁡󠁰󠁥󠁿 Prince Edward Island", "PE"),
            DropdownOption("🏴󠁣󠁡󠁱󠁣󠁿 Quebec", "QC"),
            DropdownOption("🏴󠁣󠁡󠁳󠁫󠁿 Saskatchewan", "SK"),
            DropdownOption("🏴󠁣󠁡󠁹󠁴󠁿 Yukon", "YT"),
        )

    val MEDICAL_SPECIALTIES =
        listOf(
            DropdownOption("Student", "Student"),
            DropdownOption("Other", "Other"),
            DropdownOption("Addiction Medicine", "Addiction Medicine"),
            DropdownOption("Allergy and Immunology", "Allergy and Immunology"),
            DropdownOption("Anesthesiology", "Anesthesiology"),
            DropdownOption("Cardiology", "Cardiology"),
            DropdownOption("Cardiothoracic Surgery", "Cardiothoracic Surgery"),
            DropdownOption("Critical Care Medicine", "Critical Care Medicine"),
            DropdownOption("Dermatology", "Dermatology"),
            DropdownOption("Emergency Medicine", "Emergency Medicine"),
            DropdownOption("Endocrinology", "Endocrinology"),
            DropdownOption("Family Medicine", "Family Medicine"),
            DropdownOption("Gastroenterology", "Gastroenterology"),
            DropdownOption("General Surgery", "General Surgery"),
            DropdownOption("Geriatric Medicine", "Geriatric Medicine"),
            DropdownOption("Hematology", "Hematology"),
            DropdownOption("Hematology/Oncology", "Heme/Onc"),
            DropdownOption("Hospitalist", "Hospitalist"),
            DropdownOption("Infectious Disease", "Infectious Disease"),
            DropdownOption("Internal Medicine", "Internal Medicine"),
            DropdownOption("Interventional Cardiology", "Interventional Cardiology"),
            DropdownOption("Interventional Radiology", "Interventional Radiology"),
            DropdownOption("Nephrology", "Nephrology"),
            DropdownOption("Neurology", "Neurology"),
            DropdownOption("Neurosurgery", "Neurosurgery"),
            DropdownOption("Nuclear Medicine", "Nuclear Medicine"),
            DropdownOption("Obesity Medicine", "Obesity Medicine"),
            DropdownOption("Obstetrics & Gynecology", "Obstetrics & Gynecology"),
            DropdownOption("Occupational Medicine", "Occupational Medicine"),
            DropdownOption("Oncology", "Oncology"),
            DropdownOption("Ophthalmology", "Ophthalmology"),
            DropdownOption("Orthopedic Surgery", "Orthopedic Surgery"),
            DropdownOption("Otolaryngology", "Otolaryngology"),
            DropdownOption("Pain Medicine", "Pain Medicine"),
            DropdownOption("Palliative Care", "Palliative Care"),
            DropdownOption("Pathology", "Pathology"),
            DropdownOption("Pediatrics", "Pediatrics"),
            DropdownOption("Physical Medicine & Rehabilitation", "Physical Medicine & Rehabilitation"),
            DropdownOption("Plastic Surgery", "Plastic Surgery"),
            DropdownOption("Podiatry", "Podiatry"),
            DropdownOption("Preventive Medicine", "Preventive Medicine"),
            DropdownOption("Psychiatry", "Psychiatry"),
            DropdownOption("Psychology", "Psychology"),
            DropdownOption("Pulmonology", "Pulmonology"),
            DropdownOption("Radiation Oncology", "Radiation Oncology"),
            DropdownOption("Radiology", "Radiology"),
            DropdownOption("Registered Nurse", "Registered Nurse"),
            DropdownOption("Rheumatology", "Rheumatology"),
            DropdownOption("Sleep Medicine", "Sleep Medicine"),
            DropdownOption("Sports Medicine", "Sports Medicine"),
            DropdownOption("Trauma Surgery", "Trauma Surgery"),
            DropdownOption("Urgent Care", "Urgent Care"),
            DropdownOption("Urology", "Urology"),
            DropdownOption("Vascular Surgery", "Vascular Surgery"),
        )

    val COUNTRY_CODES =
        listOf(
            CountryCode("US", "United States", "+1", "🇺🇸"),
            CountryCode("CA", "Canada", "+1", "🇨🇦"),
        )

    // Helper function to get states/provinces based on country
    fun getStatesForCountry(countryValue: String): List<DropdownOption> {
        return when (countryValue) {
            "USA" -> US_STATES
            "Canada" -> CANADIAN_PROVINCES
            else -> emptyList()
        }
    }

    // Helper function to check if a country requires state selection
    fun countryRequiresState(countryValue: String): Boolean {
        return listOf("USA", "Canada").contains(countryValue)
    }
} 
