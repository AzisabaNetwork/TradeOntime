package com.github.mori01231.tradeontime

import com.tcoded.folialib.FoliaLib
import com.tcoded.folialib.impl.PlatformScheduler
import org.bukkit.ChatColor
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class TradeOntime : JavaPlugin() {
    private val globalCoolTime = ConcurrentHashMap<UUID, Long>()

    private lateinit var foliaLib: FoliaLib

    val scheduler: PlatformScheduler
        get() = foliaLib.scheduler

    override fun onEnable() {
        foliaLib = FoliaLib(this)
        saveDefaultConfig()

        getCommand("ontimetoticket")!!.setExecutor(OntimeToOntimeTicket(this))
        getCommand("tickettoontime")!!.setExecutor(OntimeTicketToOntime(this))

        logger.info("TradeOntime has been enabled.")
    }

    override fun onDisable() {
        if (::foliaLib.isInitialized) {
            foliaLib.scheduler.cancelAllTasks()
        }
    }

    fun checkCoolTime(uuid: UUID): Boolean {
        val now = System.currentTimeMillis()
        var inCoolTime = false
        globalCoolTime.compute(uuid) { _, last ->
            when {
                last == null -> now
                now - last > COOLTIME_DURATION -> null
                else -> {
                    inCoolTime = true
                    last
                }
            }
        }
        return inCoolTime
    }

    companion object {
        private const val COOLTIME_DURATION = 5 * 1000L
    }
}

internal fun String.colored(): String = ChatColor.translateAlternateColorCodes('&', this)
