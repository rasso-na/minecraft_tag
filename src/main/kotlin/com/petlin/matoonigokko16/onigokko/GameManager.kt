package com.petlin.matoonigokko16.onigokko

import com.petlin.matoonigokko16.general.GameManager.Companion.participants
import com.petlin.matoonigokko16.ItemManager
import com.petlin.matoonigokko16.Matoonigokko16.Companion.GM
import com.petlin.matoonigokko16.Matoonigokko16.Companion.GUI
import com.petlin.matoonigokko16.Matoonigokko16.Companion.MU
import com.petlin.matoonigokko16.Matoonigokko16.Companion.plugin
import com.petlin.matoonigokko16.ParticipantsManager
import org.bukkit.*
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID

class GameManager {

    // 設定or参照用
    var hit = 10  // 必要ヒット数（設定用）
    var rescueInterval = 20  // 救助インターバル
    var minMoneyRun = 5  // 逃走者金塊獲得時の乱数最小値
    var maxMoneyRun = 10  // 逃走者金塊獲得時の乱数最大値
    var minMoneyOni = 1  // 鬼金塊獲得時の乱数最小値
    var maxMoneyOni = 3  // 鬼金塊獲得時の乱数最大値
    var oniLife = 3  // 鬼の体力
    var ballInterval = 10  // カラーボールインターバル
    var oniWaiting = 20  // 鬼の動き出す時間（秒数で指定）
    var oniGlowingTime = 5  // カラーボールを当てられた鬼の光る時間（秒数で指定）
    var detectOniDead = false

    // ゲーム中に使う変数
    var rescueIntervalOnGame = 0
    var hitOnGame = 0

    fun start() {
        rescueIntervalOnGame = rescueInterval
        addAllLeft("逃走者")  // 未登録の参加者を逃走者として追加
        getUuidByRole("逃走者").forEach {
            GM.resetPlayer(it, 2.0, 20)
        }
        getUuidByRole("鬼").forEach {
            val p = Bukkit.getPlayer(it) ?: return
            GM.resetPlayer(it, oniLife*2.0, 20)
            p.gameMode = GameMode.SPECTATOR
            setArmor(it)
        }
        object : BukkitRunnable() { override fun run() {  // 鬼の遅延タスク
            getUuidByRole("鬼").forEach {
                val p = Bukkit.getPlayer(it) ?: return
                GM.randomTp(it)
                p.gameMode = GameMode.ADVENTURE
            }
            detectOniDead = true
            MU.textToAll (MU.infoText(content = "${ChatColor.RED}鬼が動き出しました"))
        } }.runTaskLater (plugin, oniWaiting*20L)
        participants.forEach { // 参加者をランダムテレポート
            GM.randomTp(it.key)
        }
    }

    fun getUuidByRole(role: String): Set<UUID> {
        return participants.filterValues { it.role.equals(role) }.keys
    }

    fun setArmor(uuid: UUID) {
        val p = Bukkit.getPlayer(uuid) ?: return
        p.inventory.helmet = ItemManager(Material.DIAMOND_HELMET).addEnch(Enchantment.PROTECTION_ENVIRONMENTAL, 1).addEnch(Enchantment.BINDING_CURSE, 1).finish()
        p.inventory.chestplate = ItemManager(Material.DIAMOND_CHESTPLATE).addEnch(Enchantment.PROTECTION_ENVIRONMENTAL, 1).addEnch(Enchantment.BINDING_CURSE, 1).finish()
        p.inventory.leggings = ItemManager(Material.DIAMOND_LEGGINGS).addEnch(Enchantment.PROTECTION_ENVIRONMENTAL, 1).addEnch(Enchantment.BINDING_CURSE, 1).finish()
        p.inventory.boots = ItemManager(Material.DIAMOND_BOOTS).addEnch(Enchantment.PROTECTION_ENVIRONMENTAL, 1).addEnch(Enchantment.BINDING_CURSE, 1).finish()
    }

    // ランダムで鬼を1人選ぶ
    fun rdmOni() {
        val cand = getNotOni()
        if (cand.isNotEmpty()) {
            val chosen = cand.random()
            assign(chosen, "鬼", true)
        } else {
            MU.textToAll("${ChatColor.RED}役職を割り当てられるプレイヤーがいません")
        }
    }

