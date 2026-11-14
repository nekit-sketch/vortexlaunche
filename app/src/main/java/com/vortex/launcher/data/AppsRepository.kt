package com.vortex.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class AppItem(
    val pkg: String,
    val label: String,
    val icon: ImageBitmap?,
    val banner: ImageBitmap?,
    val isGame: Boolean,
)

@Singleton
class AppsRepository @Inject constructor(@ApplicationContext private val ctx: Context) {

    suspend fun load(): List<AppItem> = withContext(Dispatchers.IO) {
        val pm = ctx.packageManager
        val leanback = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        (pm.queryIntentActivities(leanback, 0) + pm.queryIntentActivities(launcher, 0))
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName != ctx.packageName }
            .map { ri ->
                val ai = ri.activityInfo.applicationInfo
                val isGame = ai.category == ApplicationInfo.CATEGORY_GAME ||
                    (ai.flags and ApplicationInfo.FLAG_IS_GAME) != 0
                AppItem(
                    pkg = ri.activityInfo.packageName,
                    label = ri.loadLabel(pm).toString(),
                    icon = runCatching { ri.loadIcon(pm).toBitmap(192, 192).asImageBitmap() }.getOrNull(),
                    banner = runCatching { ri.activityInfo.loadBanner(pm)?.toBitmap(640, 360)?.asImageBitmap() }.getOrNull(),
                    isGame = isGame,
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    fun launch(pkg: String) {
        val i = ctx.packageManager.getLeanbackLaunchIntentForPackage(pkg)
            ?: ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
    }
}
