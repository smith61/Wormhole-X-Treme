/*
 * Wormhole X-Treme Plugin for Bukkit
 * Copyright (C) 2011 Lycano <https://github.com/lycano/Wormhole-X-Treme/>
 *
 * Copyright (C) 2011 Ben Echols
 *                    Dean Bailey
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.luricos.bukkit.WormholeXTreme.Wormhole;

import de.luricos.bukkit.WormholeXTreme.Worlds.handler.WorldHandler;
import de.luricos.bukkit.WormholeXTreme.Wormhole.bukkit.commands.*;
import de.luricos.bukkit.WormholeXTreme.Wormhole.config.ConfigManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.config.Configuration;
import de.luricos.bukkit.WormholeXTreme.Wormhole.exceptions.WormholeNotAvailable;
import de.luricos.bukkit.WormholeXTreme.Wormhole.listeners.*;
import de.luricos.bukkit.WormholeXTreme.Wormhole.logic.StargateHelper;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.Stargate;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateDBManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.model.StargateManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.PermissionBackend;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.PermissionManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.backends.BukkitSupport;
import de.luricos.bukkit.WormholeXTreme.Wormhole.permissions.backends.PermissionsExSupport;
import de.luricos.bukkit.WormholeXTreme.Wormhole.player.WormholePlayerManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.plugin.WormholeWorldsSupport;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.DBUpdateUtil;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Level;

/**
 * WormholeXtreme for Bukkit.
 * 
 * @author Ben Echols (Lologarithm)
 * @author Dean Bailey (alron)
 */
public class WormholeXTreme extends JavaPlugin {

    /** listeners **/
    private static final WormholeXTremePlayerListener playerListener = new WormholeXTremePlayerListener();
    private static final WormholeXTremeBlockListener blockListener = new WormholeXTremeBlockListener();
    private static final WormholeXTremeVehicleListener vehicleListener = new WormholeXTremeVehicleListener();
    private static final WormholeXTremeEntityListener entityListener = new WormholeXTremeEntityListener();
    private static final WormholeXTremeServerListener serverListener = new WormholeXTremeServerListener();
    private static final WormholeXTremeRedstoneListener redstoneListener = new WormholeXTremeRedstoneListener();

    protected PermissionManager permissionManager;
    protected ConfigManager configManager;

    /** The wormhole x treme worlds. */
    private static WorldHandler worldHandler = null;
    
    /** The Scheduler. */
    private static BukkitScheduler scheduler = null;

    private boolean blockPluginExecution = false;

    /* (non-Javadoc)
     * @see org.bukkit.plugin.java.JavaPlugin#onLoad()
     */
    @Override
    public void onLoad() {
        // init the WXTLogger
        WXTLogger.initLogger(this.getDescription().getName(), this.getDescription().getVersion(), ConfigManager.getLogLevel());

        // send welcome message
        WXTLogger.prettyLog(Level.INFO, true, "Loading WormholeXTreme ...");
        
        // set scheduler
        WormholeXTreme.setScheduler(this.getServer().getScheduler());
        
        // Load our config files and set logging level right away.
        ConfigManager.setupConfigs(this.getDescription());

        // prepare for later usage (non-static calls)
        this.configManager = null;
        
        // set logging level after loading config
        WXTLogger.setLogLevel(ConfigManager.getLogLevel());

        // Make sure DB is up to date with latest SCHEMA
        if (!DBUpdateUtil.updateDB()) {
            WXTLogger.prettyLog(Level.SEVERE, false, "Something went wrong during DBUpdate. Please check your server.log for details. Disabling WXT for safety precautions.");
            blockPluginExecution = true;
            return;
        }

        // Load our shapes, stargates, and internal permissions.
        StargateHelper.loadShapes();
        
        if(!(new File(this.getDataFolder()," config.yml").exists())) {
        	this.saveDefaultConfig();
        }

        WXTLogger.prettyLog(Level.INFO, true, "Load complete");
    }
    
