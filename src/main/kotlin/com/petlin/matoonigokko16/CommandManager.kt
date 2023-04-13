package com.petlin.matoonigokko16

import com.petlin.matoonigokko16.Matoonigokko16.Companion.GM
import com.petlin.matoonigokko16.Matoonigokko16.Companion.MU
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player


class CommandManager : CommandExecutor {

    companion object {
        var debugMode = false
    }

    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<out String>): Boolean {

        val isPlayer = sender is Player // 実行者がプレイヤーの場合にはtrueになる

        // 実行者がプレイヤーでない場合（コンソールから実行された場合）にはメッセージを出力して処理を終了する
        if (!isPlayer) {
            sender.sendMessage("cannot execute this command from console.")
            return true
        }

        val p = sender as Player
        val uuid = p.uniqueId

        // コマンドの処理を書くよ
        // コマンドを増やしたら「resources/plugin.yml」と 「メインクラス」に追加
        if (cmd.name.equals("settei", ignoreCase = true)) {
            if (!p.isOp) {
                p.sendMessage("${ChatColor.GRAY}このコマンドはOP所持者のみが実行可能です")
                return true
            }
            // args.length が０のときとそれ以外で分ける
            // サブコマンドごとにargs.lengthを指定すること（じゃなきゃ判定時にnullエラーが出ます。あとは可読性の問題）
            if (args.isEmpty()) {
                // 引数が0のとき
                p.inventory.addItem(ItemManager(Material.PAPER).setName("メインメニュー").finish())
                p.inventory.addItem(ItemManager(Material.PAPER).setName("ゲームモード選択").finish())
                p.inventory.addItem(ItemManager(Material.PAPER).setName("ゲーム別設定").finish())
                return true
            } else {
                // 引数が１以上のとき
                if (args[0].equals("debug", ignoreCase = true) && args.size == 2) {
                    if (args[1].equals("true", ignoreCase = true) || args[1].equals("false", ignoreCase = true)) {
                        debugMode = java.lang.Boolean.parseBoolean(args[1])
                        p.sendMessage(MU.infoText(title = "デバッグモード", content = debugMode.toString()))
                        return true
                    }
                }
                if (args[0].equals("money", ignoreCase = true) && args.size == 1) {
                    GM.addMoney(p.uniqueId, 128)
                    return true
                }
                if (args[0].equals("health", ignoreCase = true) && args.size == 2) {
                    val health = args[1].toDouble()
                    p.health = health
                    return true
                }
                if (args[0].equals("maxhealth", ignoreCase = true) && args.size == 2) {
                    val maxHealth = args[1].toDouble()
                    p.maxHealth = maxHealth
                    return true
                }
                if (args[0].equals("tp", ignoreCase = true) && args.size == 1) {
                    GM.randomTp(uuid)
                    return true
                }
            }
        }
        p.sendMessage("${ChatColor.RED}**********")
        return true
    }
}