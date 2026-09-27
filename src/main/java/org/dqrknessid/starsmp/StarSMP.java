package org.dqrknessid.starsmp;

import org.bukkit.plugin.java.JavaPlugin;
import org.dqrknessid.starsmp.command.AbilityCommand;
import org.dqrknessid.starsmp.command.StarCommand;
import org.dqrknessid.starsmp.command.TrustCommand;
import org.dqrknessid.starsmp.command.TrustListCommand;
import org.dqrknessid.starsmp.command.UntrustCommand;
import org.dqrknessid.starsmp.listener.CraftingListener;
import org.dqrknessid.starsmp.listener.PassiveTickListener;
import org.dqrknessid.starsmp.listener.PlayerListener;
import org.dqrknessid.starsmp.listener.CosmicJudgementListener;
import org.dqrknessid.starsmp.listener.AstralCrownListener;
import org.dqrknessid.starsmp.manager.CooldownManager;
import org.dqrknessid.starsmp.manager.ItemManager;
import org.dqrknessid.starsmp.manager.RecipeManager;
import org.dqrknessid.starsmp.manager.StarManager;
import org.dqrknessid.starsmp.player.PlayerDataManager;
import org.dqrknessid.starsmp.star.StarType;
import org.dqrknessid.starsmp.star.impl.*;
import org.dqrknessid.starsmp.listener.TemporaryBlockListener;
import org.dqrknessid.starsmp.listener.SpearListener;
import org.dqrknessid.starsmp.listener.StarlightOathListener;
import org.dqrknessid.starsmp.command.ControlsCommand;
import org.dqrknessid.starsmp.manager.AltarManager;
import org.dqrknessid.starsmp.manager.RitualManager;
import org.dqrknessid.starsmp.manager.LegendaryLimitManager;
import org.dqrknessid.starsmp.ui.AltarCraftingUI;
import org.dqrknessid.starsmp.ui.RecipeGUI;
import java.util.UUID;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;

import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.Material;

public final class StarSMP extends JavaPlugin {

    private PlayerDataManager dataManager;
    private ItemManager itemManager;
    private StarManager starManager;
    private CooldownManager cooldownManager;
    private AltarManager altarManager;
    private RitualManager ritualManager;
    private LegendaryLimitManager limitManager;
    private RecipeManager recipeManager;
    private AltarCraftingUI altarUI;
    private RecipeGUI recipeGUI;
    private org.dqrknessid.starsmp.manager.DeathHistoryManager deathHistoryManager;

    private TemporaryBlockListener temporaryBlockListener;
    private CosmicJudgementListener cosmicJudgementListener;
    private AstralCrownListener astralCrownListener;
    private PassiveTickListener passiveTickListener;
    private SpearListener spearListener;
    private StarlightOathListener starlightOathListener;
    private org.dqrknessid.starsmp.listener.NebulaGuardListener nebulaGuardListener;
    private org.dqrknessid.starsmp.ui.StarSelectionUI starSelectionUI;
    private PlayerListener playerListener;



    // Grace Period Storage
    private long gracePeriodEnd = -1;

    public boolean isGracePeriodActive() {
        return gracePeriodEnd > 0 && System.currentTimeMillis() < gracePeriodEnd;
    }

    public long getGracePeriodRemaining() {
        if (!isGracePeriodActive()) return 0;
        return (gracePeriodEnd - System.currentTimeMillis()) / 1000;
    }

    public void setGracePeriod(long seconds) {
        if (seconds <= 0) {
            this.gracePeriodEnd = -1;
        } else {
            this.gracePeriodEnd = System.currentTimeMillis() + (seconds * 1000);
        }
    }

    // Restart Persistence Storage
    public static final org.bukkit.NamespacedKey HEALTH_MODIFIER_KEY = new org.bukkit.NamespacedKey("starsmp", "health_suppression");
    public static final org.bukkit.NamespacedKey PYRELION_HEALTH_KEY = new org.bukkit.NamespacedKey("starsmp", "pyrelion_suppression");
    public static final org.bukkit.NamespacedKey PYRELION_EXPIRY_KEY = new org.bukkit.NamespacedKey("starsmp", "pyrelion_expiry");

    public static void untrackHealthModification(UUID id) {
        untrackHealthModification(id, HEALTH_MODIFIER_KEY);
        untrackHealthModification(id, PYRELION_HEALTH_KEY);
    }

    public static void untrackHealthModification(UUID id, org.bukkit.NamespacedKey key) {
        Player p = Bukkit.getPlayer(id);
        if (p != null) {
            AttributeInstance attr = p.getAttribute(Attribute.MAX_HEALTH);
            if (attr != null) attr.removeModifier(key);
        }
    }

