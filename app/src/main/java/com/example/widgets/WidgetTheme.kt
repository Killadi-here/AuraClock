package com.example.widgets

import android.graphics.Color

enum class WidgetTheme(
    val id: String,
    val displayName: String,
    val primaryColor: Int,
    val secondaryColor: Int,
    val backgroundColor: Int,
    val textColor: Int,
    val labelColor: Int
) {
    RETRO_FLIP(
        id = "retro_flip",
        displayName = "Retro Flip",
        primaryColor = Color.parseColor("#FF9800"), // Amber
        secondaryColor = Color.parseColor("#3E2723"), // Dark Brown
        backgroundColor = Color.parseColor("#1A0F0E"), // Deep card black
        textColor = Color.parseColor("#FFE082"), // Light Amber
        labelColor = Color.parseColor("#8D6E63") // Muted brown
    ),
    NEON_CYBER(
        id = "neon_cyber",
        displayName = "Neon Cyber",
        primaryColor = Color.parseColor("#00F0FF"), // Cyan glow
        secondaryColor = Color.parseColor("#FF007A"), // Pink accent
        backgroundColor = Color.parseColor("#0C0019"), // Cyber dark
        textColor = Color.parseColor("#00F0FF"),
        labelColor = Color.parseColor("#FF007A")
    ),
    MINIMAL_SWISS(
        id = "minimal_swiss",
        displayName = "Minimal Swiss",
        primaryColor = Color.parseColor("#D32F2F"), // Red accent
        secondaryColor = Color.parseColor("#1976D2"), // Deep blue
        backgroundColor = Color.parseColor("#F5F5F5"), // Clean white
        textColor = Color.parseColor("#212121"), // Black
        labelColor = Color.parseColor("#757575") // Muted gray
    ),
    COSMIC_DARK(
        id = "cosmic_dark",
        displayName = "Cosmic Dark",
        primaryColor = Color.parseColor("#9C27B0"), // Purple
        secondaryColor = Color.parseColor("#00E5FF"), // Soft cyan
        backgroundColor = Color.parseColor("#0F1026"), // Deep space obsidian
        textColor = Color.parseColor("#E0E0FF"), // Soft white-blue
        labelColor = Color.parseColor("#7C4DFF") // Violet
    ),
    AQUA_GLASS(
        id = "aqua_glass",
        displayName = "Aqua Glass",
        primaryColor = Color.parseColor("#00BFA5"), // Turquoise
        secondaryColor = Color.parseColor("#4DD0E1"), // Light blue
        backgroundColor = Color.parseColor("#112233"), // Frosted deep blue
        textColor = Color.parseColor("#E0F7FA"), // Turquoise ice
        labelColor = Color.parseColor("#80DEEA") // Aqua gray
    );

    companion object {
        fun getById(id: String): WidgetTheme {
            return values().firstOrNull { it.id == id } ?: RETRO_FLIP
        }
    }
}
