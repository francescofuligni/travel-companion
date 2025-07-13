package com.example.travelcompanion.database.models

/**
 * Enum per i tipi di viaggio
 */
enum class TripType {
    LOCAL, ONE_DAY, MULTI_DAYS;

    companion object {
        fun fromString(value: String): TripType? =
            entries.find { it.name.equals(value, ignoreCase = true) }
    }
}
