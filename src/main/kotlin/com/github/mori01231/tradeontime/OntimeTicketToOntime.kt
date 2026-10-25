package com.github.mori01231.tradeontime

import org.black_ixx.playerpoints.PlayerPoints
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class OntimeTicketToOntime(private val plugin: TradeOntime) : CommandExecutor {
    private val pointsPerTicket: Int
        get() = plugin.config.getInt("PointsPerTicket")

    private val ticketDisplayName: String?
        get() = plugin.config.getString("MythicMobsDisplayName")

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("&c&lこのコマンドはコンソールからは実行できません".colored())
            return true
        }

        if (plugin.checkCoolTime(sender.uniqueId)) {
            sender.sendMessage("&cクールタイム中です。数秒程度時間を開けてもう一度試してください。".colored())
            return true
        }

        if (args.isEmpty()) {
            if (ontimeTickets(sender) > 0) {
                sender.sendMessage(convertOntimeTickets(sender).colored())
            } else {
                sender.sendMessage("&c&lインベントリに変換したいオンタイムチケットを入れてください。".colored())
            }
            return true
        }

        val convertTickets = args[0].toIntOrNull()
        if (convertTickets == null || convertTickets < 0) {
            sender.sendMessage("&c&l変換するオンタイムチケットの枚数は正の整数で指定してください。".colored())
            return true
        }

        val tickets = ontimeTickets(sender)
        when {
            tickets <= 0 ->
                sender.sendMessage("&c&lインベントリに変換したいオンタイムチケットを入れてください。".colored())
            tickets < convertTickets ->
                sender.sendMessage(notEnoughTicketsMessage(convertTickets).colored())
            else ->
                sender.sendMessage(convertNumberOfOntimeTickets(sender, convertTickets).colored())
        }
        return true
    }

    private fun isOntimeTicket(item: ItemStack?): Boolean =
        item != null && item.hasItemMeta() && item.itemMeta?.displayName == ticketDisplayName

    fun ontimeTickets(player: Player): Int =
        player.inventory.contents.filter(::isOntimeTicket).sumOf { it.amount }

    fun convertOntimeTickets(player: Player): String {
        var tickets = 0
        for (item in player.inventory.contents) {
            if (isOntimeTicket(item)) {
                tickets += item.amount
                item.amount = 0
            }
        }

        val points = tickets * pointsPerTicket
        givePoints(player, points)

        val message = "&bオンタイムチケット${tickets}枚をオンタイムポイント${points}ポイントに変換しました。"
        Bukkit.getConsoleSender().sendMessage("${player.name}: ${message.colored()}")
        return message
    }

    fun convertNumberOfOntimeTickets(player: Player, convertTickets: Int): String {
        if (ontimeTickets(player) < convertTickets) {
            return notEnoughTicketsMessage(convertTickets)
        }

        var unconvertedTickets = convertTickets
        for (item in player.inventory.contents) {
            if (unconvertedTickets <= 0) break
            if (!isOntimeTicket(item)) continue
            if (item.amount <= unconvertedTickets) {
                unconvertedTickets -= item.amount
                item.amount = 0
            }
            else {
                item.amount -= unconvertedTickets
                unconvertedTickets = 0
            }
        }

        val points = convertTickets * pointsPerTicket
        givePoints(player, points)

        return "&bオンタイムチケット${convertTickets}枚をオンタイムポイント${points}ポイントに変換しました。"
    }

    private fun notEnoughTicketsMessage(convertTickets: Int) =
        "&c&lオンタイムチケットが足りません。${convertTickets}枚以上のオンタイムチケットをインベントリに入れてください。"

    //Give player ontime points.
    private fun givePoints(player: Player, points: Int) {
        plugin.scheduler.runAsync {
            val result = PlayerPoints.getInstance().api.give(player.uniqueId, points)
            if (!result) {
                player.sendMessage("${ChatColor.RED}オンタイムポイント${points}ポイントの付与に失敗しました。")
                plugin.logger.severe("${player.name}のチケット->オンタイムポイントの変換に失敗しました。(${points}ポイント)")
            }
        }
    }
}
