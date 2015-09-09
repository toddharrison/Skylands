package com.goodformentertainment.canary.sky;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.goodformentertainment.canary.sky.dao.SkylandsChallenge;
import com.goodformentertainment.canary.sky.dao.SkylandsChallengeLevel;
import com.goodformentertainment.canary.sky.dao.SkylandsPlayer;
import com.goodformentertainment.canary.sky.util.InventoryUtil;
import com.goodformentertainment.canary.sky.util.menu.Menu;
import com.goodformentertainment.canary.sky.util.menu.MenuConfiguration;
import com.goodformentertainment.canary.sky.util.menu.MenuFactory;
import com.goodformentertainment.canary.sky.util.menu.MenuItem;
import com.goodformentertainment.canary.sky.util.menu.hook.MenuItemSelectHook;

import net.canarymod.api.entity.living.humanoid.Player;
import net.canarymod.api.inventory.Inventory;
import net.canarymod.api.inventory.ItemType;
import net.canarymod.api.world.World;
import net.canarymod.api.world.blocks.Block;
import net.canarymod.api.world.position.Location;
import net.canarymod.chat.ChatFormat;
import net.canarymod.database.exceptions.DatabaseReadException;
import net.canarymod.database.exceptions.DatabaseWriteException;
import net.canarymod.hook.HookHandler;
import net.canarymod.plugin.PluginListener;

public class SkylandsChallengeManager implements PluginListener {
    private static final int ITEM_DESCRIPTION_WORD_WRAP = 40;

    // private final XScoreboard scoreboard;
    private final MenuFactory menuFactory;
    private SkylandsPlayerManager playerManager;

    // public XChallengeManager(final SkylandsPlugin plugin, final XScoreboard scoreboard)
    // throws DatabaseReadException {
    public SkylandsChallengeManager(final SkylandsPlugin plugin) throws DatabaseReadException {
        final List<MenuItem> menuItems = new ArrayList<MenuItem>();
        // this.scoreboard = scoreboard;

        final List<SkylandsChallengeLevel> levels = SkylandsChallengeLevel.getAllXChallengeLevels();
        for (int row = 0; row < levels.size(); row++) {
            final List<SkylandsChallenge> challenges = SkylandsChallenge
                    .getXChallengesForLevel(levels.get(row).name);
            for (int column = 0; column < challenges.size(); column++) {
                final SkylandsChallenge challenge = challenges.get(column);
                menuItems.add(new MenuItem(challenge.name, createDescription(challenge),
                        ItemType.GreenGlassPane, row * 9 + column, false));
            }
        }

        final MenuConfiguration menuConfig = new MenuConfiguration() {
            @Override
            public void configure(final MenuItem[] menuItems) {
                throw new UnsupportedOperationException("Skylands menus are specific to players.");
            }

            @Override
            public void configure(final MenuItem[] menuItems, final Player player) {
                final SkylandsPlayer xPlayer = playerManager.getXPlayer(player);
                for (final MenuItem menuItem : menuItems) {
                    try {
                        final String challengeName = menuItem.getName();
                        final SkylandsChallenge challenge = SkylandsChallenge
                                .getXChallenge(challengeName);
                        if (xPlayer.challengesCompleted.contains(challengeName)) {
                            if (challenge.repeatable) {
                                menuItem.setIcon(ItemType.LightBlueGlassPane);
                                menuItem.setDescription(createRepeatDescription(challenge));
                            } else {
                                menuItem.setIcon(ItemType.LightGrayGlassPane);
                                menuItem.setDisabled(true);
                                menuItem.setDescription(null);
                            }
                        }
                    } catch (final Exception e) {
                        SkylandsPlugin.LOG.error(
                                "Error configuring menu items for player " + player.getName(), e);
                    }
                }
            }
        };

        SkylandsPlugin.LOG.info("Loaded " + menuItems.size() + " challenge items.");

        menuFactory = new MenuFactory(plugin, "Skylands Challenges", 6, menuConfig,
                menuItems.toArray(new MenuItem[menuItems.size()]));
    }

    public void openMenu(final Player player) {
        menuFactory.getMenu(player).display(player);
    }

    public void resetMenu(final Player player) {
        menuFactory.removeMenu(player);
    }

