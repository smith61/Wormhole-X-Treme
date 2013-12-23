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
package de.luricos.bukkit.WormholeXTreme.Wormhole.model;

import de.luricos.bukkit.WormholeXTreme.Wormhole.WormholeXTreme;
import de.luricos.bukkit.WormholeXTreme.Wormhole.logic.StargateUpdateRunnable;
import de.luricos.bukkit.WormholeXTreme.Wormhole.logic.StargateUpdateRunnable.ActionToTake;
import de.luricos.bukkit.WormholeXTreme.Wormhole.player.WormholePlayerManager;
import de.luricos.bukkit.WormholeXTreme.Wormhole.utils.WXTLogger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * WormholeXtreme Stargate Manager.
 * 
 * @author Ben Echols (Lologarithm)
 */
public class StargateManager {
    // A list of all blocks contained by all stargates. Makes for easy indexing when a player is trying
    // to enter a gate or if water is trying to flow out, also will contain the stone buttons used to activate.

    /** The all_gate_blocks. */
    private static Map<Location, Stargate> allGateBlocks = new HashMap<Location, Stargate>();
    // List of All stargates indexed by name. Useful for dialing and such
    /** The stargate_list. GateName, Stargate*/
    private static Map<String, Stargate> stargateList = new HashMap<String, Stargate>();
    // List of stargates built but not named. Indexed by the player that built it.
    /** The incomplete_stargates. PlayerName, Stargate*/
    private static Map<String, Stargate> incompleteStargates = new HashMap<String, Stargate>();
    // List of stargates that have been activated but not yet dialed. Only used for gates without public use sign.
    /** The activated_stargates. GateName, Stargate*/
    private static Map<String, Stargate> activatedStargates = new HashMap<String, Stargate>();
    // List of networks indexed by their name
    /** The stargate_networks. NetworkName, StargateNetwork*/
    private static Map<String, StargateNetwork> stargateNetworks = new HashMap<String, StargateNetwork>();
    // List of players ready to build a stargate, with the shape they are trying to build.
    /** The player_builders. PlayerName, StargateShape*/
    private static Map<String, StargateShape> playerBuilders = new HashMap<String, StargateShape>();
    // List of blocks that are part of an active animation. Only use this to make sure water doesn't flow everywhere.
    /** The Constant opening_animation_blocks. */
    private static Map<Location, Block> openingAnimationBlocks = new HashMap<Location, Block>();

    /**
     * This method adds a stargate that has been activated but not dialed by a player.
     * 
     * @param gateName Stargate's gateName
     * @param s The gate the player has activated.
     */
    public static void addActivatedStargate(String gateName, Stargate s) {
        if (!hasActivatedStargate(gateName))
            getActivatedStargates().put(gateName, s);
    }
    
    public static void addActivatedStargate(Stargate s) {
        addActivatedStargate(s.getGateName(), s);
    }

    public static boolean hasActivatedStargate(String gateName) {
        return getActivatedStargates().containsKey(gateName);

    }
    
    public static boolean hasActivatedStargate(Stargate s) {
        return hasActivatedStargate(s.getGateName());
    }
    
    /**
     * This method adds an index mapping block location to stargate.
     * NOTE: This method does not verify that the block is part of the gate,
     * so it may not persist and won't be removed by removing the stargate. This can cause a gate to stay in memory!!!
     * 
     * @param b the b
     * @param s the s
     */
    public static void addBlockIndex(Block b, Stargate s) {
        if ((b != null) && (s != null)) {
            getAllGateBlocks().put(b.getLocation(), s);
        }
    }

    /**
     * Adds the gate to network.
     * 
     * @param gate
     *            the gate
     * @param network
     *            the network
     */
    public static void addGateToNetwork(Stargate gate, String network) {
        if (!getStargateNetworks().containsKey(network)) {
            addStargateNetwork(network);
        }

        StargateNetwork net;
        if ((net = getStargateNetworks().get(network)) != null) {
            synchronized (net.getNetworkGateLock()) {
                net.getNetworkGateList().add(gate);
                if (gate.isGateSignPowered()) {
                    net.getNetworkSignGateList().add(gate);
                }
            }
        }
    }

    /**
     * Adds a gate indexed by the player that hasn't yet been named and completed.
     * 
     * @param playerName The player
     * @param stargate The Stargate
     */
    public static void addIncompleteStargate(String playerName, Stargate stargate) {
        getIncompleteStargates().put(playerName, stargate);
    }

    /**
     * Adds the player builder shape.
     * 
     * @param playerName the playerName
     * @param shape
     *            the shape
     */
    public static void addPlayerBuilderShape(String playerName, StargateShape shape) {
        getPlayerBuilders().put(playerName, shape);
    }

