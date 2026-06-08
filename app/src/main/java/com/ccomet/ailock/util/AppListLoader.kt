package com.ccomet.ailock.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.ccomet.ailock.data.model.AppCategory
import com.ccomet.ailock.data.model.InstalledAppInfo

class AppListLoader(private val context: Context) {
    fun load(query: String, lockedPackages: Set<String>): List<InstalledAppInfo> {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launcherIntent, 0)
        }
        val normalizedQuery = query.trim().lowercase()

        return resolved.asSequence()
            .mapNotNull { info ->
                val activityInfo = info.activityInfo ?: return@mapNotNull null
                val packageName = activityInfo.packageName
                if (packageName == context.packageName) return@mapNotNull null
                val label = info.loadLabel(packageManager).toString()
                InstalledAppInfo(
                    packageName = packageName,
                    appName = label,
                    category = classify(packageName, label),
                    icon = runCatching { info.loadIcon(packageManager) }.getOrNull(),
                    isLocked = packageName in lockedPackages,
                )
            }
            .distinctBy { it.packageName }
            .filter {
                normalizedQuery.isBlank() ||
                    it.appName.lowercase().contains(normalizedQuery) ||
                    it.packageName.lowercase().contains(normalizedQuery) ||
                    it.category.label.lowercase().contains(normalizedQuery)
            }
            .sortedWith(compareBy<InstalledAppInfo> { !it.isLocked }.thenBy { it.category.ordinal }.thenBy { it.appName.lowercase() })
            .toList()
    }

    fun appName(packageName: String): String {
        val packageManager = context.packageManager
        return runCatching {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, 0)
            }
            info.loadLabel(packageManager).toString()
        }.getOrDefault(packageName.substringAfterLast('.'))
    }

    private fun classify(packageName: String, appName: String): AppCategory {
        val text = "$packageName $appName".lowercase()
        return when {
            listOf("instagram", "facebook", "twitter", "x.", "tiktok", "threads").any { it in text } -> AppCategory.SNS
            listOf("youtube", "netflix", "wavve", "tving", "disney", "twitch").any { it in text } -> AppCategory.VIDEO
            listOf("game", "nexon", "riot", "steam", "roblox", "minecraft").any { it in text } -> AppCategory.GAME
            listOf("webtoon", "kakao.page", "ridibooks", "novel").any { it in text } -> AppCategory.WEBTOON
            listOf("shop", "coupang", "gmarket", "11st", "auction", "amazon").any { it in text } -> AppCategory.SHOPPING
            listOf("chrome", "browser", "firefox", "edge", "samsung.android.app.sbrowser").any { it in text } -> AppCategory.BROWSER
            listOf("kakao.talk", "messenger", "telegram", "discord", "line").any { it in text } -> AppCategory.MESSENGER
            else -> AppCategory.OTHER
        }
    }
}

