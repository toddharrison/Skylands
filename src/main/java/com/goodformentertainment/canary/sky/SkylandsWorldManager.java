package com.goodformentertainment.canary.sky;

import com.goodformentertainment.canary.playerstate.PlayerStatePlugin;
import com.goodformentertainment.canary.playerstate.api.IWorldStateManager;
import com.goodformentertainment.canary.playerstate.api.SaveState;
import com.goodformentertainment.canary.zown.Flag;
import com.goodformentertainment.canary.zown.api.IConfiguration;
import com.goodformentertainment.canary.zown.api.IZown;
import com.goodformentertainment.canary.zown.api.IZownManager;

import net.canarymod.Canary;
import net.canarymod.api.world.DimensionType;
import net.canarymod.api.world.UnknownWorldException;
import net.canarymod.api.world.World;
import net.canarymod.api.world.WorldManager;
import net.canarymod.api.world.WorldType;
import net.canarymod.api.world.position.Location;
import net.canarymod.config.Configuration;
import net.canarymod.config.WorldConfiguration;
import net.visualillusionsent.utils.PropertiesFile;

public class SkylandsWorldManager {
    private static final DimensionType X_DIMENSION = DimensionType.NORMAL;
    private static final WorldType X_TYPE = WorldType.SUPERFLAT;

    private final SkylandsConfig config;
    private final WorldManager worldManager;
    private World world;

    private final IZownManager zownManager;

    public SkylandsWorldManager(final SkylandsConfig config, final IZownManager zownManager) {
        this.config = config;
        this.zownManager = zownManager;
        worldManager = Canary.getServer().getWorldManager();
    }

    public Location getDefaultSpawn() {
        return Canary.getServer().getDefaultWorld().getSpawnLocation();
    }

    public boolean createWorld() {
        boolean success = false;

        try {
            world = worldManager.getWorld(config.getWorldName(), true);
            success = true;
        } catch (final UnknownWorldException e) {
            SkylandsPlugin.LOG.debug("Creating Skylands world " + config.getWorldName());

            final WorldConfiguration worldConfig = Configuration
                    .getWorldConfig(config.getWorldName() + "_" + X_DIMENSION.getName());
            final PropertiesFile file = worldConfig.getFile();
            file.setInt("difficulty", World.Difficulty.NORMAL.getId());
            file.setBoolean("generate-structures", false);
            file.setBoolean("allow-nether", true);
            file.setBoolean("allow-end", false);
            file.setBoolean("spawn-villagers", true);
            file.setBoolean("spawn-golems", true);
            file.setBoolean("spawn-animals", true);
            file.setBoolean("spawn-monsters", true);
            file.setString("world-type", "FLAT");
            file.setString("generator-settings", "2;0;0;");
            // file.setString("generator-settings", "3;minecraft:air;0;");
            file.save();

            if (worldManager.createWorld(config.getWorldName(), 0, X_DIMENSION, X_TYPE)) {
                world = worldManager.getWorld(config.getWorldName(), true);
                success = true;
            }
        }
        return success;
    }

    public boolean load() {
        final IWorldStateManager worldStateManager = PlayerStatePlugin.getWorldManager();

        worldStateManager.registerWorld(world,
                new SaveState[] { SaveState.CONDITIONS, SaveState.INVENTORY, SaveState.LOCATIONS });

        // Configure Skylands world zown
        final IZown zown = zownManager.getZown(world).getData();
        final IConfiguration zownConfig = zown.getConfiguration();
        // if (!zownConfig.hasCommandRestriction("/spawn")
        // || !zownConfig.hasCommandRestriction("/sethome")
        // || !zownConfig.hasCommandRestriction("/home")) {
        // if (!zownConfig.hasCommandRestriction("/spawn")) {
        // zownConfig.addCommandRestriction("/spawn");
        zownConfig.addCommandRestriction("/kit");
        // zownConfig.addCommandRestriction("/sethome");
        // zownConfig.addCommandRestriction("/home");
        zownConfig.setFlag(Flag.build.name(), true);
        if (!zownManager.saveZownConfiguration(world, zown.getName())) {
            SkylandsPlugin.LOG.error("Error saving Skylands world zown");
        }
        // }

        return world != null;
    }

    public void unload() {
        worldManager.unloadWorld(config.getWorldName(), X_DIMENSION, true);
        final IWorldStateManager worldStateManager = PlayerStatePlugin.getWorldManager();
        worldStateManager.unregisterWorld(world);
    }

    public World getWorld() {
        return world;
    }
}
