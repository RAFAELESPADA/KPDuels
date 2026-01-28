package me.rafaelauler.duels;


import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;


public class DuelPlugin extends JavaPlugin {
	public static MySQLManager my = new MySQLManager("host", 3306, "db", "user", "senha");
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
	if (DuelProtectListener.BLOCK.values() != null) {
for (Block b : DuelProtectListener.BLOCK.keySet()) {
	if (b != null) {
		b.setType(Material.AIR);
	}
}
	}
    getLogger().info("Duels plugin desativado!");
}
}
