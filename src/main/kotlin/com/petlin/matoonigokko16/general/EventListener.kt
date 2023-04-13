package com.petlin.matoonigokko16.general

import com.petlin.matoonigokko16.general.GameManager.Companion.participants
import com.petlin.matoonigokko16.Matoonigokko16.Companion.BBM
import com.petlin.matoonigokko16.Matoonigokko16.Companion.GM
import com.petlin.matoonigokko16.Matoonigokko16.Companion.GUI
import com.petlin.matoonigokko16.Matoonigokko16.Companion.hideNT
import com.petlin.matoonigokko16.Matoonigokko16.Companion.MU
import com.petlin.matoonigokko16.Matoonigokko16.Companion.OM
import com.petlin.matoonigokko16.Matoonigokko16.Companion.plugin
import org.bukkit.*
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable


class EventListener : Listener {

    @EventHandler
    fun onJoin(e: PlayerJoinEvent) {
        val p = e.player
        e.joinMessage = "${ChatColor.AQUA}${p.name} **********"
        BBM.bossbar.addPlayer(p)
        hideNT.addEntry(p.name)
        GUI.update("指名鬼")  // 鬼選択リストのアップデート
//        p.setPlayerListHeaderFooter("\n${ChatColor.AQUA}まと企画${ChatColor.WHITE}へようこそ！", "${ChatColor.YELLOW}wiki${ChatColor.GRAY}: https://mato-games.game-info.wiki/")
    }

    @EventHandler
    fun onLogin(e: AsyncPlayerPreLoginEvent) {
        val uuid = e.uniqueId
        if (GM.isOnGame && !participants.containsKey(uuid)) {  // ゲーム中 && ゲーム不参加のときにはキックされる
            e.kickMessage = "**********"
            e.result = PlayerPreLoginEvent.Result.KICK_OTHER
        }
    }

    @EventHandler
    fun onRightClick(e: PlayerInteractEvent) {  // GUIを開くアイテムだったら開く  // クリックブロック検知
        val p = e.player
        val having = e.item
        if (GM.isOnGame && isRCBlock(e, Material.ENDER_CHEST)) {
            GUI.open(p, "ショップ")
        }
        if (isGuiOpenerRC(e)) {
            GUI.open(p, having!!.itemMeta.displayName)
        }
    }

    @EventHandler
    fun onQuit(e: PlayerQuitEvent) {
        val p = e.player
        e.quitMessage = "${ChatColor.DARK_GRAY}${p.name} **********"
    }

    @EventHandler
    fun onPlayerRespawn(e: PlayerRespawnEvent) {
        val p = e.player
        val uuid = p.uniqueId
        if (GM.isOnGame) {
            GM.getRandomLoc(uuid)
        } else {
            e.respawnLocation = p.world.spawnLocation
        }
    }

    @EventHandler
    fun onChat(e: PlayerChatEvent) {
        val p = e.player
        val message = e.message
        if (p.isOp) {  // OP所持者の場合は接頭辞がつく
            e.isCancelled = true
            Bukkit.broadcastMessage("${ChatColor.AQUA}[管理]${ChatColor.WHITE}<${p.name}> $message")
        }
    }

    @EventHandler
    fun onDrag(e: InventoryDragEvent) {
        val inv = e.inventory
        if (GUI.isGui(inv)) {
            e.isCancelled = true
        }
    }

    @EventHandler
    fun onSneak(e: PlayerToggleSneakEvent) {
        val p = e.player
        val uuid = p.uniqueId
        if (p.gameMode.equals(GameMode.SPECTATOR)) {  // ｽﾍﾟｸﾃｲﾀのスニークは検知しない
            return
        }
        if (e.isSneaking) {
            if (GM.isOnGame) {
                GM.respawnSkill(uuid)
                if (GM.gamemode.equals("鬼ごっこ")) {
                    OM.rescueSkill(uuid)
                    OM.createSwordSkill(uuid)
                }
            }
        }
    }

