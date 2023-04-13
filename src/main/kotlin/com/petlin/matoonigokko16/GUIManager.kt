package com.petlin.matoonigokko16

import com.petlin.matoonigokko16.Matoonigokko16.Companion.GM
import com.petlin.matoonigokko16.Matoonigokko16.Companion.MU
import com.petlin.matoonigokko16.Matoonigokko16.Companion.OM
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.potion.PotionEffectType

class GUIManager {

    private val prices = List(10) { (it+1)*2 }

    val guis = mutableMapOf<String, Pair<Inventory, List<ItemManager>>>(
        addGui("メインメニュー", 1, listOf(
            ItemManager(Material.REDSTONE_TORCH).setName("スタート").setLore(listOf("ゲームを開始します", "ゲーム設定の後にクリックしてください")),
        )),
        addGui("ゲームモード選択", 1, listOf(
            ItemManager(Material.WHITE_CONCRETE).setName("未指定").setLore(listOf("ゲームモードを「未指定」に変更します")),
            ItemManager(Material.RED_CONCRETE).setName("鬼ごっこ").setLore(listOf("ゲームモードを「鬼ごっこ」に変更します"))
        )),
        addGui("ゲーム別設定", 1, listOf(
            ItemManager(Material.WHITE_BANNER).setName("共通メニュー").setLore(listOf("全ゲーム共通の設定を行ないます")),
            ItemManager(Material.RED_BANNER).setName("鬼ごっこメニュー").setLore(listOf("「鬼ごっこ」の設定を行ないます"))
        )),
        addGui("共通メニュー", 1, listOf(
            ItemManager(Material.GRASS_BLOCK).setName("デフォルトスポーン"),
            ItemManager(Material.ELYTRA).setName("最大高度").setLore(listOf("ランダムスポーンの最大高度")),
            ItemManager(Material.ENDER_CHEST).setName("ショップ").setLore(listOf("ショップ画面に移動します")),
            ItemManager(Material.CLOCK).setName("時間").setLore(listOf("現在の設定:${GM.secToMinSec(GM.time)}"))
        )),
        addGui("鬼ごっこメニュー", 1, listOf(
            ItemManager(Material.IRON_AXE).setName("ランダム鬼(1人)"),
            ItemManager(Material.DIAMOND_AXE).setName("指名鬼"),
            ItemManager(Material.BARRIER).setName("鬼リセット"),
            ItemManager(Material.DIAMOND_SWORD).setName("必要ヒット数").setLore(listOf("現在の設定:${OM.hit}")),
            ItemManager(Material.WOODEN_HOE).setName("救助インターバル").setLore(listOf("現在の設定:${OM.rescueInterval}秒")),
            ItemManager(Material.REDSTONE).setName("鬼の体力").setLore(listOf("現在の設定:${OM.oniLife}")),
            ItemManager(Material.SNOWBALL).setName("カラーボール").setLore(listOf("現在の設定:${OM.ballInterval}秒"))
        )),
        addGui("ショップ", 1, listOf(  // ショップになる箱。中身は各ゲームで変わる

        )),
        addGui("指名鬼", 3, listOf())
    )
    
    val shops = mapOf<String, MutableList<ItemManager>>(
        "未指定" to mutableListOf(),
        "鬼ごっこ" to mutableListOf(
            ItemManager(Material.POTION)
                .setName("**********")
                .setLore(listOf("**********"))
                .setPrice(4)
                .setPotionEffect(PotionEffectType.INVISIBILITY, 15*20, 2)
                .setPotionColor(Color.GRAY),
            ItemManager(Material.POTION)
                .setName("**********")
                .setLore(listOf("**********"))
                .setPrice(4)
                .setPotionEffect(PotionEffectType.SPEED, 15*20, 2)
                .setPotionColor(Color.OLIVE),
            ItemManager(Material.POTION)
                .setName("**********")
                .setLore(listOf("**********"))
                .setPrice(4)
                .setPotionEffect(PotionEffectType.JUMP, 15*20, 2)
                .setPotionColor(Color.RED),
            ItemManager(Material.STICK)
                .setName("**********")
                .setLore(listOf("**********", "**********"))
                .setPrice(10)
                .setUsage("クラフトして剣を製作")
                .setRoles(listOf("逃走者"))
                .addEnch(Enchantment.SILK_TOUCH, 5)
                .setDefaultOnSale(false),
            ItemManager(Material.DIAMOND)
                .setName("**********")
                .setLore(listOf("**********", "**********"))
                .setPrice(10)
                .setUsage("クラフトして剣を製作")
                .setRoles(listOf("逃走者"))
                .addEnch(Enchantment.SILK_TOUCH, 5)
                .setDefaultOnSale(false),
            ItemManager(Material.COOKED_BEEF)
                .setAmount(5)
                .setName("**********")
                .setLore(listOf("**********"))
                .setPrice(2)
                .setRoles(listOf("逃走者")),
            ItemManager(Material.IRON_CHESTPLATE)
                .setName("**********")
                .setLore(listOf("**********", "**********"))
                .setPrice(8)
                .setTimes(1)
                .setRoles(listOf("逃走者"))
                .setUsage("着る")
                .addEnch(Enchantment.PROTECTION_ENVIRONMENTAL, 5)
        )
    )

