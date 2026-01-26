package me.rafaelauler.duels;


import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;


public class DuelPlugin extends JavaPlugin {

    @Override
    public void onEnable() {

        Bukkit.getPluginManager().registerEvents(new DamageListener(), this);
        Bukkit.getPluginManager().registerEvents(new DeathListener(), this);
        Bukkit.getPluginManager().registerEvents(new MoveListener(), this);
        getCommand("duel").setExecutor(new DuelCommand());
        getCommand("setduelarena").setExecutor(new SetDuelArena());
        getCommand("setschematic").setExecutor(new SetSchematic());
        getCommand("saveschematic").setExecutor(new SaveSchematic());
        getCommand("setduelspawn").setExecutor(new SetSpawnCommand());
        Bukkit.getPluginManager().registerEvents(new SpawnListener(), this);
        Bukkit.getPluginManager().registerEvents(new DuelGUIListener(), this);
        Bukkit.getPluginManager().registerEvents(new SoupListener(), this);
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new DuelPlaceHolder().register();
        }
        Bukkit.getPluginManager().registerEvents(new DuelProtectListener(), this);
        ChallengeManager.init(this);
        getLogger().info("Duels plugin ativado!");
    }
}
