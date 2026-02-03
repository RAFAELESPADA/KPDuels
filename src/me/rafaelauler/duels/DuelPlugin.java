package me.rafaelauler.duels;


import org.bukkit.Bukkit;
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
        StatsSaveService.start();
        ServerTick.start(this);

        Bukkit.getScheduler().runTaskTimerAsynchronously(
            this,
            new StatsBatchWorker(),
            20L * 5,
            20L * 5
        );

        Bukkit.getScheduler().runTaskTimerAsynchronously(
            this,
            PlayerStatsCache::cleanup,
            20L * 60,
            20L * 60
        );
        
        ChallengeManager.init(this);

        getLogger().info("§aDuels plugin ativado com sucesso!");
    }

    @Override
    public void onDisable() {

        getLogger().info("Salvando stats em cache...");

        
        	 getLogger().info("[Duels] Flush final de stats...");

        	    try {
        	        // 1️⃣ Enfileira tudo que ainda está sujo
        	        PlayerStatsCache.getAll().stream()
        	            .filter(PlayerStats::isDirty)
        	            .forEach(StatsSaveQueue::enqueue);

        	        // 2️⃣ Para schedulers
        	        Bukkit.getScheduler().cancelTasks(this);

        	        // 3️⃣ Flush bloqueante
        	        StatsSaveService.shutdownAndFlush();

        	    } catch (Exception e) {
        	        getLogger().severe("Erro no flush final!");
        	        e.printStackTrace();
        	    } finally {
        	        if (my != null) my.close();
        	    }
        

        // Encerra duelos
        DuelManager.getAllDuels()
            .forEach(d -> DuelManager.forceEnd(d.getP1()));

        for (Duel duels : DuelManager.getAllDuels()) {
        	duels.clearPlacedBlocks();
        }
        getLogger().info("Flush final de stats completo");
        getLogger().info("§cDuels plugin desativado!");
    }




	public static MySQLManager getMy() {
		return my;
	}

	public void setMy(MySQLManager my) {
		DuelPlugin.my = my;
	}
}
