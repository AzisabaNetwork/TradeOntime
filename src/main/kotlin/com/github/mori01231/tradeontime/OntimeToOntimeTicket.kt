package com.github.mori01231.tradeontime

import org.black_ixx.playerpoints.PlayerPoints
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class OntimeToOntimeTicket(private val plugin: TradeOntime) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("&c&lこのコマンドはコンソールからは実行できません".colored())
            return true
        }

        val pointsPerTicket = plugin.config.getInt("PointsPerTicket")
        if (plugin.checkCoolTime(sender.uniqueId)) {
            sender.sendMessage("&cクールタイム中です。数秒程度時間を開けてもう一度試してください。".colored())
            return true
        }

        when (args.size) {
            1 -> {
                val points = args[0].toIntOrNull()
                if (points == null || points < 0) {
                    sender.sendMessage("&c&l変換するポイント数は正の整数で指定してください。".colored())
                    return false
                }

                val availableSlots = availableSlots(sender)
                plugin.scheduler.runAsync {
                    val hasPoints = PlayerPoints.getInstance().api.look(sender.uniqueId)
                    if (hasPoints < pointsPerTicket) {
                        sender.sendMessage("&c&lあなたの所持オンタイムポイントが${pointsPerTicket}未満のためオンタイムチケットへの変換が出来ません。".colored())
                        return@runAsync
                    }
                    if (points > hasPoints) {
                        sender.sendMessage("&c&lオンタイムポイントは${hasPoints}以下かつ${pointsPerTicket}以上の整数で指定してください。".colored())
                        return@runAsync
                    }

                    val takePoints = points - (points % pointsPerTicket)
                    if (takePoints == 0) {
                        sender.sendMessage("&c&lオンタイムポイントは${pointsPerTicket}以上の整数で指定してください。".colored())
                        return@runAsync
                    }

                    if (availableSlots < requiredSlots(takePoints / pointsPerTicket)) {
                        sender.sendMessage("&c&l現在インベントリには${availableSlots}スロットの空きがあります。".colored())
                        sender.sendMessage(NOT_ENOUGH_SLOTS_MESSAGE.colored())
                        return@runAsync
                    }

                    takePointsAndGiveTickets(sender, takePoints, pointsPerTicket)
                }
            }

            0 -> {
                val availableSlots = availableSlots(sender)
                plugin.scheduler.runAsync {
                    val hasPoints = PlayerPoints.getInstance().api.look(sender.uniqueId)
                    val takePoints = hasPoints - (hasPoints % pointsPerTicket)

                    if (takePoints == 0) {
                        sender.sendMessage("&c&lあなたの所持オンタイムポイントが${pointsPerTicket}未満のためオンタイムチケットへの変換が出来ません。".colored())
                        return@runAsync
                    }

                    if (availableSlots < requiredSlots(takePoints / pointsPerTicket)) {
                        sender.sendMessage("&c&l現在インベントリには${availableSlots}スロットの空きがあり、${availableSlots * 64 * pointsPerTicket}ポイントまでしか変換できません。".colored())
                        sender.sendMessage(NOT_ENOUGH_SLOTS_MESSAGE.colored())
                        return@runAsync
                    }

                    takePointsAndGiveTickets(sender, takePoints, pointsPerTicket)
                }
            }
        }
        return true
    }

    private fun takePointsAndGiveTickets(player: Player, takePoints: Int, pointsPerTicket: Int) {
        val playerName = player.name
        val giveItems = takePoints / pointsPerTicket
        val result = PlayerPoints.getInstance().api.take(player.uniqueId, takePoints)

        plugin.scheduler.runNextTick {
            if (!result) {
                player.sendMessage("${ChatColor.RED}オンタイムポイントの変換に失敗しました。")
                plugin.logger.severe("${playerName}のオンタイムポイント->チケットの変換に失敗しました。(${takePoints}ポイント)")
                return@runNextTick
            }
            val console = Bukkit.getConsoleSender()
            if (plugin.config.getBoolean("UseCommandForOntimeItem")) {
                Bukkit.dispatchCommand(console, ontimeTicketGiveCommand(playerName) + giveItems)
                plugin.logger.info("${playerName}にオンタイムチケットを $giveItems 個与えました。")
            } else {
                val mmItemName = plugin.config.getString("MythicMobsItemName")
                Bukkit.dispatchCommand(console, "mm i give $playerName $mmItemName $giveItems")
                plugin.logger.info("${playerName}にMMアイテム $mmItemName を $giveItems 個与えました。")
            }
            player.sendMessage("&b$takePoints オンタイムポイントをオンタイムチケット $giveItems 枚に変換しました。".colored())
        }
    }

    private fun ontimeTicketGiveCommand(playerName: String) =
        "minecraft:give $playerName paper{display:{Name:'{\"text\":\"オンタイムチケット\",\"color\":\"green\"}'," +
            "Lore:['{\"text\":\"ログインしていると一定時間ごとにもらえる。\",\"color\":\"white\"}'," +
            "'{\"text\":\"換金、特殊アイテムとの交換等に使える。\",\"color\":\"white\"}']}," +
            "HideFlags:1,Enchantments:[{id:\"minecraft:unbreaking\",lvl:1s}]} "

    private fun requiredSlots(items: Int) = (items + 63) / 64

    fun availableSlots(player: Player): Int {
        val inventory = player.inventory

        var slots = inventory.contents.count { it == null }

        if (inventory.itemInOffHand.type == Material.AIR) {
            slots--
        }

        slots -= inventory.armorContents.count { it == null }

        return slots
    }

    companion object {
        private const val NOT_ENOUGH_SLOTS_MESSAGE =
            "&c&lオンタイムチケットがインベントリに入りきりません。インベントリに空きを増やしたうえで再度コマンドを実行してください。"
    }
}
