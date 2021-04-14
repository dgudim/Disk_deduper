package com.dgudi.disk;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import static com.dgudi.disk.Main.deletedMessage;

public class FileUtils {

    final static String saveDirectory = "DiskScanPreferences\\";
    final static FileHandle paramsFile = Gdx.files.external(saveDirectory + "params.txt");
    final static long moveThreadDelay = 50;

    public static ArrayList<File> getAllFiles(String path) {
        File root = new File(path);
        File[] list = root.listFiles();
        ArrayList<File> foundFiles = new ArrayList<>();

        if (list == null) return new ArrayList<>();

        for (final File f : list) {
            if (f.isDirectory()) {
                foundFiles.addAll(getAllFiles(f.getAbsolutePath()));
            } else {
                Main.currentFile = f.toString();
                foundFiles.add(f.getAbsoluteFile());
            }
        }

        return foundFiles;
    }

    public static ArrayList<File> getAllDirectories(String path) {
        File root = new File(path);
        File[] list = root.listFiles();
        ArrayList<File> foundDirectories = new ArrayList<>();

        if (list == null) return new ArrayList<>();

        for (final File f : list) {
            if (f.isDirectory()) {
                foundDirectories.add(f);
                foundDirectories.addAll(getAllFiles(f.getAbsolutePath()));
            }
        }

        return foundDirectories;
    }

    public static String moveAccordingToFolderStructure(File getFrom, String clonePath) {
        String fullPath = clonePath + "\\" + (getFrom.getAbsolutePath().substring(3));
        int lastDirectoryIndex = fullPath.lastIndexOf("\\");
        String path = fullPath.substring(0, lastDirectoryIndex);
        boolean success = true;
        if (!new File(path).exists()) {
            success = new File(path).mkdirs();
        }
        success = success && getFrom.renameTo(new File(fullPath));
        sleep();
        if (success) {
            return fullPath;
        } else {
            Main.currentErrorMessage = "[#FF0000]Error moving to " + fullPath;
            return "Error moving to " + fullPath;
        }
    }

    static void sleep() {
        try {
            Thread.sleep(moveThreadDelay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String getFilePathWithoutName(String fullFilePath) {
        return fullFilePath.substring(0, fullFilePath.lastIndexOf(File.separator)).trim();
    }

    static String markAsDeleted(File fileToDelete) {
        String fullPath = fileToDelete.getAbsolutePath();
        String name = fileToDelete.getName();
        String pathWithoutName = getFilePathWithoutName(fullPath);
        String newName = pathWithoutName + "\\" + deletedMessage + name;
        boolean success = fileToDelete.renameTo(new File(newName));
        sleep();
        if (success) {
            return newName;
        } else {
            Main.currentErrorMessage = "[#FF0000]Error renaming to " + fullPath;
            return "Error renaming";
        }
    }

    public static Object loadObject(String path) throws IOException, ClassNotFoundException {
        FileInputStream f = new FileInputStream(Gdx.files.external(saveDirectory + path).file());
        ObjectInputStream s = new ObjectInputStream(f);
        Object object = s.readObject();
        s.close();
        return object;
    }

    public static void saveObject(String path, Object object) throws IOException {
        FileOutputStream f = new FileOutputStream(Gdx.files.external(saveDirectory + path).file());
        ObjectOutputStream s = new ObjectOutputStream(f);
        s.writeObject(object);
        s.close();
    }

}
