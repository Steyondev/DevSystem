package de.steyon.devSystem.stacktrace;

import de.steyon.devSystem.DevSystem;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

public class FileManager {

    private final DevSystem devSystem;
    private final Player player;

    public FileManager(DevSystem devSystem, Player player){
        this.devSystem = devSystem;
        this.player = player;
    }


    public void createFile(String path, String fileName) {
        try {
            new File(path, fileName).createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteFolder(String path) {
        File folder = new File(path);
        if (folder.exists()) {
            if (folder.delete()) {
                player.sendMessage(devSystem.getConfigManager().getMessage("error", "folder-deleted", folder.getName()));
            } else {
                player.sendMessage(devSystem.getConfigManager().getMessage("error", "folder-delete-fail", folder.getName()));
            }
        } else {
            player.sendMessage(devSystem.getConfigManager().getMessage("error", "folder-not-exist", folder.getName()));
        }
    }

    public void deleteFile(String path, String fileName) {
        File file = new File(path, fileName);
        if (file.exists()) {
            if (file.delete()) {
                player.sendMessage(devSystem.getConfigManager().getMessage("error", "file-deleted", fileName));
            } else {
                player.sendMessage(devSystem.getConfigManager().getMessage("error", "file-delete-fail", fileName));
            }
        } else {
            player.sendMessage(devSystem.getConfigManager().getMessage("error", "file-not-exist", fileName));
        }
    }

    public void deleteFolder(String path, String folderName) {
        File folder = new File(path, folderName);
        if (folder.exists()) {
            if (folder.delete()) {
                player.sendMessage(devSystem.getConfigManager().getMessage("error", "folder-deleted", folderName));
            } else {
                player.sendMessage(devSystem.getConfigManager().getMessage("error", "folder-delete-fail", folderName));
            }
        } else {
            player.sendMessage(devSystem.getConfigManager().getMessage("error", "folder-not-exist", folderName));
        }
    }

    public void createFolder(String path) {
        new File(path).mkdirs();
    }

    public void createFolder(String path, String folderName) {
        File folder = new File(path, folderName);
        if (!folder.exists()) {
            if (folder.mkdirs()) {
                player.sendMessage(devSystem.getConfigManager().getMessage("error", "folder-created", folderName));
            } else {
                player.sendMessage(devSystem.getConfigManager().getMessage("error", "folder-create-fail", folderName));
            }
        } else {
            player.sendMessage(devSystem.getConfigManager().getMessage("error", "folder-exists", folderName));
        }
    }

}