    /** uuidからroleを取得 */
    fun getRole(uuid: UUID): String {
        return participants[uuid]?.role ?: ""
    }

    /** 鬼の選択を初期化 */
    fun oniReset() {
        participants = participants.filterValues { !it.role.equals("鬼") } as MutableMap<UUID, ParticipantsManager>
        MU.textToAll (MU.infoText(content = "鬼の選択をリセットしました"))
        GUI.update("指名鬼")  // 鬼選択リストのアップデート
    }

    fun result(): String {
        return if (getAlive("鬼").isEmpty()) {  // 鬼が全員死亡
            "逃走者の勝利"
        } else if (getAlive("逃走者").isEmpty()) {  // 逃走者が全員死亡
            "鬼の勝利"
        } else {  // 時間切れのとき
            "TIME UP"
        }
    }

    /** あるroleの生存者（スペクテイタじゃない）の人のuuidをリストで取得（オフラインの人も含む） */
    fun getAlive(role: String): Set<UUID> {
        return participants.filter { it.value.role.equals(role) && (Bukkit.getPlayer(it.key)?.gameMode?.equals(GameMode.SPECTATOR) == false) }.keys
    }

    /** あるroleの死者（スペクテイタ）の人のuuidをリストで取得（オンラインの人のみ） */
    fun getDead(role: String): Set<UUID> {
        return participants.filter { it.value.role.equals(role) && (Bukkit.getPlayer(it.key)?.gameMode?.equals(GameMode.SPECTATOR) == true) && GM.getOnlineUuids().contains(it.key)}.keys
    }

    fun effect() {
        getUuidByRole("鬼").forEach {
            val p = Bukkit.getPlayer(it) ?: return
            p.player?.addPotionEffect(PotionEffect(PotionEffectType.SATURATION, 40, 255, false, false, false))
        }
    }

    fun reset() {
        hitOnGame = 0
        detectOniDead = false
    }

    fun giveFirstItem(uuid: UUID) {
        val p = Bukkit.getPlayer(uuid) ?: return
        if (getRole(uuid).equals("逃走者")) {
            p.inventory.addItem(GM.getItem("カラーボール")!!)
            p.inventory.addItem(GM.getItem("救助の杖")!!)
            p.inventory.addItem(GM.getItem("武器職人のつるはし")!!)
        }
        p.inventory.addItem(GM.getItem("リスポーンの呪文書")!!)
    }

    /** 逃走者もしくは鬼の生存者が0になったらtrue */
    fun isFinished(): Boolean {
        return (getAlive("鬼").isEmpty() && detectOniDead) || getAlive("逃走者").isEmpty()
    }

    fun changeHit(): String {
        val changed = MU.getNext(hit, List(6) { (it+1)*5 }) as Int
        hit = changed
        return "$changed"
    }

    fun changeRescueInterval(): String {
        val changed = MU.getNext(rescueInterval, List(6) { (it+1)*10 }) as Int
        rescueInterval = changed
        return "${changed}秒"
    }

    fun changeOniLife(): String {
        val changed = MU.getNext(oniLife, List(5) { it+1 }) as Int
        oniLife = changed
        return "$changed"
    }

    fun changeBallInterval(): String {
        val changed = MU.getNext(ballInterval, List(10) { (it+1)*2 }) as Int
        ballInterval = changed
        return "${changed}秒"
    }

    fun rescue(uuid: UUID) {
        val p = Bukkit.getPlayer(uuid) ?: return
        val cand = getDead("逃走者")
        if (cand.isNotEmpty()) {
                val t = cand.random()
                GM.respawn(t)
                MU.textToAll (MU.infoText("救助", MU.whoToWho(p.name, Bukkit.getPlayer(t)?.name ?: "")))
                rescueIntervalOnGame = rescueInterval
            } else {
                p.sendMessage (MU.infoText("${ChatColor.RED}救助不可", "誰も捕まっていません"))
            }
    }