    /**
     * Adds the given stargate to the list of stargates. Also adds all its blocks to big block index.
     * 
     * Returns false if another gate shares at least one block with another gate
     * 
     * @param s The Stargate you want added.
     * 
     * @return If the gate was added.
     */
    protected static boolean addStargate(Stargate s) {
    	// Start Smith_61 Edit
    	
    	for(Location b : s.getGateStructureBlocks()) {
    		if(getAllGateBlocks().get(b) != null && getAllGateBlocks().get(b) != s) {
    			return false;
    		}
    	}
    	
    	for(Location b : s.getGatePortalBlocks()) {
    		if(getAllGateBlocks().get(b) != null && getAllGateBlocks().get(b) != s) {
    			return false;
    		}
    	}
    	
    			
        getStargateList().put(s.getGateName(), s);
        
        for (Location b : s.getGateStructureBlocks()) {
            getAllGateBlocks().put(b, s);
        }
        
        for (Location b : s.getGatePortalBlocks()) {
            getAllGateBlocks().put(b, s);
        }
        
        return true;
        
        // End Smith_61 Edit
    }

    // Network functions
    /**
     * Adds the stargate network.
     * 
     * @param networkName the networkName
     * @return the stargate network
     */
    public static StargateNetwork addStargateNetwork(String networkName) {
        if (getStargateNetworks().containsKey(networkName))
            return getStargateNetworks().get(networkName);
        
        StargateNetwork sn = new StargateNetwork();
        sn.setNetworkName(networkName);
        getStargateNetworks().put(networkName, sn);
        
        return sn;
    }

    /**
     * Complete stargate.
     * 
     * @param playerName the playerName
     * @param stargate the stargate
     * @return true on success
     */
    public static boolean completeStargate(String playerName, Stargate stargate) {
        Stargate posDupe = StargateManager.getStargate(stargate.getGateName());
        if (posDupe != null)
            return false;
        
        // Start Smith_61 Edit
        stargate.setGateName(stargate.getGateName());
        
        
        if(addStargate(stargate)) {
        	stargate.setGateOwner(playerName);
        	stargate.completeGate(stargate.getGateName(), "");
        	
        	WXTLogger.prettyLog(Level.INFO, false, "Player: " + playerName + " completed a wormhole: " + stargate.getGateName());
        	
        	StargateDBManager.stargateToSQL(stargate);
        	
        	return true;
        }
        else {
        	Player player = Bukkit.getPlayer(playerName);
        	if(player != null) {
        		player.sendMessage(ChatColor.RED + "Stargate shares blocks with another gate.");
        	}
        	
        	return false;
        }
        
        // End Smith_61 Edit
    }
    
    /**
     * Complete stargate
     * 
     * @param player
     * @param stargate
     * 
     * @return true on success
     */
    public static boolean completeStargate(Player player, Stargate stargate) {
        return completeStargate(player.getName(), stargate);
    }

    /**
     * Complete stargate.
     * 
     * @param player the player
     * @param gateName the name
     * @param idc the idc
     * @param network the network
     * @return true on success
     */
    public static boolean completeStargate(Player player, String gateName, String idc, String network) {
        return completeStargate(player.getName(), gateName, idc, network);
    }
    
    /**
     * Complete stargate
     * 
     * @param playerName the playerName
     * @param gateName the gateName
     * @param idc the idc
     * @param network the network
     * 
     * @return true on success
     */
    public static boolean completeStargate(String playerName, String gateName, String idc, String network) {
        final Stargate complete = getIncompleteStargates().remove(playerName);
        
        if (complete != null) {
        	// Start Smith_61 Edit
        	
        	complete.setGateName(gateName);
        	
        	if(addStargate(complete)) {
                complete.setGateOwner(playerName);
                complete.completeGate(gateName, idc);
                
	            if (!network.equals("")) {
	                StargateNetwork net = StargateManager.getStargateNetwork(network);
	                if (net == null) {
	                    net = StargateManager.addStargateNetwork(network);
	                }
	                StargateManager.addGateToNetwork(complete, network);
	                complete.setGateNetwork(net);
	            }
	            
	            WXTLogger.prettyLog(Level.INFO, false, "Player: " + playerName + " completed a wormhole: " + complete.getGateName());
	            
	            StargateDBManager.stargateToSQL(complete);
	            
	            return true;
        	}
        	else {
        		Player player = Bukkit.getPlayer(playerName);
        		if(player != null) {
        			player.sendMessage(ChatColor.RED + "Stargate shares blocks with another stargate.");
        		}
        		
        		return false;
        	}
        	
        	
        	// End Smith_61 Edit
        }

        return false;        
    }

