package com.petlin.matoonigokko16.general

import com.petlin.matoonigokko16.ConfigManager
import com.petlin.matoonigokko16.ItemManager
import com.petlin.matoonigokko16.Matoonigokko16
import com.petlin.matoonigokko16.Matoonigokko16.Companion.GUI
import com.petlin.matoonigokko16.Matoonigokko16.Companion.hideNT
import com.petlin.matoonigokko16.Matoonigokko16.Companion.MU
import com.petlin.matoonigokko16.Matoonigokko16.Companion.gamemodes
import com.petlin.matoonigokko16.Matoonigokko16.Companion.plugin
import com.petlin.matoonigokko16.ParticipantsManager
import org.bukkit.*
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scoreboard.Team
import java.util.UUID

class GameManager {

    var time = 900  // 時間（設定用）

    var isOnGame: Boolean = false
    var gamemode: String = "未指定"
    var sneak = 10  // スニークスキルの秒数

    var timeOnGame = 0

    companion object {
        var participants = mutableMapOf<UUID, ParticipantsManager>()
    }

    private val items = listOf<ItemStack>(
        ItemManager(Material.SNOWBALL)
            .setName("**********")
            .setLore(listOf("**********", "**********"))
            .setUsage("鬼に投げつける")
            .finish(),
        ItemManager(Material.WOODEN_HOE)
            .setName("**********")
            .setLore(listOf("**********", "**********", "**********"))
            .setUsage("手に持って10秒間スニーク")
            .setUnbreakable()
            .addEnch(Enchantment.DAMAGE_UNDEAD, 5)
            .finish(),
        ItemManager(Material.IRON_PICKAXE)
            .setName("**********")
            .setLore(listOf("**********", "**********", "**********"))
            .setUsage("手に持って10秒間スニーク")
            .setUnbreakable()
            .addEnch(Enchantment.DIG_SPEED, 5)
            .finish(),
        ItemManager(Material.DIAMOND_SWORD)
            .setName("**********")
            .setLore(listOf("**********", "**********", "**********"))
            .setTimes(1)
            .addEnch(Enchantment.DAMAGE_UNDEAD, 5)
            .finish(),
        ItemManager(Material.GOLD_NUGGET)
            .setName("**********")
            .setLore(listOf("**********"))
            .finish(),
        ItemManager(Material.BOOK)
            .setName("**********")
            .setLore(listOf("**********"))
            .setUsage("手に持って10秒間スニーク")
            .finish()
    )

    /** ゲーム中にかけるエフェクト（再帰実行用） */
    fun effect() {
        gamemodes[gamemode]?.effect()
    }

    fun setHighest(uuid: UUID) {
        val p = Bukkit.getPlayer(uuid) ?: return
        val y = p.location.blockY
        val c = ConfigManager("settings", plugin)
        c["highest"] = y
        p.sendMessage(MU.infoText("最大高度", "$y"))
    }

    /** 名前と数を指定して特定のインベントリからアイテムを消去する */
    fun removeItem(inv: Inventory, name: String, amount: Int) {
        inv.filter { it?.itemMeta?.displayName.equals(name) }.forEach {
            if (it.amount >= amount) {
                it.amount -= amount
                return
            } else {
                it.amount = 0
            }
        }
    }

    /** 名前を指定して特定のインベントリのアイテムを数え、足りていたらtrueを返す。足りていなければ消さずにfalseを返す。 */
    fun isEnough(inv: Inventory, name: String, amount: Int): Boolean {
        return countItem(inv, name) >= amount
    }

    fun countItem(inv: Inventory, name: String): Int {
        var num = 0
        inv.filter { it?.itemMeta?.displayName.equals(name) }.forEach {
            num += it.amount
        }
        return num
    }

    /** itemsリストの中から名前でアイテムを検索 */
    fun getItem(name: String): ItemStack? {
        return items.find { it.itemMeta.displayName.equals(name) }
    }

