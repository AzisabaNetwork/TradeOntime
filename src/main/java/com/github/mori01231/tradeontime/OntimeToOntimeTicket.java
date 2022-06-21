package com.github.mori01231.tradeontime;

import org.black_ixx.playerpoints.PlayerPoints;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import static org.bukkit.Bukkit.*;

public class OntimeToOntimeTicket implements CommandExecutor {


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (sender instanceof Player){
            int pointsPerTicket = TradeOntime.getInstance().getConfig().getInt("PointsPerTicket");
            Player player = (Player) sender;
            if (TradeOntime.getInstance().checkCoolTime(player.getUniqueId())) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cクールタイム中です。数秒程度時間を開けてもう一度試してください。"));
                return true;
            }
            String PlayerName = player.getName();
            Boolean UseCommandForOntime = TradeOntime.getInstance().getConfig().getBoolean("UseCommandForOntimeItem");
            String OntimeTicketGiveCommand = "minecraft:give " + PlayerName + " paper{display:{Name:'{\"text\":\"オンタイムチケット\",\"color\":\"green\"}',Lore:['{\"text\":\"ログインしていると一定時間ごとにもらえる。\",\"color\":\"white\"}','{\"text\":\"換金、特殊アイテムとの交換等に使える。\",\"color\":\"white\"}']},HideFlags:1,Enchantments:[{id:\"minecraft:unbreaking\",lvl:1s}]} ";

