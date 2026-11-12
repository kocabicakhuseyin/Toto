package com.example.model

enum class PredictionOutcome(val code: String, val label: String) {
    HOME("1", "1"),
    DRAW("X", "0"),
    AWAY("2", "2")
}

data class MatchProbability(
    val home: List<Int>, // e.g. [79, 81, 78, 88]
    val draw: List<Int>, // e.g. [10, 9, 11, 8]
    val away: List<Int>  // e.g. [11, 9, 11, 4]
)

data class TotoMatch(
    val id: Int,
    val matchNumber: Int,
    val homeTeam: String,
    val awayTeam: String,
    val matchDate: String,
    val probability: MatchProbability,
    val selections: Set<PredictionOutcome> = emptySet(),
    val isBanko: Boolean = false,
    val isFavorited: Boolean = false,
    // Null = automatically choose the outcome with the highest available 1/0/2 percentage.
    // A manual favorite may be 1, 0 (draw) or 2.
    val favoriteOverride: PredictionOutcome? = null
) {
    // Published Spor Toto programs contain no betting/play percentages.
    // An absent percentage must never appear to be an automatic home favorite.
    val hasAutomaticFavoriteData: Boolean
        get() {
            val home = probability.home.firstOrNull() ?: return false
            val draw = probability.draw.firstOrNull() ?: return false
            val away = probability.away.firstOrNull() ?: return false
            return home in 0..100 && draw in 0..100 && away in 0..100 &&
                home + draw + away in 95..105
        }

    val favoriteKnown: Boolean
        get() = favoriteOverride != null || hasAutomaticFavoriteData

    val automaticFavoriteOutcome: PredictionOutcome
        get() {
            val homePercentage = probability.home.firstOrNull() ?: 0
            val drawPercentage = probability.draw.firstOrNull() ?: 0
            val awayPercentage = probability.away.firstOrNull() ?: 0
            return when {
                homePercentage >= drawPercentage && homePercentage >= awayPercentage ->
                    PredictionOutcome.HOME
                drawPercentage >= homePercentage && drawPercentage >= awayPercentage ->
                    PredictionOutcome.DRAW
                else -> PredictionOutcome.AWAY
            }
        }

    val favoriteOutcome: PredictionOutcome
        get() = favoriteOverride ?: automaticFavoriteOutcome
}

enum class MatchStatus(val label: String, val badgeColorHex: Long) {
    UPCOMING("Başlamadı", 0xFF64748B),
    LIVE("CANLI", 0xFFEF4444),
    HALF_TIME("Devre Arası", 0xFFF59E0B),
    FINISHED("Bitti", 0xFF10B981)
}

data class MatchLiveScore(
    val matchNumber: Int,
    val homeTeam: String,
    val awayTeam: String,
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val status: MatchStatus = MatchStatus.UPCOMING,
    val minuteText: String = "",
    val matchDate: String = "",
    val league: String = "UEFA / Süper Lig",
    val homeWinOdd: Double = 1.85,
    val drawOdd: Double = 3.20,
    val awayWinOdd: Double = 3.40,
    val isFavorited: Boolean = false
) {
    val winningOutcome: PredictionOutcome?
        get() = when {
            homeScore == null || awayScore == null -> null
            homeScore > awayScore -> PredictionOutcome.HOME
            homeScore < awayScore -> PredictionOutcome.AWAY
            else -> PredictionOutcome.DRAW
        }

    val isLive: Boolean
        get() = status == MatchStatus.LIVE || status == MatchStatus.HALF_TIME

    val scoreDisplay: String
        get() = if (homeScore != null && awayScore != null) "$homeScore - $awayScore" else "- : -"
}

