package com.petlin.matoonigokko16

import com.petlin.matoonigokko16.Matoonigokko16.Companion.GM
import com.petlin.matoonigokko16.Matoonigokko16.Companion.GUI
import com.petlin.matoonigokko16.Matoonigokko16.Companion.MU
import com.petlin.matoonigokko16.Matoonigokko16.Companion.OM
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID

class ItemManager(material: Material) {

    var name = ""
    var price = 0
    var isOnSale = true
    var isDefaultOnSale = true
    var forRole = listOf<String>()

    var itemStack = ItemStack(material)
    var pMeta = this.itemStack.itemMeta

    var original = material

    fun setAmount(amount: Int): ItemManager {
        this.itemStack.amount = amount
        return this
    }

    fun setName(name: String): ItemManager {
        this.name = name
        val m = this.itemStack.itemMeta
        m.setDisplayName(name)
        this.itemStack.itemMeta = m
        return this
    }

    fun setLore(lore: List<String>): ItemManager {
        var m = this.itemStack.itemMeta
        if (this.itemStack.type .equals(Material.POTION)) {
            m = this.itemStack.itemMeta as PotionMeta
            this.pMeta = m
        }
        m.lore = lore
        this.itemStack.itemMeta = m
        return this
    }

    fun setPrice(price: Int): ItemManager {
        this.price = price
        var lore = this.itemStack.itemMeta.lore
        lore = lore?.filter { !it.startsWith("${ChatColor.YELLOW}価格") } as MutableList<String>
        lore = lore.plus("${ChatColor.YELLOW}価格: ${ChatColor.GRAY}金塊${price}")
        this.setLore(lore)
        return this
    }

    fun setTimes(times: Int): ItemManager {
        var lore = this.itemStack.itemMeta.lore
        lore = lore?.filter { !it.startsWith("${ChatColor.YELLOW}使用可能回数") } as MutableList<String>
        lore = lore.plus("${ChatColor.YELLOW}使用可能回数: ${ChatColor.GRAY}${times}回")
        this.setLore(lore)
        this.itemStack.durability = (this.itemStack.type.maxDurability-times).toShort()
        return this
    }

    fun setUnbreakable(): ItemManager {
        val m = this.itemStack.itemMeta
        m.isUnbreakable = true
        this.itemStack.itemMeta = m
        return this
    }

    fun setRoles(roles: List<String>): ItemManager {
        this.forRole = roles
        var lore = this.itemStack.itemMeta.lore
        lore = lore?.filter { !it.startsWith("${ChatColor.YELLOW}購入可能") } as MutableList<String>
        lore = lore.plus("${ChatColor.YELLOW}購入可能: ${ChatColor.GRAY}${roles}")
        this.setLore(lore)
        return this
    }

    fun setUsage(usage: String): ItemManager {
        var lore = this.itemStack.itemMeta.lore
        lore = lore?.filter { !it.startsWith("${ChatColor.YELLOW}使用方法") } as MutableList<String>
        lore = lore.plus("${ChatColor.YELLOW}使用方法: ${ChatColor.GRAY}${usage}")
        this.setLore(lore)
        return this
    }

    fun addEnch(ench: Enchantment, lvl: Int): ItemManager  {
        val m = this.itemStack.itemMeta
        m.addEnchant(ench, lvl, true)
        this.itemStack.itemMeta = m
        return this
    }

    fun setPlayerHeadOwner(name: String): ItemManager {
        val m = this.itemStack.itemMeta as SkullMeta
        m.owner = name
        this.itemStack.itemMeta = m
        return this
    }

    fun setOnSale(onSale: Boolean): ItemManager {
        this.isOnSale = onSale
        return this
    }

    fun setDefaultOnSale(onSale: Boolean): ItemManager {
        this.isDefaultOnSale = onSale
        return this
    }

    fun setPotionEffect(effect: PotionEffectType, duration: Int, amplifier: Int): ItemManager {
        val m = this.itemStack.itemMeta as PotionMeta
        m.addCustomEffect(PotionEffect(effect, duration, amplifier, false, false, false), true)
        this.itemStack.itemMeta = m
        this.pMeta = m
        return this
    }

    fun setPotionColor(color: Color): ItemManager {
        val m = this.itemStack.itemMeta as PotionMeta
        m.color = (color)
        this.itemStack.itemMeta = m
        this.pMeta = m
        return this
    }

    fun finish(): ItemStack {
        return this.sortLore().itemStack
    }

    fun sortLore(): ItemManager {
        val lore = this.itemStack.itemMeta.lore ?: return this
        val new = lore.filter { !it.startsWith("${ChatColor.YELLOW}") } as MutableList
        val sort = lore.filter { it.startsWith("${ChatColor.YELLOW}") }.sorted()
        new.addAll(sort)
        this.setLore(new)
        return this
    }

    fun give(p: Player) {
        val m = itemStack.itemMeta as PotionMeta
    }

    fun isCanBuy(uuid: UUID): Boolean {
        val p = Bukkit.getPlayer(uuid) ?: return false
        if (this.itemStack.type.equals(Material.STRUCTURE_VOID)) {  // ストラクチャーボイドだったら購入不可
            p.sendMessage(MU.infoText("購入不可", "販売停止中"))
            return false
        }
        if (forRole.isNotEmpty() && !forRole.contains(OM.getRole(uuid))) {
            p.sendMessage(MU.infoText("購入不可", "役職専用アイテムにつき"))
            return false
        }
        if (this.price > GM.countItem(p.inventory, "金塊")) {
            p.sendMessage(MU.infoText("購入不可", "所持金不足"))
            return false
        }
        return true
    }

    fun switchOnSale() {
        if (GM.isOnGame) {
            this.itemStack.type = if (isOnSale) { Material.STRUCTURE_VOID } else { this.original }
            this.isOnSale = !this.isOnSale
        } else {
            this.itemStack.type = if (isDefaultOnSale) { Material.STRUCTURE_VOID } else { this.original }
            this.isDefaultOnSale = !this.isDefaultOnSale
        }
        GUI.update("ショップ")
    }
}