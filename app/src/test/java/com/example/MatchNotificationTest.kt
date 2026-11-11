package com.example

import com.example.model.MatchAlertEventType
import com.example.model.MatchLiveScore
import com.example.model.MatchStatus
import com.example.model.NotificationPreferences
import com.example.viewmodel.TotoViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchNotificationTest {

    @Test
    fun testToggleFavoriteMatch() {
        val viewModel = TotoViewModel()

        // Match 1 is favorited by default in initial state
        assertTrue(viewModel.isMatchFavorited(1))

        // Toggle it off
        viewModel.toggleFavoriteMatch(1)
        assertFalse(viewModel.isMatchFavorited(1))

        // Toggle it back on
        viewModel.toggleFavoriteMatch(1)
        assertTrue(viewModel.isMatchFavorited(1))

        // Toggle match 15 (not initially favorited)
        assertFalse(viewModel.isMatchFavorited(15))
        viewModel.toggleFavoriteMatch(15)
        assertTrue(viewModel.isMatchFavorited(15))
    }

    @Test
    fun testGoalAlertGenerationOnFavoritedMatch() {
        val viewModel = TotoViewModel()
        if (viewModel.isLivePolling.value) {
            viewModel.toggleLivePolling()
        }
        if (!viewModel.isMatchFavorited(1)) {
            viewModel.toggleFavoriteMatch(1)
        }

        val previousScores = listOf(
            MatchLiveScore(
                matchNumber = 1,
                homeTeam = "FRANSA",
                awayTeam = "TÜRKİYE",
                status = MatchStatus.LIVE,
                homeScore = 0,
                awayScore = 0,
                minuteText = "20'"
            )
        )
        // Prime previous state
        viewModel.detectAndNotifyMatchEvents(previousScores)
        viewModel.clearNotificationHistory()

        val newScores = listOf(
            MatchLiveScore(
                matchNumber = 1,
                homeTeam = "FRANSA",
                awayTeam = "TÜRKİYE",
                status = MatchStatus.LIVE,
                homeScore = 0,
                awayScore = 1, // GOAL for Türkiye!
                minuteText = "32'"
            )
        )
        // Trigger event
        viewModel.detectAndNotifyMatchEvents(newScores)

        val alerts = viewModel.notificationHistory.value
        val alert = alerts.find { it.matchNumber == 1 && it.eventType == MatchAlertEventType.GOAL_SCORED }
        assertNotNull("Goal alert for match 1 should be dispatched", alert)
        assertEquals(MatchAlertEventType.GOAL_SCORED, alert!!.eventType)
        assertEquals(1, alert.matchNumber)
        assertEquals("TÜRKİYE", alert.scoringTeam)
        assertEquals("0 - 1", alert.scoreDisplay)
        assertTrue(alert.title.contains("GOOOOL"))
        assertTrue(alert.message.contains("TÜRKİYE"))
    }

    @Test
    fun testMatchStartAlertGeneration() {
        val viewModel = TotoViewModel()
        if (!viewModel.isMatchFavorited(9)) {
            viewModel.toggleFavoriteMatch(9)
        }
        viewModel.clearNotificationHistory()

        val previousScores = listOf(
            MatchLiveScore(
                matchNumber = 9,
                homeTeam = "İTALYA",
                awayTeam = "ALMANYA",
                status = MatchStatus.UPCOMING,
                homeScore = 0,
                awayScore = 0,
                minuteText = ""
            )
        )
        // Prime previous state
        viewModel.detectAndNotifyMatchEvents(previousScores)

        val newScores = listOf(
            MatchLiveScore(
                matchNumber = 9,
                homeTeam = "İTALYA",
                awayTeam = "ALMANYA",
                status = MatchStatus.LIVE,
                homeScore = 0,
                awayScore = 0,
                minuteText = "1'"
            )
        )
        // Trigger event
        viewModel.detectAndNotifyMatchEvents(newScores)

        val alerts = viewModel.notificationHistory.value
        assertEquals(1, alerts.size)
        val alert = alerts.first()
        assertEquals(MatchAlertEventType.MATCH_STARTED, alert.eventType)
        assertEquals(9, alert.matchNumber)
        assertTrue(alert.title.contains("Maç Başladı"))
    }

    @Test
    fun testNonFavoritedMatchDoesNotTriggerAlertWhenOnlyFavoritesEnabled() {
        val viewModel = TotoViewModel()
        if (viewModel.isMatchFavorited(12)) {
            viewModel.toggleFavoriteMatch(12)
        }
        viewModel.updateNotificationPreferences(
            NotificationPreferences(notifyOnlyFavorites = true, notifyOnGoal = true)
        )
        viewModel.clearNotificationHistory()

        val previousScores = listOf(
            MatchLiveScore(
                matchNumber = 12,
                homeTeam = "ARJANTİN",
                awayTeam = "BREZİLYA",
                status = MatchStatus.LIVE,
                homeScore = 0,
                awayScore = 0
            )
        )
        viewModel.detectAndNotifyMatchEvents(previousScores)

        val newScores = listOf(
            MatchLiveScore(
                matchNumber = 12,
                homeTeam = "ARJANTİN",
                awayTeam = "BREZİLYA",
                status = MatchStatus.LIVE,
                homeScore = 1,
                awayScore = 0
            )
        )
        viewModel.detectAndNotifyMatchEvents(newScores)

        val alerts = viewModel.notificationHistory.value
        assertTrue(alerts.isEmpty())
    }

    @Test
    fun testGoalAlertSuppressedWhenNotifyOnGoalIsFalse() {
        val viewModel = TotoViewModel()
        if (!viewModel.isMatchFavorited(1)) {
            viewModel.toggleFavoriteMatch(1)
        }
        viewModel.updateNotificationPreferences(
            NotificationPreferences(notifyOnGoal = false, notifyOnlyFavorites = false)
        )
        viewModel.clearNotificationHistory()

        val previousScores = listOf(
            MatchLiveScore(
                matchNumber = 1,
                homeTeam = "FRANSA",
                awayTeam = "TÜRKİYE",
                status = MatchStatus.LIVE,
                homeScore = 0,
                awayScore = 0
            )
        )
        viewModel.detectAndNotifyMatchEvents(previousScores)

        val newScores = listOf(
            MatchLiveScore(
                matchNumber = 1,
                homeTeam = "FRANSA",
                awayTeam = "TÜRKİYE",
                status = MatchStatus.LIVE,
                homeScore = 1,
                awayScore = 0
            )
        )
        viewModel.detectAndNotifyMatchEvents(newScores)

        val alerts = viewModel.notificationHistory.value
        assertTrue(alerts.isEmpty())
    }

    @Test
    fun testSimulateTestGoalAlertAndInAppDismissal() {
        val viewModel = TotoViewModel()
        viewModel.clearNotificationHistory()

        viewModel.simulateTestGoalAlert()

        val history = viewModel.notificationHistory.value
        assertFalse(history.isEmpty())
        val alert = history.first()
        assertEquals(MatchAlertEventType.GOAL_SCORED, alert.eventType)

        // In-app alert banner should be active
        assertNotNull(viewModel.activeInAppAlert.value)

        // Dismiss in-app alert
        viewModel.dismissInAppAlert()
        assertNull(viewModel.activeInAppAlert.value)

        // Mark all as read
        viewModel.markAllAlertsAsRead()
        assertTrue(viewModel.notificationHistory.value.all { it.isRead })
    }
}