    /**
     * Distance to closest stargate block.
     * 
     * @param self Location of the local object.
     * @param stargate Stargate to check blocks for distance.
     * @return square of distance to the closest stargate block.
     */
    public static double distanceSquaredToClosestGateBlock(Location self, Stargate stargate) {
        double distance = Double.MAX_VALUE;
        
        if ((stargate != null) && (self != null)) {
            ArrayList<Location> gateblocks = stargate.getGateStructureBlocks();
            for (Location l : gateblocks) {
                double blockdistance = getSquaredDistance(self, l);
                if (blockdistance < distance) {
                    distance = blockdistance;
                }
            }
        }
        
        return distance;
    }

    /**
     * Find the closest stargate.
     * 
     * @param self Location of the local object.
     * @return The closest stargate to the local object.
     */
    public static Stargate findClosestStargate(Location self) {
        Stargate stargate = null;
        
        if (self != null) {
            ArrayList<Stargate> gates = StargateManager.getAllGates();
            double man = Double.MAX_VALUE;
            
            for (Stargate s : gates) {
                Location t = s.getGatePlayerTeleportLocation();
                double distance = getSquaredDistance(self, t);
                if (distance < man) {
                    man = distance;
                    stargate = s;
                }
            }
        }
        
        return stargate;
    }

    /**
     * Gets the activated stargates.
     * 
     * @return the activated stargates
     */
    private static HashMap<String, Stargate> getActivatedStargates() {
        return (HashMap<String, Stargate>) activatedStargates;
    }

    /**
     * Gets all gate blocks.
     * 
     * @return all gate blocks
     */
    private static HashMap<Location, Stargate> getAllGateBlocks() {
        return (HashMap<Location, Stargate>) allGateBlocks;
    }

    /**
     * Get all gates.
     * This is more expensive than some other methods so it probably shouldn't be called a lot.
     * 
     * @return the array list
     */
    public static ArrayList<Stargate> getAllGates() {
        ArrayList<Stargate> gates = new ArrayList<Stargate>();

        for (Stargate s : getStargateList().values()) {
            gates.add(s);
        }

        return gates;
    }

    /**
     * Gets the gate from block.
     * 
     * @param block the block
     * @return the gate from block
     */
    public static Stargate getGateFromBlock(Block block) {
        if (getAllGateBlocks().containsKey(block.getLocation())) {
            return getAllGateBlocks().get(block.getLocation());
        }

        return null;
    }

    /**
     * Gets the incomplete stargates.
     * 
     * @return the incomplete stargates
     */
    private static HashMap<String, Stargate> getIncompleteStargates() {
        return (HashMap<String, Stargate>) incompleteStargates;
    }

    /**
     * Gets the opening animation blocks.
     * 
     * @return the opening animation blocks
     */
    protected static HashMap<Location, Block> getOpeningAnimationBlocks() {
        return (HashMap<Location, Block>) openingAnimationBlocks;
    }

    /**
     * Gets the player builders.
     * 
     * @return the player builders
     */
    private static HashMap<String, StargateShape> getPlayerBuilders() {
        return (HashMap<String, StargateShape>) playerBuilders;
    }

    /**
     * Gets the player builder shape.
     * 
     * @param player the player
     * @return the stargate shape
     */
    public static StargateShape getPlayerBuilderShape(Player player) {
        return getPlayerBuilderShape(player.getName());
    }
    
    /**
     * Gets the player builder shape.
     * 
     * @param playerName the player
     * @return the stargate shape
     */
    public static StargateShape getPlayerBuilderShape(String playerName) {
        if (getPlayerBuilders().containsKey(playerName)) {
            return getPlayerBuilders().remove(playerName);
        }
        
        return null;
    }    
    
    

    /**
     * Gets the square of the distance between self and target
     * which saves the costly call to {@link Math#sqrt(double)}.
     * 
     * @param self Location of the local object.
     * @param target Location of the target object.
     * @return square of distance to target object from local object.
     */
    private static double getSquaredDistance(Location self, Location target) {
        double distance = Double.MAX_VALUE;
        if ((self != null) && (target != null)) {
            distance = Math.pow(self.getX() - target.getX(), 2) + Math.pow(self.getY() - target.getY(), 2) + Math.pow(self.getZ() - target.getZ(), 2);
        }
        return distance;
    }

    /**
     * Gets a stargate based on the name passed in. Returns null if there is no gate by that name.
     * 
     * @param gateName String name of the Stargate you want returned.
     * @return Stargate requested. Null if no stargate by that name.
     */
    public static Stargate getStargate(String gateName) {
        if (getStargateList().containsKey(gateName)) {
            return getStargateList().get(gateName);
        }
        
        return null;
    }

    /**
     * Gets the stargate list.
     * 
     * @return the stargate list
     */
    private static HashMap<String, Stargate> getStargateList() {
        return (HashMap<String, Stargate>) stargateList;
    }

