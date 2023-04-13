package com.petlin.matoonigokko16

import com.petlin.matoonigokko16.Matoonigokko16.Companion.GM
import com.petlin.matoonigokko16.Matoonigokko16.Companion.MU
import com.petlin.matoonigokko16.Matoonigokko16.Companion.gamemodes
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

class ActionbarManager {

    fun update(uuid: UUID) {
        val p = Bukkit.getPlayer(uuid) ?: return
        val content = if (GM.gamemode.equals("未指定")) {
            ""
        } else {
            gamemodes[GM.gamemode]?.actionBar(uuid)
        }
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent(content))
    }

}
