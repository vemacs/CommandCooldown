package com.eatenalive3.commandcooldown.main;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandCooldown extends JavaPlugin {
    Map<String, HashMap<String, Long>> cooldowns = new HashMap<>();
    Map<String, ArrayList<Integer>> delays = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadCooldowns();
        loadConfig();
        getServer().getPluginManager().registerEvents(new CommandCooldown.CommandListener(), this);
    }

    @SuppressWarnings("unchecked")
    public void loadCooldowns() {
        try {
            cooldowns =
                    (HashMap<String, HashMap<String, Long>>)
                            (new ObjectInputStream(new FileInputStream(getDataFolder() + File.separator + "cooldowns.ser"))).readObject();
            (new ObjectInputStream(new FileInputStream(getDataFolder() + File.separator + "cooldowns.ser"))).close();
        } catch (Exception exception) {
            getLogger().warning("cooldowns.ser could not be read! All cooldowns are now reset.");
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void loadConfig() {
        try {
            List<String> e = YamlConfiguration.loadConfiguration(new File(getDataFolder() + File.separator + "config.yml")).getStringList("commands");

            for (String str : e) {
                int index = 0;
                int nums = 0;

                while (nums < str.length()) {
                    try {
                        Integer.parseInt(String.valueOf(str.charAt(nums)));
                        index = nums;
                        break;
                    } catch (Exception exception) {
                        ++nums;
                    }
                }
                if (index != 0) {
                    String cmd = str.substring(0, index).toLowerCase();

                    if (delays.get(cmd) != null) {
                        (delays.get(cmd)).add(Integer.parseInt(str.substring(index, str.length())));
                    } else {
                        ArrayList<Integer> arraylist = new ArrayList<>();

                        arraylist.add(Integer.parseInt(str.substring(index, str.length())));
                        delays.put(cmd, arraylist);
                    }
                }
            }
        } catch (Exception exception1) {
            getLogger().warning("Commands are invalid in config.yml! Could not load the values.");
        }
    }

    @Override
    public void onDisable() {
        try {
            ObjectOutputStream e = new ObjectOutputStream(new FileOutputStream(getDataFolder() + File.separator + "cooldowns.ser"));
            e.writeObject(cooldowns);
            e.close();
        } catch (Exception exception) {
            getLogger().warning("cooldowns could not be saved to cooldowns.ser!");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender.hasPermission("commandcooldown.reload") && label.equalsIgnoreCase("commandcooldown")) {
            loadConfig();
            sender.sendMessage(ChatColor.GREEN + "Command Cooldown configuration has been reloaded.");
        }
        return true;
    }

    class CommandListener implements Listener {
        @EventHandler
        public void onCommand(PlayerCommandPreprocessEvent e) {
            Player p = e.getPlayer();
            String message = e.getMessage().toLowerCase();

            if (cooldowns.containsKey(p.getName()) && !p.hasPermission("commandcooldown.override")) {
                HashMap<String, Long> commands = cooldowns.get(e.getPlayer().getName());
                String cmd = message.substring(1, message.contains(" ") ? message.indexOf(32) : message.length());

                if (delays.containsKey(cmd)) {
                    ArrayList<Integer> possible = delays.get(cmd);
                    Integer min = null;

                    for (Integer aPossible : possible) {
                        if (p.hasPermission("commandcooldown." + cmd + aPossible) && (min == null || aPossible < min)) {
                            min = aPossible;
                        }
                    }

                    if (min != null) {
                        if (commands.containsKey(cmd)) {
                            long i = (long) min - (System.currentTimeMillis() - commands.get(cmd)) / 1000L;

                            if (i > 0L) {
                                int seconds;

                                if (i - 3600L > 0L) {
                                    seconds = (int) (((double) i / 3600.0D - (double) (i / 3600L)) * 60.0D);
                                    p.sendMessage(ChatColor.RED + "You must wait " + i / 3600L + (i / 3600L == 1L ? " hour" : " hours")
                                            + " and " + seconds + (seconds == 1 ? " minute" : " minutes") + " until you may use that command again.");
                                } else if (i - 60L > 0L) {
                                    seconds = (int) (((double) i / 60.0D - (double) (i / 60L)) * 60.0D);
                                    p.sendMessage(ChatColor.RED + "You must wait " + i / 60L + (i / 60L == 1L ? " minute" : " minutes")
                                            + " and " + seconds + (seconds == 1 ? " second" : " seconds") + " until you may use that command again.");
                                } else {
                                    p.sendMessage(ChatColor.RED + "You must wait " + i + (i == 1L ? " second" : " seconds")
                                            + " until you may use that command again.");
                                }

                                e.setCancelled(true);
                            } else {
                                commands.put(cmd, System.currentTimeMillis());
                            }
                        } else {
                            commands.put(cmd, System.currentTimeMillis());
                        }
                    }
                }
            } else {
                cooldowns.put(e.getPlayer().getName(), new HashMap<String, Long>());
            }
        }
    }
}
