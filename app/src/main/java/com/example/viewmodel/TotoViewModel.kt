package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.FormulaEngine
import com.example.model.Coupon
import com.example.model.FormulaType
import com.example.model.ForumPost
import com.example.model.MatchProbability
import com.example.model.NesineCredentials
import com.example.model.PlatformCredentials
import com.example.model.PredictionOutcome
import com.example.model.SupportedTeam
import com.example.model.TotoColumn
import com.example.model.TotoMatch
import com.example.model.UserProfile
import com.example.network.model.WeeklyBulletinResponse
import com.example.repository.SporTotoBulletinRepository
import com.example.repository.SporTotoBulletinRepositoryImpl
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.example.data.local.entity.PurchaseHistoryEntity
import com.example.data.repository.WalletRepository
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.data.local.CouponJsonStorageManager
import com.example.data.local.SavedJsonCouponMeta
import com.example.model.FilterProfileType
import com.example.model.MatchLiveScore

class TotoViewModel(
    private val bulletinRepository: SporTotoBulletinRepository = SporTotoBulletinRepositoryImpl(),
    private val walletRepository: WalletRepository? = null
) : ViewModel() {

    private val _matches = MutableStateFlow<List<TotoMatch>>(createInitialMatches())
    val matches: StateFlow<List<TotoMatch>> = _matches.asStateFlow()

    val currentBulletin: StateFlow<WeeklyBulletinResponse?> = bulletinRepository.cachedBulletin

    private val _isRefreshingBulletin = MutableStateFlow(false)
    val isRefreshingBulletin: StateFlow<Boolean> = _isRefreshingBulletin.asStateFlow()

    private val _bulletinMessage = MutableStateFlow<String?>(null)
    val bulletinMessage: StateFlow<String?> = _bulletinMessage.asStateFlow()

    private val _coupons = MutableStateFlow<List<Coupon>>(emptyList())
    val coupons: StateFlow<List<Coupon>> = _coupons.asStateFlow()

    private val _currentCoupon = MutableStateFlow<Coupon?>(null)
    val currentCoupon: StateFlow<Coupon?> = _currentCoupon.asStateFlow()

    private val _forumPosts = MutableStateFlow<List<ForumPost>>(createInitialForumPosts())
    val forumPosts: StateFlow<List<ForumPost>> = _forumPosts.asStateFlow()

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _selectedFormula = MutableStateFlow(FormulaType.GUARANTEE_14)
    val selectedFormula: StateFlow<FormulaType> = _selectedFormula.asStateFlow()

    private val _targetColumnCap = MutableStateFlow<Int?>(null)
    val targetColumnCap: StateFlow<Int?> = _targetColumnCap.asStateFlow()

    fun setTargetColumnCap(cap: Int?) {
        _targetColumnCap.value = cap
    }

    private val _filterCriteria = MutableStateFlow(com.example.model.TotoFilterCriteria())
    val filterCriteria: StateFlow<com.example.model.TotoFilterCriteria> = _filterCriteria.asStateFlow()

    val liveScores: StateFlow<List<MatchLiveScore>> = bulletinRepository.liveScores
    val liveScoreProvider: StateFlow<String> = bulletinRepository.liveScoreProvider

    private val _isLivePolling = MutableStateFlow(false)
    val isLivePolling: StateFlow<Boolean> = _isLivePolling.asStateFlow()

    private val _lastLiveUpdateTime = MutableStateFlow("Henüz güncellenmedi")
    val lastLiveUpdateTime: StateFlow<String> = _lastLiveUpdateTime.asStateFlow()

    private val _liveScoreError = MutableStateFlow<String?>(null)
    val liveScoreError: StateFlow<String?> = _liveScoreError.asStateFlow()

    private val _isRefreshingLiveScores = MutableStateFlow(false)
    val isRefreshingLiveScores: StateFlow<Boolean> = _isRefreshingLiveScores.asStateFlow()

    private val _favoritedMatchNumbers = MutableStateFlow<Set<Int>>(emptySet())
    val favoritedMatchNumbers: StateFlow<Set<Int>> = _favoritedMatchNumbers.asStateFlow()

    init {
        // Initial bulletin load & starter coupon
        refreshBulletin(forceRefresh = false)
        generateCoupon(FormulaType.GUARANTEE_14, "14 Garanti (N-1) Özel Başlangıç Kuponu")

        // Refresh only while the live-score page is visible, at most once/minute.
        viewModelScope.launch {
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            while (true) {
                if (_isLivePolling.value) {
                    val result = bulletinRepository.pollLiveScores()
                    result.onSuccess {
                        _liveScoreError.value = null
                        _lastLiveUpdateTime.value = timeFormat.format(Date())
                    }.onFailure { _liveScoreError.value = it.message ?: "Canlı veri alınamadı." }
                }
                delay(60_000L)
            }
        }

        // Sync with Room database if walletRepository is provided
        walletRepository?.let { repo ->
            viewModelScope.launch {
                repo.balanceFlow.collect { roomBalance ->
                    _userProfile.update { it.copy(walletBalanceTL = roomBalance) }
                }
            }
            viewModelScope.launch {
                repo.purchaseHistoryFlow.collect { history ->
                    if (history.isNotEmpty()) {
                        val mapped = history.map { entity ->
                            com.example.model.WalletTransaction(
                                id = entity.transactionId,
                                title = entity.title,
                                amountTL = entity.amountTL,
                                type = if (entity.transactionType == PurchaseHistoryEntity.TYPE_TOP_UP) {
                                    com.example.model.TransactionType.TOP_UP
                                } else {
                                    com.example.model.TransactionType.COLUMN_FEE
                                },
                                dateText = entity.formattedDate,
                                orderId = entity.orderId
                            )
                        }
                        _userProfile.update { it.copy(walletTransactions = mapped) }
                    }
                }
            }
        }
    }

    fun refreshBulletin(forceRefresh: Boolean = true) {
        // Ignore repeated taps while a bulletin request is already running.
        if (_isRefreshingBulletin.value) return
        _isRefreshingBulletin.value = true
        viewModelScope.launch {
            try {
                val result = bulletinRepository.getDomainMatches(forceRefresh)
                result.onSuccess { freshMatches ->
                    if (freshMatches.isNotEmpty()) {
                        _matches.value = freshMatches
                        _bulletinMessage.value = "Haftalık bülten ve canlı oranlar güncellendi!"
                    }
                }.onFailure {
                    _bulletinMessage.value = "Bülten güncellenemedi, yerel veriler kullanılıyor."
                }
            } finally {
                _isRefreshingBulletin.value = false
            }
        }
    }

    fun clearBulletinMessage() {
        _bulletinMessage.value = null
    }

    fun selectFormula(formulaType: FormulaType) {
        _selectedFormula.value = formulaType
    }

    fun togglePrediction(matchId: Int, outcome: PredictionOutcome) {
        _matches.update { list ->
            list.map { match ->
                if (match.id == matchId) {
                    val current = match.selections.toMutableSet()
                    if (current.contains(outcome)) {
                        current.remove(outcome)
                    } else {
                        current.add(outcome)
                    }
                    match.copy(selections = current)
                } else match
            }
        }
    }

    fun selectAllFavorites() {
        _matches.update { list ->
            list.map { match ->
                val h = match.probability.home.firstOrNull() ?: 50
                val d = match.probability.draw.firstOrNull() ?: 25
                val a = match.probability.away.firstOrNull() ?: 25
                val best = when {
                    h >= d && h >= a -> PredictionOutcome.HOME
                    d >= h && d >= a -> PredictionOutcome.DRAW
                    else -> PredictionOutcome.AWAY
                }
                match.copy(selections = setOf(best))
            }
        }
    }

    fun applyQuickDoubles() {
        _matches.update { list ->
            list.mapIndexed { idx, match ->
                val set = when (idx % 3) {
                    0 -> setOf(PredictionOutcome.HOME, PredictionOutcome.DRAW)
                    1 -> setOf(PredictionOutcome.DRAW, PredictionOutcome.AWAY)
                    else -> setOf(PredictionOutcome.HOME, PredictionOutcome.AWAY)
                }
                match.copy(selections = set)
            }
        }
        // Only update match selections; generating a matrix is a separate user action.
    }

    fun applyQuickDoubles10To15() {
        _matches.update { list ->
            list.mapIndexed { idx, match ->
                if (idx in 9..14) {
                    val set = when (idx) {
                        9 -> setOf(PredictionOutcome.HOME, PredictionOutcome.DRAW)
                        10 -> setOf(PredictionOutcome.DRAW, PredictionOutcome.AWAY)
                        11 -> setOf(PredictionOutcome.HOME, PredictionOutcome.AWAY)
                        12 -> setOf(PredictionOutcome.HOME, PredictionOutcome.DRAW)
                        13 -> setOf(PredictionOutcome.DRAW, PredictionOutcome.AWAY)
                        else -> setOf(PredictionOutcome.HOME, PredictionOutcome.DRAW)
                    }
                    match.copy(selections = set)
                } else {
                    if (match.selections.isEmpty()) {
                        val h = match.probability.home.firstOrNull() ?: 50
                        val d = match.probability.draw.firstOrNull() ?: 25
                        val a = match.probability.away.firstOrNull() ?: 25
                        val best = when {
                            h >= d && h >= a -> PredictionOutcome.HOME
                            d >= h && d >= a -> PredictionOutcome.DRAW
                            else -> PredictionOutcome.AWAY
                        }
                        match.copy(selections = setOf(best))
                    } else match
                }
            }
        }
        // Avoid expensive automatic coupon generation after a preset selection.
    }

    fun applyQuickDoubles1To9() {
        _matches.update { list ->
            list.mapIndexed { idx, match ->
                if (idx in 0..8) {
                    val h = match.probability.home.firstOrNull() ?: 50
                    val a = match.probability.away.firstOrNull() ?: 25
                    val set = if (h >= a) {
                        setOf(PredictionOutcome.HOME, PredictionOutcome.DRAW)
                    } else {
                        setOf(PredictionOutcome.DRAW, PredictionOutcome.AWAY)
                    }
                    match.copy(selections = set)
                } else {
                    if (match.selections.isEmpty()) {
                        val h = match.probability.home.firstOrNull() ?: 50
                        val d = match.probability.draw.firstOrNull() ?: 25
                        val a = match.probability.away.firstOrNull() ?: 25
                        val best = when {
                            h >= d && h >= a -> PredictionOutcome.HOME
                            d >= h && d >= a -> PredictionOutcome.DRAW
                            else -> PredictionOutcome.AWAY
                        }
                        match.copy(selections = setOf(best))
                    } else match
                }
            }
        }
        // The Formüller action generates the coupon when requested.
    }

    fun clearAllSelections() {
        _matches.update { list ->
            list.map { it.copy(selections = emptySet(), isBanko = false) }
        }
    }

    fun generateCoupon(
        formulaType: FormulaType,
        customTitle: String? = null,
        cap: Int? = null
    ): Coupon {
        val title = customTitle ?: "${formulaType.title} (#${_coupons.value.size + 1})"
        val effectiveCap = cap ?: _targetColumnCap.value
        val generatedCols = FormulaEngine.generateColumns(_matches.value, formulaType, maxColumnsCap = effectiveCap)
        val newCoupon = Coupon(
            id = "coupon_${System.currentTimeMillis()}",
            title = title,
            formulaType = formulaType,
            columns = generatedCols
        )
        _coupons.update { listOf(newCoupon) + it }
        _currentCoupon.value = newCoupon
        _userProfile.update { it.copy(totalAutoPlayedCoupons = it.totalAutoPlayedCoupons + 1) }
        return newCoupon
    }

    fun updateFilterCriteria(criteria: com.example.model.TotoFilterCriteria) {
        _filterCriteria.value = criteria
    }

    fun resetFilterCriteria() {
        _filterCriteria.value = com.example.model.TotoFilterCriteria()
    }

    /**
     * Applies a 1-click filter profile (Favorite weighted, Balanced, Surprise weighted)
     * and generates the filtered coupon.
     */
    fun applyFilterProfile(profile: FilterProfileType): Coupon {
        val criteria = profile.toCriteria()
        _filterCriteria.value = criteria
        return applyFiltersAndEliminate(criteria, "${profile.displayName} Kuponu")
    }

    /**
     * Applies filter criteria to candidate columns,
     * eliminating non-matching columns and saving a new filtered coupon.
     */
    fun applyFiltersAndEliminate(
        criteria: com.example.model.TotoFilterCriteria,
        customTitle: String? = null
    ): Coupon {
        _filterCriteria.value = criteria
        val effectiveCap = _targetColumnCap.value
        val isUnlimited = effectiveCap == FormulaEngine.UNLIMITED_COLUMNS_CAP
        val hasVariables = _matches.value.any { it.selections.size > 1 }

        val sourceColumns = if (hasVariables) {
            if (isUnlimited) {
                FormulaEngine.generateAllCombinations(_matches.value, limit = 65536)
            } else {
                FormulaEngine.generateColumns(_matches.value, _selectedFormula.value, maxColumnsCap = effectiveCap)
            }
        } else {
            val current = _currentCoupon.value
            if (current != null && current.columns.size > 1) current.columns
            else {
                if (isUnlimited) FormulaEngine.generateAllCombinations(_matches.value, limit = 65536)
                else FormulaEngine.generateColumns(_matches.value, _selectedFormula.value, maxColumnsCap = effectiveCap)
            }
        }

        val filteredCols = if (criteria.isActive) {
            com.example.engine.TotoFilterEngine.filterColumns(
                columns = sourceColumns,
                criteria = criteria,
                matches = _matches.value
            )
        } else {
            sourceColumns
        }

        val beforeCount = sourceColumns.size
        val afterCount = filteredCols.size
        val savings = if (beforeCount > 0) ((beforeCount - afterCount) * 100) / beforeCount else 0

        val modeLabel = if (criteria.applicationMode == com.example.model.FilterApplicationMode.PROBABILITY_STRICT) "İhtimalli" else "Garantili"
        val title = customTitle ?: "Garantili Filtreli Kupon ($modeLabel - %$savings Tasarruf)"

        val safeCols = when {
            filteredCols.isNotEmpty() -> {
                if (isUnlimited) filteredCols
                else if (effectiveCap != null && effectiveCap > 0) filteredCols.take(effectiveCap)
                else filteredCols
            }
            sourceColumns.size > 1 -> {
                if (isUnlimited) sourceColumns.take(minOf(64, sourceColumns.size))
                else sourceColumns.take(minOf(6, sourceColumns.size))
            }
            else -> sourceColumns
        }

        val newCoupon = Coupon(
            id = "filtered_${System.currentTimeMillis()}",
            title = title,
            formulaType = _selectedFormula.value,
            columns = safeCols
        )

        _coupons.update { listOf(newCoupon) + it }
        _currentCoupon.value = newCoupon
        return newCoupon
    }

    // --- Local JSON Storage Operations ---

    fun saveCurrentCouponToJson(context: Context): Result<File> {
        val coupon = _currentCoupon.value ?: return Result.failure(IllegalStateException("Kaydedilecek aktif kupon bulunamadı."))
        return CouponJsonStorageManager.saveCouponToFile(context, coupon)
    }

    fun listSavedJsonCoupons(context: Context): List<SavedJsonCouponMeta> {
        return CouponJsonStorageManager.listSavedJsonCoupons(context)
    }

    fun loadCouponFromJsonFile(file: File): Result<Coupon> {
        return CouponJsonStorageManager.loadCouponFromFile(file).onSuccess { loaded ->
            _coupons.update { existing ->
                if (existing.any { it.id == loaded.id }) existing
                else listOf(loaded) + existing
            }
            _currentCoupon.value = loaded
        }
    }

    fun deleteSavedJsonCoupon(file: File): Boolean {
        return CouponJsonStorageManager.deleteCouponFile(file)
    }

    // --- Real-time Match Results & Live Polling Operations ---

    fun toggleLivePolling() {
        _isLivePolling.update { !it }
    }

    fun setLivePolling(enabled: Boolean) {
        _isLivePolling.value = enabled
    }

    fun pollLiveScoresNow() {
        if (_isRefreshingLiveScores.value) return
        viewModelScope.launch {
            _isRefreshingLiveScores.value = true
            val result = bulletinRepository.pollLiveScores()
            result.onSuccess {
                _liveScoreError.value = null
                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                _lastLiveUpdateTime.value = timeFormat.format(Date())
            }.onFailure {
                _liveScoreError.value = it.message ?: "Canlı veri alınamadı."
            }
            delay(300)
            _isRefreshingLiveScores.value = false
        }
    }

    // Favorites are independent from notifications.
    fun toggleFavoriteMatch(matchNumber: Int): Boolean {
        var isNowFav = false
        _favoritedMatchNumbers.update { current ->
            if (current.contains(matchNumber)) {
                isNowFav = false
                current - matchNumber
            } else {
                isNowFav = true
                current + matchNumber
            }
        }
        return isNowFav
    }

    fun isMatchFavorited(matchNumber: Int): Boolean =
        _favoritedMatchNumbers.value.contains(matchNumber)

    fun importTxtColumns(content: String, formulaName: String = "Yüklenen Extra1X2 Kuponu"): Coupon? {
        val cols = FormulaEngine.parseExtra1X2Txt(content)
        if (cols.isEmpty()) return null

        val coupon = Coupon(
            id = "txt_${System.currentTimeMillis()}",
            title = formulaName,
            formulaType = FormulaType.EXTRA1X2_TXT,
            columns = cols
        )
        _coupons.update { listOf(coupon) + it }
        _currentCoupon.value = coupon
        return coupon
    }

    fun loadSampleTxt() {
        val sample = buildString {
            append("1X211X212111X21\n")
            append("11111X212111X21\n")
            append("1X2111112111X21\n")
            append("1X211X211111X21\n")
            append("11211X212111X22\n")
            append("1X111X212111X21\n")
            append("1XX11X212111X21\n")
            append("1X211X212111111\n")
            append("111111111111111\n")
        }
        importTxtColumns(sample, "Örnek Extra1X2 TXT Kuponu (9 Kolon)")
    }

    fun setCurrentCoupon(coupon: Coupon) {
        _currentCoupon.value = coupon
    }

    fun deleteCoupon(couponId: String) {
        _coupons.update { list -> list.filter { it.id != couponId } }
        if (_currentCoupon.value?.id == couponId) {
            _currentCoupon.value = _coupons.value.firstOrNull()
        }
    }

    fun saveNesineCredentials(credentials: PlatformCredentials) {
        savePlatformCredentials(credentials)
    }

    fun savePlatformCredentials(credentials: PlatformCredentials) {
        _userProfile.update {
            it.copy(platformCredentials = it.platformCredentials + (credentials.platform.id to credentials))
        }
    }

    fun topUpWallet(pkg: com.example.model.GooglePlayPackage) {
        val tx = com.example.billing.GooglePlayBillingManager.createPurchaseTransaction(pkg)
        _userProfile.update { current ->
            current.copy(
                walletBalanceTL = current.walletBalanceTL + pkg.amountTL,
                walletTransactions = listOf(tx) + current.walletTransactions
            )
        }
        walletRepository?.let { repo ->
            viewModelScope.launch {
                repo.topUp(pkg, tx.orderId ?: "GPA.SIMULATED")
            }
        }
    }

    fun canAffordCoupon(coupon: Coupon): Boolean {
        return _userProfile.value.walletBalanceTL >= coupon.columnFeeTotalTL
    }

    fun deductColumnFee(coupon: Coupon): Boolean {
        val required = coupon.columnFeeTotalTL
        val currentBalance = _userProfile.value.walletBalanceTL
        if (currentBalance < required) {
            return false
        }
        val tx = com.example.billing.GooglePlayBillingManager.createColumnPlayTransaction(coupon.totalColumns, required)
        _userProfile.update { current ->
            current.copy(
                walletBalanceTL = current.walletBalanceTL - required,
                walletTransactions = listOf(tx) + current.walletTransactions,
                totalAutoPlayedCoupons = current.totalAutoPlayedCoupons + 1
            )
        }
        walletRepository?.let { repo ->
            viewModelScope.launch {
                repo.chargeForCouponColumns(coupon.totalColumns, coupon.id)
            }
        }
        return true
    }

    fun addForumPost(content: String, attachedCouponId: String? = null, matchRef: String? = null) {
        if (content.isBlank()) return
        val profile = _userProfile.value
        val post = ForumPost(
            id = "post_${System.currentTimeMillis()}",
            authorName = profile.username,
            authorTeam = profile.favoriteTeam,
            authorTitle = "Spor Toto Uzmanı",
            content = content.trim(),
            dateText = "Bugün ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}",
            likesCount = 0,
            attachedCouponId = attachedCouponId,
            matchReference = matchRef
        )
        _forumPosts.update { listOf(post) + it }
    }

    fun toggleLikePost(postId: String) {
        _forumPosts.update { list ->
            list.map { post ->
                if (post.id == postId) {
                    val wasLiked = post.isLiked
                    post.copy(
                        isLiked = !wasLiked,
                        likesCount = if (wasLiked) post.likesCount - 1 else post.likesCount + 1
                    )
                } else post
            }
        }
    }

    fun updateFavoriteTeam(team: SupportedTeam) {
        _userProfile.update { it.copy(favoriteTeam = team) }
    }

    fun updateUsername(name: String) {
        if (name.isNotBlank()) {
            _userProfile.update { it.copy(username = name.trim()) }
        }
    }

    private fun createInitialMatches(): List<TotoMatch> {
        return listOf(
            TotoMatch(1, 1, "FRANSA", "TÜRKİYE", "15.11.2026 22:45", MatchProbability(listOf(79, 81, 78, 88), listOf(10, 9, 11, 8), listOf(11, 9, 11, 4))),
            TotoMatch(2, 2, "BULGARİSTAN", "İZLANDA", "13.11.2026 22:45", MatchProbability(listOf(43, 34, 24, 27), listOf(26, 27, 28, 34), listOf(31, 38, 48, 39))),
            TotoMatch(3, 3, "LİHTENŞTAYN", "AZERBAYCAN", "13.11.2026 22:45", MatchProbability(listOf(9, 9, 6, 6), listOf(16, 15, 14, 17), listOf(75, 75, 80, 77))),
            TotoMatch(4, 4, "LÜKSEMBURG", "ESTONYA", "13.11.2026 22:45", MatchProbability(listOf(61, 51, 46, 40), listOf(21, 24, 29, 30), listOf(18, 23, 25, 30))),
            TotoMatch(5, 5, "SLOVAKYA", "FAROE ADALARI", "13.11.2026 22:45", MatchProbability(listOf(85, 88, 84, 87), listOf(10, 7, 13, 10), listOf(5, 4, 3, 3))),
            TotoMatch(6, 6, "GÜRCİSTAN", "MACARİSTAN", "14.11.2026 20:00", MatchProbability(listOf(42, 40, 26, 33), listOf(28, 26, 29, 30), listOf(30, 32, 45, 37))),
            TotoMatch(7, 7, "NORVEÇ", "GALLER", "14.11.2026 20:00", MatchProbability(listOf(84, 86, 89, 87), listOf(10, 8, 8, 8), listOf(6, 5, 3, 5))),
            TotoMatch(8, 8, "PORTEKİZ", "DANİMARKA", "14.11.2026 22:45", MatchProbability(listOf(76, 76, 77, 68), listOf(15, 15, 16, 24), listOf(9, 7, 7, 8))),
            TotoMatch(9, 9, "İSPANYA", "İTALYA", "15.11.2026 22:45", MatchProbability(listOf(58, 62, 59, 60), listOf(24, 22, 23, 24), listOf(18, 16, 18, 16))),
            TotoMatch(10, 10, "ALMANYA", "HOLLANDA", "16.11.2026 22:45", MatchProbability(listOf(52, 55, 50, 48), listOf(26, 25, 27, 28), listOf(22, 20, 23, 24))),
            TotoMatch(11, 11, "İNGİLTERE", "BELÇİKA", "16.11.2026 22:45", MatchProbability(listOf(64, 68, 65, 62), listOf(21, 19, 20, 22), listOf(15, 13, 15, 16))),
            TotoMatch(12, 12, "HIRVATİSTAN", "POLONYA", "16.11.2026 20:00", MatchProbability(listOf(60, 58, 62, 59), listOf(23, 24, 22, 23), listOf(17, 18, 16, 18))),
            TotoMatch(13, 13, "İSVEÇ", "İSVİÇRE", "17.11.2026 22:45", MatchProbability(listOf(45, 42, 40, 44), listOf(30, 31, 32, 29), listOf(25, 27, 28, 27))),
            TotoMatch(14, 14, "AVUSTURYA", "SIRBİSTAN", "17.11.2026 22:45", MatchProbability(listOf(55, 52, 58, 54), listOf(25, 27, 24, 26), listOf(20, 21, 18, 20))),
            TotoMatch(15, 15, "ÇEKYA", "UKRAYNA", "17.11.2026 22:45", MatchProbability(listOf(48, 50, 47, 49), listOf(28, 27, 29, 28), listOf(24, 23, 24, 23)))
        )
    }

    private fun createInitialForumPosts(): List<ForumPost> {
        return listOf(
            ForumPost(
                id = "p1",
                authorName = "TATAR34",
                authorTeam = SupportedTeam.ALANYASPOR,
                authorTitle = "Usta ✨",
                content = "Neden abi duzmu biter sürpriz olmazmi ?",
                dateText = "09.11.2026 23:12:47",
                likesCount = 8
            ),
            ForumPost(
                id = "p2",
                authorName = "GALATALI34",
                authorTeam = SupportedTeam.GALATASARAY,
                authorTitle = "Usta ✨",
                content = "Bu hafta 15 in verdiği ikramiye ortada... spor toto teşkilatı balina avına çıkmış durumda... yem olursun...",
                dateText = "09.11.2026 23:12:07",
                likesCount = 24
            ),
            ForumPost(
                id = "p3",
                authorName = "CUMHURİYET",
                authorTeam = SupportedTeam.BESIKTAS,
                authorTitle = "Kral 👑",
                content = "Girilmez Bence",
                dateText = "09.11.2026 23:01:38",
                likesCount = 12
            ),
            ForumPost(
                id = "p4",
                authorName = "HUSEYIN_K",
                authorTeam = SupportedTeam.FENERBAHCE,
                authorTitle = "Usta",
                content = "Garantili 14 formülüyle kuponu kurduk! Nesine'ye tek tıkla aktardım, Fransa ve Norveç banko.",
                dateText = "09.11.2026 22:45:10",
                likesCount = 31
            )
        )
    }
}
