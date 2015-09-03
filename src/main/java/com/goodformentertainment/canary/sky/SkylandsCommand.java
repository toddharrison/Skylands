package com.goodformentertainment.canary.sky;

import java.util.Set;

import com.goodformentertainment.canary.sky.dao.SkylandsPlayer;

import net.canarymod.Canary;
import net.canarymod.api.entity.living.humanoid.Player;
import net.canarymod.api.world.position.Location;
import net.canarymod.chat.MessageReceiver;
import net.canarymod.commandsys.Command;
import net.canarymod.commandsys.CommandListener;
import net.canarymod.database.exceptions.DatabaseReadException;
import net.canarymod.database.exceptions.DatabaseWriteException;

public class SkylandsCommand implements CommandListener {
    private final SkylandsWorldManager worldManager;
    private final SkylandsPlayerManager playerManager;
    // private final XChallengeManager challengeManager;
    private final SkylandsIslandManager islandManager;
    // private final XScoreboard scoreboard;
    // private final IZownManager zownManager;

    // public SkylandsCommand(final SkylandsWorldManager worldManager,
    // final SkylandsPlayerManager playerManager,
    // /* final XChallengeManager challengeManager, */ final SkylandsIslandManager islandManager
    // /*, final XScoreboard scoreboard, final IZownManager zownManager*/) {
    public SkylandsCommand(final SkylandsWorldManager worldManager,
            final SkylandsPlayerManager playerManager, final SkylandsIslandManager islandManager) {
        this.worldManager = worldManager;
        this.playerManager = playerManager;
        // this.challengeManager = challengeManager;
        this.islandManager = islandManager;
        // this.scoreboard = scoreboard;
        // this.zownManager = zownManager;
    }

    @Command(aliases = { "sky" }, description = "Get help for Skylands", permissions = {
            "sky.command" }, toolTip = "/sky")
    public void help(final MessageReceiver caller, final String[] parameters) {
        if (caller instanceof Player) {
            final Player player = caller.asPlayer();
            player.message("Skylands");
            // player.message(
            // "Usage: /sky <(g)o | (c)hallenge | (e)xit | (l)istplayers | (t)opscores | practice |
            // restart>");
            player.message("Usage: /sky <(g)o | (e)xit | (l)istplayers | restart>");
        } else {
            // SkylandsPlugin.LOG.info(
            // "Usage: /sky <(g)o | (c)hallenge | (e)xit | (l)istplayers | (t)opscores | practice |
            // restart>");
            SkylandsPlugin.LOG.info("Usage: /sky <(g)o | (e)xit | (l)istplayers | restart>");
        }
    }

    @Command(aliases = { "go",
            "g" }, parent = "sky", description = "Go to Skylands", permissions = {
                    "sky.command.go" }, toolTip = "/sky (g)o")
    public void go(final MessageReceiver caller, final String[] parameters)
            throws DatabaseReadException, DatabaseWriteException {
        if (caller instanceof Player) {
            final Player player = caller.asPlayer();
            if (player.getWorld() != worldManager.getWorld()) {
                if (islandManager.isClearingIsland(player)) {
                    player.message("Your island is still deleting, please wait a moment...");
                } else {
                    player.teleportTo(playerManager.getIslandLocation(player));
                }
            } else {
                player.message("You are already in Skylands!");
            }
        }
    }

    // @Command(aliases = { "challenge",
    // "c" }, parent = "sky", description = "Compete a Skylands Challenge", permissions = {
    // "sky.command.challenge" }, toolTip = "/sky (c)hallenge")
    // public void challenge(final MessageReceiver caller, final String[] parameters) {
    // if (caller instanceof Player) {
    // final Player player = caller.asPlayer();
    // if (player.getWorld() == worldManager.getWorld()) {
    // challengeManager.openMenu(player);
    // } else {
    // player.message("You are not in Skylands!");
    // }
    // }
    // }

    @Command(aliases = { "exit",
            "e" }, parent = "sky", description = "Exit Skylands", permissions = {
                    "sky.command.exit" }, toolTip = "/sky (e)xit")
    public void exit(final MessageReceiver caller, final String[] parameters)
            throws DatabaseReadException, DatabaseWriteException {
        if (caller instanceof Player) {
            final Player player = (Player) caller;
            if (player.getWorld() == worldManager.getWorld()) {
                final SkylandsPlayer xPlayer = playerManager.getXPlayer(player);
                Location returnLocation = xPlayer.getReturnLocation();
                if (returnLocation == null) {
                    returnLocation = worldManager.getDefaultSpawn();
                }
                player.teleportTo(returnLocation);
            } else {
                player.message("You are not in Skylands!");
            }
        }
    }

