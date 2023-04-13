package com.petlin.matoonigokko16

import com.google.common.base.Strings
import com.petlin.matoonigokko16.CommandManager.Companion.debugMode
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player

class MyUtil {

    fun opText(text: String) {
        for (player: Player in Bukkit.getOnlinePlayers()) {
            if (player.isOp) {
                player.sendMessage(
                    infoText(title = "${ChatColor.DARK_GRAY}管理者用メッセージ", content = "${ChatColor.DARK_GRAY}${text}")
                )
            }
        }
    }

    fun titleAll(title: String = "", subtitle: String = "") {
        Bukkit.getOnlinePlayers().forEach { it.sendTitle(title, subtitle) }
    }

    fun debugMessage(text: String) {
        // デバッグモードがONのときにOP所持者に対してデバッグメッセージを表示する
        if (debugMode) {
            for (player: Player in Bukkit.getOnlinePlayers()) {
                if (player.isOp) {
                    player.sendMessage(
                        infoText(title = "${ChatColor.DARK_GRAY}debug", content = "${ChatColor.DARK_GRAY}${text}")
                    )
                }
            }
        }
    }

    fun infoText(title: String = "${ChatColor.AQUA}アナウンス", content: String): String {
        return "${ChatColor.GRAY}[ ${ChatColor.RESET}$title ${ChatColor.GRAY}]  ${ChatColor.RESET}$content"
    }

    fun textToAll(text: String) {
        Bukkit.getOnlinePlayers().forEach { p -> p.sendMessage(text) }
    }

    fun cmdUsage(player: Player, cmdName: String, cmdMap: Map<String?, String?>) {
        // コマンドの使い方を表示する（「/<command> help」用）
        player.sendMessage(Strings.padEnd("${ChatColor.GRAY}-------- 「 ${ChatColor.YELLOW}/$cmdName${ChatColor.GRAY} 」の使い方 ", 45, '-'))
        for (key: String? in cmdMap.keys) {
            val value = cmdMap[key]
            player.sendMessage(infoText(key!!, value!!))
        }
    }

    fun whoToWho(from: String, to: String): String {
        return "${ChatColor.RESET}${from} ${ChatColor.DARK_GRAY}>>> ${ChatColor.RESET}${to}"
    }

    fun getNext(now: Any, source: List<Any>): Any {
        val nextInd = source.indexOf(now) + 1
        return if (nextInd < source.size) { source[nextInd] } else { source[0] }
    }

    fun getSeparated(contents: List<String>): String {
        var res = ""
        contents.forEach {
            res += if (contents.indexOf(it) < contents.size-1) {
                "$it ${ChatColor.DARK_GRAY}|${ChatColor.RESET} "
            } else {
                it
            }
        }
        return res
    }

}