package com.ccomet.ailock.domain.usecase

import com.ccomet.ailock.data.model.LockedAppConfig
import com.ccomet.ailock.domain.model.BlockDecision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class BlockingPolicyTest {
    @Test
    fun `blank package is allowed`() {
        val decision = BlockingPolicy.evaluate(
            packageName = "",
            lockedApps = listOf(lockedApp()),
            today = DayOfWeek.MONDAY,
            todayUsageMinutes = 0,
            hasActiveTemporaryAllowance = false,
            ignoredPackages = emptySet(),
        )

        assertSame(BlockDecision.Allow, decision)
    }

    @Test
    fun `ignored package is allowed`() {
        val decision = BlockingPolicy.evaluate(
            packageName = "com.android.settings",
            lockedApps = listOf(lockedApp(packageName = "com.android.settings")),
            today = DayOfWeek.MONDAY,
            todayUsageMinutes = 0,
            hasActiveTemporaryAllowance = false,
            ignoredPackages = setOf("com.android.settings"),
        )

        assertSame(BlockDecision.Allow, decision)
    }

    @Test
    fun `temporary allowance bypasses intervention`() {
        val decision = BlockingPolicy.evaluate(
            packageName = "com.example.social",
            lockedApps = listOf(lockedApp()),
            today = DayOfWeek.MONDAY,
            todayUsageMinutes = 0,
            hasActiveTemporaryAllowance = true,
            ignoredPackages = emptySet(),
        )

        assertSame(BlockDecision.Allow, decision)
    }

    @Test
    fun `unconfigured package is allowed`() {
        val decision = BlockingPolicy.evaluate(
            packageName = "com.example.browser",
            lockedApps = listOf(lockedApp()),
            today = DayOfWeek.MONDAY,
            todayUsageMinutes = 0,
            hasActiveTemporaryAllowance = false,
            ignoredPackages = emptySet(),
        )

        assertSame(BlockDecision.Allow, decision)
    }

    @Test
    fun `locked app on inactive day is allowed`() {
        val decision = BlockingPolicy.evaluate(
            packageName = "com.example.social",
            lockedApps = listOf(lockedApp(selectedDays = setOf(DayOfWeek.TUESDAY))),
            today = DayOfWeek.MONDAY,
            todayUsageMinutes = 0,
            hasActiveTemporaryAllowance = false,
            ignoredPackages = emptySet(),
        )

        assertSame(BlockDecision.Allow, decision)
    }

    @Test
    fun `locked app on active day without active lock is allowed`() {
        val config = lockedApp(selectedDays = setOf(DayOfWeek.MONDAY))

        val decision = BlockingPolicy.evaluate(
            packageName = "com.example.social",
            lockedApps = listOf(config),
            today = DayOfWeek.MONDAY,
            todayUsageMinutes = 0,
            hasActiveTemporaryAllowance = false,
            ignoredPackages = emptySet(),
        )

        assertSame(BlockDecision.Allow, decision)
    }

    @Test
    fun `locked app over daily limit shows intervention`() {
        val config = lockedApp(selectedDays = setOf(DayOfWeek.MONDAY), dailyLimitMinutes = 30)

        val decision = BlockingPolicy.evaluate(
            packageName = "com.example.social",
            lockedApps = listOf(config),
            today = DayOfWeek.MONDAY,
            todayUsageMinutes = 30,
            hasActiveTemporaryAllowance = false,
            ignoredPackages = emptySet(),
        )

        assertTrue(decision is BlockDecision.ShowIntervention)
        val intervention = decision as BlockDecision.ShowIntervention
        assertEquals(config, intervention.config)
        assertEquals("daily hard limit exceeded", intervention.reason)
    }

    @Test
    fun `expired active session shows time limit intervention before daily limit`() {
        val config = lockedApp(selectedDays = setOf(DayOfWeek.MONDAY), dailyLimitMinutes = 10)

        val decision = BlockingPolicy.evaluate(
            packageName = "com.example.social",
            lockedApps = listOf(config),
            today = DayOfWeek.MONDAY,
            todayUsageMinutes = 3,
            hasActiveTemporaryAllowance = false,
            hasExpiredActiveSession = true,
            ignoredPackages = emptySet(),
        )

        assertTrue(decision is BlockDecision.ShowIntervention)
        val intervention = decision as BlockDecision.ShowIntervention
        assertEquals(config, intervention.config)
        assertEquals("temporary allowance expired", intervention.reason)
        assertTrue(intervention.timeLimitExceeded)
    }

    private fun lockedApp(
        packageName: String = "com.example.social",
        selectedDays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
        dailyLimitMinutes: Int? = null,
    ): LockedAppConfig = LockedAppConfig(
        packageName = packageName,
        appName = "Social",
        selectedDays = selectedDays,
        dailyLimitMinutes = dailyLimitMinutes,
    )
}
