package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.FormulaType
import com.example.ui.components.AppNavTab
import com.example.ui.components.ModernBottomNavBar
import com.example.ui.components.TopHeaderBar
import com.example.ui.screens.CouponsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExcelImportDialog
import com.example.ui.screens.FormulaWizardDialog
import com.example.ui.screens.ForumScreen
import com.example.ui.screens.GooglePlayBillingDialog
import com.example.ui.screens.MatchAnalysisScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.TxtImportDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.TotoViewModel

import androidx.compose.ui.platform.LocalContext
import com.example.data.local.AppDatabase
import com.example.data.repository.WalletRepository
import com.example.viewmodel.BillingViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContent()
            }
        }
    }
}

@Composable
fun MainAppContent() {
    val context = LocalContext.current
    val walletRepository = remember {
        WalletRepository(AppDatabase.getInstance(context).walletDao())
    }
    val viewModel: TotoViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return TotoViewModel(walletRepository = walletRepository) as T
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

    var currentTab by remember { mutableStateOf(AppNavTab.HOME) }
    var showTxtDialog by remember { mutableStateOf(false) }
    var showExcelDialog by remember { mutableStateOf(false) }
    var showFormulaWizard by remember { mutableStateOf(false) }
    var showGooglePlayDialog by remember { mutableStateOf(false) }

    // Back button returns to Home tab if inside a subscreen
    BackHandler(enabled = currentTab != AppNavTab.HOME) {
        currentTab = AppNavTab.HOME
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopHeaderBar(
                userProfile = userProfile,
                onProfileClick = { currentTab = AppNavTab.PROFILE },
                onOpenWallet = { showGooglePlayDialog = true }
            )
        },
        bottomBar = {
            ModernBottomNavBar(
                currentTab = currentTab,
                couponsCount = coupons.size,
                onTabSelected = { currentTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
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
                        onSelectAllFavorites = { viewModel.selectAllFavorites() },
                        onApplyQuickDoubles = { viewModel.applyQuickDoubles() },
                        onClearAll = { viewModel.clearAllSelections() },
                        onGenerateCoupon = {
                            viewModel.generateCoupon(FormulaType.GUARANTEE_14)
                            currentTab = AppNavTab.COUPONS
                        },
                        onOpenFormulaWizard = { showFormulaWizard = true },
                        isRefreshing = isRefreshingBulletin,
                        onRefreshBulletin = { viewModel.refreshBulletin(forceRefresh = true) }
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
                        onOpenWizard = { showFormulaWizard = true }
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
}