    /** ゲームをスタートする */
    fun start() {
        if (!isReady()) {
            return
        }
        // ここまで開始準備（設定確認）
        this.isOnGame = true  // ゲームを進行状態に
        // ゲームのスタート関係処理はこれよりも下に書く
        removeNotOnline()  // 開始時点でオンラインでない参加者をparticipantsから除外
        setGamerule()
        reset()
        GUI.setOnSaleFromDefault()
        hideNT()
        timeOnGame = time
        gamemodes[gamemode]?.start()
        MU.titleAll("${ChatColor.YELLOW}START                ", "${ChatColor.GRAY}-∴-∵-∴-∵-∴-∵-∴-∵-∴-∵-∴-∵-∴-∵-∴-∵-∴-∵-∴-∵-∴–∴-∵-∴-∵-∴-∴-∵-∴-∵-∴-∵-∴-∵-∴-∵-∴-∵-∴-∵-∴-∵-∴-∵-∴-∵-∴–∴-∵-∴-∵-∴")
//        MU.titleAll(title = "${ChatColor.GRAY}+▲+▼+▲+ ${ChatColor.AQUA}START ${ChatColor.GRAY}+▲+▼+▲+")
        MU.textToAll(MU.infoText(content = "${ChatColor.RED}${gamemode}${ChatColor.YELLOW}スタート"))
        startTimer()  // タイマースタート
    }

    /** ゲームが開始可能かの判定 */
    private fun isReady(): Boolean {
        return if (gamemode.equals("未指定")) {
            MU.textToAll("${ChatColor.RED}ゲームモードが選択されていません")
            false
        } else {
            true
        }
    }

    /**  特定のプレイヤーからすべてのエフェクトを除去 */
    fun removeEffect(uuid: UUID) {
        val p = Bukkit.getPlayer(uuid) ?: return
        p.activePotionEffects.forEach { pe -> p.removePotionEffect(pe.type) }  // ポーション効果の削除
    }

    /** ゲーム開始時のリセット。(リセットはゲームの開始時にのみ行なう) */
    fun reset() {
        gamemodes[gamemode]?.reset()
    }

    /** 参加者：体力を減らす(数はハートで指定) */
    fun reduceHeart(uuid: UUID, n: Int) {
        val p = Bukkit.getPlayer(uuid) ?: return
        val now = p.health
        p.health = now - n*2
    }

