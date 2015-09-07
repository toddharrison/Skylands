package com.goodformentertainment.canary.sky;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.goodformentertainment.canary.playerstate.hook.WorldDeathHook;
import com.goodformentertainment.canary.playerstate.hook.WorldEnterHook;
import com.goodformentertainment.canary.playerstate.hook.WorldExitHook;
import com.goodformentertainment.canary.sky.dao.SkylandsPlayer;
import com.goodformentertainment.canary.zown.Flag;
import com.goodformentertainment.canary.zown.api.IConfiguration;
import com.goodformentertainment.canary.zown.api.IZown;
import com.goodformentertainment.canary.zown.api.IZownManager;
import com.goodformentertainment.canary.zown.api.Point;
import com.goodformentertainment.canary.zown.api.impl.Tree;

import net.canarymod.Canary;
import net.canarymod.api.entity.living.humanoid.Player;
import net.canarymod.api.inventory.Item;
import net.canarymod.api.inventory.ItemType;
import net.canarymod.api.world.World;
import net.canarymod.api.world.blocks.Block;
import net.canarymod.api.world.blocks.BlockType;
import net.canarymod.api.world.position.Location;
import net.canarymod.database.exceptions.DatabaseReadException;
import net.canarymod.database.exceptions.DatabaseWriteException;
import net.canarymod.hook.HookHandler;
import net.canarymod.hook.player.ItemUseHook;
import net.canarymod.hook.player.PlayerRespawnedHook;
import net.canarymod.plugin.PluginListener;
import net.canarymod.plugin.Priority;

public class SkylandsPlayerManager implements PluginListener {
    private final SkylandsConfig config;
    private final SkylandsWorldManager worldManager;
    private final SkylandsIslandManager islandManager;
    // private final SkylandsChallengeManager challengeManager;
    private final Map<String, SkylandsPlayer> players;
    // private final Collection<String> deadPlayers;

    private final IZownManager zownManager;

    // public SkylandsPlayerManager(final SkylandsConfig config, final SkylandsWorldManager
    // worldManager,
    // final SkylandsIslandManager islandManager, final XChallengeManager challengeManager,
    // final IZownManager zownManager) {
    public SkylandsPlayerManager(final SkylandsConfig config,
            final SkylandsWorldManager worldManager, final SkylandsIslandManager islandManager,
            final SkylandsChallengeManager challengeManager, final IZownManager zownManager) {
        this.config = config;
        this.worldManager = worldManager;
        this.islandManager = islandManager;
        // this.challengeManager = challengeManager;
        challengeManager.setPlayerManager(this);
        this.zownManager = zownManager;
        // deadPlayers = Collections.synchronizedCollection(new HashSet<String>());
        players = new HashMap<String, SkylandsPlayer>();
    }

    public Set<String> getActivePlayerIds() {
        return players.keySet();
    }

    public SkylandsPlayer getXPlayer(final Player player) {
        SkylandsPlayer xPlayer = null;
        if (player != null) {
            xPlayer = players.get(player.getUUIDString());
        }
        return xPlayer;
    }

