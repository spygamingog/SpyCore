package com.spygamingog.spycore.services;

import com.spygamingog.spycore.SpyCore;
import com.spygamingog.spycore.api.DataService;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class YamlDataService implements DataService {
    private final SpyCore plugin;
    private final File dataFile;
    private FileConfiguration config;

    public YamlDataService(SpyCore plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create data.yml", e);
            }
        }
        this.config = YamlConfiguration.loadConfiguration(dataFile);
    }

    @Override
    public void save(String key, Object value) {
        config.set(key, value);
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save data.yml", e);
        }
    }

    @Override
    public Object load(String key) {
        return config.get(key);
    }

    @Override
    public <T> T load(String key, Class<T> type) {
        Object val = config.get(key);
        if (type.isInstance(val)) {
            return type.cast(val);
        }
        return null;
    }

    @Override
    public void delete(String key) {
        config.set(key, null);
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save data.yml", e);
        }
    }
}
