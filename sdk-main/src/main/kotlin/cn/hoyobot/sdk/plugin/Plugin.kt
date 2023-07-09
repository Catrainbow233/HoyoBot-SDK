package cn.hoyobot.sdk.plugin

import cn.hoyobot.sdk.HoyoBot
import cn.hoyobot.sdk.utils.Config
import cn.hoyobot.sdk.utils.FileUtils
import cn.hutool.log.Log
import com.google.common.base.Preconditions
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.jar.JarFile


abstract class Plugin {

    private var enabled = false
    private lateinit var description: PluginYAML
    private var botProxy: HoyoBot = HoyoBot.instance
    private var logger: Log = botProxy.getLogger()
    private lateinit var pluginFile: File
    private lateinit var dataFolder: File
    private lateinit var configFile: File
    private lateinit var config: Config
    private var initialized = false

    fun init(description: PluginYAML, proxy: HoyoBot, pluginFile: File) {
        Preconditions.checkArgument(!initialized, "Plugin has been already initialized!")
        initialized = true
        this.description = description
        this.botProxy = proxy
        logger = proxy.getLogger()
        this.pluginFile = pluginFile
        dataFolder = File((proxy.getPluginPath() + "/" + description.name.toLowerCase()) + "/")
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }
        configFile = File(dataFolder, "config.yml")
    }

    fun onStartup() {}

    abstract fun onEnable()

    fun onDisable() {}

    fun getResourceFile(filename: String): InputStream? {
        try {
            val pluginJar = JarFile(pluginFile)
            val entry = pluginJar.getJarEntry(filename)
            return pluginJar.getInputStream(entry)
        } catch (e: IOException) {
            botProxy.getLogger().error("Can not get plugin resource!", e)
        }
        return null
    }

    @JvmOverloads
    fun saveResource(filename: String, replace: Boolean = false): Boolean {
        return this.saveResource(filename, filename, replace)
    }

    fun saveResource(filename: String, outputName: String, replace: Boolean): Boolean {
        Preconditions.checkArgument(
            filename.trim { it <= ' ' }.isNotEmpty(),
            "Filename can not be null!"
        )
        val file = File(dataFolder, outputName)
        if (file.exists() && !replace) {
            return false
        }
        try {
            getResourceFile(filename).use { resource ->
                if (resource == null) {
                    return false
                }
                val outFolder = file.parentFile
                if (!outFolder.exists()) {
                    outFolder.mkdirs()
                }
                FileUtils.writeFile(file, resource)
            }
        } catch (e: IOException) {
            botProxy.getLogger().error("Can not save plugin file!", e)
            return false
        }
        return true
    }

    fun loadConfig() {
        try {
            this.saveResource("config.yml")
            config = Config(configFile, 2)
        } catch (e: Exception) {
            botProxy.getLogger().error("Can not load plugin config!")
        }
    }

    fun getConfig(): Config {
        return config
    }

    fun isEnabled(): Boolean {
        return enabled
    }

    fun setEnabled(enabled: Boolean) {
        if (this.enabled == enabled) {
            return
        }
        this.enabled = enabled
        try {
            if (enabled) {
                onEnable()
            } else {
                onDisable()
            }
        } catch (e: Exception) {
            logger.error(e)
        }
    }

    fun getDescription(): PluginYAML {
        return description
    }

    val name: String
        get() = description.name

    fun getBotProxy(): HoyoBot {
        return botProxy
    }
}