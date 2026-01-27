package me.rafaelauler.duels;


import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;


public class DuelPlugin extends JavaPlugin {
	public static MySQLManager my = new MySQLManager("sp-17.magnohost.com.br", 3306, "s4285_geral2", "u42852_7AnXHikZc5", "xzk9DZo!+0G3QYMUUQSx5mGp");
    @Override
    public void onEnable() {

        Bukkit.getPluginManager().registerEvents(new DamageListener(), this);
        Bukkit.getPluginManager().registerEvents(new DeathListener(), this);
        Bukkit.getPluginManager().registerEvents(new MoveListener(), this);
        getCommand("duel").setExecutor(new DuelCommand());
        getCommand("duels").setExecutor(new DuelsCommand());
        getCommand("setduelarena").setExecutor(new SetDuelArena());
        getCommand("setschematic").setExecutor(new SetSchematic());

        getCommand("accept").setExecutor(new AcceptCommand());
        getCommand("deny").setExecutor(new DenyCommand());
        getCommand("saveschematic").setExecutor(new SaveSchematic());
        getCommand("setduelspawn").setExecutor(new SetSpawnCommand());
        getCommand("listarenas").setExecutor(new ListArenasCommand());
        Bukkit.getPluginManager().registerEvents(new SpawnListener(), this);
        Bukkit.getPluginManager().registerEvents(new DuelGUIListener(), this);
        Bukkit.getPluginManager().registerEvents(new DuelProtectListener(), this);
        Bukkit.getPluginManager().registerEvents(new SoupListener(), this);
        saveDefaultConfig(); 
        getLogger().info("Eventos e comandos do plugin Duels carregados!");
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new DuelPlaceHolder().register();
        }

     // Conecta ao banco (certifique-se que você tem host, user, password e database)
     try {
         my.connect();
         Duel.setMySQL(my);
     } catch (Exception e) {
         e.printStackTrace();
     }
     StatsGUI.setMySQL(my);
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
	}DuelManager.getAllDuels().forEach(d -> DuelManager.forceEnd(d.getP1()));

    getLogger().info("Duels plugin desativado!");
}
}
