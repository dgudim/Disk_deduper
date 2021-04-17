package com.dgudi.disk;

import com.badlogic.gdx.scenes.scene2d.Touchable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.HashMap;

import static com.dgudi.disk.FileUtils.loadObject;
import static com.dgudi.disk.FileUtils.saveObject;
import static com.dgudi.disk.Main.changeUiTouchableState;

public class HashParser {

    static HashMap<String, String> hashBase_fileHashes = new HashMap<>();
    static HashMap<String, Long> hashBase_fileSizes = new HashMap<>();

    public static int currentCommits = 0;
    static final int commitThreshold = 3000;

    public static String getFileChecksum(File file) {
        long fileSize = file.length();
        String filePath = file.getAbsolutePath();
        String hashFromHashBase = getHashFromHashBase(filePath, fileSize);
        Main.currentFile = file.toString();
        if (!hashFromHashBase.equals("NO_HASH")) {
            Main.currentHash = "[#00DDFF]" + hashFromHashBase;
            return hashFromHashBase;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            FileInputStream fis = new FileInputStream(file);

            byte[] byteArray = new byte[1024];
            int bytesCount;

            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }

            fis.close();

            byte[] bytes = digest.digest();

            //Convert it to hexadecimal format
            StringBuilder sb = new StringBuilder();
            for (byte aByte : bytes) {
                sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
            }
            Main.currentHash = sb.toString();
            commitHashToHashBase(sb.toString(), filePath, fileSize);
            return sb.toString();
        } catch (Exception e) {
            Main.currentErrorMessage = "[#FF0000]Error calculating hash for " + file + ", " + e.getMessage();
            return "";
        }
    }

    @SuppressWarnings("unchecked")
    static void readHashBase() {
        Main.currentFile = "Reading hash database";
        changeUiTouchableState(Touchable.disabled);
        new Thread(new Runnable() {
            public void run() {

                try {
                    Main.currentHash = "hash base file hashes";
                    hashBase_fileHashes = (HashMap<String, String>) loadObject("hashBase_fileHashes");
                    Main.currentHash = "hash base file sizes: " + hashBase_fileHashes.size();
                    hashBase_fileSizes = (HashMap<String, Long>) loadObject("hashBase_fileSizes");

                    if (!(hashBase_fileSizes.size() == hashBase_fileHashes.size())) {
                        Main.currentErrorMessage = "[#FF0000]Error with the hash base, hashMap size mismatch";
                        hashBase_fileHashes.clear();
                        hashBase_fileSizes.clear();
                        saveHashBase();
                    }
                } catch (Exception e) {
                    Main.currentErrorMessage = "[#FF0000]Error loading hash base " + e + " " + e.getMessage();
                }
                Main.currentFile = "";
                Main.currentHash = "";
                Main.currentErrorMessage = "[#00DDFF]Info: " + hashBase_fileSizes.size() + " " + hashBase_fileHashes.size();
                changeUiTouchableState(Touchable.enabled);
            }
        }).start();
    }

    static void commitHashToHashBase(String hash, String filePath, long fileSize) {
        if (getHashFromHashBase(filePath, fileSize).equals("NO_HASH")) {
            hashBase_fileHashes.put(filePath, hash);
            hashBase_fileSizes.put(filePath, fileSize);
            Main.currentErrorMessage = "[#00DDFF]Info: current commits: " + currentCommits;
            currentCommits++;
            if (currentCommits > commitThreshold) {
                saveHashBase();
            }
        }
    }

    static void saveHashBase() {
        try {
            currentCommits = 0;
            saveObject("hashBase_fileHashes", hashBase_fileHashes);
            saveObject("hashBase_fileSizes", hashBase_fileSizes);
            saveObject("hashBase_fileHashes_backup", hashBase_fileHashes);
            saveObject("hashBase_fileSizes_backup", hashBase_fileSizes);
            Main.currentErrorMessage = "[#00DDFF]Info: saved hash base";
        } catch (IOException e) {
            Main.currentErrorMessage = "[#FF0000]Error saving hash base " + e.getMessage();
        }
    }

    static String getHashFromHashBase(String filePath, long fileSize) {
        if (hashBase_fileSizes.containsKey(filePath)) {
            if (hashBase_fileSizes.get(filePath) == fileSize) {
                return hashBase_fileHashes.get(filePath);
            } else {
                Main.currentErrorMessage = "[#00DDFF]Info: size changed for " + filePath + "(Was: " + hashBase_fileSizes.get(filePath) + ", now: " + fileSize + ")";
                hashBase_fileHashes.remove(filePath);
                hashBase_fileSizes.remove(filePath);
            }
        }
        return "NO_HASH";
    }

}