    /**  ネームタグを隠す */
    fun hideNT() {
        hideNT.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER)
    }

    /** ネームタグを見せる */
    fun showNT() {
        hideNT.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS)
    }

    /** 死んだときの処理 */
    fun death(uuid: UUID, announce: Boolean = false) {
        val p = Bukkit.getPlayer(uuid) ?: return
        p.gameMode = GameMode.SPECTATOR
        if (announce) {
            MU.textToAll(MU.infoText("${ChatColor.DARK_RED}死亡", "${ChatColor.GRAY}${p.name}"))
        }
    }

    /** プレイヤーの状態リセット（ゲームの開始時） */
    fun resetPlayer(uuid: UUID, health: Double, foodLevel: Int) {
        val p = Bukkit.getPlayer(uuid) ?: return
        p.inventory.clear()  // インベントリクリア
        p.activePotionEffects.forEach { pe -> p.removePotionEffect(pe.type) }  // ポーション効果の削除
        p.level = 0
        p.maxHealth = health; p.health = health  // 一旦体力を変更してから満タンにする
        p.foodLevel = foodLevel
        p.gameMode = GameMode.ADVENTURE
        p.enderChest.clear()
        gamemodes[gamemode]?.giveFirstItem(uuid)
    }

    /** ゲーム終了時の処理 */
    fun stop() {
        this.isOnGame = false
        MU.titleAll("${ChatColor.AQUA}FINISH               ", "${ChatColor.GRAY}-∴-∵-∴-∵-∴-∵-∴-∵-∴-∵-∴-∵-∴-∵-∴-∵-∴-∵-∴-∵-∴–∴-∵-∴-∵-∴-∴-∵-∴-∵-∴-∵-∴-∵-∴-∵-∴-∵-∴-∵-∴-∵-∴-∵-∴-∵-∴–∴-∵-∴-∵-∴")
        MU.textToAll(MU.infoText("${ChatColor.YELLOW}結果", gamemodes[gamemode]?.result() ?: ""))
        MU.textToAll(MU.infoText(content = "15秒後に全員をtpします"))
        MU.opText("この間のゲーム操作は控えてください")
        // 15秒後に設定をリセット
        object : BukkitRunnable() { override fun run() {
            setGamerule()
            showNT()
            participants.clear()
            Bukkit.getOnlinePlayers().forEach {
                fixedTp(it.uniqueId)
                it.gameMode = if (it.isOp) { GameMode.CREATIVE } else { GameMode.ADVENTURE }
            }
        } }.runTaskLater(plugin, 15*20L)
    }

    /**  */
    fun isFinished(): Boolean {
        return gamemodes[gamemode]?.isFinished() == true
    }

    /**  */
    fun changeTime(): String {
        val changed = MU.getNext(time, List(4) { (it+1)*300 }) as Int
        time = changed
        return secToMinSec(changed)
    }

    /**  */
    fun respawn(uuid: UUID) {
        val p = Bukkit.getPlayer(uuid) ?: return
        randomTp(uuid)
        p.gameMode = GameMode.ADVENTURE
    }

    /** pathから位置を取得 */
    fun getLoc(path: String): Location? {
        val c = ConfigManager("spawnPoints", plugin)
        return c.getLocation(path)
    }

    /** オンラインの人のUUIDを返す */
    fun getOnlineUuids(): List<UUID> {
        return Bukkit.getOnlinePlayers().map { it.uniqueId }
    }

    /** 参加者でかつオンラインの人のUUIDを返す */
    fun getOnlinePart(): Set<UUID> {
        return participants.filterKeys { getOnlineUuids().contains(it) }.keys
    }

    /** オンラインでないプレイヤーをparticipantsから除外 **/
    private fun removeNotOnline() {
        participants = participants.filter { getOnlineUuids().contains(it.key) } as MutableMap<UUID, ParticipantsManager>
    }

    /** 現在のブロック位置を取得して文字列化 */
    fun getLocBlock(loc: Location): String {
        val x = loc.blockX; val y = loc.blockY; val z = loc.blockZ
        return "$x / $y / $z"
    }

    fun cancelTask(uuid: UUID) {
        participants[uuid]?.task?.let { Bukkit.getScheduler().cancelTask(it) }
        participants[uuid]?.task = 0
    }

    fun setSpawn(uuid: UUID) {
        val p = Bukkit.getPlayer(uuid)!!
        val w = p.world
        val loc = p.location
        w.spawnLocation = loc
        p.sendMessage(MU.infoText("デフォルトスポーン", "${loc.blockX} / ${loc.blockY} / ${loc.blockZ}"))
    }

    /** ランダム選択で位置を返す。ループを超えたらワールドスポーン。*/
    fun getRandomLoc(uuid: UUID): Location {
        val p = Bukkit.getPlayer(uuid)!!
        val c = ConfigManager("settings", plugin)
        val highest = c.getConfig()!!.getInt("highest", Int.MAX_VALUE)
        val w = p.world
        val wb = w.worldBorder
        val cx = wb.center.blockX; val cz = wb.center.blockZ
        val half = (wb.size/2).toInt()
        var loc = w.spawnLocation
        for (i in 0..5) {  // 5(6)回までならランダムループする
            val x = (cx - half + 5..cx + half - 5).random()  // 直径というか1辺というか
            val z = (cz - half + 5..cz + half - 5).random()
            val y = w.getHighestBlockYAt(x, z) + 1
            if (y < highest) {  // 高さが指定よりも低ければ復活
                loc = Location(w, x.toDouble(), y.toDouble(), z.toDouble())
                break
            }
            if (i == 5) {
                loc = p.world.spawnLocation
            }
        }
        return loc
    }

    /** uuid指定でランダムtp */
    fun randomTp(uuid: UUID) {
        val p = Bukkit.getPlayer(uuid) ?: return
        p.teleport(getRandomLoc(uuid))
    }

    fun fixedTp(uuid: UUID) {
        val p = Bukkit.getPlayer(uuid) ?: return
        p.teleport(p.world.spawnLocation)
    }

    /** タイマーをスタート */
    fun startTimer() {
        object : BukkitRunnable() { override fun run() {
            if (timeOnGame == 0 || !isOnGame || isFinished()) { stop(); cancel(); return }
            gamemodes[gamemode]?.onTimer()
            effect()  // ゲーム中常時実行エフェクト管理
            timeOnGame--
        } }.runTaskTimer(plugin, 0L, 20L)
    }

    /** 秒数表示を分秒にして返す */
    fun secToMinSec(time: Int): String {
        val min = (time/60).toString()
        var sec = (time%60).toString()
        sec = if (sec.length.equals(1)) {"0$sec"} else {sec}
        return "${min}:${sec}"
    }

    /** ゲームモードを変更する際の処理 */
    @JvmName("setGamemode1")
    fun setGamemode(gamemode: String) {
        this.gamemode = gamemode
        MU.textToAll(MU.infoText(title = "ゲームモード", content = gamemode))
        GUI.update("ショップ")
    }

    /** ゲームルール設定 */
    fun setGamerule() {
        // ゲームルール設定
        for (w in Bukkit.getWorlds()) {
            if (isOnGame) {  // 特定の条件により変わる設定
                gamemodes[gamemode]?.setGamerule()
            } else {
                w.setGameRule(GameRule.NATURAL_REGENERATION, true) // 体力自然回復オン
                w.setGameRule(GameRule.SHOW_DEATH_MESSAGES, true) //デスメッセージ表示ON
                w.difficulty = Difficulty.PEACEFUL //難易度をピースフルにする
            }
            //  ここから下は条件にかかわらず固定の設定
            w.setGameRule(GameRule.KEEP_INVENTORY, true) //キープインベントリON
            w.setGameRule(GameRule.DO_INSOMNIA, false) //ファントム沸きOFF
            w.setGameRule(GameRule.DISABLE_RAIDS, true) //襲撃OFF
            w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false) //実績表示OFF
            w.setGameRule(GameRule.COMMAND_BLOCK_OUTPUT, false) //コマブロ出力OFF
            w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false) //時間進行OFF
            w.setGameRule(GameRule.DO_FIRE_TICK, false) //炎の延焼OFF
            w.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, false) //即時リスポーンOFF
            w.setGameRule(GameRule.DO_MOB_SPAWNING, false) //モブスポーンOFF
            w.setGameRule(GameRule.DO_PATROL_SPAWNING, false) //襲撃者スポーンOFF
            w.setGameRule(GameRule.DO_TRADER_SPAWNING, false) //行商人スポーンOFF
            w.setGameRule(GameRule.DO_WEATHER_CYCLE, false) //天気サイクルOFF
            w.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, true) //コマンドフィードバックON
        }

    }

    /** まだparticipantsにいなければ追加する。既にいれば追加しない */
    fun add(uuid: UUID) {
        participants.putIfAbsent(uuid, ParticipantsManager())
    }

    /** 金塊を増やす */
    fun addMoney(uuid: UUID, added: Int) {
        val p = Bukkit.getPlayer(uuid) ?: return
        val item = getItem("金塊")!!
        item.amount = added
        p.inventory.addItem(item)
    }

    /** 金塊を減らす */
    fun removeMoney(uuid: UUID, removed: Int) {
        val p = Bukkit.getPlayer(uuid) ?: return
        removeItem(p.inventory, "金塊", removed)
    }

    fun respawnSkill(uuid: UUID) {
        val p = Bukkit.getPlayer(uuid) ?: return
        val loc = p.location
        if (p.inventory.itemInMainHand.itemMeta?.displayName?.equals("**********") == true
            && participants[uuid]?.task == 0) {
            participants[uuid]?.task = object : BukkitRunnable() {
                override fun run() {
                    if (!p.isSneaking
                        || participants[uuid]?.sneak!! >= sneak
                        ||  p.inventory.itemInMainHand.itemMeta?.displayName?.equals("**********") != true) {
                        Matoonigokko16.GM.cancelTask(uuid)
                        participants[uuid]?.sneak = 0
//                        p.sendTitle("", "")
                        return
                    }
                    participants[uuid]?.sneak = participants[uuid]?.sneak!! + 1
                    val sneaking = participants[uuid]?.sneak!!
                    p.sendTitle("", "${ChatColor.YELLOW}|".repeat(sneaking)+"${ChatColor.GRAY}|".repeat(sneak - sneaking), 0, 21, 0)
                    if (sneaking>= sneak) {
                        p.teleport(p.world.spawnLocation)
                    }
                    loc.world.playSound(loc, Sound.ENTITY_WITHER_SPAWN, 0.7.toFloat(), 1.0.toFloat())
                }
            }.runTaskTimer(plugin, 0L, 20L).taskId
        }
    }

}