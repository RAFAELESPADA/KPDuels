package me.rafaelauler.duels;


import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;


public class DuelPlugin extends JavaPlugin {
	static MySQLManager my = new MySQLManager("localhost", 3306, "database_name", "user", "password");
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

     // Conecta ao banco (certifique-se que você tem host, user, password e database)
     try {
         my.connect();
     } catch (Exception e) {
         e.printStackTrace();
     }
     StatsGUI.setMySQL(my);
        Bukkit.getPluginManager().registerEvents(new DuelProtectListener(), this);
        ChallengeManager.init(this);
        getLogger().info("Duels plugin ativado!");
    }

@Override
public void onDisable() {
	if (my != null) {
	    try {
			my.disconnect();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

    getLogger().info("Duels plugin desativado!");
}
}