    public boolean reloadPlugin() {
        WXTLogger.prettyLog(Level.INFO, true, "Reload in progress...");
        
        // save all gates to sql and save config
        try {
            Configuration.writeFile(getDescription());
            final ArrayList<Stargate> gates = StargateManager.getAllGates();
            // Store all our gates
            for (final Stargate gate : gates) {
                if (gate.isGateActive() || gate.isGateLightsActive()) {
                    gate.shutdownStargate(false);
                }
                StargateDBManager.stargateToSQL(gate);
            }

            WXTLogger.prettyLog(Level.INFO, true, "Configuration written and stargates saved.");
        } catch (final Exception e) {
            WXTLogger.prettyLog(Level.SEVERE, false, "Caught exception while reloading: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        
        // shutdown db
        StargateDBManager.shutdown();
        
        // clear wormholePlayers
        WormholePlayerManager.unregisterAllPlayers();
        
        // disconnect from Worlds
        WormholeWorldsSupport.disableWormholeWorlds();
        
        // load config
        ConfigManager.setupConfigs(this.getDescription());
        
        // set logging level after loading config
        WXTLogger.setLogLevel(ConfigManager.getLogLevel());
        
        // reload stargate shapes
        StargateHelper.reloadShapes();
        
        // Try and attach to Permissions and iConomy and Help
        if (!ConfigManager.isWormholeWorldsSupportEnabled()) {
            WXTLogger.prettyLog(Level.INFO, true, "Wormhole Worlds support disabled in settings.txt, loading stargates and worlds ourself.");
            StargateDBManager.loadStargates(this.getServer());
        }

        // reload permission backend
        this.permissionManager.reset();

        // enable support if configured
        WormholeWorldsSupport.enableWormholeWorlds(true);
        
        // register all players
        WormholePlayerManager.registerAllOnlinePlayers();
        
        WXTLogger.prettyLog(Level.INFO, true, "Reloading complete.");
        return true;
    }    
    
    /* (non-Javadoc)
     * @see org.bukkit.plugin.Plugin#onEnable()
     */
    @Override
    public void onEnable() {
        if (blockPluginExecution) {
            WXTLogger.prettyLog(Level.INFO, true, "Startup is blocked because of a previous database error. Check your server.log");
            return;
        }

        WXTLogger.prettyLog(Level.INFO, true, "Boot sequence initiated...");

        // Try and attach to Permissions and iConomy and Help
        if (!ConfigManager.isWormholeWorldsSupportEnabled()) {
            WXTLogger.prettyLog(Level.INFO, true, "Wormhole Worlds support disabled in settings.txt, loading stargates and worlds by our self.");
            StargateDBManager.loadStargates(this.getServer());
        }

        try {
            // register Permission backends
            PermissionBackend.registerBackendAlias("pex", PermissionsExSupport.class);
            PermissionBackend.registerBackendAlias("bukkit", BukkitSupport.class);

            // resolve currently used PermissionPlugin
            this.resolvePermissionBackends();

            // init permissionManager; backend is set via config static call
            if (this.permissionManager == null) {
                this.permissionManager = new PermissionManager(this.configManager);
            }

            if (ConfigManager.isWormholeWorldsSupportEnabled()) {
                WormholeWorldsSupport.enableWormholeWorlds();
            }
        } catch (final Exception e) {
            // @TODO change this behavior to be more error friendly (skip gate instead)
            // Catched when a world is not loaded but a gate is in that world.
            // The plugin would stop working to prevent data corruption (safe-mode)
            WXTLogger.prettyLog(Level.SEVERE, false, "Caught Exception while trying to load support plugins. {" + e.getMessage() + "}");
            e.printStackTrace();
        }
        
        registerEvents(true);
        if (!ConfigManager.isWormholeWorldsSupportEnabled()) {
            registerEvents(false);
            registerCommands();
        }
        
        // register all online players onEnable/onReload
        WormholePlayerManager.registerAllOnlinePlayers();
        
        WXTLogger.prettyLog(Level.INFO, true, "Boot sequence completed");
    }   
    
    /* (non-Javadoc)
     * @see org.bukkit.plugin.Plugin#onDisable()
     */
    @Override
    public void onDisable() {
        if (blockPluginExecution) {
            WXTLogger.prettyLog(Level.INFO, true, "Disable Functions skipped because of a previous error.");
            return;
        }

        WXTLogger.prettyLog(Level.INFO, true, "Shutdown sequence initiated...");
        
        try {
            Configuration.writeFile(getDescription());
            final ArrayList<Stargate> gates = StargateManager.getAllGates();

            // Store all our gates
            for (final Stargate gate : gates) {
                if (gate.isGateActive() || gate.isGateLightsActive()) {
                    gate.shutdownStargate(false);
                }

                WXTLogger.prettyLog(Level.FINE, false, "Saving gate: '" + gate.getGateName() + "', GateFace: '" + gate.getGateFacing().name() + "'");

                StargateDBManager.stargateToSQL(gate);
            }

            StargateDBManager.shutdown();

            // remove permissionManager instance
            if (this.permissionManager != null) {
                this.permissionManager.end();
            }

            // clear wormholePlayers
            WormholePlayerManager.unregisterAllPlayers();            
            
            WXTLogger.prettyLog(Level.INFO, true, "Successfully shutdown WXT.");
        } catch (final Exception e) {
            WXTLogger.prettyLog(Level.SEVERE, false, "Caught exception while shutting down: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get the permissionManager.
     * 
     * @return the PermissionManager
     */
    public static PermissionManager getPermissionManager() {
        try {
            if (!isPluginAvailable()) {
                if (WXTLogger.getLogLevel().intValue() < Level.WARNING.intValue())
                    throw new WormholeNotAvailable("This plugin is not ready yet." + ((!getThisPlugin().isEnabled()) ? " Loading sequence is still in progress." : ""));
            }
        } catch (WormholeNotAvailable e) {
            WXTLogger.prettyLog(Level.WARNING, false, e.getMessage());
        }

        return ((WormholeXTreme) getThisPlugin()).permissionManager;
    }

    /**
     * Resolve permissions plugin
     *
     * first enabled will be used.
     * Config node permissions.backend will be set to linked backend
     */
    private void resolvePermissionBackends() {
        for (String providerAlias : PermissionBackend.getRegisteredAliases()) {
            String pluginName = PermissionBackend.getBackendPluginName(providerAlias);
            WXTLogger.prettyLog(Level.INFO, false, "Attempting to use supported permissions plugin '" + pluginName + "'");

            Plugin permToLoad = Bukkit.getPluginManager().getPlugin(pluginName);
            if ((pluginName.equals(PermissionBackend.getDefaultBackend().getProviderName())) || ((permToLoad != null) && (permToLoad.isEnabled()))) {
                ConfigManager.setPermissionBackend(providerAlias);
                WXTLogger.prettyLog(Level.INFO, false, "Config node PERMISSIONS_BACKEND changed to '" + providerAlias + "'");
                return;
            } else {
                WXTLogger.prettyLog(Level.FINE, false, "Permission backend '" + providerAlias + "' was not found as plugin or not enabled!");
            }
        }
    }

    /**
     * Gets the scheduler.
     * 
     * @return the scheduler
     */
    public static BukkitScheduler getScheduler() {
        return scheduler;
    }

    /**
     * Gets the plugin.
     * 
     * @return the plugin instance
     */
    public static WormholeXTreme getThisPlugin() {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("WormholeXTreme");
        if (plugin == null || !(plugin instanceof WormholeXTreme)) {
            throw new RuntimeException("'WormholeXTreme' not found. 'WormholeXTreme' plugin disabled?");
        }

        return ((WormholeXTreme) plugin);
    }

    /**
     * Gets the wormhole x treme worlds.
     * 
     * @return the wormhole x treme worlds
     */
    public static WorldHandler getWorldHandler() {
        return worldHandler;
    }

    /**
     * Register commands.
     */
    public static void registerCommands() {
        final WormholeXTreme tp = getThisPlugin();
        tp.getCommand("wxforce").setExecutor(new Force());
        tp.getCommand("wxidc").setExecutor(new WXIDC());
        tp.getCommand("wxcompass").setExecutor(new Compass());
        tp.getCommand("wxcomplete").setExecutor(new Complete());
        tp.getCommand("wxremove").setExecutor(new WXRemove());
        tp.getCommand("wxlist").setExecutor(new WXList());
        tp.getCommand("wxgo").setExecutor(new Go());
        tp.getCommand("dial").setExecutor(new Dial());
        tp.getCommand("wxbuild").setExecutor(new Build());
        tp.getCommand("wxbuildlist").setExecutor(new BuildList());
        tp.getCommand("wormhole").setExecutor(new Wormhole());
        tp.getCommand("wxreload").setExecutor(new WXReload());
        tp.getCommand("wxstatus").setExecutor(new WXStatus());
    }

    /**
     * Register events.
     */
    public static void registerEvents(boolean critical) {
        WormholeXTreme wxt = getThisPlugin();
        if (critical) {
            // Listen for enable/disable events (MONITOR)
            Bukkit.getServer().getPluginManager().registerEvents(serverListener, wxt);
        } else {
            // Listen on Block events (NORMAL)
            Bukkit.getServer().getPluginManager().registerEvents(blockListener, wxt);

            // Listen on Player events (NORMAL)
            Bukkit.getServer().getPluginManager().registerEvents(playerListener, wxt);

            // redstone listener (NORMAL)
            Bukkit.getServer().getPluginManager().registerEvents(redstoneListener, wxt);

            // Handle minecarts going through portal (NORMAL)
            Bukkit.getServer().getPluginManager().registerEvents(vehicleListener, wxt);

            // Handle player walking through the lava (NORMAL)
            // Handle Creeper explosions damaging Gate components.
            Bukkit.getServer().getPluginManager().registerEvents(entityListener, wxt);
        }
    }

    /**
     * Sets the scheduler.
     * 
     * @param scheduler
     *            the new scheduler
     */
    protected static void setScheduler(BukkitScheduler scheduler) {
        WormholeXTreme.scheduler = scheduler;
    }

    /**
     * Sets the wormhole x treme worlds.
     * 
     * @param worldHandler
     *            the new wormhole x treme worlds
     */
    public static void setWorldHandler(WorldHandler worldHandler) {
        WormholeXTreme.worldHandler = worldHandler;
    }

    public static boolean isPluginAvailable() {
        Plugin plugin = getThisPlugin();

        return (plugin instanceof WormholeXTreme) && ((WormholeXTreme) plugin).permissionManager != null;
    }
}
