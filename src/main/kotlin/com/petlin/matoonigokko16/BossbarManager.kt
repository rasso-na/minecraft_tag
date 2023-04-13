package com.petlin.matoonigokko16

import com.petlin.matoonigokko16.Matoonigokko16.Companion.GM
import com.petlin.matoonigokko16.Matoonigokko16.Companion.max
import com.petlin.matoonigokko16.Matoonigokko16.Companion.MU
import com.petlin.matoonigokko16.Matoonigokko16.Companion.gamemodes
import com.petlin.matoonigokko16.Matoonigokko16.Companion.online
import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle

class BossbarManager {

    var bossbar = Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SOLID)

    fun update() {
        val title: String
        val progress: Double
        if (GM.gamemode.equals("未指定")) {
            title = MU.getSeparated(listOf("人数:${online}/${max}"))
            progress = online.toDouble() / max.toDouble()
        } else {
            val pair = gamemodes[GM.gamemode]?.bossBar()
            title = pair?.first ?: ""
            progress = pair?.second ?: 0.0
        }
        bossbar.setTitle(title)
        bossbar.progress = progress
    }

}