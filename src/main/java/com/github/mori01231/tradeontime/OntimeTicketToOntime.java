package com.github.mori01231.tradeontime;

import org.black_ixx.playerpoints.PlayerPoints;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import static org.bukkit.Bukkit.getLogger;
import static org.bukkit.Bukkit.getServer;

public class OntimeTicketToOntime implements CommandExecutor {
    int pointsPerTicket = TradeOntime.getInstance().getConfig().getInt("PointsPerTicket");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {


        //Executed by Player
        if (sender instanceof Player){
            Player player = (Player) sender;
            if (TradeOntime.getInstance().checkCoolTime(player.getUniqueId())) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cクールタイム中です。数秒程度時間を開けてもう一度試してください。"));
                return true;
            }


            //Convert all ontime tickets in inventory to ontime points.
            if(args.length == 0) {
                //Player has at least 1 ontime ticket in their inventory
                if(OntimeTickets(player) > 0){
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', ConvertOntimeTickets(player)));
                }
                //Player has no ontime tickets in their inventory
                else{
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lインベントリに変換したいオンタイムチケットを入れてください。"));
                }
            }
            //Convert ontime tickets in inventory to ontime points up until the given number.
            else{
                try{
                    if(Integer.parseInt(args[0]) < 0){
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&l変換するオンタイムチケットの枚数は正の整数で指定してください。"));
                        return true;
                    }
                }catch(Exception e){
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&l変換するオンタイムチケットの枚数は正の整数で指定してください。"));
                    return true;
                }


                //Player has at least 1 ontime ticket in their inventory
                if(OntimeTickets(player) > 0){
                    int ConvertTickets = Integer.parseInt(args[0]);
                    //Player doesn't have enough tickets
                    if (OntimeTickets(player) < ConvertTickets){
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lオンタイムチケットが足りません。" + ConvertTickets + "枚以上のオンタイムチケットをインベントリに入れてください。"));
                    }
                    //Player has enough tickets
                    else{
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', ConvertNumberOfOntimeTickets(player, ConvertTickets)));
                    }
                }
                //Player has no ontime tickets in their inventory
                else{
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lインベントリに変換したいオンタイムチケットを入れてください。"));
                }
            }

        }
        //Executed by Console
        else{
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lこのコマンドはコンソールからは実行できません"));
        }

        return true;
    }

    //get amount of available slots in player inventory excluding armor and offhand
    public int OntimeTickets(Player player){

        //get display name of ontime ticket
        String MMDisplayName = TradeOntime.getInstance().getConfig().getString("MythicMobsDisplayName");

        //get player inventory.
        Inventory inv = player.getInventory();

        //initializing counter for slots.
        int tickets=0;

        //counting the number of available slots.
        for (ItemStack item: inv.getContents()) {
            try{
                if(item.getItemMeta().getDisplayName().equals(MMDisplayName)){
                    tickets += item.getAmount();
                }

            }catch (Exception e){
            }
        }


        //return the number of available slots.
        return tickets;
    }

    public String ConvertOntimeTickets(Player player){

        //get display name of ontime ticket
        String MMDisplayName = TradeOntime.getInstance().getConfig().getString("MythicMobsDisplayName");

        //get player inventory.
        Inventory inv = player.getInventory();

        //initializing counter for tickets and points.
        int tickets = 0;
        int points = 0;

        //counting and deleting tickets.
        for (ItemStack item: inv.getContents()) {
            try{
                if(item.getItemMeta().getDisplayName().equals(MMDisplayName)){
                    tickets += item.getAmount();
                    item.setAmount(0);
                }

            }catch (Exception e){
            }
        }

        points = tickets * pointsPerTicket;

        //Give player ontime points.
        int finalPoints = points;
        Bukkit.getScheduler().runTaskAsynchronously(TradeOntime.getInstance(), () -> {
            boolean result = PlayerPoints.getInstance().getAPI().give(player.getUniqueId(), finalPoints);
            if (!result) {
                player.sendMessage(ChatColor.RED + "オンタイムポイント" + finalPoints + "ポイントの付与に失敗しました。");
                getLogger().severe(player.getName() + "のチケット->オンタイムポイントの変換に失敗しました。(" + finalPoints + "ポイント)");
            }
        });

        //Create the message to be sent to the player
        String ReturnMessage = "&bオンタイムチケット" + tickets + "枚をオンタイムポイント" + points + "ポイントに変換しました。";

        Bukkit.getConsoleSender().sendMessage(player.getName() + ": " + ChatColor.translateAlternateColorCodes('&', ReturnMessage));

        //Return the message to be sent to the player
        return ReturnMessage;
    }

    public String ConvertNumberOfOntimeTickets(Player player, Integer ConvertTickets){

        //get display name of ontime ticket
        String MMDisplayName = TradeOntime.getInstance().getConfig().getString("MythicMobsDisplayName");

        //get player inventory.
        Inventory inv = player.getInventory();

        //Initializing the message to be sent to the player
        String ReturnMessage;

        //initializing counter for tickets and points.
        int initialtickets = 0;
        int UnconvertedTickets = 0;
        int points = 0;

        initialtickets = OntimeTickets(player);
        UnconvertedTickets = ConvertTickets;

        if (initialtickets < ConvertTickets){
            //Create the message to be sent to the player
            ReturnMessage = "&c&lオンタイムチケットが足りません。" + ConvertTickets + "枚以上のオンタイムチケットをインベントリに入れてください。";

            //Return the message to be sent to the player
            return ReturnMessage;
        }

        //counting and deleting tickets.
        for (ItemStack item: inv.getContents()) {
            try{
                if(item.getItemMeta().getDisplayName().equals(MMDisplayName)){
                    //Full stack can be converted
                    if (item.getAmount() <= UnconvertedTickets){
                        UnconvertedTickets -= item.getAmount();
                        item.setAmount(0);
                    }
                    //Only part of stack can be converted
                    else{
                        item.setAmount(item.getAmount() - UnconvertedTickets);
                        UnconvertedTickets = 0;
                    }
                }

            }catch (Exception e){
            }
        }

        points = ConvertTickets * pointsPerTicket;

        //Give player ontime points.
        int finalPoints = points;
        Bukkit.getScheduler().runTaskAsynchronously(TradeOntime.getInstance(), () -> {
            boolean result = PlayerPoints.getInstance().getAPI().give(player.getUniqueId(), finalPoints);
            if (!result) player.sendMessage(ChatColor.RED + "オンタイムポイント" + finalPoints + "ポイントの付与に失敗しました。");
            getLogger().severe(player.getName() + "のチケット->オンタイムポイントの変換に失敗しました。(" + finalPoints + "ポイント)");
        });

        //Create the message to be sent to the player
        ReturnMessage = "&bオンタイムチケット" + ConvertTickets + "枚をオンタイムポイント" + points + "ポイントに変換しました。";

        //Return the message to be sent to the player
        return ReturnMessage;

    }
}