    public Location getIslandLocation(final Player player)
            throws DatabaseReadException, DatabaseWriteException {
        SkylandsPlugin.LOG.debug("GETTING ISLAND LOCATION");

        final World world = worldManager.getWorld();
        final SkylandsPlayer xPlayer = addPlayer(player);
        Location location = xPlayer.getLocation();
        if (location == null) {
            SkylandsPlugin.LOG.debug("CREATING ISLAND");

            // Spiral algorithm for island placement (-1 starting index is at 0,0)
            // TODO combine into common method in IslandManager
            // TODO updated to start count at 1 instead of 0 (change 1 to 2 below)
            final Point islandRelativePoint = islandManager
                    .getIslandSpiralLocation(xPlayer.islandId - 1);
            final int x = islandRelativePoint.x * config.getMaxSize()
                    + SkylandsIslandManager.xOffset;
            final int y = config.getHeight();
            final int z = islandRelativePoint.z * config.getMaxSize()
                    + SkylandsIslandManager.zOffset;
            islandManager.generateIsland(world, x, y, z);
            location = new Location(world, x, y + 5, z - 1, 0, 0);

            // Create island zown if it doesn't already exist
            Tree<? extends IZown> playerZown = zownManager.getZown(location);
            if (playerZown == zownManager.getZown(world)) {
                // Location is in the world zown, create player zown
                final int zownRadius = (config.getMaxSize() - 10) / 2;
                final Point minPoint = new Point(x - zownRadius, -100, z - zownRadius);
                final Point maxPoint = new Point(x + zownRadius, 255, z + zownRadius);

                // final String name = "Skylands_Player_" + player.getUUIDString();
                String name = player.getName();

                Tree<? extends IZown> existingZown = zownManager.getZown(world, name);
                if (existingZown != null) {
                    if (existingZown.getData().isOwner(player)) {
                        // Remove the player's zown if it exists somewhere else in the world
                        zownManager.removeZown(world, name);
                    } else {
                        // Zown name conflict, choose a unique one
                        int count = 1;
                        while (existingZown != null) {
                            name = player.getName() + count++;
                            existingZown = zownManager.getZown(world, name);
                        }
                    }
                }

                playerZown = zownManager.createZown(world, name, null, minPoint, maxPoint);
                final IConfiguration playerZownConfig = playerZown.getData().getConfiguration();
                playerZownConfig.addCommandRestriction("/spawn");
                // playerZownConfig.addCommandRestriction("/sethome");
                // playerZownConfig.addCommandRestriction("/home");
                // playerZownConfig.setFlag(Flag.playerexit.name(), false);

                playerZown.getData().addOwner(player);
                playerZownConfig.setFlag(Flag.build.name(), false);
                playerZownConfig.setFlag(Flag.interact.name(), false);
                playerZownConfig.setFlag(Flag.playerimmune.name(), false);
                playerZownConfig.setFlag(Flag.villagerimmune.name(), false);
                playerZownConfig.setFlag(Flag.playerclaim.name(), false);
                playerZownConfig.setFlag(Flag.mobgrief.name(), false);
                playerZownConfig.setFlag(Flag.pvp.name(), false);

                SkylandsPlugin.LOG.debug("Created Skylands player zown");
                if (!zownManager.saveZownConfiguration(world, name)) {
                    SkylandsPlugin.LOG.error("Error saving Skylands player zown");
                }
            }
        }
        return location;
    }

    public SkylandsPlayer addPlayer(final Player player)
            throws DatabaseReadException, DatabaseWriteException {
        final SkylandsPlayer xPlayer = SkylandsPlayer.getXPlayer(player);
        players.put(player.getUUIDString(), xPlayer);
        return xPlayer;
    }

    public SkylandsPlayer removePlayer(final Player player) {
        return players.remove(player.getUUIDString());
    }

    public void persist(final SkylandsPlayer xPlayer) throws DatabaseWriteException {
        xPlayer.update();
    }

    // @HookHandler(priority = Priority.PASSIVE)
    // public void onDeath(final PlayerDeathHook hook)
    // throws DatabaseReadException, DatabaseWriteException {
    // final Player player = hook.getPlayer();
    // final World world = player.getWorld();
    // if (world == worldManager.getWorld()) {
    // // final SkylandsPlayer xPlayer = SkylandsPlayer.getXPlayer(player);
    // // if (!xPlayer.practice) {
    // // deadPlayers.add(player.getUUIDString());
    // //
    // // xPlayer.setLocation(null);
    // // challengeManager.resetMenu(player);
    // // xPlayer.challengesCompleted.clear();
    // // persist(xPlayer);
    // // islandManager.clearIsland(world, player, xPlayer.islandId);
    // //
    // // SkylandsPlugin.LOG.debug("DIED, CLEAR ISLAND");
    // // }
    // }
    // }

