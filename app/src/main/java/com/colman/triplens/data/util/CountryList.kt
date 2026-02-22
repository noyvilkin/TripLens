package com.colman.triplens.data.util

/**
 * Minimal offline fallback for country names.
 *
 * The primary source is the RestCountries API (fetched dynamically
 * via [com.colman.triplens.data.repo.PostRepository.fetchAllCountryNames]).
 * This list is only used if the API call fails (e.g. no network).
 */
object CountryList {

    /**
     * Small fallback list – covers the most commonly searched travel
     * destinations so the autocomplete still works offline.
     * The full list (~250 countries) comes from the API at runtime.
     */
    val FALLBACK_COUNTRIES: List<String> = listOf(
        "Argentina", "Australia", "Austria", "Belgium", "Brazil",
        "Canada", "Chile", "China", "Colombia", "Croatia",
        "Czech Republic", "Denmark", "Egypt", "Finland", "France",
        "Germany", "Greece", "Hungary", "Iceland", "India",
        "Indonesia", "Ireland", "Israel", "Italy", "Japan",
        "Jordan", "Kenya", "Malaysia", "Mexico", "Morocco",
        "Netherlands", "New Zealand", "Norway", "Peru", "Philippines",
        "Poland", "Portugal", "Romania", "Russia", "Saudi Arabia",
        "Singapore", "South Africa", "South Korea", "Spain", "Sweden",
        "Switzerland", "Thailand", "Turkey", "United Arab Emirates",
        "United Kingdom", "United States", "Vietnam"
    )
}
