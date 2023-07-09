package cn.hoyobot.sdk

import cn.hoyobot.sdk.event.EventManager
import cn.hoyobot.sdk.event.proxy.ProxyBotStartEvent
import cn.hoyobot.sdk.network.BotEntry
import cn.hoyobot.sdk.network.RaknetInterface
import cn.hoyobot.sdk.plugin.PluginManager
import cn.hoyobot.sdk.scheduler.BotScheduler
import cn.hoyobot.sdk.utils.Config
import cn.hoyobot.sdk.utils.ConfigSection
import cn.hutool.log.Log
import cn.hutool.log.LogFactory
import lombok.Getter
import java.io.File
import kotlin.properties.Delegates
import kotlin.system.exitProcess


@Getter
open class HoyoBot {

    companion object {
        lateinit var instance: HoyoBot
    }

    private var address = "0.0.0.0"
    private var port = 80
    private var handlerPath = "/bot"
    private val version = "1.0.0"
    private val path = System.getProperty("user.dir") + "/"
    private val pluginPath = path + "plugins"
    private val logger: Log = LogFactory.get("HoyoBot")
    private var isRunning = false
    private lateinit var botScheduler: BotScheduler
    private lateinit var eventManager: EventManager
    private lateinit var pluginManager: PluginManager
    private var botEntry: BotEntry = BotEntry()
    private lateinit var properties: Config
    private var runningTime by Delegates.notNull<Long>()
    private lateinit var raknetInterface: RaknetInterface
    private var currentTick = 0
    private var httpFilter = false

    fun initBotProxy() {
        instance = this
        this.runningTime = System.currentTimeMillis()
        this.logger.info("HoyoBot - ${this.version}")
        this.logger.info("Find updates at: https://github.com/HoyoBot/HoyoBot-SDK")

        if (!File(pluginPath).exists()) {
            File(pluginPath).mkdirs()
        }

        this.logger.info("Loading HoyoBot properties...")
        properties = Config(this.path + "bot.properties", Config.PROPERTIES, object : ConfigSection() {
            init {
                put("bot_id", "")
                put("bot_secret", "")
                put("server-ip", "0:0:0:0")
                put("port", 80)
                put("villa-id", "0")
                put("http_filter", false)
                put("http_call_back", "/bot")
            }
        })
        this.botEntry.botID = this.properties.getString("bot_id")
        this.botEntry.botSecret = this.properties.getString("bot_secret")
        this.botEntry.villaID = this.properties.getString("villa-id")
        this.address = this.properties.getString("server-ip")
        this.port = this.properties.getString("port").toInt()
        this.handlerPath = this.properties.getString("http_call_back")
        this.httpFilter = this.properties.getBoolean("http_filter", false)
        this.logger.info("Create bot successfully!")

        this.botScheduler = BotScheduler()
        this.eventManager = EventManager(this)
        this.properties.save(true)

        this.raknetInterface = RaknetInterface(this)
        this.raknetInterface.start()

        this.getLogger().info("Loading plugins...")
        this.pluginManager = PluginManager(this)
        this.getPluginManager().enableAllPlugins()

        this.getEventManager().callEvent(ProxyBotStartEvent(this))
        this.initProxy()
    }

    private fun initProxy() {
        this.getLogger().info("Totally load ${this.getPluginManager().getPluginMap().size} plugins")
        this.isRunning = true
        this.getLogger()
            .info("Done! HoyoBot is running on " + port + ". (" + (System.currentTimeMillis() - this.runningTime) + "ms)")
        this.tickProcessor()
        this.shutdown()
    }

    private fun shutdown() {
        isRunning = false
        this.pluginManager.disableAllPlugins()
        exitProcess(0)
    }

    fun getBot(): BotEntry {
        return this.botEntry
    }

    open fun tickProcessor() {
        while (isRunning) {
            tick()
            try {
                Thread.sleep(1)
            } catch (e: InterruptedException) {
                logger.error(e)
            }
        }
    }

    fun getEventManager(): EventManager {
        return this.eventManager
    }

    private fun tick() {
        ++this.currentTick
        this.getScheduler().mainThreadHeartbeat(this.currentTick)
    }

    fun getLogger(): Log {
        return this.logger
    }

    fun getPluginPath(): String {
        return this.pluginPath
    }

    fun getScheduler(): BotScheduler {
        return this.botScheduler
    }

    fun getPort(): Int {
        return this.port
    }

    fun getHttpCallBackPath(): String {
        return this.handlerPath
    }

    fun getVersion(): String {
        return this.version
    }

    fun isEnabledFilter(): Boolean {
        return this.httpFilter
    }

    fun getVillaID(): String {
        return this.botEntry.villaID
    }

    fun getPluginManager(): PluginManager {
        return this.pluginManager
    }

}