    public boolean completeChallenge(final Player player, final String name)
            throws DatabaseReadException, DatabaseWriteException {
        boolean success = false;

        final SkylandsChallenge xChallenge = SkylandsChallenge.getXChallenge(name);
        if (xChallenge != null) {
            final Map<ItemType, Integer> requiredItems = xChallenge.getItemsRequired();
            final Inventory inv = player.getInventory();
            // final int minPlayerScore = xChallenge.minPlayerScore;

            if ("onPlayer".equals(xChallenge.type)) {
                final SkylandsPlayer xPlayer = playerManager.getXPlayer(player);

                // Verify the player has a high enough score
                // if (xPlayer.practice || minPlayerScore < 0
                // || scoreboard.getScore(player) >= minPlayerScore) {
                // Verify the player has the needed items
                final boolean allItemsPresent = requiredItems.size() == 0
                        || InventoryUtil.hasItems(inv, requiredItems);

                if (allItemsPresent) {
                    if (requiredItems.size() > 0 && xChallenge.consumeItems) {
                        InventoryUtil.removeItems(inv, requiredItems);
                    }

                    // Give player reward
                    if (xPlayer.challengesCompleted.contains(name)) {
                        // Repeat
                        for (final ItemType itemType : xChallenge.getItemsRepeatReward().keySet()) {
                            final int count = xChallenge.getItemsRepeatReward().get(itemType);
                            for (int i = 0; i < count; i++) {
                                inv.addItem(itemType);
                            }
                        }
                    } else {
                        // First time
                        for (final ItemType itemType : xChallenge.getItemsReward().keySet()) {
                            final int count = xChallenge.getItemsReward().get(itemType);
                            for (int i = 0; i < count; i++) {
                                inv.addItem(itemType);
                            }
                        }
                    }

                    success = true;
                }
                // } else {
                // player.message("You need to have a score greater than " + minPlayerScore
                // + " to complete this challenge!");
                // }
            } else if ("onIsland".equals(xChallenge.type)) {
                SkylandsPlugin.LOG.debug(player.getName() + " onIsland check");
                // // Verify the player has a high enough score
                // if (minPlayerScore < 0 || scoreboard.getScore(player) >= minPlayerScore) {
                final int range = 10;

                final Location loc = player.getLocation();
                final World world = loc.getWorld();
                final int xMin = loc.getBlockX() - range;
                final int yMin = Math.max(0, loc.getBlockY() - range);
                final int zMin = loc.getBlockZ() - range;
                final int xMax = loc.getBlockX() + range;
                final int yMax = Math.min(255, loc.getBlockY() + range);
                final int zMax = loc.getBlockZ() + range;

                for (int x = xMin; x < xMax; x++) {
                    for (int y = yMin; y < yMax; y++) {
                        for (int z = zMin; z < zMax; z++) {
                            final Block block = world.getBlockAt(x, y, z);
                            if (!block.isAir()) {
                                final ItemType itemType = ItemType
                                        .fromString(block.getType().getMachineName());
                                if (requiredItems.containsKey(itemType)) {
                                    int neededCount = requiredItems.get(itemType);
                                    if (--neededCount <= 0) {
                                        requiredItems.remove(itemType);
                                    } else {
                                        requiredItems.put(itemType, neededCount);
                                    }
                                }
                            }
                        }
                    }
                }

                if (requiredItems.isEmpty()) {
                    // Give player reward
                    for (final ItemType itemType : xChallenge.getItemsReward().keySet()) {
                        final int count = xChallenge.getItemsReward().get(itemType);
                        for (int i = 0; i < count; i++) {
                            inv.addItem(itemType);
                        }
                    }

                    success = true;
                }
                // } else {
                // player.message("You need to have a score greater than " + minPlayerScore
                // + " to complete this challenge!");
                // }
            } else {
                throw new IllegalStateException("Unrecognized xChallenge type: " + xChallenge.type);
            }
        }

        if (success) {
            final SkylandsPlayer xPlayer = playerManager.getXPlayer(player);

            // if (!xPlayer.practice) {
            // Add value to the scoreboard for the value of the challenge
            if (xPlayer.challengesCompleted.contains(name)) {
                // // Repeat reward
                // scoreboard.addToScore(player, xChallenge.scoreRepeatReward);
            } else {
                // // First time reward
                // scoreboard.addToScore(player, xChallenge.scoreReward);
                player.getWorld().broadcastMessage(player.getName() + " has completed " + name);
                SkylandsPlugin.LOG.debug(player.getName() + " completed " + name);
            }
            // }

            // Record that the player completed this challenge
            if (!xPlayer.challengesCompleted.contains(name)) {
                xPlayer.challengesCompleted.add(name);
                xPlayer.update();
            }
        }

        return success;
    }

    @HookHandler
    public void onMenuSelect(final MenuItemSelectHook hook)
            throws DatabaseReadException, DatabaseWriteException {
        final Menu menu = hook.getMenu();
        if (menu.getMenuFactory() == menuFactory) {
            final Player player = hook.getPlayer();
            final MenuItem menuItem = hook.getMenuItem();
            if (completeChallenge(player, menuItem.getName())) {
                menuItem.update();
            }
        }
    }

    protected void setPlayerManager(final SkylandsPlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    private String[] createDescription(final SkylandsChallenge challenge) {
        final List<String> description = new LinkedList<String>();
        description.addAll(splitString(challenge.description, ChatFormat.GREEN));
        description.addAll(splitString(challenge.rewardDescription, ChatFormat.DARK_GREEN));
        return description.toArray(new String[description.size()]);
    }

    private String[] createRepeatDescription(final SkylandsChallenge challenge) {
        final List<String> description = new LinkedList<String>();
        description.addAll(splitString(challenge.description, ChatFormat.GREEN));
        description.addAll(splitString(challenge.repeatRewardDescription, ChatFormat.DARK_GREEN));
        return description.toArray(new String[description.size()]);
    }

    private List<String> splitString(final String s, final ChatFormat prefix) {
        final List<String> list = new LinkedList<String>();
        int i = 0;
        while (i < s.length()) {
            if (i + ITEM_DESCRIPTION_WORD_WRAP >= s.length()) {
                if (prefix != null) {
                    list.add(prefix + s.substring(i));
                } else {
                    list.add(s.substring(i));
                }
                i = s.length();
            } else {
                final int j = s.lastIndexOf(" ", i + ITEM_DESCRIPTION_WORD_WRAP) + 1;
                if (prefix != null) {
                    list.add(prefix + s.substring(i, j));
                } else {
                    list.add(s.substring(i, j));
                }
                i = j;
            }
        }
        return list;
    }
}