    @HookHandler(priority = Priority.PASSIVE)
    public void onSkylandsDeath(final WorldDeathHook hook)
            throws DatabaseReadException, DatabaseWriteException {
        if (hook.getDeathLocation().getWorld() == worldManager.getWorld()) {
            final Player player = hook.getPlayer();

            hook.setSpawnLocation(player.getHome());

            // final SkylandsPlayer xPlayer = SkylandsPlayer.getXPlayer(player);
            // hook.setSpawnLocation(xPlayer.getReturnLocation());
            // SkylandsPlugin.LOG.debug("RESET DEATH RESPAWN LOCATION " +
            // xPlayer.getReturnLocation());
        }
    }

    @HookHandler(priority = Priority.PASSIVE)
    public void onSkylandRespawned(final PlayerRespawnedHook hook) {
        final Location location = hook.getLocation();
        if (location.getWorld() == worldManager.getWorld()) {
            final Player player = hook.getPlayer();

            // Enable player flight
            player.getCapabilities().setMayFly(true);
            player.getCapabilities().setFlying(true);
            player.updateCapabilities();
        }
    }

    @HookHandler(priority = Priority.PASSIVE)
    public void onWorldEnter(final WorldEnterHook hook)
            throws DatabaseReadException, DatabaseWriteException {
        if (hook.getWorld() == worldManager.getWorld()) {
            final Player player = hook.getPlayer();
            final SkylandsPlayer xPlayer = addPlayer(player);

            final Location fromLocation = hook.getFromLocation();
            if (fromLocation != null) {
                xPlayer.setReturnLocation(fromLocation);
                persist(xPlayer);
            }

            // Enable player flight
            player.getCapabilities().setMayFly(true);
            player.getCapabilities().setFlying(true);
            player.updateCapabilities();

            SkylandsPlugin.LOG.debug(player.getName() + " entered Skylands");
        }
    }

    @HookHandler(priority = Priority.PASSIVE)
    public void onWorldExit(final WorldExitHook hook)
            throws DatabaseReadException, DatabaseWriteException {
        if (hook.getWorld() == worldManager.getWorld()) {
            final Player player = hook.getPlayer();
            final SkylandsPlayer xPlayer = removePlayer(player);

            // if (!deadPlayers.remove(player.getUUIDString())) {
            final Location fromLocation;
            // if (xPlayer.practice) {
            fromLocation = player.getHome();
            // } else {
            // fromLocation = hook.getFromLocation();
            // }
            if (fromLocation != null) {
                xPlayer.setLocation(fromLocation);
                persist(xPlayer);
            }
            // }

            // Disable player flight
            player.getCapabilities().setMayFly(false);
            player.getCapabilities().setFlying(false);
            player.updateCapabilities();

            SkylandsPlugin.LOG.debug(player.getName() + " left Skylands");
        }
    }

    // @HookHandler(priority = Priority.NORMAL)
    // public void onHealthChange(final HealthChangeHook hook) {
    // final Player player = hook.getPlayer();
    // if (player.getWorld() == worldManager.getWorld()) {
    // final SkylandsPlayer xPlayer = getXPlayer(player);
    // if (xPlayer != null && !xPlayer.practice) {
    // final float oldHealth = hook.getOldValue();
    // final float newHealth = hook.getNewValue();
    // if (oldHealth > 0 && oldHealth < newHealth) {
    // hook.setCanceled();
    // }
    // }
    // }
    // }

    /**
     * Convert obsidian into a lava bucket when clicked on by an empty bucket.
     */
    @HookHandler(priority = Priority.NORMAL)
    public void onItemUse(final ItemUseHook hook) {
        final Player player = hook.getPlayer();
        if (player.getWorld() == worldManager.getWorld()) {
            final Block block = hook.getBlockClicked();
            if (block != null && block.getType() == BlockType.Obsidian) {
                final Item item = hook.getItem();
                if (item.getType() == ItemType.Bucket) {
                    block.setType(BlockType.Air);
                    block.update();
                    player.getInventory().removeItem(item);
                    player.giveItem(Canary.factory().getItemFactory().newItem(ItemType.LavaBucket));
                    hook.setCanceled();
                }
            }
        }
    }
}