    /**
     * Gets the stargate network.
     * 
     * @param name the name
     * @return the stargate network
     */
    public static StargateNetwork getStargateNetwork(String name) {
        if (getStargateNetworks().containsKey(name)) {
            return getStargateNetworks().get(name);
        }
        
        return null;
    }

    /**
     * Gets the stargate networks.
     * 
     * @return the stargate networks
     */
    private static HashMap<String, StargateNetwork> getStargateNetworks() {
        return (HashMap<String, StargateNetwork>) stargateNetworks;
    }

    /**
     * Checks if is block in gate.
     * 
     * @param block the block
     * @return If block is a "gate" block it returns true.
     *         This is useful to stop damage from being applied from an underpriveledged user.
     *         Also used to stop flow of water, and prevent portal physics
     */
    public static boolean isBlockInGate(Block block) {
        return isLocationInGate(block.getLocation());
    }

    /**
     * Checks if the given location is in gate range
     *
     * @param loc
     * @return same as isBlockInGate; instead of a block the location is used
     */
    public static boolean isLocationInGate(Location loc) {
        return getAllGateBlocks().containsKey(loc) || getOpeningAnimationBlocks().containsKey(loc);
    }

    /**
     * Checks if is stargate.
     * 
     * @param gateName the name
     * @return true, if is stargate
     */
    public static boolean isStargate(String gateName) {
        return getStargateList().containsKey(gateName);
    }

    /**
     * Removes the stargate by gateName.
     * 
     * @param gateName the gate name
     * @return Stargate that was activated. Null if no active gate present.
     */
    public static Stargate removeActivatedStargate(String gateName) {
        return getActivatedStargates().remove(gateName);
    }

    /**
     * This method removes an index mapping block location to stargate.
     * NOTE: This method does not verify that the block has actually been removed from a gate
     * so it may not persist and can be readded when server is restarted.
     * 
     * @param block the b
     */
    public static void removeBlockIndex(Block block) {
        if (block != null) {
            getAllGateBlocks().remove(block.getLocation());
        }
    }

    /**
     * Removes an incomplete stargate from the list.
     * 
     * @param player The player who created the gate.
     */
    public static void removeIncompleteStargate(Player player) {
        removeIncompleteStargate(player.getName());
    }
    
    /**
     * Remove an incomplete stargate from the list by playerName
     * 
     * @param playerName 
     */
    public static void removeIncompleteStargate(String playerName) {
        getIncompleteStargates().remove(playerName);
    }

    /**
     * Removes the stargate from the list of stargates.
     * Also removes all blocks from this gate.
     * 
     * @param s The gate to be removed.
     */
    public static void removeStargate(Stargate s) {
        getStargateList().remove(s.getGateName());

        if (WormholePlayerManager.findPlayerByGateName(s.getGateName()) != null)
            WormholePlayerManager.findPlayerByGateName(s.getGateName()).removeStargate(s);

        StargateDBManager.removeStargateFromSQL(s);
        if (s.getGateNetwork() != null) {
            synchronized (s.getGateNetwork().getNetworkGateLock()) {
                s.getGateNetwork().getNetworkGateList().remove(s);
                if (s.isGateSignPowered()) {
                    s.getGateNetwork().getNetworkSignGateList().remove(s);
                }

                for (Stargate s2 : s.getGateNetwork().getNetworkSignGateList()) {
                    if ((s2.getGateDialSignTarget() != null) && (s2.getGateDialSignTarget().getGateId() == s.getGateId()) && s2.isGateSignPowered()) {
                        s2.setGateDialSignTarget(null);
                        if (s.getGateNetwork().getNetworkSignGateList().size() > 1) {
                            s2.setGateDialSignIndex(0);
                            WormholeXTreme.getScheduler().scheduleSyncDelayedTask(WormholeXTreme.getThisPlugin(), new StargateUpdateRunnable(s2, ActionToTake.DIAL_SIGN_CLICK));
                        }
                    }
                }
            }
        }

        for (Location b : s.getGateStructureBlocks()) {
            getAllGateBlocks().remove(b);
        }

        for (Location b : s.getGatePortalBlocks()) {
            getAllGateBlocks().remove(b);
        }        
    }
    
    /**
     * Get Stargate by Player
     * 
     * @param player the player instance
     * @return Stargate instance
     */
    public static Stargate getStargateByPlayer(Player player) {
        return getStargateByPlayer(player.getName());
    }
    
    /**
     * Get Stargate by playeName
     * 
     * @param playerName the playerName
     * 
     * @return Stargate instance
     */
    public static Stargate getStargateByPlayer(String playerName) {
        return getActivatedStargates().get(playerName);
    }
}