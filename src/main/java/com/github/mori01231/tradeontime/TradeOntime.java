package com.github.mori01231.tradeontime;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TradeOntime extends JavaPlugin {
    private static final int COOLTIME_DURATION = 5 * 1000;
    private final Map<UUID, Long> globalCoolTime = new HashMap<>();

    private static TradeOntime instance;
    public TradeOntime (){
        instance = this;
    }
    public static TradeOntime getInstance() {
        return instance;
    }


    @Override
    public void onEnable() {
        getLogger().info("TradeOntime has been enabled.");
        this.getCommand("ontimetoticket").setExecutor(new OntimeToOntimeTicket());
        this.getCommand("tickettoontime").setExecutor(new OntimeTicketToOntime());

        this.saveDefaultConfig();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    /**
     * Checks the cooltime state of the player.
     * @param uuid the uuid to check
     * @return true if cooltime is still not yet elapsed; false otherwise
     */
    public boolean checkCoolTime(UUID uuid){
        if(globalCoolTime.containsKey(uuid)){
            if(System.currentTimeMillis() - globalCoolTime.get(uuid) > COOLTIME_DURATION){
                globalCoolTime.remove(uuid);
                return false;
            } else {
                return true;
            }
        }
        globalCoolTime.put(uuid, System.currentTimeMillis());
        return false;
    }
}
