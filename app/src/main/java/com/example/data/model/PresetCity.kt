package com.example.data.model

data class PresetCity(
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val codeName: String
) {
    companion object {
        val DEFAULT = PresetCity("Paris", "France", 48.8566, 2.3522, "PAR")
        
        val CITIES = listOf(
            PresetCity("New York", "USA", 40.7128, -74.0060, "NYC"),
            PresetCity("Paris", "France", 48.8566, 2.3522, "PAR"),
            PresetCity("Tokyo", "Japan", 35.6762, 139.6503, "TYO"),
            PresetCity("London", "UK", 51.5074, -0.1278, "LON"),
            PresetCity("Sydney", "Australia", -33.8688, 151.2093, "SYD"),
            PresetCity("Cairo", "Egypt", 30.0444, 31.2357, "CAI"),
            PresetCity("Mumbai", "India", 19.0760, 72.8777, "BOM"),
            PresetCity("Rio de Janeiro", "Brazil", -22.9068, -43.1729, "RIO")
        )
    }
}
