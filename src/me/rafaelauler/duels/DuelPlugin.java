package me.rafaelauler.duels;


import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;


public class DuelPlugin extends JavaPlugin {

    private static DuelPlugin instance;
    private static MySQLManager my;

    public static DuelPlugin getInstance() {
        return instance;
    }

    public MySQLManager getMySQL() {
        return getMy();
    }

    @Override
    public void onEnable() {
        instance = this;
        my = new MySQLManager();
        saveDefaultConfig();
        // 🔥 Inicializa MySQL DEPOIS do plugin subir
        my.connect( getConfig().getString("mysql.host"),
                getConfig().getInt("mysql.port"),
                getConfig().getString("mysql.database"),
                getConfig().getString("mysql.user"),
                getConfig().getString("mysql.password"));
        Duel.setMySQL(getMy());
        StatsSaveService.start();
        // Eventos
        Bukkit.getPluginManager().registerEvents(new DamageListener(), this);
        Bukkit.getPluginManager().registerEvents(new DeathListener(), this);
        Bukkit.getPluginManager().registerEvents(new MoveListener(), this);
        Bukkit.getPluginManager().registerEvents(new SpawnListener(), this);
        Bukkit.getPluginManager().registerEvents(new DuelGUIListener(), this);
        Bukkit.getPluginManager().registerEvents(new DuelProtectListener(), this);
        Bukkit.getPluginManager().registerEvents(new SoupListener(), this);

        // Comandos
        getCommand("duel").setExecutor(new DuelCommand());
        getCommand("duels").setExecutor(new DuelsCommand());
        getCommand("accept").setExecutor(new AcceptCommand());
        getCommand("deny").setExecutor(new DenyCommand());
        getCommand("setduelarena").setExecutor(new SetDuelArena());
        getCommand("setschematic").setExecutor(new SetSchematic());
        getCommand("saveschematic").setExecutor(new SaveSchematic());
        getCommand("setduelspawn").setExecutor(new SetSpawnCommand());
        getCommand("listarenas").setExecutor(new ListArenasCommand());

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new DuelPlaceHolder().register();
        }
        Bukkit.getScheduler().runTaskTimerAsynchronously(
        	    this,
        	    PlayerStatsCache::cleanup,
        	    20L * 60,
        	    20L * 60
        	);
        Bukkit.getScheduler().runTaskTimerAsynchronously(
        	    this,
        	    new StatsSaveWorker(),
        	    20L,
        	    20L
        	);
        Bukkit.getScheduler().runTaskTimerAsynchronously(
        	    this,
        	    new StatsBatchSaveWorker(),
        	    20L,
        	    20L
        	);
        ChallengeManager.init(this);

        getLogger().info("§aDuels plugin ativado com sucesso!");
    }

    @Override
    public void onDisable() {

        getLogger().info("Salvando stats em cache...");

        if (my != null) {
            try {
                int count = PlayerStatsCache.getAll().size();

                for (PlayerStats stats : PlayerStatsCache.getAll()) {
                    my.saveStats(stats);
                }

                PlayerStatsCache.clear();
                getLogger().info("Salvos " + count + " stats no MySQL.");

            } catch (Exception e) {
                getLogger().severe("Erro ao salvar stats!");
                e.printStackTrace();
            } finally {
            	Bukkit.getScheduler().cancelTasks(this);
                my.close(); // fecha HikariCP
            }
        }

        // Encerra duelos
        DuelManager.getAllDuels()
            .forEach(d -> DuelManager.forceEnd(d.getP1()));

        // Limpa blocos protegidos
        if (DuelProtectListener.BLOCK != null) {
            DuelProtectListener.BLOCK.keySet().forEach(b -> {
                if (b != null) b.setType(Material.AIR);
            });
        }
        getLogger().info("Flush final de stats...");

        for (PlayerStats stats : PlayerStatsCache.getAll()) {
            StatsSaveQueue.enqueue(stats);
        }
        StatsSaveService.shutdownAndFlush();
        new StatsSaveWorker().run();
        getLogger().info("§cDuels plugin desativado!");
    }




	public static MySQLManager getMy() {
		return my;
	}

	public void setMy(MySQLManager my) {
		DuelPlugin.my = my;
	}
}