    @EventHandler(ignoreCancelled=true)
    fun onInvClick(e: InventoryClickEvent) {
        val p = e.whoClicked as Player
        val uuid = p.uniqueId
        val inv = e.inventory
        val cinv = e.clickedInventory ?: return
        val item = e.currentItem ?: return
        val name = item.itemMeta?.displayName ?: ""
        if (GUI.isGui(cinv) && GUI.isShopItem(item.itemMeta.displayName)) {  // クリックされたのがGUI(上の部分のみ)だった場合  // ショップアイテムなら
            if (GM.isOnGame) {  // ゲーム中
                if (GUI.getShopIM(item.itemMeta.displayName).isCanBuy(uuid)) {
                    GM.removeMoney(uuid, GUI.getShopIM(item.itemMeta.displayName).price)
                    p.inventory.addItem(item)
                }
            } else {  // ゲーム外
                if (e.action.equals(InventoryAction.PICKUP_ALL)) {  // 左クリックだったら
                    GUI.changePrice(item.itemMeta.displayName)
                } else if (e.action.equals(InventoryAction.PICKUP_HALF)) {  // 右クリックだったら
                    GUI.getShopIM(item.itemMeta.displayName).switchOnSale()
                }
            }
        }
        if (GUI.isGui(inv)) {  // 開かれているのがGUI(下のプレイヤーインベントリも含む)だった場合
            e.isCancelled = true
            if (isGuiOpener(item)) {  // GUIを開くアイテムだったら開く
                GUI.open(p, item.itemMeta.displayName)
            }
            if (iconClicked(e, "未指定")) {
                GM.setGamemode("未指定")
            }
            if (iconClicked(e, "鬼ごっこ")) {
                GM.setGamemode("鬼ごっこ")
            }
//            if (iconClicked(e, "牢屋")) {
//                p.sendMessage(GM.setLoc("牢屋", p.location))
//            }
            if (iconClicked(e, "時間")) {
                GUI.changeSetting(inv, name, GM.changeTime())
            }
            if (iconClicked(e, "必要ヒット数")) {
                GUI.changeSetting(inv, name, OM.changeHit())
            }
            if (iconClicked(e, "救助インターバル")) {
                GUI.changeSetting(inv, name, OM.changeRescueInterval())
            }
            if (iconClicked(e, "鬼の体力")) {
                GUI.changeSetting(inv, name, OM.changeOniLife())
            }
            if (iconClicked(e, "カラーボール")) {
                GUI.changeSetting(inv, name, OM.changeBallInterval())
            }
            if (iconClicked(e, "最大高度")) {
                GM.setHighest(uuid)
            }
            if (iconClicked(e, "デフォルトスポーン")) {
                GM.setSpawn(uuid)
            }
            if (iconClicked(e, "ランダム鬼(1人)")) {
                OM.rdmOni()
            }
            if (iconClicked(e, "鬼リセット")) {
                OM.oniReset()
            }
            if (iconClicked(e, "スタート")) {
                GM.start()
            }
            if (iconClickedEndsWith(e, "を鬼にする")) {
                val t = Bukkit.getPlayer(item.itemMeta?.displayName?.removeSuffix("を鬼にする")!!)?.uniqueId ?: return
                OM.assign(t, "鬼", true)
                GUI.update("指名鬼")
            }
        }
    }

    @EventHandler
    fun onPickArrow(e: PlayerPickupArrowEvent) { e.isCancelled = true }

    @EventHandler
    fun onDrop(e: PlayerDropItemEvent) { e.isCancelled = true }

    @EventHandler
    fun onDamage(e: EntityDamageEvent) {
        if (e.entity.type.equals(EntityType.PLAYER)) {  // eがPlayerの場合のみ
            if (GM.isOnGame) {
                if (GM.gamemode.equals("鬼ごっこ")) {
                    e.damage = 0.0
                }
            }
        }
    }

    @EventHandler
    fun onThrow(e: ProjectileLaunchEvent) {
        if (GM.isOnGame) {
            if (e.entity is Snowball) {
                if (GM.gamemode.equals("鬼ごっこ")) {
                    val sb = e.entity as Snowball
                    val p = sb.shooter as Player
                    object : BukkitRunnable() { override fun run() {
                        p.inventory.addItem(GM.getItem("カラーボール")!!)
                    } }.runTaskLater(plugin, OM.ballInterval*20L)
                }
            }
        }
    }

    @EventHandler
    fun onDeath(e: PlayerDeathEvent) {
        val p = e.entity.player as Player
        val uuid = p.uniqueId
        if (GM.isOnGame && !p.gameMode.equals(GameMode.SPECTATOR)) {
            if (GM.gamemode.equals("鬼ごっこ")) {
                OM.deathPenalty(uuid)
                GM.death(uuid)
            }
        }
    }

