package com.petlin.matoonigokko16

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import java.io.File
import java.io.IOException
import java.util.logging.Level

class ConfigManager {
    //This stuff will be initialized in your constructor
    private val PLUGIN: Plugin
    private val FILENAME: String
    private val FOLDER: File
    private var config: FileConfiguration?
    private var configFile: File?

    //This first constructor is for putting the config file directly into the getDataFolder().
    constructor(filename: String, instance: Plugin) {
        var filename = filename
        if (!filename.endsWith(".yml")) {
            filename += ".yml"
        }
        FILENAME = filename
        PLUGIN = instance
        FOLDER = PLUGIN.dataFolder
        config =
            null //this.config and this.configFile are set to null here, but they will be properly initialized in the reload() method.
        configFile = null
        reload() //We will define this method in a minute, hold your horses.
    }

    //The second constructor is for putting the config file into a specified folder.
    constructor(folder: File, filename: String, instance: Plugin) {
        var filename = filename
        if (!filename.endsWith(".yml")) {
            filename += ".yml" //Check whether the filename already has the extension, and if it doesn't, add it.
        }
        FILENAME = filename
        PLUGIN =
            instance //You can also remove the instance from your constructor and use Bukkit.getPluginManager().getPlugin("<pluginname>");
        FOLDER = folder
        config =
            null //this.config and this.configFile are set to null here, but they will be properly initialized in the reload() method.
        configFile = null
        reload() //We will define this method in a minute, hold your horses.
    }

    fun getConfig(): FileConfiguration? {
        if (config == null) {
            reload()
        }
        return config //Return the config so that the player can save, set, and get from the config.
    }

    fun reload() {
        if (!FOLDER.exists()) { //Checks if folder exists
            try {
                if (FOLDER.mkdir()) { //Attempts to make a folder if the folder does not exist.
                    PLUGIN.logger.log(Level.INFO, "Folder " + FOLDER.name + " created.")
                } else {
                    PLUGIN.logger.log(Level.WARNING, "Unable to create folder " + FOLDER.name + ".")
                }
            } catch (e: Exception) {
            }
        }
        configFile = File(FOLDER, FILENAME) //Makes the file in the folder
        if (!configFile!!.exists()) { //Creates the file if it doesn't exist
            try {
                configFile!!.createNewFile()
            } catch (e: IOException) {
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile!!) //Loads the file through YAML's system
    }

    fun saveDefaultConfig() {
        if (configFile == null) {
            configFile = File(PLUGIN.dataFolder, FILENAME)
        }
        if (!configFile!!.exists()) {
            PLUGIN.saveResource(FILENAME, false) //Gets resource from jar (from the same place as plugin.yml)
        }
    }

    fun save() { //This method makes it so that you don't have to do config.getConfig().save(configFile); every time you want to save.
        if (config == null || configFile == null) {
            return
        }
        try {
            getConfig()!!.save(configFile!!)
        } catch (ex: IOException) {
            PLUGIN.logger.log(Level.WARNING, "Could not save config to " + configFile!!.name, ex)
        }
    }

    operator fun set(path: String?, o: Any?) { //Shortens the path to set something.
        getConfig()!![path!!] = o
        save()
        //You could also add a save() here, but I decided not to, just so it only had to save once per time I set things.
    }

    fun setLocation(
        path: String,
        l: Location
    ) { //I found myself putting locations in configs a lot, so I made a generic location setting tool.
        getConfig()!!["$path.w"] = l.world.name
        getConfig()!!["$path.x"] = l.x
        getConfig()!!["$path.y"] = l.y
        getConfig()!!["$path.z"] = l.z
        getConfig()!!["$path.yaw"] = l.yaw
        getConfig()!!["$path.pitch"] = l.pitch
        save()
    }

    fun getLocation(path: String): Location? { //You can get locations that you set with the above method by using this one.
        if (getConfig()!!.contains(path)) {
            return Location(
                Bukkit.getWorld(getConfig()!!.getString("$path.w")!!),
                getConfig()!!.getDouble(
                    "$path.x"
                ),
                getConfig()!!.getDouble("$path.y"),
                getConfig()!!.getDouble("$path.z"),
                ("" + getConfig()!!.getDouble("$path.yaw")).toFloat(),
                ("" + getConfig()!!.getDouble(
                    "$path.pitch"
                )).toFloat()
            )
        } else {
            return null
        }
    }

    fun getStartWith(start: String): List<String> {
        return getConfig()!!.getKeys(false).filter { str -> str.startsWith(start) }.sorted()
    }
}

/**
 * Configクラスの使い方

Config c = new Config(player.getUniqueId(), this); //Using "this" only works if it's in the main class, otherwise pass an instance of the main class into the class you're using and use the variable you set it to.
c.set("are-cookies-good", true);
c.set("multiple.lines.possible", true);
// c.save();
boolean cookies = c.getConfig().getBoolean().get("are-cookies-good");

↓ 詳細はこちら（チュートリアル） ↓
https://bukkit.org/threads/tutorial-multiple-yaml-configs-and-player-files.262296/
 */