            if(args.length == 1) {
                String MMItemName = TradeOntime.getInstance().getConfig().getString("MythicMobsItemName");

                int points;

                try{
                    points = Integer.parseInt(args[0]);
                }catch(Exception e){
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&l変換するポイント数は正の整数で指定してください。"));
                    return false;
                }

                //Points was not negative
                if (points < 0){
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&l変換するポイント数は正の整数で指定してください。"));
                    return false;
                }

                Bukkit.getScheduler().runTaskAsynchronously(TradeOntime.getInstance(), () -> {
                    int haspoints = PlayerPoints.getInstance().getAPI().look(player.getUniqueId());
                    //points was less than pointsPerTicket
                    if (haspoints < pointsPerTicket){
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lあなたの所持オンタイムポイントが" + pointsPerTicket + "未満のためオンタイムチケットへの変換が出来ません。"));
                        return;
                    }

                    //Convert to ontime tickets
                    if (points <= haspoints){
                        int takepoints = points - (points % pointsPerTicket);
                        getLogger().info(String.valueOf(takepoints));

                        //points was between 0 and pointsPerTicket
                        if(takepoints == 0){
                            Bukkit.getScheduler().runTask(TradeOntime.getInstance(), () -> {
                                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lオンタイムポイントは" + pointsPerTicket + "以上の整数で指定してください。"));
                            });
                            return;
                        }

                        int giveitems = takepoints/pointsPerTicket;
                        int RequiredSlots = 100;

                        if (giveitems % 64 == 0){
                            RequiredSlots = giveitems / 64;
                        }else{
                            RequiredSlots = (giveitems - (giveitems % 64) + 64) / 64;
                        }
                        getLogger().info("slots " + RequiredSlots);
                        //If the players doesn't have enough slots, no transaction
                        if (AvailableSlots(player) < RequiredSlots) {
                            Bukkit.getScheduler().runTask(TradeOntime.getInstance(), () -> {
                                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&l現在インベントリには" + AvailableSlots(player) + "スロットの空きがあります。"));
                                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lオンタイムチケットがインベントリに入りきりません。インベントリに空きを増やしたうえで再度コマンドを実行してください。"));
                            });
                            return;
                        }

                        boolean result = PlayerPoints.getInstance().getAPI().take(player.getUniqueId(), takepoints);
                        // Drop items after switching back to the main thread
                        Bukkit.getScheduler().runTask(TradeOntime.getInstance(), () -> {
                            if (!result) {
                                sender.sendMessage(ChatColor.RED + "オンタイムポイントの変換に失敗しました。");
                                getLogger().severe(PlayerName + "のオンタイムポイント->チケットの変換に失敗しました。(" + takepoints + "ポイント)");
                                return;
                            }
                            //Actual transaction
                            if(UseCommandForOntime){
                                getServer().dispatchCommand(getServer().getConsoleSender(), OntimeTicketGiveCommand + giveitems);
                                getLogger().info(PlayerName + "にオンタイムチケットを " + giveitems + " 個与えました。");
                            }else{
                                getServer().dispatchCommand(getServer().getConsoleSender(), "mm i give " + PlayerName + " " + MMItemName + " " + giveitems);
                                getLogger().info(PlayerName + "にMMアイテム " + MMItemName + " を " + giveitems + " 個与えました。");
                            }
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b" + takepoints + " オンタイムポイントをオンタイムチケット " + giveitems + " 枚に変換しました。"));
                        });
                    }

                    //Has less points than the points in argument
                    else{
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lオンタイムポイントは" + haspoints + "以下かつ" + pointsPerTicket + "以上の整数で指定してください。"));
                        return;
                    }
                });
            }
            //No points argument
            else if(args.length == 0){
                String MMItemName = TradeOntime.getInstance().getConfig().getString("MythicMobsItemName");

                //int haspoints = PlayerPoints(player);
                Bukkit.getScheduler().runTaskAsynchronously(TradeOntime.getInstance(), () -> {
                    int haspoints = PlayerPoints.getInstance().getAPI().look(player.getUniqueId());
                    int takepoints = haspoints - (haspoints % pointsPerTicket);
                    System.out.println(takepoints);

                    //points was between 0 and pointsPetTicket
                    if(takepoints == 0){
                        Bukkit.getScheduler().runTask(TradeOntime.getInstance(), () -> {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lあなたの所持オンタイムポイントが" + pointsPerTicket + "未満のためオンタイムチケットへの変換が出来ません。"));
                        });
                        return;
                    }

                    int giveitems = takepoints/pointsPerTicket;
                    int RequiredSlots = 100;

                    if (giveitems % 64 == 0){
                        RequiredSlots = giveitems / 64;
                    }else{
                        RequiredSlots = (giveitems - (giveitems % 64) + 64) / 64;
                    }

                    //If the players doesn't have enough slots, no transaction
                    if (AvailableSlots(player) < RequiredSlots) {
                        Bukkit.getScheduler().runTask(TradeOntime.getInstance(), () -> {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&l現在インベントリには" + AvailableSlots(player) + "スロットの空きがあり、" + AvailableSlots(player) * 64 * pointsPerTicket + "ポイントまでしか変換できません。"));
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lオンタイムチケットがインベントリに入りきりません。インベントリに空きを増やしたうえで再度コマンドを実行してください。"));
                        });
                        return;
                    }

                    boolean result = PlayerPoints.getInstance().getAPI().take(player.getUniqueId(), takepoints);
                    // Drop items after switching back to the main thread
                    Bukkit.getScheduler().runTask(TradeOntime.getInstance(), () -> {
                        if (!result) {
                            sender.sendMessage(ChatColor.RED + "オンタイムポイントの変換に失敗しました。");
                            getLogger().severe(PlayerName + "のオンタイムポイント->チケットの変換に失敗しました。(" + takepoints + "ポイント)");
                            return;
                        }
                        //Actual transaction
                        if(UseCommandForOntime){
                            getServer().dispatchCommand(getServer().getConsoleSender(), OntimeTicketGiveCommand + giveitems);
                            getLogger().info(PlayerName + "にオンタイムチケットを " + giveitems + " 個与えました。");
                        }else{
                            getServer().dispatchCommand(getServer().getConsoleSender(), "mm i give " + PlayerName + " " + MMItemName + " " + giveitems);
                            getLogger().info(PlayerName + "にMMアイテム " + MMItemName + " を " + giveitems + " 個与えました。");
                        }
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b" + takepoints + " オンタイムポイントをオンタイムチケット " + giveitems + " 枚に変換しました。"));
                    });
                });



            }
        }
        //Can't convert tickets from console
        else{
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lこのコマンドはコンソールからは実行できません"));
        }
        return true;
    }


    //get amount of available slots in player inventory excluding armor and offhand
    public int AvailableSlots(Player player){
        //get player inventory.
        Inventory inv = player.getInventory();

        //initializing counter for slots.
        int slots=0;

        //counting the number of available slots.
        for (ItemStack item: inv.getContents()) {
            if(item == null) {
                slots++;
            }
        }

        //Compensating for empty offhand slot
        if(player.getInventory().getItemInOffHand().getType() == Material.AIR){
            slots--;
        }

        //Compensating for empty armor slots
        for (ItemStack item: player.getInventory().getArmorContents()){
            if(item == null) {
                slots--;
            }
        }

        //return the number of available slots.
        return slots;
    }

    public int PlayerPoints(Player player){
        int haspoints = 0;

        if (PlayerPoints.getInstance().getAPI() != null) {
            haspoints = PlayerPoints.getInstance().getAPI().look(player.getUniqueId());
            getLogger().info("Has " + haspoints + " points.");
        }
        return haspoints;
    }
}