    fun createSword(p: Player) {
        val inv = p.inventory
        if (GM.isEnough(p.inventory, "つよいけん", 1)) {
            p.sendMessage (MU.infoText("${ChatColor.BLUE}製作不可", "「つよいけん」は一度にひとつまで所持可能です"))
            return
        }
        if (GM.isEnough(p.inventory, "かがやくダイヤ", 2) && GM.isEnough(p.inventory, "つやのある木の棒", 1)) {
            GM.removeItem(inv, "かがやくダイヤ", 2)
            GM.removeItem(inv, "つやのある木の棒", 1)
            inv.addItem(GM.getItem("つよいけん")!!)
            MU.textToAll (MU.infoText(content = "誰かが「つよいけん」を手に入れた......"))
        } else {
            p.sendMessage (MU.infoText("${ChatColor.BLUE}製作不可", "材料不足"))
        }
    }

    /** 鬼以外の人のUUIDを返す */
    fun getNotOni(): List<UUID> {
        return Bukkit.getOnlinePlayers().mapNotNull { it.uniqueId }.filterNot { getUuidByRole("鬼").contains(it) }
    }

    /** タイマーをスタート */
    fun onTimer() {
            if (rescueIntervalOnGame > 0) { rescueIntervalOnGame-- }
    }

    fun setGamerule() {
        // ゲームルール設定
        for (w in Bukkit.getWorlds()) {
            w.setGameRule(GameRule.NATURAL_REGENERATION, false) // 体力自然回復オフ
            w.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false) //デスメッセージ表示OFF
            w.difficulty = Difficulty.NORMAL //難易度をノーマルにする
        }
    }

    /** 役職割当て */
    fun assign(uuid: UUID, role: String, announce: Boolean = false): Boolean {
        val p = Bukkit.getPlayer(uuid) ?: return false
        GM.add(uuid)
        val res = isNotAssigned(uuid)
        if (res) {  // まだassignされていなければ
            participants[uuid]?.role = role
            if (announce) { MU.textToAll (MU.infoText(role, p.name)) }
        } else {  // すでに何かのroleを持っていた場合 -> assignしない
            if (announce) { MU.textToAll("${ChatColor.RED}${p.name}はすでに「${role}」の役職を持っています") }
        }
        return res
    }

    /**  デスペナルティ */
    fun deathPenalty(uuid: UUID) {
        val p = Bukkit.getPlayer(uuid) ?: return
        val inv = p.inventory
        if ((0..1).random() == 1) {
            val now = GM.countItem(inv, "金塊")
            p.sendMessage(MU.infoText("${ChatColor.DARK_RED}デスペナルティ", "所持金半減"))
            GM.removeItem(inv, "金塊", now/2)
        } else {
            p.sendMessage(MU.infoText("${ChatColor.DARK_RED}デスペナルティ", "なし"))
        }
    }

    /** サーバーにいる中でまだ追加されてない人をすべてparticipantsに追加し、指定のデフォルトroleを与える(スタート時の処理) */
    private fun addAllLeft(role: String) {
        Bukkit.getOnlinePlayers().map { it.uniqueId }.forEach { uuid -> GM.add(uuid) }
        participants.filter { isNotAssigned(it.key) }.forEach { assign(it.key, role) } // 役職なしの参加者をすべてroleにAssign
    }

    /** roleがまだなければtrue、既にあればfalse */
    fun isNotAssigned(uuid: UUID): Boolean {
        return participants[uuid]?.role.equals("")
    }
    
    fun bossBar(): Pair<String, Double> {
        val title: String
        val progress: Double
        if (GM.isOnGame) {  // ゲーム中
            title = MU.getSeparated(listOf(GM.secToMinSec(GM.timeOnGame), "生存者:${getAlive("逃走者").size}", "HIT:${hitOnGame}/${hit}", if (rescueIntervalOnGame > 0) { "救助可能まで${rescueIntervalOnGame}秒" } else { "救助可能" }))
            progress = GM.timeOnGame.toDouble() / GM.time.toDouble()
        } else {  // ゲーム外
            title = MU.getSeparated(listOf(GM.secToMinSec(GM.time), "鬼:${getUuidByRole("鬼").size}", "HIT:${hit}", "救助:${rescueInterval}秒", "鬼の体力:${oniLife}", "カラーボール:${ballInterval}秒"))
            progress = 0.0
        }
        return Pair(title, progress)
    }

    fun actionBar(uuid: UUID): String {
        return if (GM.isOnGame) {
            MU.getSeparated(listOf("役職:${getRole(uuid)}"))
        } else {
            ""
        }
    }

    fun rescueSkill(uuid: UUID) {
        val p = Bukkit.getPlayer(uuid) ?: return
        val loc = p.location
        if (rescueIntervalOnGame <= 0
            && getRole(uuid).equals("逃走者")
            && p.inventory.itemInMainHand.itemMeta?.displayName?.equals("救助の杖") == true
            && participants[uuid]?.task == 0) {
            participants[uuid]?.task = object : BukkitRunnable() {
                override fun run() {
                    if (!p.isSneaking
                        || participants[uuid]?.sneak!! >= GM.sneak
                        || rescueIntervalOnGame > 0
                        || p.inventory.itemInMainHand.itemMeta?.displayName?.equals("救助の杖") != true) {
                        GM.cancelTask(uuid)
                        participants[uuid]?.sneak = 0
                        GM.removeEffect(uuid)
//                        p.sendTitle("", "")
                        return
                    }
                    participants[uuid]?.sneak = participants[uuid]?.sneak!! + 1
                    val sneaking = participants[uuid]?.sneak!!
                    p.sendTitle("", "${ChatColor.YELLOW}|".repeat(sneaking)+"${ChatColor.GRAY}|".repeat(GM.sneak - sneaking), 0, 21, 0)
                    if (sneaking >= GM.sneak) { rescue(uuid) }
                    loc.world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.7.toFloat(), 0.7.toFloat())
                    p.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, Int.MAX_VALUE, 0, false, false, false))
                    p.addPotionEffect(PotionEffect(PotionEffectType.SLOW, Int.MAX_VALUE, 255, false, false, false))
                    p.addPotionEffect(PotionEffect(PotionEffectType.JUMP, Int.MAX_VALUE, 200, false, false, false))
                    p.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, Int.MAX_VALUE, 255, false, false, false))
                    p.addPotionEffect(PotionEffect(PotionEffectType.HUNGER, Int.MAX_VALUE, 10, false, false, false))
                }
            }.runTaskTimer(plugin, 0L, 20L).taskId
        }
    }

    fun createSwordSkill(uuid: UUID) {
        val p = Bukkit.getPlayer(uuid) ?: return
        val loc = p.location
        if (getRole(uuid).equals("逃走者")
            && p.inventory.itemInMainHand.itemMeta?.displayName?.equals("武器職人のつるはし") == true
            && participants[uuid]?.task == 0) {
            participants[uuid]?.task = object : BukkitRunnable() {
                override fun run() {
                    if (!p.isSneaking
                        || participants[uuid]?.sneak!! >= GM.sneak
                        ||  p.inventory.itemInMainHand.itemMeta?.displayName?.equals("武器職人のつるはし") != true) {
                        GM.cancelTask(uuid)
                        participants[uuid]?.sneak = 0
                        GM.removeEffect(uuid)
//                        p.sendTitle("", "")
                        return
                    }
                    participants[uuid]?.sneak = participants[uuid]?.sneak!! + 1
                    val sneaking = participants[uuid]?.sneak!!
                    p.sendTitle("", "${ChatColor.YELLOW}|".repeat(sneaking)+"${ChatColor.GRAY}|".repeat(GM.sneak - sneaking), 0, 21, 0)
                    if (sneaking>= GM.sneak) { createSword(p) }
                    loc.world.playSound(loc, Sound.BLOCK_ANVIL_LAND, 0.7.toFloat(), (5..15).random().toFloat()/10)
                    p.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, Int.MAX_VALUE, 0, false, false, false))
                    p.addPotionEffect(PotionEffect(PotionEffectType.SLOW, Int.MAX_VALUE, 255, false, false, false))
                    p.addPotionEffect(PotionEffect(PotionEffectType.JUMP, Int.MAX_VALUE, 200, false, false, false))
                    p.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, Int.MAX_VALUE, 255, false, false, false))
                    p.addPotionEffect(PotionEffect(PotionEffectType.HUNGER, Int.MAX_VALUE, 10, false, false, false))
                }
            }.runTaskTimer(plugin, 0L, 20L).taskId
        }
    }
    
}