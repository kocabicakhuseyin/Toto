package com.example.model

/**
 * Filter Mode for Toto filters:
 * - PROBABILITY_STRICT: "Filtrelerim Tam Uygulansın İHTİMALLİ"
 * - GUARANTEE_FLEXIBLE: "Filtrelerim Esnek Uygulansın GARANTİLİ"
 */
enum class FilterApplicationMode(val title: String, val subtitle: String) {
    PROBABILITY_STRICT("Filtrelerim Tam Uygulansın", "İHTİMALLİ"),
    GUARANTEE_FLEXIBLE("Filtrelerim Esnek Uygulansın", "GARANTİLİ")
}

/**
 * Filter Criteria model representing all filters shown in user screenshots:
 * - Total Surprises (Beraberlik + Ters Sürpriz)
 * - Total Draws (0 / X)
 * - Total Hard Surprises (Ters galibiyet)
 * - Total 1s and 2s count
 * - 1-9 Matches Sub-filters
 * - 10-15 Matches Sub-filters
 * - Consecutive Streaks (Art arda gelen 1, 0, 2)
 * - Custom Match Surprise list
 */
data class TotoFilterCriteria(
    val applicationMode: FilterApplicationMode = FilterApplicationMode.PROBABILITY_STRICT,

    // 15 Maç Genel Filtreleri (Empty set means filter is not active)
    val allowedTotalSurprises: Set<Int> = emptySet(), // 0..6
    val allowedTotalDraws: Set<Int> = emptySet(),     // 0..6
    val allowedTotalHardSurprises: Set<Int> = emptySet(), // 0..3
    val allowedCountOf1s: Set<Int> = emptySet(),      // 7..14
    val allowedCountOf2s: Set<Int> = emptySet(),      // 1..5

    // 1-9 Maçları (Süper Lig / İlk Yarı)
    val allowedSurprises1To9: Set<Int> = emptySet(),
    val allowedDraws1To9: Set<Int> = emptySet(),
    val allowedHardSurprises1To9: Set<Int> = emptySet(),

    // 10-15 Maçları (Avrupa / İkinci Yarı)
    val allowedSurprises10To15: Set<Int> = emptySet(),
    val allowedDraws10To15: Set<Int> = emptySet(),
    val allowedHardSurprises10To15: Set<Int> = emptySet(),

    // Ard Arda Gelenler (Seri) Filtresi
    val maxConsecutive1s: Int? = null, // e.g. max 3 or 4
    val maxConsecutiveDraws: Int? = null, // e.g. max 2 or 3
    val maxConsecutive2s: Int? = null, // e.g. max 2

    // Özel Sürpriz Filtreleme
    val customSurpriseMatches: Set<Int> = emptySet(), // e.g. {9, 10, 12, 13, 14, 15}
    val allowedCustomSurprisesCount: Set<Int> = emptySet() // e.g. {1, 2, 3}
) {
    val isActive: Boolean
        get() = allowedTotalSurprises.isNotEmpty() ||
                allowedTotalDraws.isNotEmpty() ||
                allowedTotalHardSurprises.isNotEmpty() ||
                allowedCountOf1s.isNotEmpty() ||
                allowedCountOf2s.isNotEmpty() ||
                allowedSurprises1To9.isNotEmpty() ||
                allowedDraws1To9.isNotEmpty() ||
                allowedHardSurprises1To9.isNotEmpty() ||
                allowedSurprises10To15.isNotEmpty() ||
                allowedDraws10To15.isNotEmpty() ||
                allowedHardSurprises10To15.isNotEmpty() ||
                maxConsecutive1s != null ||
                maxConsecutiveDraws != null ||
                maxConsecutive2s != null ||
                (customSurpriseMatches.isNotEmpty() && allowedCustomSurprisesCount.isNotEmpty())

    val activeFilterCount: Int
        get() {
            var count = 0
            if (allowedTotalSurprises.isNotEmpty()) count++
            if (allowedTotalDraws.isNotEmpty()) count++
            if (allowedTotalHardSurprises.isNotEmpty()) count++
            if (allowedCountOf1s.isNotEmpty()) count++
            if (allowedCountOf2s.isNotEmpty()) count++
            if (allowedSurprises1To9.isNotEmpty()) count++
            if (allowedDraws1To9.isNotEmpty()) count++
            if (allowedHardSurprises1To9.isNotEmpty()) count++
            if (allowedSurprises10To15.isNotEmpty()) count++
            if (allowedDraws10To15.isNotEmpty()) count++
            if (allowedHardSurprises10To15.isNotEmpty()) count++
            if (maxConsecutive1s != null) count++
            if (maxConsecutiveDraws != null) count++
            if (maxConsecutive2s != null) count++
            if (customSurpriseMatches.isNotEmpty() && allowedCustomSurprisesCount.isNotEmpty()) count++
            return count
        }
}

