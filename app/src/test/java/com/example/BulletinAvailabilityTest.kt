package com.example

import com.example.engine.NesineAutoPlayJsInjector
import com.example.model.Coupon
import com.example.model.FormulaType
import com.example.model.MatchLiveScore
import com.example.model.PredictionOutcome
import com.example.model.TotoColumn
import com.example.model.TotoMatch
import com.example.network.NetworkModule
import com.example.network.SporTotoApiService
import com.example.network.model.BulletinMatchDto
import com.example.network.model.NO_NEW_BULLETIN_ERROR
import com.example.network.model.WeeklyBulletinResponse
import com.example.network.model.isPlayable
import com.example.repository.NoNewBulletinException
import com.example.repository.SporTotoBulletinRepository
import com.example.repository.SporTotoBulletinRepositoryImpl
import com.example.viewmodel.TotoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class BulletinAvailabilityTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun inactiveOrIncompleteBulletinIsNotPlayable() {
        val activeBulletin = NetworkModule.createFallbackBulletin()
        assertTrue(activeBulletin.isPlayable())

        val inactive = activeBulletin.copy(isActive = false)
        assertFalse(inactive.isPlayable())

        val incomplete = activeBulletin.copy(matches = activeBulletin.matches.take(10))
        assertFalse(incomplete.isPlayable())

        val allFinished = activeBulletin.copy(
            matches = activeBulletin.matches.map { it.copy(status = "FINISHED") }
        )
        assertFalse(allFinished.isPlayable())

        val nullBulletin: WeeklyBulletinResponse? = null
        assertFalse(nullBulletin.isPlayable())
    }

    @Test
    fun repositoryReturnsYeniBultenYuklenmediWhenApiHasNoPlayableBulletin() = runBlocking {
        val base = NetworkModule.createFallbackBulletin()
        val inactiveService = object : SporTotoApiService {
            override suspend fun getCurrentWeeklyBulletin(): Response<WeeklyBulletinResponse> =
                Response.success(base.copy(isActive = false))

            override suspend fun getBulletinByWeek(weekNumber: Int): Response<WeeklyBulletinResponse> =
                Response.error(404, "{}".toResponseBody("application/json".toMediaType()))

            override suspend fun getLiveOdds(weekNumber: Int?): Response<List<BulletinMatchDto>> =
                Response.success(emptyList())
        }

        val repo = SporTotoBulletinRepositoryImpl(apiService = inactiveService)
        val bulletinResult = repo.getWeeklyBulletin(forceRefresh = true)
        assertTrue(bulletinResult.isFailure)
        assertEquals(NO_NEW_BULLETIN_ERROR, bulletinResult.exceptionOrNull()?.message)

        val domainResult = repo.getDomainMatches(forceRefresh = true)
        assertTrue(domainResult.isFailure)
        assertEquals(NO_NEW_BULLETIN_ERROR, domainResult.exceptionOrNull()?.message)
    }

    @Test
    fun viewModelSetsYeniBultenYuklenmediAndBlocksPlayWhenNoBulletinAvailable() = runBlocking {
        val emptyRepo = object : SporTotoBulletinRepository {
            private val _cached = MutableStateFlow<WeeklyBulletinResponse?>(null)
            override val cachedBulletin: StateFlow<WeeklyBulletinResponse?> = _cached.asStateFlow()
            override val liveScores: StateFlow<List<MatchLiveScore>> =
                MutableStateFlow<List<MatchLiveScore>>(emptyList()).asStateFlow()
            override val liveScoreProvider: StateFlow<String> =
                MutableStateFlow("Test").asStateFlow()

            override suspend fun getWeeklyBulletin(forceRefresh: Boolean): Result<WeeklyBulletinResponse> =
                Result.failure(NoNewBulletinException())

            override suspend fun getBulletinByWeek(weekNumber: Int): Result<WeeklyBulletinResponse> =
                Result.failure(NoNewBulletinException())

            override suspend fun refreshLiveOdds(): Result<List<BulletinMatchDto>> =
                Result.success(emptyList())

            override suspend fun pollLiveScores(): Result<List<MatchLiveScore>> =
                Result.failure(IllegalStateException("none"))

            override suspend fun getDomainMatches(forceRefresh: Boolean): Result<List<TotoMatch>> =
                Result.failure(NoNewBulletinException())
        }

        val vm = TotoViewModel(bulletinRepository = emptyRepo)
        testDispatcher.scheduler.runCurrent()

        assertFalse(vm.hasPlayableBulletin.value)
        assertEquals(NO_NEW_BULLETIN_ERROR, vm.bulletinError.value)
        assertTrue(vm.matches.value.isEmpty())

        val initialBalance = vm.userProfile.value.walletBalanceTL
        val sampleColumn = TotoColumn(1, List(15) { PredictionOutcome.HOME })
        val sampleCoupon = Coupon("c1", "Test", FormulaType.GUARANTEE_14, listOf(sampleColumn))

        assertFalse(vm.deductColumnFee(sampleCoupon))
        assertEquals(initialBalance, vm.userProfile.value.walletBalanceTL, 0.001)
        assertFalse(vm.ensurePlayableBulletin())

        val blockedCoupon = vm.generateCoupon(FormulaType.GUARANTEE_14)
        assertEquals(0, blockedCoupon.totalColumns)
        assertEquals(NO_NEW_BULLETIN_ERROR, blockedCoupon.title)
    }

    @Test
    fun jsInjectorIncludesYeniBultenYuklenmediAndBakiyeYetersizMessages() {
        val picks = List(15) { PredictionOutcome.HOME }
        val script = NesineAutoPlayJsInjector.buildColumnFillScript(TotoColumn(1, picks))
        assertTrue(script.contains("Yeni bülten yüklenmedi"))
        assertTrue(script.contains("Bakiye yetersiz"))
    }

    @Test
    fun viewModelSetsBakiyeYetersizWhenBalanceIsInsufficient() = runBlocking {
        val fallbackBulletin = NetworkModule.createFallbackBulletin()
        val playableRepo = object : SporTotoBulletinRepository {
            private val _cached = MutableStateFlow<WeeklyBulletinResponse?>(fallbackBulletin)
            override val cachedBulletin: StateFlow<WeeklyBulletinResponse?> = _cached.asStateFlow()
            override val liveScores: StateFlow<List<MatchLiveScore>> =
                MutableStateFlow<List<MatchLiveScore>>(emptyList()).asStateFlow()
            override val liveScoreProvider: StateFlow<String> =
                MutableStateFlow("Test").asStateFlow()

            override suspend fun getWeeklyBulletin(forceRefresh: Boolean): Result<WeeklyBulletinResponse> =
                Result.success(fallbackBulletin)

            override suspend fun getBulletinByWeek(weekNumber: Int): Result<WeeklyBulletinResponse> =
                Result.success(fallbackBulletin)

            override suspend fun refreshLiveOdds(): Result<List<BulletinMatchDto>> =
                Result.success(fallbackBulletin.matches)

            override suspend fun pollLiveScores(): Result<List<MatchLiveScore>> =
                Result.success(emptyList())

            override suspend fun getDomainMatches(forceRefresh: Boolean): Result<List<TotoMatch>> =
                Result.success((1..15).map { idx ->
                    TotoMatch(
                        id = idx,
                        matchNumber = idx,
                        homeTeam = "Home$idx",
                        awayTeam = "Away$idx",
                        matchDate = "2026-11-15",
                        probability = com.example.model.MatchProbability(listOf(50), listOf(25), listOf(25))
                    )
                })
        }

        val vm = TotoViewModel(bulletinRepository = playableRepo)
        testDispatcher.scheduler.runCurrent()

        // Default balance is 50.0 TL -> 260 columns require 52.0 TL (> 50.0 TL)
        val picks = List(15) { PredictionOutcome.HOME }
        val expensiveCoupon = Coupon(
            id = "c_expensive",
            title = "Büyük Kupon",
            formulaType = FormulaType.GUARANTEE_14,
            columns = (1..260).map { TotoColumn(it, picks) }
        )

        assertFalse(vm.canAffordCoupon(expensiveCoupon))
        assertFalse(vm.deductColumnFee(expensiveCoupon))
        assertEquals(com.example.model.INSUFFICIENT_BALANCE_ERROR, vm.walletError.value)
        assertTrue(vm.bulletinMessage.value.orEmpty().contains("Bakiye yetersiz"))
        assertEquals(50.0, vm.userProfile.value.walletBalanceTL, 0.001)
    }
}