    @Command(aliases = { "listplayers",
            "l" }, parent = "sky", description = "List the current Players in Skylands", permissions = {
                    "sky.command.list" }, toolTip = "/sky (l)istplayers")
    public void listPlayers(final MessageReceiver caller, final String[] parameters) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Current Skylands Players: ");

        final Set<String> uuids = playerManager.getActivePlayerIds();
        if (uuids == null || uuids.isEmpty()) {
            sb.append("none");
        } else {
            boolean added = false;
            for (final String uuid : uuids) {
                if (added) {
                    sb.append(", ");
                }
                final Player player = Canary.getServer().getPlayerFromUUID(uuid);
                sb.append(player.getName());
                added = true;
            }
        }

        if (caller instanceof Player) {
            final Player player = caller.asPlayer();
            player.message(sb.toString());
        } else {
            SkylandsPlugin.LOG.info(sb.toString());
        }
    }

    // @Command(aliases = { "topscores",
    // "t" }, parent = "sky", description = "List the top scores in Skylands", permissions = {
    // "sky.command.top" }, toolTip = "/sky (t)opscores")
    // public void topScores(final MessageReceiver caller, final String[] parameters) {
    // final StringBuilder sb = new StringBuilder();
    // sb.append("Skylands Top Scores:");
    //
    // final SortedSet<XHighScore> highScores = scoreboard.getHighScores();
    // if (highScores == null || highScores.isEmpty()) {
    // sb.append("\n none");
    // } else {
    // for (final XHighScore highScore : highScores) {
    // sb.append("\n ");
    // sb.append(highScore.highScore);
    // sb.append(" : ");
    // sb.append(highScore.playerName);
    // }
    // }
    //
    // if (caller instanceof Player) {
    // final Player player = caller.asPlayer();
    // player.message(sb.toString());
    // } else {
    // SkylandsPlugin.LOG.info(sb.toString());
    // }
    // }

    // @Command(aliases = {
    // "practice" }, parent = "sky", description = "Change an island to practice", permissions = {
    // "sky.command.practice" }, toolTip = "/sky practice")
    // public void practice(final MessageReceiver caller, final String[] parameters)
    // throws DatabaseWriteException {
    // if (caller instanceof Player) {
    // final Player player = caller.asPlayer();
    // if (player.getWorld() == worldManager.getWorld()) {
    // final Location playerLocation = player.getLocation();
    //
    // final SkylandsPlayer xPlayer = playerManager.getXPlayer(player);
    // if (!xPlayer.practice) {
    // xPlayer.practice = true;
    // xPlayer.setLocation(playerLocation);
    // playerManager.persist(xPlayer);
    //
    // player.setHome(playerLocation);
    // final IZown zown = zownManager.getZown(playerLocation).getData();
    // zown.getConfiguration().removeCommandRestriction("/home");
    // zown.getConfiguration().removeCommandRestriction("/sethome");
    // zownManager.saveZownConfiguration(playerLocation.getWorld(), zown.getName());
    //
    // player.message("Set Skylands to practice mode.");
    // player.message(
    // "/home is available. Score is frozen. Use /sky restart to clear.");
    // } else {
    // player.message("You are already in practice mode.");
    // }
    // } else {
    // player.message("You are not in Skylands!");
    // }
    // }
    // }

    @Command(aliases = {
            "restart" }, parent = "sky", description = "Restart your island", permissions = {
                    "sky.command.restart" }, toolTip = "/sky restart")
    public void restart(final MessageReceiver caller, final String[] parameters)
            throws DatabaseWriteException, DatabaseReadException {
        if (caller instanceof Player) {
            final Player player = caller.asPlayer();
            if (player.getWorld() == worldManager.getWorld()) {
                // final SkylandsPlayer xPlayer = playerManager.getXPlayer(player);
                // if (xPlayer.practice) {
                // xPlayer.practice = false;
                // xPlayer.setLocation(null);
                //
                // final Location playerLocation = player.getLocation();
                // final IZown zown = zownManager.getZown(playerLocation).getData();
                // zown.getConfiguration().addCommandRestriction("/home");
                // zown.getConfiguration().addCommandRestriction("/sethome");
                // zownManager.saveZownConfiguration(playerLocation.getWorld(), zown.getName());
                // }
                // playerManager.persist(xPlayer);
                //
                // player.kill();

                final SkylandsPlayer xPlayer = SkylandsPlayer.getXPlayer(player);
                Location returnLocation = xPlayer.getReturnLocation();
                if (returnLocation == null) {
                    returnLocation = worldManager.getDefaultSpawn();
                }
                player.teleportTo(returnLocation);

                xPlayer.setLocation(null);
                // challengeManager.resetMenu(player);
                // xPlayer.challengesCompleted.clear();
                xPlayer.update();
                islandManager.clearIsland(worldManager.getWorld(), player, xPlayer.islandId);

            } else {
                player.message("You are not in Skylands!");
            }
        }
    }
}