    fun addGui(title: String, rows: Int, items: List<ItemManager>, owner: InventoryHolder? = null): Pair<String, Pair<Inventory, List<ItemManager>>> {
        val inv = Bukkit.createInventory(owner, rows*9, title)
        return Pair(title, Pair(inv, items))
    }

    fun get(name: String): Inventory {
        return guis[name]!!.first
    }

    fun getItems(name: String): List<ItemManager> {
        return guis[name]!!.second
    }

    fun getItem(invname: String, itemname:String): ItemManager {
        return guis[invname]!!.second.find { it.itemStack.itemMeta.displayName.equals(itemname) }!!
    }

    fun update(name: String) {
        val inv = get(name)
        inv.clear()
        if (name.equals("指名鬼")) {
            OM.getNotOni().forEach {
                val p = Bukkit.getPlayer(it) ?: return
                inv.addItem(ItemManager(Material.PLAYER_HEAD).setName("${p.name}を鬼にする").setPlayerHeadOwner(p.name).finish())
            }
        } else if (name.equals("ショップ")) {
            if (GM.isOnGame) {  // ゲーム中だったらisOnSale(一時的な値)を参照
                shops[GM.gamemode]!!.forEach { if (it.isOnSale) { it.itemStack.type = it.original } else { it.itemStack.type = Material.STRUCTURE_VOID } }
            } else {
                shops[GM.gamemode]!!.forEach {
                    if (it.isDefaultOnSale) {
                        it.itemStack.type = it.original
                        if (it.original.equals(Material.POTION)) {
                            it.itemStack.itemMeta = it.pMeta
                            it.setPrice(it.price).sortLore()
                        }
                    } else {
                        it.itemStack.type = Material.STRUCTURE_VOID
                    }
                }
            }
            shops[GM.gamemode]!!.map { it.sortLore().itemStack }.forEach { item -> inv.addItem(item) }
        } else {
            getItems(name).map { it.sortLore().itemStack }.forEach { item -> inv.addItem(item) }
        }
    }

    fun open(p: Player, name: String) {
        p.closeInventory()
        update(name)
        p.openInventory(get(name))
    }

    fun isGui(inv: Inventory?): Boolean {
        return guis.keys.any { get(it).equals(inv) }
    }

    fun isShopItem(name: String): Boolean {
        return shops[GM.gamemode]!!.any { it.name.equals(name) }
    }

    fun getShopIM(name: String): ItemManager {
        return shops[GM.gamemode]!!.find { it.name.equals(name) }!!
    }

    // アイテムの購入可能状態を設定
    fun setOnSaleFromDefault() {
        shops[GM.gamemode]!!.forEach { it.isOnSale = it.isDefaultOnSale }
    }

    fun changePrice(name: String) {
        val i = shops[GM.gamemode]!!.indexOf(getShopIM(name))
        shops[GM.gamemode]!![i].setPrice(MU.getNext(shops[GM.gamemode]!![i].price, prices) as Int).finish()
        update("ショップ")
    }

    fun getInvContainItem(name: String): Map<String, Pair<Inventory, List<ItemManager>>> {
        return guis.filter { it.value.second.contains(getShopIM(name)) }
    }

    fun getItem(name: String): ItemManager? {
        return guis.flatMap { it.value.second }.find { it1 -> it1.name.equals(name) }
    }

    fun getLore(name: String): MutableList<String>? {
        return getItem(name)?.itemStack?.itemMeta?.lore
    }

    fun changeSetting(inv: Inventory, name: String, value: String) {
        val invName = getInvName(inv)
        val i = guis[invName]?.second?.indexOf(getItem(name))!!  // 変更対象のインデックスを取得
        guis[invName]?.second?.get(i)?.setLore(listOf("現在の設定:$value"))?.finish()
        update(invName)
    }

    /** インベントリの名前を取得 */
    fun getInvName(inv: Inventory): String {
        return guis.filterValues { it.first.equals(inv) }.keys.first()
    }
}