    @EventHandler
    fun onDamageByEntity(e: EntityDamageByEntityEvent) {
        if (GM.isOnGame) {
            if (GM.gamemode.equals("鬼ごっこ")) {
                if (e.damager is Player && e.entity is Player) {
                    val p = e.damager as Player
                    val puuid = p.uniqueId
                    val t = e.entity as Player
                    val tuuid = t.uniqueId
                    if (OM.getRole(puuid).equals("鬼")
                        && OM.getRole(tuuid).equals("逃走者")) {  // 鬼から逃走者への攻撃
                        if (t.inventory.chestplate?.itemMeta?.displayName.equals("**********")) {
                            listOf(p, t).forEach { it.sendMessage(MU.infoText("${ChatColor.YELLOW}防御", "${t.name}の「${t.inventory.chestplate?.itemMeta?.displayName}」が壊れた")) }
                            t.inventory.chestplate?.amount = 0  // チェストプレートを削除
                            return
                        }
                        GM.reduceHeart(tuuid, 1)
                        MU.textToAll(MU.infoText(title = "${ChatColor.YELLOW}確保", content = MU.whoToWho("${ChatColor.RED}${p.name}", t.name)))
                        GM.addMoney(puuid, (OM.minMoneyOni..OM.maxMoneyOni).random())
                    } else if (OM.getRole(puuid).equals("逃走者") && OM.getRole(tuuid).equals("鬼")) {  // 逃走者から鬼への攻撃
                        if (p.inventory.itemInMainHand.itemMeta?.displayName.equals("**********")) {
                            GM.reduceHeart(tuuid, 1)
                            val after = (t.health/2).toInt()
                            if (after > 0) { MU.textToAll(MU.infoText(title = "${ChatColor.YELLOW}攻撃", content = MU.whoToWho(p.name, "${ChatColor.RED}${t.name} ${ChatColor.GRAY}(残り体力:${after})"))) }
                            if (after <= 0) { MU.textToAll(MU.infoText(title = "${ChatColor.RED}討伐", content = MU.whoToWho(p.name, "${ChatColor.RED}${t.name}"))) }
                            return
                        }
                        e.isCancelled = true
                    }
                }
                if (e.damager is Snowball && e.entity is Player) {
                    val sb = e.damager as Snowball
                    val p = sb.shooter as Player
                    val puuid = p.uniqueId
                    val t = e.entity as Player
                    val tuuid = t.uniqueId
                    if (OM.getRole(tuuid).equals("鬼")) {
                        MU.textToAll(MU.infoText("${ChatColor.YELLOW}カラーボール", MU.whoToWho(p.name, "${ChatColor.RED}${t.name}")))
                        t.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, OM.oniGlowingTime * 20, 0, false, false, false))
                        GM.addMoney(puuid, (OM.minMoneyRun..OM.maxMoneyRun).random())
                        OM.hitOnGame++
                        if (OM.hitOnGame == OM.hit) {  // ヒット数が必要数に到達したらアイテム開放＠鬼ごっこ
                            if (!GUI.getShopIM("**********").isOnSale || !GUI.getShopIM("**********").isOnSale) {
                                MU.textToAll(MU.infoText(content = "ショップに剣の素材が追加された！"))
                                GUI.getShopIM("**********").setOnSale(true)
                                GUI.getShopIM("**********").setOnSale(true)
                            }
                        }
                    }
                }
            }
        }
    }

    // 鬼：透明化してるあいだだけ装備消したいね @鬼ごっこ
    @EventHandler
    fun onPotionEffectChange(e: EntityPotionEffectEvent) {
        val p: Player = e.entity as Player
        val old = e.oldEffect
        val new = e.newEffect
    }

    fun isRC(e: PlayerInteractEvent, having: String): Boolean {
        val res = ((e.action.equals(org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) || e.action.equals(org.bukkit.event.block.Action.RIGHT_CLICK_AIR))
                && e.hand!!.equals(EquipmentSlot.HAND) && e.item?.itemMeta?.displayName.equals(having))
        e.isCancelled = res
        return res
    }

    private fun isRCBlock(e: PlayerInteractEvent, material: Material): Boolean {
        val res = (e.action.equals(org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)
                && e.hand!!.equals(EquipmentSlot.HAND)
                && e.clickedBlock?.type?.equals(material) == true)
        e.isCancelled = res
        return res
    }

    private fun isGuiOpenerRC(e: PlayerInteractEvent): Boolean {
        var res = false
        if ((e.action.equals(org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) || e.action.equals(org.bukkit.event.block.Action.RIGHT_CLICK_AIR))
            && e.hand!!.equals(EquipmentSlot.HAND) && isGuiOpener(e.item)) {
            res = true
            e.isCancelled = true
        }
        return res
    }

    private fun iconClicked(e: InventoryClickEvent, icon: String): Boolean {
        var res = false
        if (e.currentItem?.itemMeta?.displayName.equals(icon)) {
            res = true
            e.isCancelled = true
        }
        return res
    }
    private fun isGuiOpener(item: ItemStack?): Boolean {
        return GUI.guis.containsKey(item?.itemMeta?.displayName)
    }
    private fun iconClickedStartsWith(e: InventoryClickEvent, start: String): Boolean {
        var res = false
        if (e.currentItem?.itemMeta?.displayName?.startsWith(start) == true) {
            res = true
            e.isCancelled = true
        }
        return res
    }
    private fun iconClickedEndsWith(e: InventoryClickEvent, end: String): Boolean {
        var res = false
        if (e.currentItem?.itemMeta?.displayName?.endsWith(end) == true) {
            res = true
            e.isCancelled = true
        }
        return res
    }


}