    @Override
    public void onEnable() {
        // Configuration
        saveDefaultConfig();

        // Initialize Managers
        this.dataManager = new PlayerDataManager(this);
        this.itemManager = new ItemManager(this);
        this.starManager = new StarManager(this, dataManager);
        this.cooldownManager = new CooldownManager(this, starManager);
        this.altarManager = new AltarManager(this);
        this.ritualManager = new RitualManager(this);
        this.limitManager = new LegendaryLimitManager(this);
        this.recipeManager = new RecipeManager(this, itemManager);
        this.deathHistoryManager = new org.dqrknessid.starsmp.manager.DeathHistoryManager(this);
        this.temporaryBlockListener = new TemporaryBlockListener();

        // Start Physics Safety Task (Failsafe for "infinite levitation" bugs)
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                    if (player.getGameMode() == org.bukkit.GameMode.CREATIVE || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;
                    
                    StarType type = starManager.getActiveStar(player);
                    boolean isZephyros = (type == StarType.ZEPHYROS && starManager.isPassiveActive(player));
                    
                    // Failsafe 1: Gravity check
                    if (!player.hasGravity()) {
                        player.setGravity(true);
                    }
                    
                    // Failsafe 2: Unauthorized flight cleanup
                    if (!isZephyros) {
                        if (player.getAllowFlight()) {
                            player.setAllowFlight(false);
                            player.setFlying(false);
                        }
                    } else {
                        // Zephyros can have allowFlight, but should never be actively flying
                        if (player.isFlying()) {
                            player.setFlying(false);
                        }
                    }
                }
            }
        }.runTaskTimer(this, 100L, 20L); // Run every second after a 5s delay

        // Register Stars
        starManager.registerStar(new AqualithStar(this));
        starManager.registerStar(new PyrelionStar(this));
        starManager.registerStar(new ZephyrosStar(this));
        starManager.registerStar(new GravarynStar(this));
        starManager.registerStar(new VoltyraStar(this));
        starManager.registerStar(new VerdarynStar(this));
        starManager.registerStar(new UmbryxStar(this));
        starManager.registerStar(new LuminaryStar(this));
        starManager.registerStar(new AetherionStar(this));
        starManager.registerStar(new ChronovarStar(this));

        // Listeners
        this.playerListener = new PlayerListener(this, starManager, itemManager, dataManager, cooldownManager);
        getServer().getPluginManager().registerEvents(playerListener, this);
        this.cosmicJudgementListener = new CosmicJudgementListener(this, itemManager);
        getServer().getPluginManager().registerEvents(cosmicJudgementListener, this);
        getServer().getPluginManager().registerEvents(new CraftingListener(this, itemManager), this);
        getServer().getPluginManager().registerEvents(temporaryBlockListener, this);
        getServer().getPluginManager().registerEvents(altarManager, this);
        this.starSelectionUI = new org.dqrknessid.starsmp.ui.StarSelectionUI();
        getServer().getPluginManager().registerEvents(new org.dqrknessid.starsmp.listener.StarSelectionListener(), this);

        // Passive Ticking Task
        this.passiveTickListener = new PassiveTickListener(this, starManager, itemManager);
        passiveTickListener.start();

        // Astral Crown Listener

        this.astralCrownListener = new AstralCrownListener(this, itemManager);
        getServer().getPluginManager().registerEvents(astralCrownListener, this);

        // Spear Listener
        this.altarUI = new AltarCraftingUI(this, itemManager);
        Bukkit.getPluginManager().registerEvents(altarUI, this);

        this.recipeGUI = new RecipeGUI(this);
        Bukkit.getPluginManager().registerEvents(recipeGUI, this);

        // Spear Listener
        this.spearListener = new SpearListener(this, itemManager);
        getServer().getPluginManager().registerEvents(spearListener, this);

        // Starlight Oath Listener
        this.starlightOathListener = new StarlightOathListener(this, itemManager);
        getServer().getPluginManager().registerEvents(starlightOathListener, this);

        // Nebula Guard Listener
        this.nebulaGuardListener = new org.dqrknessid.starsmp.listener.NebulaGuardListener(this, itemManager);
        getServer().getPluginManager().registerEvents(nebulaGuardListener, this);

        // Commands
        StarCommand starCmd = new StarCommand(this, starManager, itemManager);
        getCommand("stars").setExecutor(starCmd);
        getCommand("stars").setTabCompleter(starCmd);

        AbilityCommand abilityCmd = new AbilityCommand(this, starManager, cooldownManager, dataManager);
        getCommand("ability1").setExecutor(abilityCmd);
        getCommand("ability1").setTabCompleter(abilityCmd);

        ControlsCommand controlsCmd = new ControlsCommand(dataManager);
        getCommand("controls").setExecutor(controlsCmd);
        getCommand("controls").setTabCompleter(controlsCmd);

        getCommand("trust").setExecutor(new TrustCommand(this));
        getCommand("untrust").setExecutor(new UntrustCommand(this));
        getCommand("trustlist").setExecutor(new TrustListCommand(this));

        // Start safety check task (every 10 seconds / 200 ticks)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            long now = System.currentTimeMillis();
            for (Player player : Bukkit.getOnlinePlayers()) {
                // Cleanup expired Pyrelion suppression
                org.bukkit.persistence.PersistentDataContainer pdc = player.getPersistentDataContainer();
                if (pdc.has(PYRELION_EXPIRY_KEY, org.bukkit.persistence.PersistentDataType.LONG)) {
                    long expiry = pdc.get(PYRELION_EXPIRY_KEY, org.bukkit.persistence.PersistentDataType.LONG);
                    if (now >= expiry) {
                        untrackHealthModification(player.getUniqueId(), PYRELION_HEALTH_KEY);
                        pdc.remove(PYRELION_EXPIRY_KEY);
                    }
                }

                // Remove outdated or mismatched star fragments to prevent legacy material exploits
                Material currentFragmentMat = itemManager.createStarFragment().getType();
                for (int i = 0; i < player.getInventory().getSize(); i++) {
                    org.bukkit.inventory.ItemStack item = player.getInventory().getItem(i);
                    if (item != null && itemManager.isCustomItem(item, "STAR_FRAGMENT")) {
                        if (item.getType() != currentFragmentMat) {
                            player.getInventory().setItem(i, null);
                        }
                    }
                }
                
                // Safety check to restore missing star
                if (starManager.hasReceivedStar(player)) {
                    boolean hasStar = false;
                    
                    // 1. Main Inventory
                    for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
                        if (itemManager.isCustomItem(item, "STAR")) {
                            hasStar = true;
                            break;
                        }
                    }
                    
                    // 2. Cursor Item (Handling lifting/hovering)
                    if (!hasStar && itemManager.isCustomItem(player.getItemOnCursor(), "STAR")) {
                        hasStar = true;
                    }
                    
                    // 3. Open Inventory (Top part, e.g. crafting grid, during interactions)
                    if (!hasStar) {
                        org.bukkit.inventory.Inventory topInv = player.getOpenInventory().getTopInventory();
                        if (topInv != null) {
                            for (org.bukkit.inventory.ItemStack item : topInv.getContents()) {
                                if (itemManager.isCustomItem(item, "STAR")) {
                                    hasStar = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (!hasStar) {
                        playerListener.updateStarInInventory(player);
                    }
                }
            }
        }, 200L, 200L);

        // PAPI Integration
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new org.dqrknessid.starsmp.integration.StarSMPExpansion(this, starManager).register();
            getLogger().info("PlaceholderAPI detected. Registered placeholders!");
        }

        getLogger().info("StarSMP Enabled successfully. (GemGamemode Structure)");
    }

    @Override
    public void onDisable() {
        getLogger().info("StarSMP: Reverting temporary modifications...");
        if (temporaryBlockListener != null) {
            temporaryBlockListener.revertAll();
        }
        if (cosmicJudgementListener != null) {
            cosmicJudgementListener.cleanup();
        }
        if (spearListener != null) {
            spearListener.cleanup();
        }
        if (ritualManager != null) {
            ritualManager.cleanup();
        }
        getLogger().info("StarSMP Disabled safely.");
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public org.dqrknessid.starsmp.ui.StarSelectionUI getStarSelectionUI() {
        return starSelectionUI;
    }

    public TemporaryBlockListener getTemporaryBlockListener() {
        return temporaryBlockListener;
    }

    public PassiveTickListener getPassiveTickListener() {
        return passiveTickListener;
    }

    public AltarManager getAltarManager() {
        return altarManager;
    }

    public RitualManager getRitualManager() {
        return ritualManager;
    }

    public LegendaryLimitManager getLegendaryLimitManager() {
        return limitManager;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    public AltarCraftingUI getAltarUI() {
        return altarUI;
    }

    public RecipeGUI getRecipeGUI() {
        return recipeGUI;
    }

    public StarManager getStarManager() {
        return starManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return dataManager;
    }

    public PlayerListener getPlayerListener() {
        return playerListener;
    }

    public org.dqrknessid.starsmp.manager.DeathHistoryManager getDeathHistoryManager() {
        return deathHistoryManager;
    }


}