enum class FormulaType(val title: String, val description: String, val badge: String, val guaranteeTarget: Int = 14) {
    GUARANTEE_15("15 Garanti (Tam Kapsama)", "N-0: Seçimlerin hepsi tutarsa kesin 15 Garanti", "15 GARANTİ", 15),
    GUARANTEE_14("14 Garanti (N-1 İndirgeme)", "N-1: En az 14 Garanti (%15-20 ihtimalle 15)", "14 GARANTİ", 14),
    GUARANTEE_13("13 Garanti (N-2 İndirgeme)", "N-2: En az 13 Garanti (%90+ kolon tasarrufu)", "13 GARANTİ", 13),
    GUARANTEE_12("12 Garanti (N-3 İndirgeme)", "N-3: En az 12 Garanti (Bütçe dostu minimum kolon)", "12 GARANTİ", 12),
    GUARANTEED_9_COL("9 Kolonlu Sistem", "15 maçta dengeli 9 kolonluk kapsama", "9 KOLON", 13),
    PERCENTAGE_FORMULA("% Yüzdesel Formülü", "Olasılık yüzdelerine göre akıllı eleme", "YÜZDE", 14),
    SUPER_SEVEN("Süper Yedili Formülü", "7 Banko + 8 Değişkenli Sistem", "S7", 13),
    EXTRA1X2_TXT("Extra1X2 TXT Kolonları", "Harici TXT Kolon Dosyasından Otomatik Yükleme", "TXT", 14),
    EXCEL_UPLOAD("Excel Kuponları Yükle", "Excel ve CSV Formatında Kolon Yükleme", "EXCEL", 14)
}

enum class BettingPlatform(
    val id: String,
    val displayName: String,
    val shortName: String,
    val brandColorHex: Long,
    val textColorHex: Long,
    val packageName: String,
    val webUrl: String,
    val domain: String,
    val iconEmoji: String
) {
    NESINE(
        id = "nesine",
        displayName = "NESİNE.COM",
        shortName = "Nesine",
        brandColorHex = 0xFFF59E0B, // Amber Gold
        textColorHex = 0xFF000000,
        packageName = "com.pordiva.nesine.android",
        webUrl = "https://www.nesine.com/spor-toto",
        domain = "nesine.com",
        iconEmoji = "🟡"
    ),
    IDDAA(
        id = "iddaa",
        displayName = "iddaa.com",
        shortName = "İddaa",
        brandColorHex = 0xFF059669, // Forest Green
        textColorHex = 0xFFFFFFFF,
        packageName = "com.iddaa.android",
        webUrl = "https://www.iddaa.com/spor-toto",
        domain = "iddaa.com",
        iconEmoji = "👑"
    ),
    BILYONER(
        id = "bilyoner",
        displayName = "Bilyoner",
        shortName = "Bilyoner",
        brandColorHex = 0xFF10B981, // Emerald Green
        textColorHex = 0xFFFFFFFF,
        packageName = "com.bilyoner.android",
        webUrl = "https://www.bilyoner.com/spor-toto",
        domain = "bilyoner.com",
        iconEmoji = "🟢"
    ),
    MISLI(
        id = "misli",
        displayName = "misli.com",
        shortName = "Misli",
        brandColorHex = 0xFFEA580C, // Orange
        textColorHex = 0xFFFFFFFF,
        packageName = "com.misli.android",
        webUrl = "https://www.misli.com/spor-toto",
        domain = "misli.com",
        iconEmoji = "🟠"
    ),
    BIREBIN(
        id = "birebin",
        displayName = "birebin",
        shortName = "Birebin",
        brandColorHex = 0xFFE11D48, // Rose Red
        textColorHex = 0xFFFFFFFF,
        packageName = "com.birebin.android",
        webUrl = "https://www.birebin.com/spor-toto",
        domain = "birebin.com",
        iconEmoji = "🔴"
    ),
    OLEY(
        id = "oley",
        displayName = "oley.com",
        shortName = "Oley",
        brandColorHex = 0xFF0284C7, // Sky Blue
        textColorHex = 0xFFFFFFFF,
        packageName = "com.oley.android",
        webUrl = "https://www.oley.com/spor-toto",
        domain = "oley.com",
        iconEmoji = "🔵"
    )
}

const val COLUMN_FEE_TL: Double = 0.20 // Her bir kolon için 0.20 TL (20 kuruş) ödenmesi gereklidir

data class TotoColumn(
    val index: Int,
    val predictions: List<PredictionOutcome> // Exactly 15 predictions
) {
    val asString: String
        get() = predictions.joinToString("") { it.code }
}

