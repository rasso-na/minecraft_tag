package com.petlin.matoonigokko16

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scoreboard.Team


class Matoonigokko16 : JavaPlugin() {

    companion object{

        lateinit var plugin: Matoonigokko16
        var GM = com.petlin.matoonigokko16.general.GameManager()
        var OM = com.petlin.matoonigokko16.onigokko.GameManager()
        var MU = MyUtil()
        var BBM = BossbarManager()
        var ABM = ActionbarManager()
        var GUI = GUIManager()
        lateinit var hideNT: Team
        val gamemodes = mapOf(
            "鬼ごっこ" to OM,
        )

        var online = 0
        var max = 0

    }

    init {
        plugin = this
    }

    override fun onEnable() {
        // Plugin startup logic
        val enableText = "**********"
        logger.info(enableText)
        server.pluginManager.registerEvents(com.petlin.matoonigokko16.general.EventListener(), this)
        server.pluginManager.registerEvents(com.petlin.matoonigokko16.onigokko.EventListener(), this)
        getCommand("settei")?.setExecutor(CommandManager())
        GM.setGamerule()

        // チームが無ければ作る  // すでにあれば作らない(そのまま使う)
        hideNT = Bukkit.getScoreboardManager().mainScoreboard.getTeam("hideNT") ?: Bukkit.getScoreboardManager().mainScoreboard.registerNewTeam("hideNT")
        hideNT.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS)
        hideNT.setCanSeeFriendlyInvisibles(false)
        hideNT.setAllowFriendlyFire(true)

        object : BukkitRunnable() { override fun run() {
            online = Bukkit.getOnlinePlayers().size
            max = Bukkit.getMaxPlayers()
            Bukkit.getOnlinePlayers().forEach {
                ABM.update(it.uniqueId)
            }
            BBM.update()
        } }.runTaskTimer(this, 0L, 20L)

    }

    override fun onDisable() {
        // Plugin shutdown logic
        Bukkit.getOnlinePlayers().forEach { it.kickPlayer("サーバーをリロードもしくは停止しています......") }
        val disableText = "**********"
        logger.info(disableText)
    }
}