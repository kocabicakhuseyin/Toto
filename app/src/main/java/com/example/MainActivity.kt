package com.example

import android.os.Bundle
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.Coupon
import com.example.model.FormulaType
import com.example.model.NesineReceiptParser
import com.example.ui.components.AppNavTab
import com.example.ui.components.ModernBottomNavBar
import com.example.ui.components.TopHeaderBar
import com.example.ui.screens.CouponsScreen
import com.example.ui.screens.CouponPoolScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExcelImportDialog
import com.example.ui.screens.FormulaWizardDialog
import com.example.ui.screens.ForumScreen
import com.example.ui.screens.GooglePlayBillingDialog
import com.example.ui.screens.MatchAnalysisScreen
import com.example.ui.screens.MatchResultsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.TotoFilterDialog
import com.example.ui.screens.TxtImportDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.TotoViewModel

import androidx.compose.ui.platform.LocalContext
import com.example.data.local.AppDatabase
import com.example.data.repository.WalletRepository
import com.example.repository.PoolRepository
import com.example.repository.SporTotoBulletinRepositoryImpl
import com.example.viewmodel.BillingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var incomingNesineUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingNesineUrl = extractNesineShareLink(intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.rgb(15, 23, 42)),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.rgb(15, 23, 42))
        )
        setContent {
            MyApplicationTheme {
                MainAppContent(
                    incomingNesineUrl = incomingNesineUrl,
                    onNesineUrlHandled = { incomingNesineUrl = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingNesineUrl = extractNesineShareLink(intent)
    }

    private fun extractNesineShareLink(intent: Intent?): String? {
        val source = when (intent?.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_VIEW -> intent.dataString
            else -> null
        }.orEmpty()
        return Regex("https://[^\\s<>]+", RegexOption.IGNORE_CASE)
            .findAll(source)
            .map { it.value.trimEnd('.', ',', ')', ']', ';') }
            .firstOrNull(NesineReceiptParser::isShareUrl)
    }
}

@Composable
fun MainAppContent(incomingNesineUrl: String? = null, onNesineUrlHandled: () -> Unit = {}) {
    val context = LocalContext.current
    val walletRepository = remember {
        WalletRepository(AppDatabase.getInstance(context).walletDao())
    }
    val poolRepository = remember { PoolRepository(context) }
    val saveScope = rememberCoroutineScope()
    val viewModel: TotoViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return TotoViewModel(
                    bulletinRepository = SporTotoBulletinRepositoryImpl(
                        applicationContext = context.applicationContext
                    ),
                    walletRepository = walletRepository,
                    applicationContext = context.applicationContext
                ) as T
            }
        }
    )
    val application = context.applicationContext as android.app.Application
    val billingViewModel: BillingViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return BillingViewModel(
                    application = application,
                    walletRepository = walletRepository
                ) as T
            }
        }
    )
    val matches by viewModel.matches.collectAsStateWithLifecycle()
    val coupons by viewModel.coupons.collectAsStateWithLifecycle()
    val currentCoupon by viewModel.currentCoupon.collectAsStateWithLifecycle()
    val forumPosts by viewModel.forumPosts.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isRefreshingBulletin by viewModel.isRefreshingBulletin.collectAsStateWithLifecycle()
    val filterCriteria by viewModel.filterCriteria.collectAsStateWithLifecycle()
    val targetColumnCap by viewModel.targetColumnCap.collectAsStateWithLifecycle()

    val liveScores by viewModel.liveScores.collectAsStateWithLifecycle()
    val currentBulletin by viewModel.currentBulletin.collectAsStateWithLifecycle()
    val liveScoreProvider by viewModel.liveScoreProvider.collectAsStateWithLifecycle()
    val hasApiFootballKey by viewModel.hasApiFootballKey.collectAsStateWithLifecycle()
    val freeScoreRequestsRemaining by viewModel.freeScoreRequestsRemaining.collectAsStateWithLifecycle()
    val lastLiveUpdateTime by viewModel.lastLiveUpdateTime.collectAsStateWithLifecycle()
    val isRefreshingLiveScores by viewModel.isRefreshingLiveScores.collectAsStateWithLifecycle()

    val favoritedMatchNumbers by viewModel.favoritedMatchNumbers.collectAsStateWithLifecycle()
    val liveScoreError by viewModel.liveScoreError.collectAsStateWithLifecycle()

    val saveCouponToDevice: (Coupon) -> Unit = { coupon ->
        if (coupon.totalColumns == 0) {
            Toast.makeText(context, "Kaydedilecek kolon bulunmuyor.", Toast.LENGTH_SHORT).show()
        } else {
            saveScope.launch {
                val result = withContext(Dispatchers.IO) {
                    viewModel.saveCouponToJson(context, coupon)
                }
                result.onSuccess { file ->
                    Toast.makeText(
                        context,
                        "Kupon kaydedildi: ${file.name} • Kayıtlı JSON Aç bölümünden erişebilirsiniz.",
                        Toast.LENGTH_LONG
                    ).show()
                }.onFailure { error ->
                    Toast.makeText(
                        context,
                        "Kupon kaydedilemedi: ${error.message ?: "Bilinmeyen hata"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    val liveMatchesCount = remember(liveScores) { liveScores.count { it.isLive } }

    var currentTab by remember { mutableStateOf(AppNavTab.HOME) }
    var showPool by remember { mutableStateOf(false) }
    LaunchedEffect(incomingNesineUrl) {
        if (incomingNesineUrl != null) showPool = true
    }
    LaunchedEffect(currentTab, showPool) {
        val showLiveScores = currentTab == AppNavTab.RESULTS || showPool
        viewModel.setLivePolling(showLiveScores)
        if (showLiveScores) viewModel.pollLiveScoresNow()
    }
    var showTxtDialog by remember { mutableStateOf(false) }
    var showExcelDialog by remember { mutableStateOf(false) }
    var showFormulaWizard by remember { mutableStateOf(false) }
    var showGooglePlayDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }

    // Back button returns to Home tab if inside a subscreen
    BackHandler(enabled = showPool || currentTab != AppNavTab.HOME) {
        if (showPool) showPool = false else currentTab = AppNavTab.HOME
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopHeaderBar(
                userProfile = userProfile,
                onProfileClick = { currentTab = AppNavTab.PROFILE },
                onOpenWallet = { showGooglePlayDialog = true },
                onLiveClick = { currentTab = AppNavTab.RESULTS },
                liveMatchesCount = liveMatchesCount
            )
        },
        bottomBar = {
            ModernBottomNavBar(
                currentTab = currentTab,
                couponsCount = coupons.size,
                liveMatchesCount = liveMatchesCount,
                onTabSelected = { currentTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (showPool) {
                CouponPoolScreen(
                    weekKey = currentBulletin?.let { "${it.season}-${it.weekNumber}" }.orEmpty(),
                    matches = matches,
                    coupons = coupons,
                    liveScores = liveScores,
                    repository = poolRepository,
                    incomingNesineUrl = incomingNesineUrl,
                    displayUsername = userProfile.username,
                    onNesineUrlHandled = onNesineUrlHandled,
                    onBack = { showPool = false }
                )
            } else when (currentTab) {
                AppNavTab.HOME -> {
                    DashboardScreen(
                        userProfile = userProfile,
                        onFormulaSelected = { formula ->
                            viewModel.selectFormula(formula)
                            viewModel.generateCoupon(formula)
                            currentTab = AppNavTab.COUPONS
                        },
                        onNavigateTab = { tab -> currentTab = tab },
                        onOpenTxtImportDialog = { showTxtDialog = true },
                        onOpenExcelImportDialog = { showExcelDialog = true },
                        onOpenWizardDialog = { showFormulaWizard = true },
                        onOpenWallet = { showGooglePlayDialog = true }
                    )
                }

                AppNavTab.ANALYSIS -> {
                    MatchAnalysisScreen(
                        matches = matches,
                        onTogglePrediction = { matchId, outcome ->
                            viewModel.togglePrediction(matchId, outcome)
                        },
                        onSetMatchFavorite = { matchId, favorite ->
                            viewModel.setMatchFavorite(matchId, favorite)
                        },
                        onSelectAllFavorites = { viewModel.selectAllFavorites() },
                        onApplyQuickDoubles = { viewModel.applyQuickDoubles() },
                        onApplyQuickDoubles10To15 = { viewModel.applyQuickDoubles10To15() },
                        onApplyQuickDoubles1To9 = { viewModel.applyQuickDoubles1To9() },
                        onClearAll = { viewModel.clearAllSelections() },
                        onGenerateCoupon = {
                            viewModel.generateCoupon(FormulaType.GUARANTEE_14)
                            currentTab = AppNavTab.COUPONS
                        },
                        onOpenFormulaWizard = { showFormulaWizard = true },
                        onOpenFilterDialog = { showFilterDialog = true },
                        targetColumnCap = targetColumnCap,
                        onSelectTargetColumnCap = { viewModel.setTargetColumnCap(it) },
                        onApplyFilterProfile = { profile ->
                            viewModel.applyFilterProfile(profile)
                            currentTab = AppNavTab.COUPONS
                        },
                        isRefreshing = isRefreshingBulletin,
                        onRefreshBulletin = { viewModel.refreshBulletin(forceRefresh = true) },
                        favoritedMatchNumbers = favoritedMatchNumbers,
                        onToggleFavorite = { viewModel.toggleFavoriteMatch(it) }
                    )
                }

                AppNavTab.RESULTS -> {
                    MatchResultsScreen(
                        liveScores = liveScores,
                        providerName = liveScoreProvider,
                        hasApiKey = hasApiFootballKey,
                        freeRequestsRemaining = freeScoreRequestsRemaining,
                        onSaveApiKey = { viewModel.saveApiFootballKey(it) },
                        lastUpdateTime = lastLiveUpdateTime,
                        isRefreshing = isRefreshingLiveScores,
                        errorMessage = liveScoreError,
                        onPollNow = { viewModel.pollLiveScoresNow() }
                    )
                }

                AppNavTab.COUPONS -> {
                    CouponsScreen(
                        coupons = coupons,
                        currentCoupon = currentCoupon,
                        matches = matches,
                        savedCredentials = userProfile.nesineCredentials,
                        walletBalanceTL = userProfile.walletBalanceTL,
                        onSelectCoupon = { viewModel.setCurrentCoupon(it) },
                        onDeleteCoupon = { viewModel.deleteCoupon(it) },
                        onSaveCredentials = { viewModel.saveNesineCredentials(it) },
                        onNavigateToAnalysis = { currentTab = AppNavTab.ANALYSIS },
                        onOpenWallet = { showGooglePlayDialog = true },
                        onDeductFeeAndPlay = { coupon -> viewModel.deductColumnFee(coupon) },
                        onShareToForum = { coupon ->
                            viewModel.addForumPost("Garantili ${coupon.formulaType.badge} kuponumu inceleyebilirsiniz (${coupon.totalColumns} kolon)!", coupon.id)
                        },
                        onOpenWizard = { showFormulaWizard = true },
                        onOpenFilterDialog = { showFilterDialog = true },
                        onSaveCouponToJson = saveCouponToDevice,
                        onLoadSavedJsonCoupons = {
                            viewModel.listSavedJsonCoupons(context)
                        },
                        onLoadCouponFromFile = { file ->
                            val res = viewModel.loadCouponFromJsonFile(file)
                            res.onSuccess { loaded ->
                                Toast.makeText(context, "'${loaded.title}' başarıyla açıldı!", Toast.LENGTH_SHORT).show()
                            }.onFailure {
                                Toast.makeText(context, "Dosya okuma hatası: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDeleteSavedJsonFile = { file ->
                            val deleted = viewModel.deleteSavedJsonCoupon(file)
                            if (deleted) {
                                Toast.makeText(context, "Kayıtlı dosya silindi.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onNavigateToLiveResults = { currentTab = AppNavTab.RESULTS },
                        onOpenPool = { showPool = true }
                    )
                }

                AppNavTab.FORUM -> {
                    ForumScreen(
                        forumPosts = forumPosts,
                        userProfile = userProfile,
                        onAddPost = { content, couponId ->
                            viewModel.addForumPost(content, couponId)
                        },
                        onToggleLike = { postId ->
                            viewModel.toggleLikePost(postId)
                        }
                    )
                }

                AppNavTab.PROFILE -> {
                    ProfileScreen(
                        userProfile = userProfile,
                        onUpdateUsername = { viewModel.updateUsername(it) },
                        onSelectTeam = { viewModel.updateFavoriteTeam(it) },
                        onOpenWallet = { showGooglePlayDialog = true }
                    )
                }
            }

        }
    }

    if (showFormulaWizard) {
        FormulaWizardDialog(
            initialFormula = FormulaType.GUARANTEE_14,
            matches = matches,
            onDismiss = { showFormulaWizard = false },
            onConfirmFormula = { formula ->
                viewModel.selectFormula(formula)
                viewModel.generateCoupon(formula)
                currentTab = AppNavTab.COUPONS
            }
        )
    }

    if (showTxtDialog) {
        TxtImportDialog(
            onDismiss = { showTxtDialog = false },
            onImportTxt = { content ->
                val coupon = viewModel.importTxtColumns(content)
                if (coupon != null) {
                    currentTab = AppNavTab.COUPONS
                }
            },
            onLoadSample = {
                viewModel.loadSampleTxt()
                currentTab = AppNavTab.COUPONS
            }
        )
    }

    if (showExcelDialog) {
        ExcelImportDialog(
            onDismiss = { showExcelDialog = false },
            onImportContent = { content ->
                val coupon = viewModel.importTxtColumns(content, "Excel'den Yüklenen Kupon")
                if (coupon != null) {
                    currentTab = AppNavTab.COUPONS
                }
            }
        )
    }

    if (showGooglePlayDialog) {
        GooglePlayBillingDialog(
            currentBalanceTL = userProfile.walletBalanceTL,
            requiredFeeTL = currentCoupon?.columnFeeTotalTL,
            onDismiss = { showGooglePlayDialog = false },
            onPurchaseSuccess = { pkg ->
                viewModel.topUpWallet(pkg)
            }
        )
    }

    if (showFilterDialog) {
        TotoFilterDialog(
            initialCriteria = filterCriteria,
            matches = matches,
            targetColumnCap = targetColumnCap,
            sourceColumns = viewModel.currentFilterSourceColumns(),
            onDismiss = { showFilterDialog = false },
            onApplyFilters = { criteria, matrixResult, saveToDevice ->
                val coupon = viewModel.applyFiltersAndEliminate(
                    criteria = criteria,
                    precomputedMatrix = matrixResult
                )
                if (saveToDevice) saveCouponToDevice(coupon)
                currentTab = AppNavTab.COUPONS
            }
        )
    }

}