/**
 * 1-Click Ready-to-Use Filter Profiles requested by User:
 * - FAVORITE_WEIGHTED ('Favori Ağırlıklı')
 * - BALANCED ('Dengeli')
 * - SURPRISE_WEIGHTED ('Sürpriz Oran')
 */
enum class FilterProfileType(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconEmoji: String,
    val badge: String,
    val brandColorHex: Long
) {
    FAVORITE_WEIGHTED(
        id = "favori",
        title = "Favori Ağırlıklı",
        subtitle = "0-2 Sürpriz, Güvenli & Sağlam",
        iconEmoji = "🛡️",
        badge = "Kasa Güvenli",
        brandColorHex = 0xFF10B981 // Emerald Green
    ),
    BALANCED(
        id = "dengeli",
        title = "Dengeli",
        subtitle = "2-4 Sürpriz, Standart Dağılım",
        iconEmoji = "⚖️",
        badge = "Tavsiye Edilen",
        brandColorHex = 0xFF0284C7 // Sky Blue
    ),
    SURPRISE_WEIGHTED(
        id = "surpriz",
        title = "Sürpriz Oran",
        subtitle = "4-6 Sürpriz, Büyük İkramiye",
        iconEmoji = "💥",
        badge = "Yüksek Kazanç",
        brandColorHex = 0xFFF59E0B // Gold / Amber
    );

    val displayName: String get() = title

    fun toCriteria(): TotoFilterCriteria {
        return when (this) {
            FAVORITE_WEIGHTED -> TotoFilterCriteria(
                applicationMode = FilterApplicationMode.PROBABILITY_STRICT,
                allowedTotalSurprises = setOf(0, 1, 2),
                allowedTotalDraws = setOf(0, 1, 2),
                allowedTotalHardSurprises = setOf(0, 1),
                allowedCountOf1s = setOf(9, 10, 11, 12, 13, 14),
                allowedCountOf2s = setOf(1, 2),
                maxConsecutive1s = 4
            )
            BALANCED -> TotoFilterCriteria(
                applicationMode = FilterApplicationMode.GUARANTEE_FLEXIBLE,
                allowedTotalSurprises = setOf(2, 3, 4),
                allowedTotalDraws = setOf(1, 2, 3),
                allowedTotalHardSurprises = setOf(0, 1, 2),
                allowedCountOf1s = setOf(7, 8, 9, 10, 11),
                allowedCountOf2s = setOf(1, 2, 3, 4),
                allowedSurprises1To9 = setOf(1, 2, 3),
                allowedSurprises10To15 = setOf(1, 2, 3),
                maxConsecutive1s = 3,
                maxConsecutiveDraws = 2
            )
            SURPRISE_WEIGHTED -> TotoFilterCriteria(
                applicationMode = FilterApplicationMode.PROBABILITY_STRICT,
                allowedTotalSurprises = setOf(4, 5, 6),
                allowedTotalDraws = setOf(2, 3, 4, 5),
                allowedTotalHardSurprises = setOf(1, 2, 3),
                allowedCountOf1s = setOf(7, 8, 9),
                allowedCountOf2s = setOf(2, 3, 4, 5),
                allowedSurprises10To15 = setOf(2, 3, 4, 5),
                maxConsecutive1s = 3
            )
        }
    }
}
