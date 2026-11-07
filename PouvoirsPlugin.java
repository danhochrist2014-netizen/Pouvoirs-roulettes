package com.pouvoirs.roulette;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class PouvoirsPlugin extends JavaPlugin {

    private PowerManager powerManager;
    private PowerMenu powerMenu;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        powerManager = new PowerManager(this);
        powerMenu = new PowerMenu(this, powerManager);

        getServer().getPluginManager().registerEvents(powerMenu, this);
        getServer().getPluginManager().registerEvents(new PowerListener(this, powerManager, powerMenu), this);

        PouvoirCommand command = new PouvoirCommand(this, powerManager, powerMenu);
        if (getCommand("pouvoir") != null) {
            getCommand("pouvoir").setExecutor(command);
            getCommand("pouvoir").setTabCompleter(command);
        }

        // Si le plugin est (re)charge pendant que des joueurs sont connectes
        for (Player player : Bukkit.getOnlinePlayers()) {
            powerManager.load(player);
        }
        powerManager.startRefreshTask();

        getLogger().info("PouvoirsRoulette active : " + Power.values().length + " pouvoirs charges.");
    }

    @Override
    public void onDisable() {
        if (powerMenu != null) {
            powerMenu.shutdown();
        }
    }
}