data class Coupon(
    val id: String,
    val title: String,
    val formulaType: FormulaType,
    val columns: List<TotoColumn>,
    val createdAt: Long = System.currentTimeMillis(),
    val pricePerColumnTL: Double = 10.0 // Spor Toto yasal kolon ücreti
) {
    val totalColumns: Int get() = columns.size
    val totalCostTL: Double get() = totalColumns * pricePerColumnTL
    val columnFeeTotalTL: Double get() = totalColumns * COLUMN_FEE_TL // Otomatik oynatma bakiye bedeli (0.20 TL / Kolon)

    /**
     * Calculates distribution counts for 1, X, 2 across all 15 matches.
     * Returns List of Triple(homeCount, drawCount, awayCount) of size 15.
     */
    val matchDistributions: List<Triple<Int, Int, Int>>
        get() {
            return (0 until 15).map { matchIdx ->
                var h = 0
                var d = 0
                var a = 0
                columns.forEach { col ->
                    when (col.predictions.getOrNull(matchIdx)) {
                        PredictionOutcome.HOME -> h++
                        PredictionOutcome.DRAW -> d++
                        PredictionOutcome.AWAY -> a++
                        null -> {}
                    }
                }
                Triple(h, d, a)
            }
        }
}

enum class SupportedTeam(val teamName: String, val primaryColor: Long, val secondaryColor: Long, val shortName: String) {
    FENERBAHCE("Fenerbahçe", 0xFF002D72, 0xFFFFED00, "FB"),
    GALATASARAY("Galatasaray", 0xFFA90432, 0xFFFDB913, "GS"),
    BESIKTAS("Beşiktaş", 0xFF000000, 0xFFFFFFFF, "BJK"),
    TRABZONSPOR("Trabzonspor", 0xFF800000, 0xFF00BFFF, "TS"),
    ALANYASPOR("Alanyaspor", 0xFF008000, 0xFFFFA500, "ALN"),
    MILLI_TAKIM("Türkiye", 0xFFE30A17, 0xFFFFFFFF, "TR")
}

data class ForumPost(
    val id: String,
    val authorName: String,
    val authorTeam: SupportedTeam,
    val authorTitle: String = "Usta",
    val content: String,
    val dateText: String,
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val attachedCouponId: String? = null,
    val matchReference: String? = null
)

data class PlatformCredentials(
    val platform: BettingPlatform = BettingPlatform.NESINE,
    val username: String = "",
    val password: String = "",
    val isRemembered: Boolean = true
)

// Legacy alias for backwards compatibility
typealias NesineCredentials = PlatformCredentials

enum class AutoPlayStepState {
    IDLE,
    CONNECTING,
    LOGGING_IN,
    SUBMITTING_COLUMNS,
    COMPLETED,
    ERROR
}

typealias NesineAutoPlayState = AutoPlayStepState

enum class TransactionType {
    TOP_UP,
    COLUMN_FEE
}

data class WalletTransaction(
    val id: String,
    val title: String,
    val amountTL: Double,
    val type: TransactionType,
    val dateText: String,
    val orderId: String? = null
)

data class GooglePlayPackage(
    val id: String,
    val title: String,
    val amountTL: Double,
    val columnCount: Int, // amountTL / 0.20
    val bonusBadge: String? = null,
    val isPopular: Boolean = false
)

data class UserProfile(
    val username: String = "HUSEYIN_K",
    val favoriteTeam: SupportedTeam = SupportedTeam.FENERBAHCE,
    val walletBalanceTL: Double = 50.0, // Başlangıç Google Play bakiyesi (250 kolon oynama hakkı)
    val walletTransactions: List<WalletTransaction> = listOf(
        WalletTransaction(
            id = "tx_init",
            title = "Google Play Bakiye Yükleme",
            amountTL = 50.0,
            type = TransactionType.TOP_UP,
            dateText = "09.11.2026 23:00",
            orderId = "GPA.3821-4912-9214-72183"
        )
    ),
    val platformCredentials: Map<String, PlatformCredentials> = emptyMap(),
    val unreadMessagesCount: Int = 1,
    val hasFreeAutoPlay: Boolean = false,
    val totalAutoPlayedCoupons: Int = 30251
) {
    val nesineCredentials: PlatformCredentials
        get() = platformCredentials[BettingPlatform.NESINE.id] ?: PlatformCredentials(BettingPlatform.NESINE)

    val availableColumnCount: Int
        get() = (walletBalanceTL / COLUMN_FEE_TL).toInt()
}
